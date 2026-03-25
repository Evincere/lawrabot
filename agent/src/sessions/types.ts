/** Shared types for the session system. */

export type MessageRole = "user" | "assistant" | "system" | "tool";

export interface ChatMessage {
  role: MessageRole;
  content: string;
  timestamp: number;
  metadata?: Record<string, unknown>;
}

export interface ToolCallMessage {
  role: "tool";
  toolName: string;
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
