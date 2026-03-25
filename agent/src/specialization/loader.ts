import fs from "node:fs";
import path from "node:path";
import { createJiti } from "jiti";
import type { Logger } from "../utils/logger.js";
import type { AgentTool } from "../tools/types.js";

export interface Specialization {
  name: string;
  description: string;
  systemPrompt: string;
  tools: AgentTool[];
}

/**
 * Load a specialization from a directory.
 * Expects: agent.config.json5, prompts/system.md, tools/*.ts
 */
export async function loadSpecialization(specDir: string, log: Logger): Promise<Specialization> {
  const absDir = path.resolve(specDir);
  if (!fs.existsSync(absDir)) {
    throw new Error(`Specialization directory not found: ${absDir}`);
  }

  // 1. Load system prompt
  const promptPath = path.join(absDir, "prompts", "system.md");
  let systemPrompt = "You are a helpful AI assistant.";
  if (fs.existsSync(promptPath)) {
    systemPrompt = fs.readFileSync(promptPath, "utf-8").trim();
    log.info(`Loaded system prompt from ${promptPath} (${systemPrompt.length} chars)`);
  } else {
    log.warn(`No system prompt found at ${promptPath} — using default`);
  }

  // 2. Discover and load domain tools
  const toolsDir = path.join(absDir, "tools");
  const tools: AgentTool[] = [];
  if (fs.existsSync(toolsDir)) {
    const jiti = createJiti(absDir, { interopDefault: true });
    const files = fs.readdirSync(toolsDir).filter((f) => f.endsWith(".ts") || f.endsWith(".js"));
    for (const file of files) {
      try {
        const toolPath = path.join(toolsDir, file);
        const mod = (await jiti.import(toolPath)) as Record<string, unknown>;
        const tool = (mod.default ?? mod) as AgentTool;
        if (tool.name && typeof tool.handler === "function") {
          tools.push(tool);
          log.info(`Loaded domain tool: ${tool.name} from ${file}`);
        } else {
          log.warn(`Skipping ${file}: no valid tool export (need name + handler)`);
        }
      } catch (error) {
        const msg = error instanceof Error ? error.message : String(error);
        log.error(`Failed to load tool ${file}: ${msg}`);
      }
    }
  }

  return {
    name: "Specialized Agent",
    description: "",
    systemPrompt,
    tools,
  };
}
