/** Hook system types. */

export type HookName =
  | "message_received"
  | "before_reply"
  | "after_reply"
  | "session_start"
  | "session_end"
  | "tool_called"
  | "error";

export interface HookEvent {
  hookName: HookName;
  data: Record<string, unknown>;
  timestamp: number;
}

export type HookHandler = (event: HookEvent) => void | Promise<void>;

export interface HookRegistration {
  hookName: HookName;
  handler: HookHandler;
  priority?: number;
  source: string;
}
