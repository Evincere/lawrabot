import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { SSEClientTransport } from "@modelcontextprotocol/sdk/client/sse.js";
import { EventSource } from "eventsource";
import pino from "pino";
import path from "node:path";
import fs from "node:fs";
import type { Logger } from "../utils/logger.js";
import type { AgentTool } from "./types.js";
import type { MCPServerConfig } from "../config/schema.js";

/**
 * Dedicated logger for legal audit of MCP tool calls.
 * This persists requests and responses in separate files as required.
 */
function createAuditLogger() {
  const logDir = path.resolve(".data/audit");
  if (!fs.existsSync(logDir)) {
    fs.mkdirSync(logDir, { recursive: true });
  }

  return pino({
    name: "mcp-audit",
    level: "info",
    transport: {
      targets: [
        {
          target: "pino/file",
          options: { destination: path.join(logDir, "mcp-transactions.log") },
        },
        {
          target: "pino-pretty", // Also show in console during dev
          options: { colorize: true },
        },
      ],
    },
  });
}

const auditLog = createAuditLogger();

/** Timeout for individual MCP tool calls (ms). Generous for BLSG scraping. */
const TOOL_CALL_TIMEOUT_MS = 120_000;

/** Timeout for the lightweight health-check ping (ms). */
const HEALTH_PING_TIMEOUT_MS = 8_000;

/** Interval between SSE keepalive pings (ms). 5 minutes. */
const KEEPALIVE_INTERVAL_MS = 5 * 60 * 1000;

/**
 * Internal bookkeeping for a connected MCP server.
 */
interface ServerEntry {
  config: MCPServerConfig;
  client: Client;
  toolDefinitions: Array<{ name: string; description: string; inputSchema: any }>;
  keepaliveTimer?: ReturnType<typeof setInterval>;
}

/**
 * Manages connections to MCP servers and exposes their tools as AgentTools.
 *
 * Implements:
 *  - Automatic reconnection when the SSE pipe goes stale.
 *  - Periodic keepalive pings to prevent silent TCP death.
 *  - One transparent retry on timeout before surfacing the error.
 */
export class MCPManager {
  private servers: Map<string, ServerEntry> = new Map();

  constructor(private readonly log: Logger) {}

  // ──────────────────────────────────────────────────────────
  // PUBLIC API
  // ──────────────────────────────────────────────────────────

  /** Maximum number of connection attempts before giving up. */
  private static readonly MAX_CONNECT_RETRIES = 10;
  /** Initial delay between retries (ms). Doubles each attempt up to MAX_RETRY_DELAY_MS. */
  private static readonly INITIAL_RETRY_DELAY_MS = 2_000;
  /** Maximum delay between retries (ms). */
  private static readonly MAX_RETRY_DELAY_MS = 30_000;

  /**
   * Connect to an MCP server via SSE and return its tools.
   * Retries with exponential backoff if the server isn't ready yet (common at container startup).
   */
  async connectServer(config: MCPServerConfig): Promise<AgentTool[]> {
    this.log.info(`[MCP] Connecting to server "${config.name}" at ${config.url}...`);

    let lastError = "";
    let delay = MCPManager.INITIAL_RETRY_DELAY_MS;

    for (let attempt = 1; attempt <= MCPManager.MAX_CONNECT_RETRIES; attempt++) {
      try {
        const entry = await this.createConnection(config);
        this.servers.set(config.name, entry);

        this.log.info(`[MCP] Connected to "${config.name}" — ${entry.toolDefinitions.length} tools discovered (attempt ${attempt})`);

        // Start keepalive pings on this connection
        this.startKeepalive(config.name);

        // Build stable tool handles that survive reconnections
        return entry.toolDefinitions.map((tool) => this.buildToolHandle(config.name, tool));
      } catch (error) {
        lastError = error instanceof Error ? error.message : String(error);

        if (attempt < MCPManager.MAX_CONNECT_RETRIES) {
          this.log.warn(`[MCP] Connection attempt ${attempt}/${MCPManager.MAX_CONNECT_RETRIES} failed for "${config.name}": ${lastError}. Retrying in ${delay / 1000}s...`);
          await new Promise((resolve) => setTimeout(resolve, delay));
          delay = Math.min(delay * 2, MCPManager.MAX_RETRY_DELAY_MS);
        }
      }
    }

    this.log.error(`[MCP] Failed to connect to "${config.name}" after ${MCPManager.MAX_CONNECT_RETRIES} attempts: ${lastError}`);
    return [];
  }

  /**
   * Close all active MCP connections.
   */
  async shutdown(): Promise<void> {
    for (const [name, entry] of this.servers) {
      this.log.info(`[MCP] Disconnecting from "${name}"...`);
      if (entry.keepaliveTimer) clearInterval(entry.keepaliveTimer);
      try { await entry.client.close(); } catch { /* best effort */ }
    }
    this.servers.clear();
  }

  // ──────────────────────────────────────────────────────────
  // PRIVATE — Connection lifecycle
  // ──────────────────────────────────────────────────────────

  /**
   * Create a fresh SSE transport + Client + discover tools.
   */
  private async createConnection(config: MCPServerConfig): Promise<Omit<ServerEntry, "keepaliveTimer">> {
    // @ts-ignore — EventSource polyfill for Node.js
    globalThis.EventSource = EventSource;
    const transport = new SSEClientTransport(new URL(config.url));

    const client = new Client(
      { name: "templateclaw-agent", version: "0.1.0" },
      { capabilities: {} },
    );

    await client.connect(transport);

    const { tools: mcpTools } = await client.listTools();

    return {
      config,
      client,
      toolDefinitions: mcpTools.map((t) => ({
        name: t.name,
        description: t.description || "",
        inputSchema: t.inputSchema,
      })),
    };
  }

  /**
   * Tear down the old connection and establish a new one, preserving
   * the same tool handle references (they resolve `client` at call-time).
   */
  private async reconnect(serverName: string): Promise<void> {
    const existing = this.servers.get(serverName);
    if (!existing) throw new Error(`[MCP] No server "${serverName}" to reconnect.`);

    this.log.info(`[MCP] Reconnecting to "${serverName}"...`);

    // Stop old keepalive
    if (existing.keepaliveTimer) clearInterval(existing.keepaliveTimer);

    // Tear down old client (best-effort)
    try { await existing.client.close(); } catch { /* ignored */ }

    // Build a new connection
    const fresh = await this.createConnection(existing.config);

    existing.client = fresh.client;
    existing.toolDefinitions = fresh.toolDefinitions;

    // Restart keepalive
    this.startKeepalive(serverName);

    this.log.info(`[MCP] Reconnected to "${serverName}" — ${fresh.toolDefinitions.length} tools`);
  }

  // ──────────────────────────────────────────────────────────
  // PRIVATE — Keepalive
  // ──────────────────────────────────────────────────────────

  /**
   * Periodically send a lightweight `listTools()` ping to keep the SSE
   * connection alive and detect silent TCP death early.
   */
  private startKeepalive(serverName: string): void {
    const entry = this.servers.get(serverName);
    if (!entry) return;

    entry.keepaliveTimer = setInterval(async () => {
      try {
        await entry.client.listTools({ timeout: HEALTH_PING_TIMEOUT_MS } as any);
        // Connection is healthy — nothing to do
      } catch {
        this.log.warn(`[MCP] Keepalive ping failed for "${serverName}". Reconnecting...`);
        try {
          await this.reconnect(serverName);
        } catch (err) {
          const msg = err instanceof Error ? err.message : String(err);
          this.log.error(`[MCP] Reconnection failed for "${serverName}": ${msg}`);
        }
      }
    }, KEEPALIVE_INTERVAL_MS);

    // Don't let the timer prevent Node from exiting
    if (entry.keepaliveTimer.unref) entry.keepaliveTimer.unref();
  }

  // ──────────────────────────────────────────────────────────
  // PRIVATE — Tool handle factory
  // ──────────────────────────────────────────────────────────

  /**
   * Returns an AgentTool whose `handler` always resolves
   * `this.servers.get(serverName).client` at call-time, so it
   * automatically picks up reconnected clients.
   */
  private buildToolHandle(
    serverName: string,
    tool: { name: string; description: string; inputSchema: any },
  ): AgentTool {
    return {
      name: tool.name,
      description: tool.description,
      parameters: tool.inputSchema.properties as any,
      handler: async (args: any) => {
        const transactionId = Math.random().toString(36).substring(7);
        auditLog.info({
          type: "request",
          transactionId,
          server: serverName,
          tool: tool.name,
          args,
          timestamp: new Date().toISOString(),
        });

        try {
          const result = await this.callWithRetry(serverName, tool.name, args, transactionId);

          // Extract text content
          const content = (result.content as any[])
            .filter((c) => c.type === "text")
            .map((c) => (c as any).text)
            .join("\n");

          return { content };
        } catch (error) {
          const msg = error instanceof Error ? error.message : String(error);
          auditLog.error({
            type: "error",
            transactionId,
            server: serverName,
            tool: tool.name,
            error: msg,
            timestamp: new Date().toISOString(),
          });
          throw error;
        }
      },
    };
  }

  /**
   * Call a tool with one automatic retry on timeout:
   *  1. Try with current client.
   *  2. If it times out → reconnect → retry once.
   *  3. If retry also fails → throw.
   */
  private async callWithRetry(
    serverName: string,
    toolName: string,
    args: any,
    transactionId: string,
  ): Promise<any> {
    const entry = this.servers.get(serverName);
    if (!entry) throw new Error(`[MCP] Server "${serverName}" not connected.`);

    try {
      const result = await entry.client.callTool(
        { name: toolName, arguments: args },
        undefined,
        { timeout: TOOL_CALL_TIMEOUT_MS },
      );

      auditLog.info({
        type: "response",
        transactionId,
        server: serverName,
        tool: toolName,
        result,
        timestamp: new Date().toISOString(),
      });

      return result;
    } catch (firstError) {
      const msg = firstError instanceof Error ? firstError.message : String(firstError);
      const isTimeout = msg.includes("timed out") || msg.includes("timeout");

      if (!isTimeout) throw firstError;

      // ── Timeout detected → reconnect and retry once ──
      this.log.warn(`[MCP] Tool "${toolName}" timed out. Reconnecting and retrying...`);
      auditLog.info({
        type: "retry",
        transactionId,
        server: serverName,
        tool: toolName,
        reason: msg,
        timestamp: new Date().toISOString(),
      });

      await this.reconnect(serverName);

      const retryEntry = this.servers.get(serverName)!;
      const retryResult = await retryEntry.client.callTool(
        { name: toolName, arguments: args },
        undefined,
        { timeout: TOOL_CALL_TIMEOUT_MS },
      );

      auditLog.info({
        type: "response",
        transactionId,
        server: serverName,
        tool: toolName,
        result: retryResult,
        note: "succeeded after reconnect",
        timestamp: new Date().toISOString(),
      });

      return retryResult;
    }
  }
}
