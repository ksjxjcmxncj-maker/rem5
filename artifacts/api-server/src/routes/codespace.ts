import { Router, type IRouter } from "express";
import {
  getStatus,
  getAccounts,
  manualStart,
  manualStop,
  enterMaintenance,
  exitMaintenance,
  syncRepos,
  ACCOUNTS,
} from "../lib/codespace-manager.js";

const router: IRouter = Router();

/** GET /api/codespace/status — trạng thái hiện tại */
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
 * POST /api/codespace/maintenance/enter
 * Body: { index: 0|1|2, files?: string[] }
 * Đưa 1 tài khoản vào bảo trì → bật bản còn lại + đồng bộ file
 */
router.post("/codespace/maintenance/enter", async (req, res) => {
  const { index, files } = req.body as { index?: number; files?: string[] };
  if (index === undefined || !Number.isInteger(index) || index < 0 || index >= ACCOUNTS.length) {
    res.status(400).json({ ok: false, error: `index phải là 0, 1 hoặc 2` });
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
 * Body: { index: 0|1|2 }
 * Kết thúc bảo trì → đồng bộ file mới nhất về bản vừa xong + đưa lại vào rotation
 */
router.post("/codespace/maintenance/exit", async (req, res) => {
  const { index } = req.body as { index?: number };
  if (index === undefined || !Number.isInteger(index) || index < 0 || index >= ACCOUNTS.length) {
    res.status(400).json({ ok: false, error: `index phải là 0, 1 hoặc 2` });
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
 * Body: { sourceIndex?: 0|1|2, files?: string[] }
 * Đồng bộ thủ công file từ 1 repo sang tất cả repo còn lại
 */
router.post("/codespace/sync", async (req, res) => {
  const { sourceIndex = 0, files } = req.body as { sourceIndex?: number; files?: string[] };
  if (!Number.isInteger(sourceIndex) || sourceIndex < 0 || sourceIndex >= ACCOUNTS.length) {
    res.status(400).json({ ok: false, error: `sourceIndex phải là 0, 1 hoặc 2` });
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
