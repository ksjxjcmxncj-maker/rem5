import { Router, type IRouter } from "express";
import healthRouter from "./health";
import codespaceRouter from "./codespace";
import wsUrlRouter from "./wsUrl";

const router: IRouter = Router();

router.use(healthRouter);
router.use(wsUrlRouter);
router.use(codespaceRouter);

export default router;
