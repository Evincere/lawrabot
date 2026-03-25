import type { Logger } from "../utils/logger.js";
import type { SessionManager } from "../sessions/manager.js";
import type { ToolRegistry } from "../tools/registry.js";
import type { HookRunner } from "../hooks/runner.js";
import type { LLMProvider, LLMMessage } from "../llm/types.js";
import type { InboundMessage, OutboundMessage, ChannelAdapter } from "../channels/types.js";
import type { ToolContext } from "../tools/types.js";

export interface MessageRouterDeps {
  sessionManager: SessionManager;
  toolRegistry: ToolRegistry;
  hookRunner: HookRunner;
  llmProvider: LLMProvider;
  channels: Map<string, ChannelAdapter>;
  systemPrompt: string;
  log: Logger;
}

/**
 * The message router is the brain of the agent.
 * Inbound message → session lookup → LLM call → tool execution loop → reply.
 */
export function createMessageRouter(deps: MessageRouterDeps) {
  const { sessionManager, toolRegistry, hookRunner, llmProvider, channels, systemPrompt, log } = deps;

  const MAX_TOOL_ROUNDS = 10;

  return async function handleInbound(message: InboundMessage): Promise<void> {
    const { channelId, conversationId, senderId, text } = message;

    log.info(`[${channelId}] Message from ${senderId}: ${text.slice(0, 80)}...`);

    // Fire hook
    await hookRunner.run("message_received", { channelId, conversationId, senderId, text });

    // Handle /reset command
    if (text.trim().toLowerCase() === "/reset") {
      sessionManager.reset(channelId, conversationId);
      await sendReply(channelId, conversationId, { text: "🔄 Conversation reset. Starting fresh!" });
      return;
    }

    // Get or create session
    const session = sessionManager.getOrCreate(channelId, conversationId);

    // Add user message
    sessionManager.addMessage(session, {
      role: "user",
      content: text,
      timestamp: Date.now(),
    });

    try {
      // Build LLM messages
      const llmMessages: LLMMessage[] = [
        { role: "system" as const, content: systemPrompt },
        ...sessionManager.getContextMessages(session),
      ];

      // Get tools for function calling
      const tools = toolRegistry.toLLMTools();
      const toolCtx: ToolContext = {
        sessionId: session.id,
        channelId,
        conversationId,
        senderId,
        config: {},
      };

      // Agent loop: call LLM, execute tools, repeat until text reply
      let round = 0;
      while (round < MAX_TOOL_ROUNDS) {
        round++;
        log.debug(`LLM round ${round}`);

        const response = await llmProvider.complete({
          messages: llmMessages,
          tools: tools.length > 0 ? tools : undefined,
        });

        // If the model returned tool calls, execute them and loop
        if (response.finishReason === "tool_calls" && response.toolCalls.length > 0) {
          // Add assistant message with tool calls
          llmMessages.push({
            role: "assistant",
            content: response.content ?? "",
            tool_calls: response.toolCalls,
          });

          // Execute each tool call
          for (const tc of response.toolCalls) {
            await hookRunner.run("tool_called", { toolName: tc.function.name });

            let params: Record<string, unknown>;
            try {
              params = JSON.parse(tc.function.arguments) as Record<string, unknown>;
            } catch {
              params = {};
            }

            const result = await toolRegistry.execute(tc.function.name, params, toolCtx);

            // If tool produced a file, send it alongside the text reply later
            if (result.file) {
              await sendReply(channelId, conversationId, {
                text: result.content,
                file: result.file,
              });
            }

            // Add tool result for the LLM
            llmMessages.push({
              role: "tool",
              content: result.content,
              tool_call_id: tc.id,
            });
          }

          continue; // Let the LLM process tool results
        }

        // We got a text response — send it
        const replyText = response.content ?? "I'm not sure how to respond.";

        // Persist assistant message
        sessionManager.addMessage(session, {
          role: "assistant",
          content: replyText,
          timestamp: Date.now(),
        });

        // Fire hook and send
        await hookRunner.run("before_reply", { channelId, conversationId, text: replyText });
        await sendReply(channelId, conversationId, { text: replyText });
        await hookRunner.run("after_reply", { channelId, conversationId, text: replyText });

        break;
      }

      if (round >= MAX_TOOL_ROUNDS) {
        log.warn(`Max tool rounds (${MAX_TOOL_ROUNDS}) reached for ${channelId}:${conversationId}`);
        await sendReply(channelId, conversationId, {
          text: "I've been thinking too hard. Could you rephrase your request?",
        });
      }
    } catch (error) {
      const msg = error instanceof Error ? error.message : String(error);
      log.error(`Agent error: ${msg}`);
      await hookRunner.run("error", { channelId, conversationId, error: msg });
      await sendReply(channelId, conversationId, {
        text: "Sorry, something went wrong. Please try again.",
      });
    }
  };

  async function sendReply(channelId: string, conversationId: string, payload: OutboundMessage) {
    const channel = channels.get(channelId);
    if (!channel) {
      log.error(`No channel adapter found for "${channelId}"`);
      return;
    }
    try {
      await channel.sendMessage(conversationId, payload);
    } catch (error) {
      const msg = error instanceof Error ? error.message : String(error);
      log.error(`Failed to send reply on ${channelId}: ${msg}`);
    }
  }
}
