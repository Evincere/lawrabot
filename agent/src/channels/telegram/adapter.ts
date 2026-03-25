import { Bot, InputFile, type Context } from "grammy";
import type { Logger } from "../../utils/logger.js";
import type { ChannelAdapter, ChannelConfig, InboundHandler, OutboundMessage } from "../types.js";

/**
 * Telegram channel adapter using grammY.
 */
export class TelegramAdapter implements ChannelAdapter {
  readonly id = "telegram";
  readonly name = "Telegram";

  private bot: Bot | null = null;
  private handler: InboundHandler | null = null;

  constructor(private readonly log: Logger) {}

  onMessage(handler: InboundHandler): void {
    this.handler = handler;
  }

  async start(config: ChannelConfig): Promise<void> {
    const token = config.botToken as string;
    if (!token) {
      throw new Error("Telegram bot token is required (telegram.botToken)");
    }

    const allowedChatIds = (config.allowedChatIds as number[]) ?? [];
    this.bot = new Bot(token);

    this.bot.on("message:text", async (ctx: Context) => {
      const chatId = ctx.chat?.id;
      const senderId = ctx.from?.id;
      const text = ctx.message?.text;

      if (!chatId || !text) return;

      // Enforce allowlist if configured
      if (allowedChatIds.length > 0 && !allowedChatIds.includes(chatId)) {
        this.log.warn(`Telegram: ignored message from unauthorized chat ${chatId}`);
        return;
      }

      this.handler?.({
        channelId: this.id,
        conversationId: String(chatId),
        senderId: String(senderId ?? chatId),
        text,
        timestamp: (ctx.message?.date ?? 0) * 1000 || Date.now(),
      });
    });

    this.bot.start();
    this.log.info("Telegram adapter started");
  }

  async stop(): Promise<void> {
    this.bot?.stop();
    this.bot = null;
    this.log.info("Telegram adapter stopped");
  }

  async sendMessage(to: string, payload: OutboundMessage): Promise<void> {
    if (!this.bot) {
      this.log.error("Telegram: cannot send — bot not started");
      return;
    }

    const chatId = Number(to);
    if (Number.isNaN(chatId)) {
      this.log.error(`Telegram: invalid chat ID "${to}"`);
      return;
    }

    if (payload.file) {
      // Send as document
      await this.bot.api.sendDocument(chatId, new InputFile(payload.file.data, payload.file.name), {
        caption: payload.text,
      });
    } else {
      await this.bot.api.sendMessage(chatId, payload.text, { parse_mode: "Markdown" });
    }
  }
}
