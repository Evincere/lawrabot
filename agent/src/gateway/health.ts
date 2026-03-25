import express from "express";
import type { Logger } from "../utils/logger.js";

export function createHealthRoutes(log: Logger) {
  const router = express.Router();

  router.get("/healthz", (_req, res) => {
    res.json({ status: "ok", timestamp: new Date().toISOString() });
  });

  router.get("/readyz", (_req, res) => {
    res.json({ status: "ready", timestamp: new Date().toISOString() });
  });

  return router;
}
