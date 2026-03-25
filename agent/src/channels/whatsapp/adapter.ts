import makeWASocket, {
  DisconnectReason,
  useMultiFileAuthState,
  fetchLatestBaileysVersion,
  Browsers,
  type WASocket,
  type proto,
} from "@whiskeysockets/baileys";
import pino from "pino";
import qrcode from "qrcode-terminal";
import type { Logger } from "../../utils/logger.js";
import type { ChannelAdapter, ChannelConfig, InboundHandler, OutboundMessage } from "../types.js";

/**
 * WhatsApp channel adapter using Baileys (multi-device).
 */
export class WhatsAppAdapter implements ChannelAdapter {
  readonly id = "whatsapp";
  readonly name = "WhatsApp";

  private socket: WASocket | null = null;
  private handler: InboundHandler | null = null;
  private authDir = ".data/whatsapp-auth";

  constructor(private readonly log: Logger) { }

  onMessage(handler: InboundHandler): void {
    this.handler = handler;
  }

  async start(config: ChannelConfig): Promise<void> {
    if (config.authDir) {
      this.authDir = config.authDir as string;
    }

    const { state, saveCreds } = await useMultiFileAuthState(this.authDir);
    const { version } = await fetchLatestBaileysVersion();

    this.socket = makeWASocket({
      auth: state,
      version,
      browser: Browsers.macOS("Desktop"),
      logger: pino({ level: "silent" }) as any,
    });

    this.socket.ev.on("creds.update", saveCreds);

    this.socket.ev.on("connection.update", (update) => {
      const { connection, lastDisconnect, qr } = update;

      if (qr) {
        qrcode.generate(qr, { small: true });
        this.log.info("Scan the QR code above to connect WhatsApp");
      }

      if (connection === "close") {
        const statusCode = (lastDisconnect?.error as any)?.output?.statusCode;
        const shouldReconnect = statusCode !== DisconnectReason.loggedOut;
        this.log.warn(`WhatsApp disconnected (code: ${statusCode}). Reconnect: ${shouldReconnect}`);
        if (shouldReconnect) {
          this.start(config);
        }
      } else if (connection === "open") {
        this.log.info("WhatsApp connected successfully");
      }
    });

    this.socket.ev.on("messages.upsert", (m) => {
      for (const msg of m.messages) {
        if (!msg.message || msg.key.fromMe) continue;

        const text = this.extractText(msg);
        if (!text) continue;

        const conversationId = msg.key.remoteJid ?? "unknown";
        const senderId = msg.key.participant ?? msg.key.remoteJid ?? "unknown";

        this.handler?.({
          channelId: this.id,
          conversationId,
          senderId,
          text,
          timestamp: (msg.messageTimestamp as number) * 1000 || Date.now(),
        });
      }
    });

    this.log.info("WhatsApp adapter started — scan QR code if prompted");
  }

  async stop(): Promise<void> {
    this.socket?.end(undefined);
    this.socket = null;
    this.log.info("WhatsApp adapter stopped");
  }

  async sendMessage(to: string, payload: OutboundMessage): Promise<void> {
    if (!this.socket) {
      this.log.error("WhatsApp: cannot send — not connected");
      return;
    }

    if (payload.file) {
      await this.socket.sendMessage(to, {
        document: payload.file.data,
        mimetype: payload.file.mimeType,
        fileName: payload.file.name,
        caption: payload.text,
      });
    } else {
      await this.socket.sendMessage(to, { text: payload.text });
    }
  }

  private extractText(msg: proto.IWebMessageInfo): string | null {
    const m = msg.message;
    if (!m) return null;
    return (
      m.conversation ??
      m.extendedTextMessage?.text ??
      m.imageMessage?.caption ??
      m.videoMessage?.caption ??
      null
    );
  }
}
