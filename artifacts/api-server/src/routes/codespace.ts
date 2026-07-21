import { Router, type IRouter } from "express";
import {
  getStatus,
  getAccounts,
  manualStart,
  manualStop,
  enterMaintenance,
  exitMaintenance,
  syncRepos,
  refreshTokens,
  ACCOUNTS,
} from "../lib/codespace-manager.js";

const router: IRouter = Router();

/** GET /api/codespace/status — trạng thái hiện tại + kết quả match token */
router.get("/codespace/status", (_req, res) => {
  res.json({ ...getStatus(), accounts: getAccounts() });
});

/** POST /api/codespace/start — bật thủ công */
router.post("/codespace/start", async (_req, res) => {
  try {
    await manualStart();
    res.json({ ok: true, status: getStatus() });
  } catch (err) {
    res.status(500).json({ ok: false, error: String(err) });
  }
});

/** POST /api/codespace/stop — tắt thủ công */
router.post("/codespace/stop", async (_req, res) => {
  try {
    await manualStop();
    res.json({ ok: true, status: getStatus() });
  } catch (err) {
    res.status(500).json({ ok: false, error: String(err) });
  }
});

/**
 * POST /api/codespace/refresh-tokens
 * Chạy lại auto-detect token → tài khoản (dùng khi thêm secret mới)
 */
router.post("/codespace/refresh-tokens", async (_req, res) => {
  try {
    await refreshTokens();
    res.json({ ok: true, accounts: getAccounts(), status: getStatus() });
  } catch (err) {
    res.status(500).json({ ok: false, error: String(err) });
  }
});

/**
 * POST /api/codespace/maintenance/enter
 * Body: { index: 0|1|2|3, files?: string[] }
 */
router.post("/codespace/maintenance/enter", async (req, res) => {
  const { index, files } = req.body as { index?: number; files?: string[] };
  if (index === undefined || !Number.isInteger(index) || index < 0 || index >= ACCOUNTS.length) {
    res.status(400).json({ ok: false, error: `index phải từ 0 đến ${ACCOUNTS.length - 1}` });
    return;
  }
  try {
    const result = await enterMaintenance(index, files);
    res.json({ ok: true, ...result, status: getStatus() });
  } catch (err) {
    res.status(500).json({ ok: false, error: String(err) });
  }
});

/**
 * POST /api/codespace/maintenance/exit
 * Body: { index: 0|1|2|3 }
 */
router.post("/codespace/maintenance/exit", async (req, res) => {
  const { index } = req.body as { index?: number };
  if (index === undefined || !Number.isInteger(index) || index < 0 || index >= ACCOUNTS.length) {
    res.status(400).json({ ok: false, error: `index phải từ 0 đến ${ACCOUNTS.length - 1}` });
    return;
  }
  try {
    const result = await exitMaintenance(index);
    res.json({ ok: true, ...result, status: getStatus() });
  } catch (err) {
    res.status(500).json({ ok: false, error: String(err) });
  }
});

/**
 * POST /api/codespace/sync
 * Body: { sourceIndex?: 0|1|2|3, files?: string[] }
 */
router.post("/codespace/sync", async (req, res) => {
  const { sourceIndex = 0, files } = req.body as { sourceIndex?: number; files?: string[] };
  if (!Number.isInteger(sourceIndex) || sourceIndex < 0 || sourceIndex >= ACCOUNTS.length) {
    res.status(400).json({ ok: false, error: `sourceIndex phải từ 0 đến ${ACCOUNTS.length - 1}` });
    return;
  }
  try {
    const results = await syncRepos(sourceIndex, files);
    res.json({ ok: true, results, status: getStatus() });
  } catch (err) {
    res.status(500).json({ ok: false, error: String(err) });
  }
});

export default router;
