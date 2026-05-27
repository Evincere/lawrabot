import express from "express";
import type { AppConfig } from "../config/schema.js";
import type { Logger } from "../utils/logger.js";
import type { ChannelAdapter } from "../channels/types.js";
import { SessionManager } from "../sessions/manager.js";
import { ToolRegistry } from "../tools/registry.js";
import { MCPManager } from "../tools/mcp.js";
import { HookRunner } from "../hooks/runner.js";
import { createLLMProvider } from "../llm/router.js";
import { createMessageRouter } from "./message-router.js";
import { createHealthRoutes } from "./health.js";
import { datetimeTool } from "../tools/built-in/datetime.js";
import { generatePdfTool, sendFileTool } from "../tools/built-in/documents.js";
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
  const mcpManager = new MCPManager(log);
  const hookRunner = new HookRunner(log);
  const llmProvider = createLLMProvider(config.llm, log);

  // ─── 3. Register Built-in Tools ────────────────────────────
  toolRegistry.register(datetimeTool);
  toolRegistry.register(generatePdfTool);
  toolRegistry.register(sendFileTool);

  // ─── 4. Register Domain Tools from Specialization ──────────
  toolRegistry.registerAll(spec.tools);

  // ─── 4b. Register MCP Tools from External Servers ──────────
  for (const serverConfig of config.mcpServers) {
    if (serverConfig.enabled) {
      const mcpTools = await mcpManager.connectServer(serverConfig);
      toolRegistry.registerAll(mcpTools);
    }
  }

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
    specDir,
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

  // Proactive push endpoint — permite al backend enviar mensajes sin esperar al ciudadano
  app.post("/push", async (req, res) => {
    const { phoneNumber, message, taskId } = req.body as {
      phoneNumber?: string;
      message?: string;
      taskId?: string;
    };

    if (!phoneNumber || !message) {
      res.status(400).json({ error: "phoneNumber and message are required" });
      return;
    }

    const wa = channels.get("whatsapp");
    if (!wa) {
      res.status(503).json({ error: "WhatsApp channel is not available" });
      return;
    }

    // Construir el JID estándar de WhatsApp desde el número de teléfono
    const jid = `${phoneNumber}@s.whatsapp.net`;

    try {
      log.info(`[push] Sending proactive message to ${jid} (task: ${taskId ?? "n/a"})`);
      
      // 1. Enviar mensaje real vía WhatsApp
      await wa.sendMessage(jid, { text: message });
      log.info(`[push] Message sent successfully to ${jid}`);

      // 2. Inyectar en el historial de sesión para mantener el contexto del LLM
      const session = sessionManager.getOrCreate("whatsapp", jid);
      sessionManager.addMessage(session, {
        role: "assistant",
        content: `[SISTEMA - Mensaje Proactivo enviado al ciudadano por solicitud del operador humano (taskId: ${taskId ?? "n/a"})]\n${message}`,
        timestamp: Date.now(),
      });
      log.info(`[push] Context injected into session history for ${jid}`);

      res.json({ success: true, jid, taskId });
    } catch (error) {
      const msg = error instanceof Error ? error.message : String(error);
      log.error(`[push] Failed to send message to ${jid}: ${msg}`);
      res.status(500).json({ error: msg });
    }

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
      await mcpManager.shutdown();
      httpServer.close();
      log.info("Gateway shut down complete");
    },
  };
}
