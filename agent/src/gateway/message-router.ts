import fs from "node:fs";
import path from "node:path";
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
  specDir: string;
  log: Logger;
}

/**
 * The message router is the brain of the agent.
 * Inbound message → session lookup → LLM call → tool execution loop → reply.
 */
export function createMessageRouter(deps: MessageRouterDeps) {
  const { sessionManager, toolRegistry, hookRunner, llmProvider, channels, systemPrompt, specDir, log } = deps;

  const MAX_TOOL_ROUNDS = 10;

  // ── Debounce: acumula mensajes rápidos del mismo conversation ──
  const DEBOUNCE_WINDOW_MS = 2500; // 2.5 segundos de silencio antes de procesar
  const pendingMessages = new Map<string, { messages: InboundMessage[]; timer: ReturnType<typeof setTimeout> }>();

  /**
   * Coalesce rapid-fire messages from the same conversation into a single
   * processing unit. This handles the common WhatsApp pattern of users sending
   * a document + caption as two separate messages, or selecting multiple files.
   */
  function debounceInbound(message: InboundMessage): void {
    const key = `${message.channelId}:${message.conversationId}`;
    const existing = pendingMessages.get(key);

    if (existing) {
      // Hay mensajes pendientes — agregar y resetear el timer
      clearTimeout(existing.timer);
      existing.messages.push(message);
      log.info(`[debounce] Accumulated message for ${key} (${existing.messages.length} total)`);
    } else {
      // Primer mensaje — crear buffer
      pendingMessages.set(key, { messages: [message], timer: null as any });
      log.info(`[debounce] New message buffer for ${key}`);
    }

    // (Re)iniciar timer — cuando expire, procesar todo junto
    const entry = pendingMessages.get(key)!;
    entry.timer = setTimeout(async () => {
      const accumulated = pendingMessages.get(key);
      pendingMessages.delete(key);

      if (!accumulated || accumulated.messages.length === 0) return;

      if (accumulated.messages.length === 1) {
        // Un solo mensaje — procesar directamente
        await processMessage(accumulated.messages[0]);
      } else {
        // Múltiples mensajes — coalescerlos en uno
        log.info(`[debounce] Coalescing ${accumulated.messages.length} messages for ${key}`);
        const coalesced = coalesceMessages(accumulated.messages);
        await processMessage(coalesced);
      }
    }, DEBOUNCE_WINDOW_MS);
  }

  /**
   * Merge multiple InboundMessages into a single one by concatenating
   * text content. Media blocks and captions are preserved.
   */
  function coalesceMessages(messages: InboundMessage[]): InboundMessage {
    const base = messages[0];
    const parts: string[] = [];

    for (const msg of messages) {
      if (msg.text.trim()) {
        parts.push(msg.text.trim());
      }
    }

    return {
      ...base,
      text: parts.join("\n\n"),
      timestamp: messages[messages.length - 1].timestamp, // Usar el timestamp más reciente
    };
  }

  async function processMessage(message: InboundMessage): Promise<void> {
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
      // Construir el contexto de la herramienta
      const toolCtx: ToolContext = {
        sessionId: session.id,
        channelId,
        conversationId,
        senderId,
        config: {},
      };

      // Obtener las herramientas filtradas y el contexto de la etapa desde la base de datos
      const { tools, stageContext } = await getFilteredTools(
        toolRegistry,
        toolCtx,
        phoneFromJid,
        log,
      );

      // Resolver y ensamblar el prompt de sistema dinámico según la etapa actual
      const currentStage = stageContext ? stageContext.stage : "NO_EXPEDIENTE";
      const finalSystemPrompt = await getDynamicSystemPrompt(
        specDir,
        currentStage,
        systemPrompt,
        log,
      );

      // Construir los mensajes para el LLM con el system prompt dinámico en el índice 0
      const llmMessages: LLMMessage[] = [
        { role: "system" as const, content: finalSystemPrompt },
        ...sessionManager.getContextMessages(session),
      ];

      // Si tenemos contexto de etapa con un expediente activo, inyectamos una pista de sistema
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
          let replyText =
            response.content ||
            "";

          // ── Retry especial para mensajes con MEDIA que el LLM no procesó ──
          const userText = text; // captured from InboundMessage
          const hasMedia = userText.includes("[MEDIA]");
          if (!replyText && hasMedia) {
            log.warn(`LLM returned empty response for a message with [MEDIA]. Retrying with simplified prompt...`);

            // Extraer la ruta del archivo del bloque [MEDIA]
            const mediaMatch = userText.match(/\[MEDIA\]\s*localPath=(\S+)\s+fileName=(\S+)\s+mimeType=(\S+)\s*\[\/MEDIA\]/);
            if (mediaMatch) {
              const [, localPath, fileName, mimeType] = mediaMatch;
              // Extraer el texto del usuario (sin el bloque [MEDIA])
              const captionText = userText.replace(/\n*\[MEDIA\].*\[\/MEDIA\]/s, "").replace(/\[METADATA\].*?\[\/METADATA\]\n?/, "").trim();

              llmMessages.push({
                role: "user",
                content: `[SYSTEM] El usuario acaba de enviar un archivo. Datos del archivo:\n- Ruta: ${localPath}\n- Nombre: ${fileName}\n- Tipo MIME: ${mimeType}\n- Mensaje del usuario: "${captionText}"\n\nDEBES llamar a submit_digital_evidence con estos datos. Determiná el documentType correcto y, si es un acta de nacimiento o CUD, incluí el childFullName inferido del mensaje del usuario. Luego confirmá brevemente la recepción.`,
              });

              const retryMediaResponse = await llmProvider.complete({
                messages: llmMessages,
                tools: tools.length > 0 ? tools : undefined,
              });

              // Si el retry generó tool calls, procesarlas
              if (retryMediaResponse.finishReason === "tool_calls" && retryMediaResponse.toolCalls.length > 0) {
                log.info(`Media retry triggered tool call: ${retryMediaResponse.toolCalls.map(tc => tc.function.name).join(", ")}`);
                // Re-inyectar en el loop para que se procese
                sessionManager.addMessage(session, {
                  role: "assistant",
                  content: retryMediaResponse.content ?? "",
                  toolCalls: retryMediaResponse.toolCalls,
                  timestamp: Date.now(),
                });
                llmMessages.push({
                  role: "assistant" as const,
                  content: retryMediaResponse.content ?? "",
                  tool_calls: retryMediaResponse.toolCalls,
                });

                for (const tc of retryMediaResponse.toolCalls) {
                  const args = typeof tc.function.arguments === "string"
                    ? JSON.parse(tc.function.arguments)
                    : tc.function.arguments;

                  const result = await toolRegistry.execute(tc.function.name, args, toolCtx);
                  sessionManager.addMessage(session, {
                    role: "tool",
                    content: result.content,
                    toolCallId: tc.id,
                    timestamp: Date.now(),
                  });
                  llmMessages.push({
                    role: "tool" as const,
                    content: result.content,
                    tool_call_id: tc.id,
                  });
                }

                continue; // Let the LLM process tool results
              }

              // If retry returned text, use it
              if (retryMediaResponse.content) {
                replyText = retryMediaResponse.content;
                log.info(`Media retry returned text response (${replyText.length} chars).`);
              }
            }
          }

          // Fallback final si sigue vacío
          if (!replyText) {
            replyText = "Lo siento, no he podido procesar una respuesta adecuada. ¿Podrías reformular tu mensaje?";
          }

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
  }

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

  // Exponer debounceInbound como el handler público
  return debounceInbound;
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
    // Ignorar bloques muy cortos (como encabezados) para que no eliminen párrafos grandes
    if (a.size < 5 || b.size < 5) return 0;
    
    let intersection = 0;
    for (const word of a) {
      if (b.has(word)) intersection++;
    }
    const union = a.size + b.size - intersection;
    return intersection / union;
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

/**
 * Carga dinámicamente el sub-prompt de la etapa actual y lo integra con el prompt base.
 * Aplica degradación segura y fallbacks semánticos si no existe el archivo correspondiente.
 */
async function getDynamicSystemPrompt(
  specDir: string,
  stage: string | null | undefined,
  basePrompt: string,
  log: Logger
): Promise<string> {
  // 1. Normalizar la etapa. Si viene nula o vacía, asumimos "NO_EXPEDIENTE"
  const normalizedStage = (stage || "NO_EXPEDIENTE").toUpperCase().trim();

  // 2. Definir la ruta física del archivo específico para esta fase
  const phaseFilePath = path.join(specDir, "prompts", "phases", `${normalizedStage}.md`);

  // 3. Intentar cargar el archivo específico de fase
  if (fs.existsSync(phaseFilePath)) {
    try {
      const stagePrompt = fs.readFileSync(phaseFilePath, "utf-8").trim();
      log.info(`[DynamicPrompt] Loaded stage instructions for: ${normalizedStage}`);
      return assemblePrompt(basePrompt, stagePrompt, normalizedStage);
    } catch (error) {
      log.error(`[DynamicPrompt] Error reading prompt for stage ${normalizedStage}: ${error}`);
    }
  }

  // 4. Mecanismo de Fallback Semántico si el archivo no existe en el disco
  log.warn(`[DynamicPrompt] Phase prompt not found at ${phaseFilePath}. Applying semantic fallback.`);

  // CASO DE FALLO 1: Si falló cargar el archivo de "ERROR"
  if (normalizedStage === "ERROR") {
    // Inyectamos una directiva de contingencia segura directamente para que el LLM no alucine
    const defaultErrorPrompt = `[ATENCIÓN: El sistema ha experimentado una contingencia técnica al recuperar el expediente. Disculpate de manera sumamente cálida y empática con el ciudadano por esta demora, e invitalo a intentar escribir nuevamente en unos minutos. Está PROHIBIDO realizar llamadas a herramientas de base de datos hasta que el servicio se normalice.]`;
    return assemblePrompt(basePrompt, defaultErrorPrompt, "ERROR_FALLBACK");
  }

  // CASO DE FALLO 2: Si falló cargar "NO_EXPEDIENTE"
  if (normalizedStage === "NO_EXPEDIENTE") {
    log.info(`[DynamicPrompt] Falling back to base system prompt for welcome message.`);
    return basePrompt;
  }

  // FALLBACK ABSOLUTO (Cualquier otra etapa física faltante)
  log.info(`[DynamicPrompt] Falling back to base system prompt only.`);
  return basePrompt;
}

/**
 * Ensambla el prompt base con el sub-prompt usando delimitadores semánticos estructurados.
 */
function assemblePrompt(basePrompt: string, stagePrompt: string, stageName: string): string {
  return [
    `# SISTEMA: LAWRABOT — MINISTERIO PÚBLICO DE LA DEFENSA (MENDOZA)`,
    ``,
    `[REGLAS_CORE_GLOBALES]`,
    basePrompt,
    `[/REGLAS_CORE_GLOBALES]`,
    ``,
    `[INSTRUCCIONES_ETAPA_ACTUAL: ${stageName}]`,
    stagePrompt,
    `[/INSTRUCCIONES_ETAPA_ACTUAL]`
  ].join("\n");
}
