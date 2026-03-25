import fs from "node:fs";
import path from "node:path";
import JSON5 from "json5";
import { AppConfigSchema, type AppConfig } from "./schema.js";

export function loadConfig(specDir: string): AppConfig {
  const configPath = path.join(specDir, "agent.config.json5");
  if (!fs.existsSync(configPath)) {
    // Return defaults if no config file provided
    return AppConfigSchema.parse({});
  }

  const raw = fs.readFileSync(configPath, "utf-8");
  const parsed = JSON5.parse(raw) as unknown;
  const result = AppConfigSchema.safeParse(parsed);

  if (!result.success) {
    const issues = result.error.issues
      .map((i) => `  - ${i.path.join(".")}: ${i.message}`)
      .join("\n");
    throw new Error(`Invalid config at ${configPath}:\n${issues}`);
  }

  return result.data;
}

export function mergeConfigs(base: AppConfig, overrides: Partial<AppConfig>): AppConfig {
  return AppConfigSchema.parse({ ...base, ...overrides });
}
