/** Types for LLM provider abstraction. */

export interface LLMMessage {
  role: "system" | "user" | "assistant" | "tool";
  content: string;
  tool_calls?: LLMToolCall[];
  tool_call_id?: string;
}

export interface LLMToolCall {
  id: string;
  type: "function";
  function: {
    name: string;
    arguments: string;
  };
}

export interface LLMToolDefinition {
  type: "function";
  function: {
    name: string;
    description: string;
    parameters: Record<string, unknown>;
  };
}

export interface LLMCompletionRequest {
  messages: LLMMessage[];
  tools?: LLMToolDefinition[];
  temperature?: number;
  maxTokens?: number;
}

export interface LLMCompletionResponse {
  content: string | null;
  toolCalls: LLMToolCall[];
  usage?: { promptTokens: number; completionTokens: number; totalTokens: number };
  finishReason: "stop" | "tool_calls" | "length" | "error";
}

export interface LLMProvider {
  id: string;
  name: string;
  complete(request: LLMCompletionRequest): Promise<LLMCompletionResponse>;
}
