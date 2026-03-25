import fs from "node:fs";
import path from "node:path";
import type { Session, SessionMessage } from "./types.js";

/**
 * Persists session messages to JSONL files for conversation history.
 * One file per conversation: <persistDir>/<channelId>/<conversationId>.jsonl
 */
export class SessionStore {
  constructor(private readonly persistDir: string) {}

  /** Append a message to the session's JSONL file. */
  appendMessage(session: Session, message: SessionMessage): void {
    const filePath = this.resolveFilePath(session);
    fs.mkdirSync(path.dirname(filePath), { recursive: true });
    const line = JSON.stringify(message) + "\n";
    fs.appendFileSync(filePath, line, "utf-8");
  }

  /** Load all messages from a session's JSONL file. */
  loadMessages(channelId: string, conversationId: string): SessionMessage[] {
    const filePath = this.resolveFilePathRaw(channelId, conversationId);
    if (!fs.existsSync(filePath)) {
      return [];
    }
    const content = fs.readFileSync(filePath, "utf-8");
    const lines = content.trim().split("\n").filter(Boolean);
    const messages: SessionMessage[] = [];
    for (const line of lines) {
      try {
        messages.push(JSON.parse(line) as SessionMessage);
      } catch {
        // Skip malformed lines
      }
    }
    return messages;
  }

  /** Delete a session's conversation file. */
  deleteSession(channelId: string, conversationId: string): void {
    const filePath = this.resolveFilePathRaw(channelId, conversationId);
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
    }
  }

  private resolveFilePath(session: Session): string {
    return this.resolveFilePathRaw(session.channelId, session.conversationId);
  }

  private resolveFilePathRaw(channelId: string, conversationId: string): string {
    // Sanitize to prevent path traversal
    const safeChannel = channelId.replace(/[^a-zA-Z0-9_-]/g, "_");
    const safeConvo = conversationId.replace(/[^a-zA-Z0-9_-]/g, "_");
    return path.join(this.persistDir, safeChannel, `${safeConvo}.jsonl`);
  }
}
