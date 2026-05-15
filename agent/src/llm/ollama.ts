import type { LLMConfig } from "../config/schema.js";
import type { Logger } from "../utils/logger.js";
import type {
  LLMCompletionRequest,
  LLMCompletionResponse,
  LLMProvider,
  LLMToolCall,
} from "./types.js";

interface ApiKeyStatus {
  key: string;
  rateLimitedUntil: number;
}


/**
 * Ollama LLM provider — communicates via the Ollama REST API.
 * Supports tool calling via the /api/chat endpoint.
 */
export class OllamaProvider implements LLMProvider {
  readonly id = "ollama";
  readonly name = "Ollama";
  private baseUrl: string;
  private model: string;
  private keys: ApiKeyStatus[] = [];
  private defaultTemp: number;
  private defaultMaxTokens: number;

  constructor(config: LLMConfig, private readonly log: Logger) {
    this.baseUrl = config.baseUrl.replace(/\/+$/, "");
    this.model = config.model;
    
    if (config.apiKey) {
      if (Array.isArray(config.apiKey)) {
        this.keys = config.apiKey.map(k => ({ key: k, rateLimitedUntil: 0 }));
      } else {
        this.keys = [{ key: config.apiKey, rateLimitedUntil: 0 }];
      }
    }
    
    this.defaultTemp = config.temperature;
    this.defaultMaxTokens = config.maxTokens;
    this.log.info(`Ollama provider initialized: ${this.model} @ ${this.baseUrl} (Keys available: ${this.keys.length})`);
  }

  private getActiveKey(): string | undefined {
    if (this.keys.length === 0) return undefined;
    const now = Date.now();
    for (const k of this.keys) {
      if (k.rateLimitedUntil < now) {
        return k.key;
      }
    }
    return undefined; // All keys are exhausted
  }

  private markKeyRateLimited(keyStr: string) {
    const k = this.keys.find(x => x.key === keyStr);
    if (k) {
      const freezeTimeHours = 1;
      k.rateLimitedUntil = Date.now() + freezeTimeHours * 60 * 60 * 1000;
      this.log.warn(`Ollama API key (ends with ...${keyStr.slice(-4)}) reached rate limit. Freezing for ${freezeTimeHours} hour(s).`);
    }
  }


  async complete(request: LLMCompletionRequest): Promise<LLMCompletionResponse> {
    const body: Record<string, unknown> = {
      model: this.model,
      messages: request.messages.map((m) => {
        const msg: Record<string, unknown> = { role: m.role, content: m.content };
        if (m.tool_call_id) {
          msg.tool_call_id = m.tool_call_id;
        }
        // Preservar tool_calls en mensajes de assistant para que el LLM
        // sepa qué herramientas ya invocó y no las repita en bucle.
        if (m.tool_calls && m.tool_calls.length > 0) {
          msg.tool_calls = m.tool_calls.map((tc) => ({
            function: {
              name: tc.function.name,
              arguments: typeof tc.function.arguments === "string"
                ? JSON.parse(tc.function.arguments)
                : tc.function.arguments,
            },
          }));
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

    const maxAttempts = this.keys.length > 0 ? this.keys.length : 1;
    let attempt = 0;

    while (attempt < maxAttempts) {
      attempt++;
      const currentKey = this.getActiveKey();

      if (this.keys.length > 0 && !currentKey) {
        throw new Error("All Ollama API keys are currently rate-limited (429). Please try again later.");
      }

      const headers: Record<string, string> = {
        "Content-Type": "application/json",
      };

      if (currentKey) {
        headers["Authorization"] = `Bearer ${currentKey}`;
      }

      const response = await fetch(`${this.baseUrl}/api/chat`, {
        method: "POST",
        headers,
        body: JSON.stringify(body),
      });

      if (!response.ok) {
        if (response.status === 429 && currentKey) {
          this.markKeyRateLimited(currentKey);
          if (attempt < maxAttempts) {
            this.log.info(`Retrying Ollama request with next available key (Attempt ${attempt + 1}/${maxAttempts})...`);
            continue; // Intentar con la siguiente key
          }
        }
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
    } // End of while loop

    throw new Error("Ollama API failed after exhausting available keys.");
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
