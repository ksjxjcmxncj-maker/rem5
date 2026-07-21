import { Router, type IRouter } from "express";
import healthRouter from "./health";
import codespaceRouter from "./codespace";

const router: IRouter = Router();

router.use(healthRouter);
router.use(codespaceRouter);

export default router;
