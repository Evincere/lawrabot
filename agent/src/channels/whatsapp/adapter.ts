import pkg from "@whiskeysockets/baileys";
const makeWASocket = (pkg as any).default || pkg;
const {
  DisconnectReason,
  useMultiFileAuthState,
  fetchLatestBaileysVersion,
  Browsers,
  downloadMediaMessage,
  getContentType,
} = pkg as any;

import type { WASocket, proto } from "@whiskeysockets/baileys";
import pino from "pino";
import qrcode from "qrcode-terminal";
import * as fs from "fs";
import * as path from "path";
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

    const sock = makeWASocket({
      auth: state,
      version,
      browser: Browsers.macOS("Desktop"),
      logger: pino({ level: "silent" }) as any,
    });
    this.socket = sock;

    sock.ev.on("creds.update", saveCreds);

    sock.ev.on("connection.update", (update: any) => {
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

    sock.ev.on("messages.upsert", async (m: any) => {
      for (const msg of m.messages) {
        this.log.info(`[whatsapp] raw message: ${JSON.stringify({ key: msg.key, message: msg.message })}`);
        
        if (!msg.message || msg.key.fromMe) {
          this.log.debug(`[whatsapp] Skipping message: no message body or fromMe=true`);
          continue;
        }

        // Mark message as read (blue checks)
        sock.readMessages([msg.key]).catch((err: any) => {
          this.log.error(`Failed to mark message as read: ${err.message}`);
        });

        const text = this.extractText(msg);
        const mediaInfo = await this.processMediaIfPresent(msg);
        
        this.log.info(`[whatsapp] extracted text: "${text}", media: ${!!mediaInfo}`);

        // Skip if there's no text AND no media
        if (!text && !mediaInfo) {
          this.log.warn(`[whatsapp] Skipping message: text and mediaInfo are null`);
          continue;
        }

        // Construct final message text with metadata if media exists
        let finalText = text ?? "";
        if (mediaInfo) {
          finalText += `\n\n[MEDIA] localPath=${mediaInfo.localPath} fileName=${mediaInfo.fileName} mimeType=${mediaInfo.mimeType} [/MEDIA]`;
        }

        const conversationId = msg.key.remoteJid ?? "unknown";
        const senderId = msg.key.participant ?? msg.key.remoteJid ?? "unknown";

        this.log.info(`[whatsapp] Routing message from ${senderId} to conversation ${conversationId}`);
        
        this.handler?.({
          channelId: this.id,
          conversationId,
          senderId,
          text: finalText.trim(),
          timestamp: (msg.messageTimestamp as number) * 1000 || Date.now(),
        });
      }
    });

    this.log.info("WhatsApp adapter started — scan QR code if prompted");
  }

  private async processMediaIfPresent(msg: proto.IWebMessageInfo): Promise<{ localPath: string; fileName: string; mimeType: string } | null> {
    let m = msg.message!;
    if (m.ephemeralMessage?.message) m = m.ephemeralMessage.message;
    if (m.viewOnceMessage?.message) m = m.viewOnceMessage.message;
    if (m.viewOnceMessageV2?.message) m = m.viewOnceMessageV2.message;

    const type = getContentType(m);
    if (type !== "imageMessage" && type !== "documentMessage" && type !== "videoMessage" && type !== "documentWithCaptionMessage") {
      return null;
    }

    try {
      const buffer = await downloadMediaMessage(msg, "buffer", {});
      const mediaDir = path.join(".data", "media");
      if (!fs.existsSync(mediaDir)) {
        fs.mkdirSync(mediaDir, { recursive: true });
      }

      const document = m.documentMessage || m.imageMessage || m.videoMessage || m.documentWithCaptionMessage?.message?.documentMessage;
      const fileName = ((document as any)?.fileName) || 
                       (type === "imageMessage" ? `img_${Date.now()}.jpg` : (type === "videoMessage" ? `vid_${Date.now()}.mp4` : `doc_${Date.now()}.pdf`));
      const mimeType = (document as any)?.mimetype || (type === "imageMessage" ? "image/jpeg" : (type === "videoMessage" ? "video/mp4" : "application/pdf"));
      
      const safeFileName = `${Date.now()}_${fileName.replace(/[^a-zA-Z0-9.-]/g, "_")}`;
      const localPath = path.resolve(path.join(mediaDir, safeFileName));

      fs.writeFileSync(localPath, buffer as Buffer);
      this.log.info(`[whatsapp] Downloaded media to ${localPath}`);

      return {
        localPath,
        fileName,
        mimeType
      };
    } catch (err: any) {
      this.log.error(`[whatsapp] Error downloading media: ${err.message}`);
      return null;
    }
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

    const formattedText = this.formatForWhatsApp(payload.text);

    if (payload.file) {
      await this.socket.sendMessage(to, {
        document: payload.file.data,
        mimetype: payload.file.mimeType,
        fileName: payload.file.name,
        caption: formattedText,
      });
    } else {
      await this.socket.sendMessage(to, { text: formattedText });
    }
  }

  private formatForWhatsApp(text: string): string {
    return text
      // Convert Markdown bold to WhatsApp bold
      .replace(/\*\*/g, "*")
      // Convert level 1 headers to UPPERCASE bold
      .replace(/^#\s+(.+)$/gm, (_, p1) => `*${p1.toUpperCase().trim()}*`)
      // Convert level 2 headers to UPPERCASE bold
      .replace(/^##\s+(.+)$/gm, (_, p1) => `*${p1.toUpperCase().trim()}*`)
      // Convert level 3+ headers to bold (preserving case)
      .replace(/^###+\s+(.+)$/gm, (_, p1) => `*${p1.trim()}*`)
      // Clean up any redundant multiple asterisks
      .replace(/\*\*+/g, "*")
      .trim();
  }

  async sendPresence(to: string, type: "typing" | "recording" | "available" | "paused"): Promise<void> {
    if (!this.socket) return;

    const presenceMap: Record<string, any> = {
      typing: "composing",
      recording: "recording",
      available: "available",
      paused: "paused",
    };

    const baileyType = presenceMap[type] || "composing";
    await this.socket.sendPresenceUpdate(baileyType, to);
  }

  private extractText(msg: proto.IWebMessageInfo): string | null {
    let m = msg.message;
    if (!m) return null;
    
    if (m.ephemeralMessage?.message) {
      m = m.ephemeralMessage.message;
    } else if (m.viewOnceMessage?.message) {
      m = m.viewOnceMessage.message;
    } else if (m.viewOnceMessageV2?.message) {
      m = m.viewOnceMessageV2.message;
    } else if (m.viewOnceMessageV2Extension?.message) {
      m = m.viewOnceMessageV2Extension.message;
    } else if (m.documentWithCaptionMessage?.message) {
      m = m.documentWithCaptionMessage.message;
    }

    return (
      m.conversation ||
      m.extendedTextMessage?.text ||
      m.imageMessage?.caption ||
      m.videoMessage?.caption ||
      m.documentMessage?.caption ||
      null
    );
  }
}
