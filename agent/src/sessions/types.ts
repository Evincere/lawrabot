import type { LLMToolCall } from "../llm/types.js";

export type MessageRole = "user" | "assistant" | "system" | "tool";

export interface ChatMessage {
  role: "user" | "assistant" | "system";
  content: string;
  timestamp: number;
  toolCalls?: LLMToolCall[];
  metadata?: Record<string, unknown>;
}

export interface ToolCallMessage {
  role: "tool";
  toolCallId: string;
  content: string;
  timestamp: number;
}

export type SessionMessage = ChatMessage | ToolCallMessage;

export interface Session {
  id: string;
  channelId: string;
  conversationId: string;
  messages: SessionMessage[];
  createdAt: number;
  updatedAt: number;
  metadata?: Record<string, unknown>;
}
