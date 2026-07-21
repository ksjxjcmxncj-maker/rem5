import { Router, type IRouter } from "express";
import { getStatus, manualStart, manualStop } from "../lib/codespace-manager.js";

const router: IRouter = Router();

/** GET /api/codespace/status — trạng thái hiện tại */
router.get("/codespace/status", (_req, res) => {
  res.json(getStatus());
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

export default router;
