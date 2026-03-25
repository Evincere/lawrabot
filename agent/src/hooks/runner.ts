import type { Logger } from "../utils/logger.js";
import type { HookHandler, HookName, HookEvent, HookRegistration } from "./types.js";

/**
 * Priority-based hook runner.
 * Hooks with higher priority run first. Same priority runs in registration order.
 */
export class HookRunner {
  private hooks: HookRegistration[] = [];

  constructor(private readonly log: Logger) {}

  /** Register a hook handler. */
  register(hookName: HookName, handler: HookHandler, opts?: { priority?: number; source?: string }): void {
    this.hooks.push({
      hookName,
      handler,
      priority: opts?.priority ?? 0,
      source: opts?.source ?? "unknown",
    });
    // Sort by priority descending
    this.hooks.sort((a, b) => (b.priority ?? 0) - (a.priority ?? 0));
    this.log.debug(`Hook registered: ${hookName} from ${opts?.source ?? "unknown"}`);
  }

  /** Run all handlers for a given hook. */
  async run(hookName: HookName, data: Record<string, unknown> = {}): Promise<void> {
    const event: HookEvent = { hookName, data, timestamp: Date.now() };
    const handlers = this.hooks.filter((h) => h.hookName === hookName);

    await Promise.allSettled(
      handlers.map(async (reg) => {
        try {
          await reg.handler(event);
        } catch (error) {
          const msg = error instanceof Error ? error.message : String(error);
          this.log.error(`Hook ${hookName} from ${reg.source} failed: ${msg}`);
        }
      }),
    );
  }

  /** Get count of hooks for a given event. */
  count(hookName?: HookName): number {
    if (hookName) {
      return this.hooks.filter((h) => h.hookName === hookName).length;
    }
    return this.hooks.length;
  }
}
