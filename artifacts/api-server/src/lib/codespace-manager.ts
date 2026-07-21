import { schedule, type ScheduledTask } from "node-cron";
import { logger } from "./logger.js";
import { setCurrentWsUrl } from "../routes/wsUrl.js";

// ─── Account config ────────────────────────────────────────────────────────
interface CodespaceAccount {
  label: string;
  token: string;          // được điền tự động bởi matchTokensToAccounts()
  codespaceId: string;
  login: string;
  repo: string;
}

// Pool tất cả tên env var chứa token — thứ tự không quan trọng,
// hệ thống tự tìm token nào thuộc tài khoản nào khi khởi động.
const TOKEN_ENV_VARS = [
  "GITHUB_PERSONHHAL_ACCESS_TOKEN",
  "GITHUB_PERGSONJAL_ACCESS_TOKEN",
  "GITHUGB_PERSONJAJL_ACCESS_TOKEN",
  "GITHUB_PERSONAJL_ACCESS_TOKEN",
  "J",
];

export const ACCOUNTS: CodespaceAccount[] = [
  {
    label: "Primary (ksjxjcmxncj-maker)",
    token: "",
    codespaceId: "improved-fishstick-966vx76qqgx7cqjp",
    login: "ksjxjcmxncj-maker",
    repo: "ksjxjcmxncj-maker/rem5",
  },
  {
    label: "Backup-1 (kgxxyixgikcgxittixxi-collab)",
    token: "",
    codespaceId: "crispy-space-capybara-5v564w74jqgf45x4",
    login: "kgxxyixgikcgxittixxi-collab",
    repo: "kgxxyixgikcgxittixxi-collab/rem5",
  },
  {
    label: "Backup-2 (idkyoohdtsu-netizen)",
    token: "",
    codespaceId: "cuddly-space-orbit-qvvrx7jq5gv6246wg",
    login: "idkyoohdtsu-netizen",
    repo: "idkyoohdtsu-netizen/rem5",
  },
  {
    label: "Backup-3 (kdjfjcjks)",
    token: "",
    codespaceId: "glowing-yodel-jr6p46ppvgg3wwp",
    login: "kdjfjcjks",
    repo: "kdjfjcjks/rem5",
  },
];

// ─── Token auto-detection ──────────────────────────────────────────────────

/**
 * Đọc tất cả token từ env vars, gọi GitHub /user để xác định login,
 * rồi gán đúng token vào đúng tài khoản trong ACCOUNTS.
 * Không cần nhớ thứ tự — hệ thống tự map khi khởi động.
 */
async function matchTokensToAccounts(): Promise<void> {
  // Reset tokens
  for (const acc of ACCOUNTS) acc.token = "";

  const pool = TOKEN_ENV_VARS
    .map((name) => ({ name, value: process.env[name] ?? "" }))
    .filter(({ value }) => value.length > 10);

  if (pool.length === 0) {
    logger.error("No tokens found in env vars — check Replit Secrets");
    return;
  }

  for (const { name, value: token } of pool) {
    try {
      const res = await fetch("https://api.github.com/user", {
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: "application/vnd.github+json",
          "User-Agent": "codespace-manager",
        },
      });
      if (!res.ok) {
        logger.warn({ envVar: name, status: res.status }, "Token invalid or expired — skip");
        continue;
      }
      const body = (await res.json()) as Record<string, unknown>;
      const login = String(body["login"] ?? "");
      const acc = ACCOUNTS.find((a) => a.login === login);
      if (acc) {
        acc.token = token;
        logger.info({ envVar: name, label: acc.label, login }, "Token matched ✓");
      } else {
        logger.info({ envVar: name, login }, "Token valid but login not in ACCOUNTS — unused");
      }
    } catch (err) {
      logger.warn({ envVar: name, err }, "Token check request failed");
    }
  }

  // Báo cáo kết quả
  for (const acc of ACCOUNTS) {
    if (!acc.token) {
      logger.warn({ label: acc.label, login: acc.login }, "No token found — account will be skipped in rotation");
    }
  }
}

// ─── State ─────────────────────────────────────────────────────────────────
export interface ManagerStatus {
  running: boolean;
  activeIndex: number | null;
  activeLabel: string | null;
  activeCodespaceId: string | null;
  activeWebUrl: string | null;
  maintenanceIndices: number[];
  lastCheck: string | null;
  lastError: string | null;
  lastSync: string | null;
  nextStart: string;
  nextStop: string;
  tokenMatchLog: string[];
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
  tokenMatchLog: [],
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

async function putRepoFile(
  token: string,
  repo: string,
  filePath: string,
  content: string,
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

export async function syncRepos(
  sourceIndex = 0,
  filePaths: string[] = SYNC_PATHS,
): Promise<SyncResult[]> {
  const src = ACCOUNTS[sourceIndex];
  if (!src) return [];

  const targets = ACCOUNTS.filter((_, i) => i !== sourceIndex);
  const results: SyncResult[] = [];

  for (const filePath of filePaths) {
    const srcFile = await getRepoFile(src.token, src.repo, filePath);
    if (!srcFile) {
      logger.warn({ repo: src.repo, filePath }, "Sync: file not found on source — skip");
      for (const t of targets) {
        results.push({ file: filePath, target: t.repo, ok: false, error: "not found on source" });
      }
      continue;
    }

    for (const t of targets) {
      const existing = await getRepoFile(t.token, t.repo, filePath);
      const res = await putRepoFile(
        t.token, t.repo, filePath,
        srcFile.content, existing?.sha ?? null,
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

function applyActiveWebUrl(url: string): void {
  state.activeWebUrl = url;
  setCurrentWsUrl(url);
}

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
      applyActiveWebUrl(cs.webUrl);
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

    // Chờ tối đa 180s (36×5s) — codespace thật cần 2-3 phút để khởi động
    for (let t = 0; t < 36; t++) {
      await sleep(5000);
      const cs2 = await getCodespaceState(acc);
      if (!cs2) break;
      const s2 = cs2.state.toLowerCase();
      if (s2 === "available" || s2 === "running") {
        state.activeIndex = i;
        state.activeLabel = acc.label;
        state.activeCodespaceId = acc.codespaceId;
        applyActiveWebUrl(cs2.webUrl);
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

export async function enterMaintenance(
  index: number,
  syncFiles?: string[],
): Promise<{ started: string | null; syncResults: SyncResult[] }> {
  const acc = ACCOUNTS[index];
  if (!acc) throw new Error(`Invalid account index: ${index}`);

  if (!state.maintenanceIndices.includes(index)) {
    state.maintenanceIndices.push(index);
  }

  const paths = syncFiles ?? SYNC_PATHS;
  let syncResults: SyncResult[] = [];
  if (state.activeIndex !== null && state.activeIndex !== index) {
    logger.info({ label: acc.label, from: ACCOUNTS[state.activeIndex]!.label },
      "Maintenance enter: syncing latest from active → maintained account");
    const src = ACCOUNTS[state.activeIndex]!;
    for (const filePath of paths) {
      const srcFile = await getRepoFile(src.token, src.repo, filePath);
      if (!srcFile) {
        syncResults.push({ file: filePath, target: acc.repo, ok: false, error: "not found on source" });
        continue;
      }
      const existing = await getRepoFile(acc.token, acc.repo, filePath);
      const res = await putRepoFile(acc.token, acc.repo, filePath,
        srcFile.content, existing?.sha ?? null,
        `sync pre-maintenance: ${filePath} from ${src.repo}`);
      syncResults.push({ file: filePath, target: acc.repo, ...res });
    }
    state.lastSync = new Date().toISOString();
  }

  if (state.activeIndex === index) {
    await stopCodespace(acc);
    state.activeIndex = null;
    state.activeLabel = null;
    state.activeCodespaceId = null;
    state.activeWebUrl = null;
    logger.info({ label: acc.label }, "Maintenance enter: stopped codespace");
  }

  if (state.running) {
    await activateBestAccount();
  }

  return { started: state.activeLabel, syncResults };
}

export async function exitMaintenance(
  index: number,
): Promise<{ syncResults: SyncResult[] }> {
  const acc = ACCOUNTS[index];
  if (!acc) throw new Error(`Invalid account index: ${index}`);

  logger.info({ label: acc.label }, "Maintenance exit: syncing updates → remaining accounts");
  const syncResults = await syncRepos(index, SYNC_PATHS);

  state.maintenanceIndices = state.maintenanceIndices.filter((i) => i !== index);
  logger.info({ label: acc.label }, "Maintenance exit: account back in rotation");

  return { syncResults };
}

// ─── Public API ────────────────────────────────────────────────────────────

export function startManager(): void {
  // Tự động phát hiện token → tài khoản khi khởi động
  matchTokensToAccounts().then(() => {
    const matched = ACCOUNTS.filter((a) => a.token).map((a) => a.label);
    const missing = ACCOUNTS.filter((a) => !a.token).map((a) => a.label);
    state.tokenMatchLog = [
      ...matched.map((l) => `✓ ${l}`),
      ...missing.map((l) => `✗ ${l} — no token`),
    ];
    logger.info({ matched: matched.length, missing: missing.length }, "Token auto-detection complete");
  }).catch((err) => {
    logger.error({ err }, "Token auto-detection failed");
  });

  tasks.push(
    schedule("0 4 * * *", async () => {
      logger.info("Scheduler: 04:00 — re-matching tokens then starting");
      await matchTokensToAccounts(); // refresh tokens mỗi ngày để phát hiện token hết hạn
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

  logger.info("Codespace manager started — 04:00–23:50 ICT, health check 5 min, token auto-detect enabled");
}

export function getStatus(): ManagerStatus { return { ...state }; }
export function getAccounts() {
  return ACCOUNTS.map((a, i) => ({
    index: i,
    label: a.label,
    login: a.login,
    codespaceId: a.codespaceId,
    hasToken: a.token.length > 0,
  }));
}
export async function manualStart(): Promise<void> {
  await matchTokensToAccounts();
  state.running = true;
  await activateBestAccount();
}
export async function manualStop(): Promise<void> { await stopActive(); }
export async function refreshTokens(): Promise<void> { await matchTokensToAccounts(); }
