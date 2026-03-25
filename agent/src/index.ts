#!/usr/bin/env node
import { Command } from "commander";
import path from "node:path";
import { loadConfig } from "./config/loader.js";
import { createLogger } from "./utils/logger.js";
import { startGateway } from "./gateway/server.js";

const program = new Command();

program
  .name("templateclaw")
  .description("TemplateClaw — Reusable AI Agent Template")
  .version("0.1.0");

program
  .command("start")
  .description("Start the agent with a specialization")
  .requiredOption("--spec <path>", "Path to the specialization directory")
  .option("--port <number>", "Override HTTP port")
  .action(async (opts: { spec: string; port?: string }) => {
    const log = createLogger("templateclaw");
    const specDir = path.resolve(opts.spec);

    try {
      // Load config from spec directory
      const config = loadConfig(specDir);

      // Apply CLI overrides
      if (opts.port) {
        config.port = Number(opts.port);
      }

      log.info(`Specialization: ${specDir}`);

      // Start the gateway
      const gateway = await startGateway({ config, specDir, log });

      // Handle shutdown signals
      const shutdown = async () => {
        log.info("Received shutdown signal");
        await gateway.close();
        process.exit(0);
      };

      process.on("SIGINT", shutdown);
      process.on("SIGTERM", shutdown);
    } catch (error) {
      const msg = error instanceof Error ? error.message : String(error);
      log.error(`Failed to start: ${msg}`);
      process.exit(1);
    }
  });

program
  .command("validate")
  .description("Validate a specialization directory without starting")
  .requiredOption("--spec <path>", "Path to the specialization directory")
  .action(async (opts: { spec: string }) => {
    const log = createLogger("templateclaw");
    const specDir = path.resolve(opts.spec);

    try {
      const config = loadConfig(specDir);
      log.info(`Config valid: ${config.name}`);

      const { loadSpecialization } = await import("./specialization/loader.js");
      const spec = await loadSpecialization(specDir, log);
      log.info(`Specialization valid: ${spec.tools.length} tools loaded`);
      log.info("✅ All checks passed");
    } catch (error) {
      const msg = error instanceof Error ? error.message : String(error);
      log.error(`Validation failed: ${msg}`);
      process.exit(1);
    }
  });

program.parse();
