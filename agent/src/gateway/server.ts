import express from "express";
import type { AppConfig } from "../config/schema.js";
import type { Logger } from "../utils/logger.js";
import type { ChannelAdapter } from "../channels/types.js";
import { SessionManager } from "../sessions/manager.js";
import { ToolRegistry } from "../tools/registry.js";
import { HookRunner } from "../hooks/runner.js";
import { createLLMProvider } from "../llm/router.js";
import { createMessageRouter } from "./message-router.js";
import { createHealthRoutes } from "./health.js";
import { datetimeTool } from "../tools/built-in/datetime.js";
import { generatePdfTool } from "../tools/built-in/documents.js";
import { loadSpecialization } from "../specialization/loader.js";
import { WhatsAppAdapter } from "../channels/whatsapp/adapter.js";
import { TelegramAdapter } from "../channels/telegram/adapter.js";
import type { Server } from "node:http";

export interface GatewayOptions {
  config: AppConfig;
  specDir: string;
  log: Logger;
}

export interface GatewayServer {
  close(): Promise<void>;
}

/**
 * Start the gateway server — the central orchestrator.
 */
export async function startGateway(opts: GatewayOptions): Promise<GatewayServer> {
  const { config, specDir, log } = opts;

  log.info(`Starting TemplateClaw gateway: ${config.name}`);

  // ─── 1. Load Specialization ────────────────────────────────
  const spec = await loadSpecialization(specDir, log);
  log.info(`Loaded specialization: ${spec.tools.length} domain tools`);

  // ─── 2. Initialize Core Systems ────────────────────────────
  const sessionManager = new SessionManager(
    config.sessions.persistDir,
    config.sessions.maxHistoryMessages,
    log,
  );

  const toolRegistry = new ToolRegistry(log);
  const hookRunner = new HookRunner(log);
  const llmProvider = createLLMProvider(config.llm, log);

  // ─── 3. Register Built-in Tools ────────────────────────────
  toolRegistry.register(datetimeTool);
  toolRegistry.register(generatePdfTool);

  // ─── 4. Register Domain Tools from Specialization ──────────
  toolRegistry.registerAll(spec.tools);

  log.info(`Tool registry: ${toolRegistry.size} tools (${toolRegistry.list().join(", ")})`);

  // ─── 5. Initialize Channels ────────────────────────────────
  const channels = new Map<string, ChannelAdapter>();

  if (config.whatsapp.enabled) {
    const wa = new WhatsAppAdapter(log);
    channels.set("whatsapp", wa);
  }

  if (config.telegram.enabled) {
    const tg = new TelegramAdapter(log);
    channels.set("telegram", tg);
  }

  // ─── 6. Create Message Router ──────────────────────────────
  const handleInbound = createMessageRouter({
    sessionManager,
    toolRegistry,
    hookRunner,
    llmProvider,
    channels,
    systemPrompt: spec.systemPrompt,
    log,
  });

  // ─── 7. Wire Channels to Router ────────────────────────────
  for (const [id, channel] of channels) {
    channel.onMessage(handleInbound);
    log.info(`Channel "${id}" wired to message router`);
  }

  // ─── 8. Start Channels ─────────────────────────────────────
  if (config.whatsapp.enabled) {
    await channels.get("whatsapp")!.start({
      authDir: config.whatsapp.authDir,
    });
  }

  if (config.telegram.enabled) {
    await channels.get("telegram")!.start({
      botToken: config.telegram.botToken,
      allowedChatIds: config.telegram.allowedChatIds,
    });
  }

  // ─── 9. Start HTTP Server ──────────────────────────────────
  const app = express();
  app.use(express.json());
  app.use(createHealthRoutes(log));

  // Status endpoint
  app.get("/status", (_req, res) => {
    res.json({
      name: config.name,
      channels: Array.from(channels.keys()),
      tools: toolRegistry.list(),
      llm: { provider: config.llm.provider, model: config.llm.model },
      hooks: hookRunner.count(),
    });
  });

  const httpServer: Server = app.listen(config.port, () => {
    log.info(`Gateway HTTP server listening on port ${config.port}`);
    log.info(`  Health:  http://localhost:${config.port}/healthz`);
    log.info(`  Status:  http://localhost:${config.port}/status`);
  });

  await hookRunner.run("session_start", { port: config.port });

  // ─── 10. Return Close Handle ───────────────────────────────
  return {
    async close() {
      log.info("Shutting down gateway...");
      await hookRunner.run("session_end", {});
      for (const [id, channel] of channels) {
        await channel.stop();
        log.info(`Channel "${id}" stopped`);
      }
      httpServer.close();
      log.info("Gateway shut down complete");
    },
  };
}
