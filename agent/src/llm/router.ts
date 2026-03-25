import type { LLMConfig } from "../config/schema.js";
import type { Logger } from "../utils/logger.js";
import { OllamaProvider } from "./ollama.js";
import type { LLMProvider } from "./types.js";

export function createLLMProvider(config: LLMConfig, log: Logger): LLMProvider {
  switch (config.provider) {
    case "ollama":
      return new OllamaProvider(config, log);
    default:
      throw new Error(`Unsupported LLM provider: ${config.provider}`);
  }
}
