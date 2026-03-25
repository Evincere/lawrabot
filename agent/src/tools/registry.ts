import type { Logger } from "../utils/logger.js";
import type { LLMToolDefinition } from "../llm/types.js";
import type { AgentTool, ToolContext, ToolResult } from "./types.js";

/**
 * Registry for agent tools (function calling).
 * Manages registration, lookup, and execution of tools.
 */
export class ToolRegistry {
  private tools = new Map<string, AgentTool>();

  constructor(private readonly log: Logger) {}

  /** Register a tool. */
  register(tool: AgentTool): void {
    if (this.tools.has(tool.name)) {
      this.log.warn(`Tool "${tool.name}" is already registered — overwriting`);
    }
    this.tools.set(tool.name, tool);
    this.log.info(`Registered tool: ${tool.name}`);
  }

  /** Register multiple tools. */
  registerAll(tools: AgentTool[]): void {
    for (const tool of tools) {
      this.register(tool);
    }
  }

  /** Get a tool by name. */
  get(name: string): AgentTool | undefined {
    return this.tools.get(name);
  }

  /** Execute a tool by name. */
  async execute(name: string, params: Record<string, unknown>, ctx: ToolContext): Promise<ToolResult> {
    const tool = this.tools.get(name);
    if (!tool) {
      return { content: `Error: tool "${name}" not found` };
    }
    try {
      this.log.info(`Executing tool: ${name}`);
      const result = await tool.handler(params, ctx);
      this.log.info(`Tool ${name} completed`);
      return result;
    } catch (error) {
      const message = error instanceof Error ? error.message : String(error);
      this.log.error(`Tool ${name} failed: ${message}`);
      return { content: `Error executing tool "${name}": ${message}` };
    }
  }

  /** Convert registered tools to LLM tool definitions for function calling. */
  toLLMTools(): LLMToolDefinition[] {
    const definitions: LLMToolDefinition[] = [];
    for (const tool of this.tools.values()) {
      const properties: Record<string, unknown> = {};
      const required: string[] = [];
      for (const [key, param] of Object.entries(tool.parameters)) {
        properties[key] = {
          type: param.type,
          description: param.description,
          ...(param.enum ? { enum: param.enum } : {}),
        };
        if (param.required !== false) {
          required.push(key);
        }
      }
      definitions.push({
        type: "function",
        function: {
          name: tool.name,
          description: tool.description,
          parameters: {
            type: "object",
            properties,
            required,
          },
        },
      });
    }
    return definitions;
  }

  /** Get number of registered tools. */
  get size(): number {
    return this.tools.size;
  }

  /** List all tool names. */
  list(): string[] {
    return Array.from(this.tools.keys());
  }
}
