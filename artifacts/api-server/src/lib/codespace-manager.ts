import { schedule, type ScheduledTask } from "node-cron";
import { logger } from "./logger.js";

// ─── Account config ────────────────────────────────────────────────────────
interface CodespaceAccount {
  label: string;
  token: string;
  codespaceId: string;
  login: string;
}

const ACCOUNTS: CodespaceAccount[] = [
  {
    label: "Primary (ksjxjcmxncj-maker)",
    token: process.env["GITHUB_PERSONAZL_ACCESS_TOKEN"] ?? "",
    codespaceId: "improved-fishstick-966vx76qqgx7cqjp",
    login: "ksjxjcmxncj-maker",
  },
  {
    label: "Backup-1 (kgxxyixgikcgxittixxi-collab)",
    token: process.env["GITHUB_PERSONALA_ACCESS_TOKEN"] ?? "",
    codespaceId: "crispy-space-capybara-5v564w74jqgf45x4",
    login: "kgxxyixgikcgxittixxi-collab",
  },
  {
    label: "Backup-2 (idkyoohdtsu-netizen)",
    token: process.env["GITHUB_PERSONACL_ACCESS_TOKEN"] ?? "",
    codespaceId: "cuddly-space-orbit-qvvrx7jq5gv6246wg",
    login: "idkyoohdtsu-netizen",
  },
];

// ─── State ─────────────────────────────────────────────────────────────────
export interface ManagerStatus {
  running: boolean;
  activeIndex: number | null;
  activeLabel: string | null;
  activeCodespaceId: string | null;
  activeWebUrl: string | null;
  lastCheck: string | null;
  lastError: string | null;
  nextStart: string;
  nextStop: string;
}

let state: ManagerStatus = {
  running: false,
  activeIndex: null,
  activeLabel: null,
  activeCodespaceId: null,
  activeWebUrl: null,
  lastCheck: null,
  lastError: null,
  nextStart: "04:00 ICT",
  nextStop: "23:50 ICT",
};

const tasks: ScheduledTask[] = [];

// ─── GitHub API helpers ────────────────────────────────────────────────────
async function ghFetch(
  token: string,
  path: string,
  method = "GET",
): Promise<{ ok: boolean; status: number; body: unknown }> {
  const res = await fetch(`https://api.github.com${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${token}`,
      "User-Agent": "codespace-manager",
      Accept: "application/vnd.github+json",
    },
  });
  let body: unknown;
  try {
    body = await res.json();
  } catch {
    body = null;
  }
  return { ok: res.ok, status: res.status, body };
}

/** Get current state of a codespace. Returns null on error. */
async function getCodespaceState(
  acc: CodespaceAccount,
): Promise<{ state: string; webUrl: string } | null> {
  const r = await ghFetch(
    acc.token,
    `/user/codespaces/${acc.codespaceId}`,
  );
  if (!r.ok) return null;
  const b = r.body as Record<string, unknown>;
  return {
    state: String(b["state"] ?? "Unknown"),
    webUrl: String(b["web_url"] ?? ""),
  };
}

/** Start a codespace. Returns error message or null on success. */
async function startCodespace(acc: CodespaceAccount): Promise<string | null> {
  const r = await ghFetch(
    acc.token,
    `/user/codespaces/${acc.codespaceId}/start`,
    "POST",
  );
  if (!r.ok) {
    const msg =
      (r.body as Record<string, unknown>)?.["message"] ?? `HTTP ${r.status}`;
    return String(msg);
  }
  return null;
}

/** Stop a codespace. */
async function stopCodespace(acc: CodespaceAccount): Promise<void> {
  await ghFetch(
    acc.token,
    `/user/codespaces/${acc.codespaceId}/stop`,
    "POST",
  );
}

// ─── Core logic ────────────────────────────────────────────────────────────

/** Try to bring up the best available codespace (primary first, then backups).
 *  Sets state.activeIndex when successful. */
async function activateBestAccount(): Promise<void> {
  for (let i = 0; i < ACCOUNTS.length; i++) {
    const acc = ACCOUNTS[i]!;
    if (!acc.token) {
      logger.warn({ label: acc.label }, "Token missing — skip");
      continue;
    }

    logger.info({ label: acc.label }, "Trying to start codespace");

    // Check current state first
    const cs = await getCodespaceState(acc);
    if (!cs) {
      logger.warn({ label: acc.label }, "Cannot reach codespace — skip");
      state.lastError = `${acc.label}: unreachable`;
      continue;
    }

    const s = cs.state.toLowerCase();
    if (s === "available" || s === "starting" || s === "running") {
      // Already up — just record it
      state.activeIndex = i;
      state.activeLabel = acc.label;
      state.activeCodespaceId = acc.codespaceId;
      state.activeWebUrl = cs.webUrl;
      state.lastError = null;
      logger.info({ label: acc.label, csState: cs.state }, "Codespace already active");
      return;
    }

    // Try to start
    const err = await startCodespace(acc);
    if (err) {
      logger.warn({ label: acc.label, err }, "Start failed — trying next");
      state.lastError = `${acc.label}: ${err}`;
      continue;
    }

    // Wait up to 60 s for it to come up
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
        logger.info({ label: acc.label }, "Codespace started successfully");
        return;
      }
    }

    logger.warn({ label: acc.label }, "Timed out waiting — trying next");
    state.lastError = `${acc.label}: start timed out`;
  }

  logger.error("All codespaces failed to start");
}

/** Health check: verify current active codespace is OK, failover if not. */
async function healthCheck(): Promise<void> {
  state.lastCheck = new Date().toISOString();

  if (state.activeIndex === null) {
    // Not started yet (outside schedule window) — nothing to check
    return;
  }

  const acc = ACCOUNTS[state.activeIndex]!;
  const cs = await getCodespaceState(acc);

  if (!cs) {
    logger.warn({ label: acc.label }, "Health check: unreachable — failover");
    state.activeIndex = null;
    state.lastError = `${acc.label}: health check unreachable`;
    await activateBestAccount();
    return;
  }

  const s = cs.state.toLowerCase();
  const healthy = s === "available" || s === "running" || s === "starting";
  if (!healthy) {
    logger.warn({ label: acc.label, csState: cs.state }, "Health check: unhealthy — failover");
    state.activeIndex = null;
    state.lastError = `${acc.label}: state=${cs.state}`;
    await activateBestAccount();
  } else {
    logger.info({ label: acc.label, csState: cs.state }, "Health check: OK");
  }
}

/** Stop whichever codespace is currently active. */
async function stopActive(): Promise<void> {
  if (state.activeIndex === null) return;
  const acc = ACCOUNTS[state.activeIndex]!;
  logger.info({ label: acc.label }, "Stopping codespace (end of schedule)");
  await stopCodespace(acc);
  state.running = false;
  state.activeIndex = null;
  state.activeLabel = null;
  state.activeCodespaceId = null;
  state.activeWebUrl = null;
}

function sleep(ms: number) {
  return new Promise<void>((r) => setTimeout(r, ms));
}

// ─── Public API ────────────────────────────────────────────────────────────

/** Start the scheduler. Call once at server boot. */
export function startManager(): void {
  // Daily start at 04:00 ICT (Asia/Ho_Chi_Minh)
  tasks.push(
    schedule(
      "0 4 * * *",
      async () => {
        logger.info("Scheduler: 04:00 — starting server");
        state.running = true;
        await activateBestAccount();
      },
      { timezone: "Asia/Ho_Chi_Minh" },
    ),
  );

  // Daily stop at 23:50 ICT
  tasks.push(
    schedule(
      "50 23 * * *",
      async () => {
        logger.info("Scheduler: 23:50 — stopping server");
        await stopActive();
      },
      { timezone: "Asia/Ho_Chi_Minh" },
    ),
  );

  // Health check every 5 minutes
  tasks.push(
    schedule("*/5 * * * *", async () => {
      if (state.running) await healthCheck();
    }),
  );

  logger.info("Codespace manager started — schedule 04:00–23:50 ICT, health check every 5 min");
}

/** Return current manager status (for the API). */
export function getStatus(): ManagerStatus {
  return { ...state };
}

/** Manually start right now (for the API / testing). */
export async function manualStart(): Promise<void> {
  state.running = true;
  await activateBestAccount();
}

/** Manually stop right now (for the API). */
export async function manualStop(): Promise<void> {
  await stopActive();
}
