import type { Logger } from "../utils/logger.js";
import type { SessionManager } from "../sessions/manager.js";
import type { ToolRegistry } from "../tools/registry.js";
import type { HookRunner } from "../hooks/runner.js";
import type { LLMProvider, LLMMessage } from "../llm/types.js";
import type { InboundMessage, OutboundMessage, ChannelAdapter } from "../channels/types.js";
import type { ToolContext } from "../tools/types.js";
import { getFilteredTools, buildStageHint } from "./tool-gate.js";

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
      await sendReply(channelId, conversationId, { text: "🔄 La conversación ha sido reiniciada correctamente. ¿En qué puedo asesorarte?" });
      return;
    }

    // Get or create session
    const session = sessionManager.getOrCreate(channelId, conversationId);

    // Extract the raw phone number from the WhatsApp JID (e.g., "5492634515362@s.whatsapp.net" → "5492634515362")
    const phoneFromJid = senderId.replace(/@.*$/, "");

    // Enrich the user message with sender metadata so the LLM can access the real phone number
    // instead of hallucinating it. The metadata block is prepended to the first message of each session.
    const enrichedContent = `[METADATA] phoneNumber=${phoneFromJid} [/METADATA]\n${text}`;

    // Add user message
    sessionManager.addMessage(session, {
      role: "user",
      content: enrichedContent,
      timestamp: Date.now(),
    });

    try {
      // Build LLM messages
      const llmMessages: LLMMessage[] = [
        { role: "system" as const, content: systemPrompt },
        ...sessionManager.getContextMessages(session),
      ];

      // Build tool context
      const toolCtx: ToolContext = {
        sessionId: session.id,
        channelId,
        conversationId,
        senderId,
        config: {},
      };

      // Get stage-aware filtered tools
      const { tools, stageContext } = await getFilteredTools(
        toolRegistry,
        toolCtx,
        phoneFromJid,
        log,
      );

      // If we have stage context with an active expediente, inject a hint for the LLM
      if (stageContext && stageContext.stage !== "NO_EXPEDIENTE" && stageContext.stage !== "ERROR") {
        llmMessages.push({
          role: "system" as const,
          content: buildStageHint(stageContext),
        });
      }

      // Signal "typing" to the user
      await updatePresence(channelId, conversationId, "typing");

      // Patience timer (25 seconds)
      const TYPING_TIMEOUT = 25000;
      const patienceTimer = setInterval(async () => {
        try {
          log.info(`[${channelId}] Sending patience message to ${senderId}...`);
          await sendReply(channelId, conversationId, { text: "Un momento, por favor. Estoy procesando tu solicitud..." });
          // After sending a message, the typing indicator is lost, so we restart it
          await updatePresence(channelId, conversationId, "typing");
        } catch (err) {
          log.error(`Failed to send patience message: ${err}`);
        }
      }, TYPING_TIMEOUT);

      // Agent loop: call LLM, execute tools, repeat until text reply
      let round = 0;
      // Detección de bucles: rastrear invocaciones repetidas consecutivas
      let lastToolSignature = "";
      let consecutiveDuplicates = 0;
      const MAX_CONSECUTIVE_DUPLICATES = 2;

      try {
        while (round < MAX_TOOL_ROUNDS) {
          round++;
          log.debug(`LLM round ${round}`);

          const response = await llmProvider.complete({
            messages: llmMessages,
            tools: tools.length > 0 ? tools : undefined,
          });

          // If the model returned tool calls, execute them and loop
          if (response.finishReason === "tool_calls" && response.toolCalls.length > 0) {
            // ── Detección de bucle por invocaciones duplicadas ──
            const currentSignature = response.toolCalls
              .map((tc) => `${tc.function.name}:${tc.function.arguments}`)
              .sort()
              .join("|");

            if (currentSignature === lastToolSignature) {
              consecutiveDuplicates++;
              log.warn(
                `Duplicate tool call detected (${consecutiveDuplicates}/${MAX_CONSECUTIVE_DUPLICATES}): ${response.toolCalls.map((tc) => tc.function.name).join(", ")}`,
              );
              if (consecutiveDuplicates >= MAX_CONSECUTIVE_DUPLICATES) {
                log.error(`Tool call loop detected. Breaking out after ${consecutiveDuplicates} consecutive identical invocations.`);
                // Insertar mensaje de sistema para forzar respuesta de texto
                llmMessages.push({
                  role: "user",
                  content: "[SYSTEM] Ya has ejecutado esta herramienta con los mismos parámetros. No la repitas. Responde al usuario con la información que ya obtuviste.",
                });
                continue;
              }
            } else {
              consecutiveDuplicates = 0;
            }
            lastToolSignature = currentSignature;

            // Add assistant message with tool calls to session
            sessionManager.addMessage(session, {
              role: "assistant",
              content: response.content ?? "",
              toolCalls: response.toolCalls,
              timestamp: Date.now(),
            });

            // Add to LLM messages
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

              // Add tool result to session
              sessionManager.addMessage(session, {
                role: "tool",
                toolCallId: tc.id,
                content: result.content,
                timestamp: Date.now(),
              });

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
          let replyText = response.content ?? "Lo siento, no he podido procesar una respuesta adecuada. ¿Podrías reformular tu mensaje?";

          // ── Post-procesamiento: deduplicación de bloques ──
          replyText = deduplicateBlocks(replyText);

          // ── Detección de output degenerado ──
          if (isDegenerate(replyText)) {
            log.warn(`Degenerate LLM output detected (${replyText.length} chars). Attempting recovery...`);

            // Inyectar instrucción correctiva y reintentar UNA sola vez
            llmMessages.push({
              role: "user",
              content: "[SYSTEM] Tu respuesta anterior fue inválida (output corrupto). Responde con UN SOLO párrafo breve indicando el siguiente paso del trámite. Máximo 3 oraciones. NO generes texto largo.",
            });

            const retryResponse = await llmProvider.complete({
              messages: llmMessages,
              tools: tools.length > 0 ? tools : undefined,
            });

            const retryText = retryResponse.content ?? "";
            if (!isDegenerate(retryText) && retryText.length > 10) {
              replyText = retryText;
              log.info(`Recovery successful (${retryText.length} chars).`);
            } else {
              log.error(`Recovery also degenerated. Using static fallback.`);
              replyText = "Perfecto, los datos fueron registrados. ¿Podemos continuar con el siguiente paso del trámite?";
            }
          }

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
      } finally {
        clearInterval(patienceTimer);
      }

      if (round >= MAX_TOOL_ROUNDS) {
        log.warn(`Max tool rounds (${MAX_TOOL_ROUNDS}) reached for ${channelId}:${conversationId}`);
        await sendReply(channelId, conversationId, {
          text: "He tenido dificultades para procesar esta solicitud debido a su complejidad. Por favor, intenta simplificar tu consulta.",
        });
      }
    } catch (error) {
      const msg = error instanceof Error ? error.message : String(error);
      log.error(`Agent error: ${msg}`);
      await hookRunner.run("error", { channelId, conversationId, error: msg });
      await sendReply(channelId, conversationId, {
        text: "Disculpa, ha ocurrido un error técnico inesperado. Por favor, intenta nuevamente en unos momentos.",
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

  async function updatePresence(
    channelId: string,
    conversationId: string,
    type: "typing" | "recording" | "available" | "paused",
  ) {
    const channel = channels.get(channelId);
    if (channel && channel.sendPresence) {
      try {
        await channel.sendPresence(conversationId, type);
      } catch (error) {
        const msg = error instanceof Error ? error.message : String(error);
        log.error(`Failed to send presence update on ${channelId}: ${msg}`);
      }
    }
  }
}

/**
 * Detecta output degenerado del LLM (loops de "...", "The...", caracteres sueltos, etc.).
 * Retorna true si el texto parece corrupto y no debería enviarse al usuario.
 */
function isDegenerate(text: string): boolean {
  if (!text || text.length < 30) return false;

  // Contar ocurrencias de "..." (3+ puntos consecutivos) Y "…" (unicode ellipsis)
  const asciiEllipsis = (text.match(/\.{3,}/g) || []).length;
  const unicodeEllipsis = (text.match(/…/g) || []).length;
  const totalEllipsis = asciiEllipsis + unicodeEllipsis;

  // Si hay más de 5 ellipsis en total, es basura
  if (totalEllipsis > 5) return true;

  // Ratio de contenido real vs. espacios/puntos/newlines/ellipsis/asteriscos
  const meaningfulChars = text.replace(/[\s.*…\n\r\-()¡¿?!]/g, "").length;
  const totalChars = text.length;
  const meaningfulRatio = meaningfulChars / totalChars;

  // Si menos del 35% del mensaje es contenido real, es degenerado
  if (totalChars > 100 && meaningfulRatio < 0.35) return true;

  // Patrón de repetición: palabras cortas seguidas de puntos
  const repeatedFragments = (text.match(/\b(The|We|This|\.\.\.)\.\.\./gi) || []).length;
  if (repeatedFragments > 3) return true;

  // Patrón de cambio de idioma abrupto (español→inglés) indica confusión del modelo
  const englishWords = (text.match(/\b(The|We|This|That|And|But|For|With)\b/g) || []).length;
  if (englishWords > 3) return true;

  return false;
}

/**
 * Detecta y elimina bloques duplicados en la respuesta del LLM.
 * El modelo a veces genera dos versiones de la misma respuesta
 * dentro de una sola completion ("doble borrador").
 */
function deduplicateBlocks(text: string): string {
  if (!text || text.length < 80) return text;

  // Dividir en bloques por doble salto de línea o por patrones de inicio de frase
  // que indican un "reinicio" del modelo (ej: "Perfecto," al medio del texto)
  const blocks = text.split(/\n{2,}/).filter((b) => b.trim().length > 0);

  if (blocks.length < 2) return text;

  // Extraer palabras clave significativas de un bloque (>3 chars, lowercase)
  const getKeywords = (block: string): Set<string> => {
    const words = block
      .toLowerCase()
      .replace(/[^a-záéíóúñü\s]/g, "")
      .split(/\s+/)
      .filter((w) => w.length > 3);
    return new Set(words);
  };

  // Calcular overlap entre dos conjuntos de palabras (Jaccard index)
  const overlap = (a: Set<string>, b: Set<string>): number => {
    if (a.size === 0 || b.size === 0) return 0;
    let intersection = 0;
    for (const word of a) {
      if (b.has(word)) intersection++;
    }
    return intersection / Math.min(a.size, b.size);
  };

  const kept: string[] = [blocks[0]];
  const keptKeywords: Set<string>[] = [getKeywords(blocks[0])];

  for (let i = 1; i < blocks.length; i++) {
    const blockKw = getKeywords(blocks[i]);
    let isDuplicate = false;

    // Comparar con todos los bloques ya aceptados
    for (const prevKw of keptKeywords) {
      if (overlap(prevKw, blockKw) > 0.5) {
        isDuplicate = true;
        break;
      }
    }

    if (!isDuplicate) {
      kept.push(blocks[i]);
      keptKeywords.push(blockKw);
    }
  }

  return kept.join("\n\n");
}
