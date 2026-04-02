/** Channel adapter interface — implemented by each messaging platform. */

export interface InboundMessage {
  channelId: string;
  conversationId: string;
  senderId: string;
  text: string;
  timestamp: number;
  replyTo?: string;
  metadata?: Record<string, unknown>;
}

export interface OutboundMessage {
  text: string;
  file?: {
    name: string;
    data: Buffer;
    mimeType: string;
  };
}

export interface ChannelConfig {
  [key: string]: unknown;
}

export type InboundHandler = (message: InboundMessage) => void | Promise<void>;

export interface ChannelAdapter {
  /** Unique channel identifier (e.g., "whatsapp", "telegram"). */
  id: string;
  /** Human-readable channel name. */
  name: string;
  /** Start the channel adapter. */
  start(config: ChannelConfig): Promise<void>;
  /** Stop the channel adapter gracefully. */
  stop(): Promise<void>;
  /** Send a message to a user/conversation. */
  sendMessage(to: string, payload: OutboundMessage): Promise<void>;
  /** Register an inbound message handler. */
  onMessage(handler: InboundHandler): void;
  /** Send a presence update (e.g., "typing", "recording"). */
  sendPresence?(to: string, type: "typing" | "recording" | "available" | "paused"): Promise<void>;
}
