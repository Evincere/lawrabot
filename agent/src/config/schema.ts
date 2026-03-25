import { z } from "zod";

// ─── Channel Config ──────────────────────────────────────────────
export const WhatsAppConfigSchema = z.object({
  enabled: z.boolean().default(false),
  phoneNumber: z.string().optional(),
  authDir: z.string().default(".data/whatsapp-auth"),
});

export const TelegramConfigSchema = z.object({
  enabled: z.boolean().default(false),
  botToken: z.string().optional(),
  allowedChatIds: z.array(z.number()).default([]),
});

// ─── LLM Config ──────────────────────────────────────────────────
export const LLMConfigSchema = z.object({
  provider: z.enum(["ollama"]).default("ollama"),
  model: z.string().default("qwen3.5:cloud"),
  baseUrl: z.string().default("http://localhost:11434"),
  temperature: z.number().min(0).max(2).default(0.7),
  maxTokens: z.number().positive().default(4096),
});

// ─── Sessions Config ─────────────────────────────────────────────
export const SessionsConfigSchema = z.object({
  persistDir: z.string().default(".data/sessions"),
  maxHistoryMessages: z.number().positive().default(50),
});

// ─── Root Config ─────────────────────────────────────────────────
export const AppConfigSchema = z.object({
  name: z.string().default("TemplateClaw Agent"),
  description: z.string().default("An AI-powered assistant"),
  port: z.number().default(18789),
  whatsapp: WhatsAppConfigSchema.default({}),
  telegram: TelegramConfigSchema.default({}),
  llm: LLMConfigSchema.default({}),
  sessions: SessionsConfigSchema.default({}),
});

export type AppConfig = z.infer<typeof AppConfigSchema>;
export type WhatsAppConfig = z.infer<typeof WhatsAppConfigSchema>;
export type TelegramConfig = z.infer<typeof TelegramConfigSchema>;
export type LLMConfig = z.infer<typeof LLMConfigSchema>;
export type SessionsConfig = z.infer<typeof SessionsConfigSchema>;
