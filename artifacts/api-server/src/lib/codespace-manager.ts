import { schedule, type ScheduledTask } from "node-cron";
import { logger } from "./logger.js";

// ─── Account config ────────────────────────────────────────────────────────
interface CodespaceAccount {
  label: string;
  token: string;
  codespaceId: string;
  login: string;
  repo: string; // GitHub repo: "login/rem5"
}

export const ACCOUNTS: CodespaceAccount[] = [
  {
    label: "Primary (ksjxjcmxncj-maker)",
    token: process.env["GITHUB_PERSONAZL_ACCESS_TOKEN"] ?? "",
    codespaceId: "improved-fishstick-966vx76qqgx7cqjp",
    login: "ksjxjcmxncj-maker",
    repo: "ksjxjcmxncj-maker/rem5",
  },
  {
    label: "Backup-1 (kgxxyixgikcgxittixxi-collab)",
    token: process.env["GITHUB_PERSONALA_ACCESS_TOKEN"] ?? "",
    codespaceId: "crispy-space-capybara-5v564w74jqgf45x4",
    login: "kgxxyixgikcgxittixxi-collab",
    repo: "kgxxyixgikcgxittixxi-collab/rem5",
  },
  {
    label: "Backup-2 (idkyoohdtsu-netizen)",
    token: process.env["GITHUB_PERSONACL_ACCESS_TOKEN"] ?? "",
    codespaceId: "cuddly-space-orbit-qvvrx7jq5gv6246wg",
    login: "idkyoohdtsu-netizen",
    repo: "idkyoohdtsu-netizen/rem5",
  },
];

// ─── State ─────────────────────────────────────────────────────────────────
export interface ManagerStatus {
  running: boolean;
  activeIndex: number | null;
  activeLabel: string | null;
  activeCodespaceId: string | null;
  activeWebUrl: string | null;
  maintenanceIndices: number[];    // tài khoản đang bảo trì
  lastCheck: string | null;
  lastError: string | null;
  lastSync: string | null;
  nextStart: string;
  nextStop: string;
}

let state: ManagerStatus = {
  running: false,
  activeIndex: null,
  activeLabel: null,
  activeCodespaceId: null,
  activeWebUrl: null,
  maintenanceIndices: [],
  lastCheck: null,
  lastError: null,
  lastSync: null,
  nextStart: "04:00 ICT",
  nextStop: "23:50 ICT",
};

const tasks: ScheduledTask[] = [];

// ─── GitHub API helpers ────────────────────────────────────────────────────
async function ghFetch(
  token: string,
  path: string,
  method = "GET",
  body?: unknown,
): Promise<{ ok: boolean; status: number; body: unknown }> {
  const res = await fetch(`https://api.github.com${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${token}`,
      "User-Agent": "codespace-manager",
      Accept: "application/vnd.github+json",
      ...(body ? { "Content-Type": "application/json" } : {}),
    },
    ...(body ? { body: JSON.stringify(body) } : {}),
  });
  let resBody: unknown;
  try { resBody = await res.json(); } catch { resBody = null; }
  return { ok: res.ok, status: res.status, body: resBody };
}

async function getCodespaceState(
  acc: CodespaceAccount,
): Promise<{ state: string; webUrl: string } | null> {
  const r = await ghFetch(acc.token, `/user/codespaces/${acc.codespaceId}`);
  if (!r.ok) return null;
  const b = r.body as Record<string, unknown>;
  return { state: String(b["state"] ?? "Unknown"), webUrl: String(b["web_url"] ?? "") };
}

async function startCodespace(acc: CodespaceAccount): Promise<string | null> {
  const r = await ghFetch(acc.token, `/user/codespaces/${acc.codespaceId}/start`, "POST");
  if (!r.ok) {
    const msg = (r.body as Record<string, unknown>)?.["message"] ?? `HTTP ${r.status}`;
    return String(msg);
  }
  return null;
}

async function stopCodespace(acc: CodespaceAccount): Promise<void> {
  await ghFetch(acc.token, `/user/codespaces/${acc.codespaceId}/stop`, "POST");
}

function sleep(ms: number) { return new Promise<void>((r) => setTimeout(r, ms)); }

// ─── Repo sync helpers ─────────────────────────────────────────────────────

/** Lấy nội dung + SHA của 1 file từ 1 repo */
async function getRepoFile(
  token: string,
  repo: string,
  filePath: string,
): Promise<{ content: string; sha: string } | null> {
  const r = await ghFetch(token, `/repos/${repo}/contents/${filePath}`);
  if (!r.ok) return null;
  const b = r.body as Record<string, unknown>;
  if (!b["content"]) return null;
  return {
    content: String(b["content"]).replace(/\n/g, ""),
    sha: String(b["sha"] ?? ""),
  };
}

/** Push 1 file lên repo (tạo mới hoặc update) */
async function putRepoFile(
  token: string,
  repo: string,
  filePath: string,
  content: string,      // base64
  sha: string | null,
  message: string,
): Promise<{ ok: boolean; error?: string }> {
  const body: Record<string, unknown> = { message, content };
  if (sha) body["sha"] = sha;
  const r = await ghFetch(token, `/repos/${repo}/contents/${filePath}`, "PUT", body);
  if (!r.ok) {
    const msg = (r.body as Record<string, unknown>)?.["message"] ?? `HTTP ${r.status}`;
    return { ok: false, error: String(msg) };
  }
  return { ok: true };
}

// ─── Sync logic ────────────────────────────────────────────────────────────

/** Các file cần đồng bộ giữa các repo */
const SYNC_PATHS = [
  "scripts/codespace_autostart.sh",
  "scripts/ws_bridge_server.py",
  ".devcontainer/devcontainer.json",
  ".devcontainer/setup.sh",
];

export interface SyncResult {
  file: string;
  target: string;
  ok: boolean;
  error?: string;
}

/**
 * Đồng bộ file từ sourceIndex sang tất cả repo khác (bỏ qua bản đang bảo trì nếu muốn).
 * Nếu không truyền filePaths thì dùng SYNC_PATHS mặc định.
 */
export async function syncRepos(
  sourceIndex = 0,
  filePaths: string[] = SYNC_PATHS,
): Promise<SyncResult[]> {
  const src = ACCOUNTS[sourceIndex];
  if (!src) return [];

  const targets = ACCOUNTS.filter((_, i) => i !== sourceIndex);
  const results: SyncResult[] = [];

  for (const filePath of filePaths) {
    // Đọc file từ source
    const srcFile = await getRepoFile(src.token, src.repo, filePath);
    if (!srcFile) {
      logger.warn({ repo: src.repo, filePath }, "Sync: file not found on source — skip");
      for (const t of targets) {
        results.push({ file: filePath, target: t.repo, ok: false, error: "not found on source" });
      }
      continue;
    }

    // Push sang từng target
    for (const t of targets) {
      const existing = await getRepoFile(t.token, t.repo, filePath);
      const res = await putRepoFile(
        t.token,
        t.repo,
        filePath,
        srcFile.content,
        existing?.sha ?? null,
        `sync: ${filePath} from ${src.repo}`,
      );
      results.push({ file: filePath, target: t.repo, ...res });
      if (res.ok) {
        logger.info({ file: filePath, target: t.repo }, "Sync: pushed OK");
      } else {
        logger.warn({ file: filePath, target: t.repo, err: res.error }, "Sync: push failed");
      }
    }
  }

  state.lastSync = new Date().toISOString();
  return results;
}

// ─── Core manager logic ────────────────────────────────────────────────────

/** Chọn codespace tốt nhất (bỏ qua bản đang bảo trì). */
async function activateBestAccount(): Promise<void> {
  for (let i = 0; i < ACCOUNTS.length; i++) {
    if (state.maintenanceIndices.includes(i)) {
      logger.info({ label: ACCOUNTS[i]!.label }, "Under maintenance — skip");
      continue;
    }
    const acc = ACCOUNTS[i]!;
    if (!acc.token) { logger.warn({ label: acc.label }, "Token missing — skip"); continue; }

    logger.info({ label: acc.label }, "Trying to start codespace");
    const cs = await getCodespaceState(acc);
    if (!cs) {
      logger.warn({ label: acc.label }, "Unreachable — skip");
      state.lastError = `${acc.label}: unreachable`;
      continue;
    }

    const s = cs.state.toLowerCase();
    if (s === "available" || s === "starting" || s === "running") {
      state.activeIndex = i;
      state.activeLabel = acc.label;
      state.activeCodespaceId = acc.codespaceId;
      state.activeWebUrl = cs.webUrl;
      state.lastError = null;
      logger.info({ label: acc.label, csState: cs.state }, "Already active");
      return;
    }

    const err = await startCodespace(acc);
    if (err) {
      logger.warn({ label: acc.label, err }, "Start failed — trying next");
      state.lastError = `${acc.label}: ${err}`;
      continue;
    }

    for (let t = 0; t < 12; t++) {
      await sleep(5000);
      const cs2 = await getCodespaceState(acc);
      if (!cs2) break;
      const s2 = cs2.state.toLowerCase();
      if (s2 === "available" || s2 === "running") {
        state.activeIndex = i;
        state.activeLabel = acc.label;
        state.activeCodespaceId = acc.codespaceId;
        state.activeWebUrl = cs2.webUrl;
        state.lastError = null;
        logger.info({ label: acc.label }, "Started successfully");
        return;
      }
    }

    logger.warn({ label: acc.label }, "Timeout — trying next");
    state.lastError = `${acc.label}: start timed out`;
  }

  logger.error("All codespaces failed to start");
}

async function healthCheck(): Promise<void> {
  state.lastCheck = new Date().toISOString();
  if (state.activeIndex === null) return;

  const acc = ACCOUNTS[state.activeIndex]!;
  const cs = await getCodespaceState(acc);

  if (!cs) {
    logger.warn({ label: acc.label }, "Health check: unreachable — failover");
    state.activeIndex = null;
    state.lastError = `${acc.label}: unreachable`;
    await activateBestAccount();
    return;
  }

  const s = cs.state.toLowerCase();
  if (!(s === "available" || s === "running" || s === "starting")) {
    logger.warn({ label: acc.label, csState: cs.state }, "Health check: unhealthy — failover");
    state.activeIndex = null;
    state.lastError = `${acc.label}: state=${cs.state}`;
    await activateBestAccount();
  } else {
    logger.info({ label: acc.label, csState: cs.state }, "Health check: OK");
  }
}

async function stopActive(): Promise<void> {
  if (state.activeIndex === null) return;
  const acc = ACCOUNTS[state.activeIndex]!;
  logger.info({ label: acc.label }, "Stopping (end of schedule)");
  await stopCodespace(acc);
  state.running = false;
  state.activeIndex = null;
  state.activeLabel = null;
  state.activeCodespaceId = null;
  state.activeWebUrl = null;
}

// ─── Maintenance API ───────────────────────────────────────────────────────

/**
 * Đánh dấu 1 tài khoản vào bảo trì.
 * - Stop codespace của nó (nếu đang chạy).
 * - Tự động bật bản còn lại.
 * - Đồng bộ file sang các bản còn lại.
 */
export async function enterMaintenance(
  index: number,
  syncFiles?: string[],
): Promise<{ started: string | null; syncResults: SyncResult[] }> {
  const acc = ACCOUNTS[index];
  if (!acc) throw new Error(`Invalid account index: ${index}`);

  if (!state.maintenanceIndices.includes(index)) {
    state.maintenanceIndices.push(index);
  }

  // Nếu bản này đang active thì chuyển sang bản khác
  if (state.activeIndex === index) {
    await stopCodespace(acc);
    state.activeIndex = null;
    state.activeLabel = null;
    state.activeCodespaceId = null;
    state.activeWebUrl = null;
    logger.info({ label: acc.label }, "Maintenance: stopped active codespace");
  }

  // Bật bản còn lại nếu đang trong giờ hoạt động
  if (state.running) {
    await activateBestAccount();
  }

  // Đồng bộ file từ bản bảo trì sang các bản còn lại
  logger.info({ label: acc.label }, "Maintenance: syncing files to remaining accounts");
  const syncResults = await syncRepos(index, syncFiles ?? SYNC_PATHS);

  return {
    started: state.activeLabel,
    syncResults,
  };
}

/**
 * Kết thúc bảo trì cho 1 tài khoản.
 * - Đồng bộ file mới nhất từ bản đang chạy về bản vừa bảo trì xong.
 * - Đưa tài khoản vào danh sách ứng viên lại (ưu tiên theo index).
 */
export async function exitMaintenance(
  index: number,
): Promise<{ syncResults: SyncResult[] }> {
  const acc = ACCOUNTS[index];
  if (!acc) throw new Error(`Invalid account index: ${index}`);

  // Đồng bộ từ bản hiện tại về bản vừa bảo trì xong
  const sourceIndex = state.activeIndex ?? 0;
  const syncResults = await syncRepos(sourceIndex, SYNC_PATHS);

  // Bỏ khỏi danh sách bảo trì
  state.maintenanceIndices = state.maintenanceIndices.filter((i) => i !== index);
  logger.info({ label: acc.label }, "Maintenance: ended, account back in rotation");

  return { syncResults };
}

// ─── Public API ────────────────────────────────────────────────────────────

export function startManager(): void {
  tasks.push(
    schedule("0 4 * * *", async () => {
      logger.info("Scheduler: 04:00 — starting");
      state.running = true;
      await activateBestAccount();
    }, { timezone: "Asia/Ho_Chi_Minh" }),
  );

  tasks.push(
    schedule("50 23 * * *", async () => {
      logger.info("Scheduler: 23:50 — stopping");
      await stopActive();
    }, { timezone: "Asia/Ho_Chi_Minh" }),
  );

  tasks.push(
    schedule("*/5 * * * *", async () => {
      if (state.running) await healthCheck();
    }),
  );

  logger.info("Codespace manager started — 04:00–23:50 ICT, health check 5 min, sync enabled");
}

export function getStatus(): ManagerStatus { return { ...state }; }
export function getAccounts() { return ACCOUNTS.map((a, i) => ({ index: i, label: a.label, login: a.login, codespaceId: a.codespaceId })); }
export async function manualStart(): Promise<void> { state.running = true; await activateBestAccount(); }
export async function manualStop(): Promise<void> { await stopActive(); }
