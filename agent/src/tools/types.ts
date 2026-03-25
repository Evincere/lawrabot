/** Types for the tool (function calling) system. */

export interface ToolParameter {
  type: "string" | "number" | "boolean" | "object" | "array";
  description: string;
  required?: boolean;
  enum?: string[];
  items?: ToolParameter;
  properties?: Record<string, ToolParameter>;
}

export interface ToolContext {
  sessionId: string;
  channelId: string;
  conversationId: string;
  senderId?: string;
  config: Record<string, unknown>;
}

export interface ToolResult {
  /** Text result to feed back to the LLM. */
  content: string;
  /** Optional file attachment (path or buffer). */
  file?: {
    name: string;
    data: Buffer;
    mimeType: string;
  };
}

export interface AgentTool {
  name: string;
  description: string;
  parameters: Record<string, ToolParameter>;
  handler(params: Record<string, unknown>, ctx: ToolContext): Promise<ToolResult>;
}
