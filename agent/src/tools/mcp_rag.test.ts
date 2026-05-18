import { describe, it, expect, beforeAll, afterAll } from "vitest";
import { MCPManager } from "./mcp.js";
import { createLogger } from "../utils/logger.js";
import type { AgentTool } from "./types.js";

describe("Divorce RAG & Legal Knowledge MCP Test", () => {
  let mcpManager: MCPManager;
  let tools: AgentTool[] = [];
  const log = createLogger("test-rag");

  beforeAll(async () => {
    mcpManager = new MCPManager(log);
    // Connect to the divorce MCP server running on port 8081
    tools = await mcpManager.connectServer({
      name: "LawraBot-Divorce-Service",
      url: "http://127.0.0.1:8081/mcp/sse",
      enabled: true,
    });
  });

  afterAll(async () => {
    // Clean up connections if any
  });

  it("should successfully discover the consultar_normativa tool", () => {
    const consultarNormativa = tools.find((t) => t.name === "consultar_normativa");
    expect(consultarNormativa).toBeDefined();
    expect(consultarNormativa?.description).toContain("base de conocimientos");
  });

  it("should retrieve Mendoza Supreme Court jurisprudence on economic compensation for invisible care", async () => {
    const consultarNormativa = tools.find((t) => t.name === "consultar_normativa");
    expect(consultarNormativa).toBeDefined();

    const ctx = {
      sessionId: "test-session",
      userId: "test-user",
      phoneNumber: "5492610000000",
      specDir: "./agent/specs/divorce-assistant",
      channelId: "test-channel",
      conversationId: "test-conversation",
      config: {} as any,
      log,
    };

    const result = await consultarNormativa!.handler(
      {
        phoneNumber: "5492610000000",
        query: "T.P.V. c/ P.A.E. compensación económica desequilibrio",
      },
      ctx
    );

    console.log("RAG Result (Tareas de Cuidado & Suprema Corte Mendoza):\n", result.content);
    
    // Check if the result contains our newly indexed jurisprudence
    expect(result.content).toContain("T.P.V. c/ P.A.E.");
    expect(result.content).toContain("trabajo invisible");
    expect(result.content).toContain("desequilibrio");
  });

  it("should explain the separate and autonomous nature of the asset division (liquidación de bienes)", async () => {
    const consultarNormativa = tools.find((t) => t.name === "consultar_normativa");
    expect(consultarNormativa).toBeDefined();

    const ctx = {
      sessionId: "test-session",
      userId: "test-user",
      phoneNumber: "5492610000000",
      specDir: "./agent/specs/divorce-assistant",
      channelId: "test-channel",
      conversationId: "test-conversation",
      config: {} as any,
      log,
    };

    const result = await consultarNormativa!.handler(
      {
        phoneNumber: "5492610000000",
        query: "sociedad de bienes constituye un proceso posterior y autónomo",
      },
      ctx
    );

    console.log("RAG Result (Liquidación de Bienes & Deudas):\n", result.content);

    expect(result.content).toContain("proceso posterior y autónomo");
    expect(result.content).toContain("incidente");
    expect(result.content).toContain("Juzgados de Familia");
    expect(result.content).toContain("nunca suspende ni posterga");
  });

  it("should explain the coordination with Domestic Violence courts", async () => {
    const consultarNormativa = tools.find((t) => t.name === "consultar_normativa");
    expect(consultarNormativa).toBeDefined();

    const ctx = {
      sessionId: "test-session",
      userId: "test-user",
      phoneNumber: "5492610000000",
      specDir: "./agent/specs/divorce-assistant",
      channelId: "test-channel",
      conversationId: "test-conversation",
      config: {} as any,
      log,
    };

    const result = await consultarNormativa!.handler(
      {
        phoneNumber: "5492610000000",
        query: "coordinación Juzgados de Violencia Familiar y de Género exclusión del hogar",
      },
      ctx
    );

    console.log("RAG Result (Violencia Familiar & Articulación):\n", result.content);

    expect(result.content).toContain("Juzgados de Violencia");
    expect(result.content).toContain("coordinación");
    expect(result.content).toContain("exclusión del hogar");
    expect(result.content).toContain("subordinarse");
  });
});
