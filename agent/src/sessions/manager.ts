import type { Logger } from "../utils/logger.js";
import { SessionStore } from "./store.js";
import type { Session, SessionMessage, ChatMessage } from "./types.js";

/**
 * Manages active sessions with per-conversation isolation.
 * Sessions are keyed by `channelId:conversationId`.
 */
export class SessionManager {
  private sessions = new Map<string, Session>();
  private store: SessionStore;

  constructor(
    private readonly persistDir: string,
    private readonly maxHistory: number,
    private readonly log: Logger,
  ) {
    this.store = new SessionStore(persistDir);
  }

  /** Get or create a session for a channel+conversation pair. */
  getOrCreate(channelId: string, conversationId: string): Session {
    const key = this.makeKey(channelId, conversationId);
    let session = this.sessions.get(key);
    if (!session) {
      // Try to restore from disk
      const savedMessages = this.store.loadMessages(channelId, conversationId);
      session = {
        id: key,
        channelId,
        conversationId,
        messages: savedMessages,
        createdAt: Date.now(),
        updatedAt: Date.now(),
      };
      this.sessions.set(key, session);
      if (savedMessages.length > 0) {
        this.log.info(`Restored session ${key} with ${savedMessages.length} messages`);
      }
    }
    return session;
  }

  /** Add a message to a session and persist it. */
  addMessage(session: Session, message: SessionMessage): void {
    session.messages.push(message);
    session.updatedAt = Date.now();
    this.store.appendMessage(session, message);

    // Trim in-memory history if too long (keep system messages)
    if (session.messages.length > this.maxHistory) {
      const systemMsgs = session.messages.filter((m) => m.role === "system");
      const nonSystem = session.messages.filter((m) => m.role !== "system");
      session.messages = [...systemMsgs, ...nonSystem.slice(-this.maxHistory)];
    }
  }

  /** Get messages formatted for LLM context. */
  getContextMessages(session: Session): Array<{ role: "user" | "assistant" | "system" | "tool"; content: string; tool_call_id?: string }> {
    return session.messages.map((msg) => ({
      role: msg.role,
      content: msg.content,
      tool_call_id: msg.role === "tool" ? (msg as any).toolCallId : undefined,
    }));
  }

  /** Reset a session (clear history). */
  reset(channelId: string, conversationId: string): void {
    const key = this.makeKey(channelId, conversationId);
    this.sessions.delete(key);
    this.store.deleteSession(channelId, conversationId);
    this.log.info(`Reset session ${key}`);
  }

  private makeKey(channelId: string, conversationId: string): string {
    return `${channelId}:${conversationId}`;
  }
}
