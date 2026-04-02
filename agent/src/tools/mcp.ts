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

/**
 * Manages connections to MCP servers and exposes their tools as AgentTools.
 */
export class MCPManager {
  private clients: Map<string, Client> = new Map();

  constructor(private readonly log: Logger) {}

  /**
   * Connect to an MCP server via SSE and return its tools.
   */
  async connectServer(config: MCPServerConfig): Promise<AgentTool[]> {
    this.log.info(`[MCP] Connecting to server "${config.name}" at ${config.url}...`);

    try {
      // 1. Initialize Transport
      // @ts-ignore - EventSource polyfill for Node
      globalThis.EventSource = EventSource;
      const transport = new SSEClientTransport(new URL(config.url));

      // 2. Initialize Client
      const client = new Client(
        { name: "templateclaw-agent", version: "0.1.0" },
        { capabilities: {} }
      );

      await client.connect(transport);
      this.clients.set(config.name, client);

      // 3. Discover Tools
      const { tools: mcpTools } = await client.listTools();
      this.log.info(`[MCP] Connected to "${config.name}" — ${mcpTools.length} tools discovered`);

      // 4. Map to AgentTool format
      return mcpTools.map((tool) => ({
        name: tool.name,
        description: tool.description || "",
        parameters: tool.inputSchema.properties as any, // Mapped to LLM schema format
        handler: async (args: any) => {
          // LEGAL AUDIT: Log the request
          const transactionId = Math.random().toString(36).substring(7);
          auditLog.info({
            type: "request",
            transactionId,
            server: config.name,
            tool: tool.name,
            args,
            timestamp: new Date().toISOString(),
          });

          try {
            const result = await client.callTool({
              name: tool.name,
              arguments: args,
            });

            // LEGAL AUDIT: Log the response
            auditLog.info({
              type: "response",
              transactionId,
              server: config.name,
              tool: tool.name,
              result,
              timestamp: new Date().toISOString(),
            });

            // Extract content and handle formatting
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
              server: config.name,
              tool: tool.name,
              error: msg,
              timestamp: new Date().toISOString(),
            });
            throw error;
          }
        },
      }));
    } catch (error) {
      const msg = error instanceof Error ? error.message : String(error);
      this.log.error(`[MCP] Failed to connect to "${config.name}": ${msg}`);
      return [];
    }
  }

  /**
   * Close all active MCP connections.
   */
  async shutdown(): Promise<void> {
    for (const [name, client] of this.clients) {
      this.log.info(`[MCP] Disconnecting from "${name}"...`);
      await client.close();
    }
    this.clients.clear();
  }
}
