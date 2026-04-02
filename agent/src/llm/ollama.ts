import type { LLMConfig } from "../config/schema.js";
import type { Logger } from "../utils/logger.js";
import type {
  LLMCompletionRequest,
  LLMCompletionResponse,
  LLMProvider,
  LLMToolCall,
} from "./types.js";

/**
 * Ollama LLM provider — communicates via the Ollama REST API.
 * Supports tool calling via the /api/chat endpoint.
 */
export class OllamaProvider implements LLMProvider {
  readonly id = "ollama";
  readonly name = "Ollama";
  private baseUrl: string;
  private model: string;
  private apiKey?: string;
  private defaultTemp: number;
  private defaultMaxTokens: number;

  constructor(config: LLMConfig, private readonly log: Logger) {
    this.baseUrl = config.baseUrl.replace(/\/+$/, "");
    this.model = config.model;
    this.apiKey = config.apiKey;
    this.defaultTemp = config.temperature;
    this.defaultMaxTokens = config.maxTokens;
    this.log.info(`Ollama provider initialized: ${this.model} @ ${this.baseUrl} (Auth: ${this.apiKey ? "YES" : "NO"})`);
  }

  async complete(request: LLMCompletionRequest): Promise<LLMCompletionResponse> {
    const body: Record<string, unknown> = {
      model: this.model,
      messages: request.messages.map((m) => {
        const msg: Record<string, unknown> = { role: m.role, content: m.content };
        if (m.tool_call_id) {
          msg.tool_call_id = m.tool_call_id;
        }
        return msg;
      }),
      stream: false,
      options: {
        temperature: request.temperature ?? this.defaultTemp,
        num_predict: request.maxTokens ?? this.defaultMaxTokens,
      },
    };

    if (request.tools && request.tools.length > 0) {
      body.tools = request.tools;
    }

    this.log.debug(`Sending request to Ollama: ${this.model}`);

    const headers: Record<string, string> = {
      "Content-Type": "application/json",
    };

    if (this.apiKey) {
      headers["Authorization"] = `Bearer ${this.apiKey}`;
    }

    const response = await fetch(`${this.baseUrl}/api/chat`, {
      method: "POST",
      headers,
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      const errorText = await response.text().catch(() => "unknown error");
      throw new Error(`Ollama API error ${response.status}: ${errorText}`);
    }

    const data = (await response.json()) as OllamaChatResponse;

    const toolCalls: LLMToolCall[] = [];
    if (data.message?.tool_calls) {
      for (const tc of data.message.tool_calls) {
        toolCalls.push({
          id: `call_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
          type: "function",
          function: {
            name: tc.function.name,
            arguments: JSON.stringify(tc.function.arguments),
          },
        });
      }
    }

    const hasToolCalls = toolCalls.length > 0;

    return {
      content: data.message?.content ?? null,
      toolCalls,
      usage: data.eval_count
        ? {
            promptTokens: data.prompt_eval_count ?? 0,
            completionTokens: data.eval_count ?? 0,
            totalTokens: (data.prompt_eval_count ?? 0) + (data.eval_count ?? 0),
          }
        : undefined,
      finishReason: hasToolCalls ? "tool_calls" : "stop",
    };
  }
}

// ─── Ollama response shape ───────────────────────────────────────
interface OllamaChatResponse {
  message?: {
    role: string;
    content: string;
    tool_calls?: Array<{
      function: { name: string; arguments: Record<string, unknown> };
    }>;
  };
  eval_count?: number;
  prompt_eval_count?: number;
}
