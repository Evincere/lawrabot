import type { LLMToolDefinition } from "../llm/types.js";
import type { ToolRegistry } from "../tools/registry.js";
import type { ToolContext } from "../tools/types.js";
import type { Logger } from "../utils/logger.js";

export interface StageContext {
  stage: string;
  hasIncomeProof: boolean;
  pendingDocuments: string[];
  allowedTools: string[];
}

/**
 * Consulta el stage del expediente vía la herramienta MCP get_stage_context
 * y filtra las herramientas disponibles para el LLM según el resultado.
 *
 * Si la consulta falla o no hay expediente, devuelve todas las herramientas
 * sin filtrar (modo degradado seguro).
 */
export async function getFilteredTools(
  toolRegistry: ToolRegistry,
  toolCtx: ToolContext,
  phoneNumber: string,
  log: Logger,
): Promise<{ tools: LLMToolDefinition[]; stageContext: StageContext | null }> {
  const allTools = toolRegistry.toLLMTools();

  if (!phoneNumber) {
    return { tools: allTools, stageContext: null };
  }

  try {
    const result = await toolRegistry.execute(
      "get_stage_context",
      { phoneNumber },
      toolCtx,
    );

    const stageContext: StageContext = JSON.parse(result.content);

    if (stageContext.stage === "NO_EXPEDIENTE" || stageContext.stage === "ERROR") {
      return { tools: allTools, stageContext };
    }

    const allowedSet = new Set(stageContext.allowedTools);

    const filteredTools = allTools.filter(
      (tool) => allowedSet.has(tool.function.name),
    );

    log.info(
      `[ToolGate] Stage: ${stageContext.stage} | Allowed: ${filteredTools.length}/${allTools.length} tools | Pending docs: ${stageContext.pendingDocuments.join(", ") || "none"}`,
    );

    return { tools: filteredTools, stageContext };
  } catch (error) {
    const msg = error instanceof Error ? error.message : String(error);
    log.warn(`[ToolGate] Failed to get stage context: ${msg}. Using all tools.`);
    return { tools: allTools, stageContext: null };
  }
}

/**
 * Genera un fragmento de prompt que informa al LLM sobre el estado actual del
 * expediente y los documentos pendientes. Se inyecta como mensaje de sistema
 * adicional justo antes de la llamada al LLM.
 */
export function buildStageHint(stageContext: StageContext): string {
  const lines: string[] = [
    `[STAGE_CONTEXT] Etapa actual del expediente: ${stageContext.stage}.`,
  ];

  if (stageContext.pendingDocuments.length > 0) {
    lines.push(
      `Documentos pendientes de recibir: ${stageContext.pendingDocuments.join(", ")}.`,
    );
    lines.push(
      "PROHIBIDO avanzar a la siguiente fase hasta que todos los documentos pendientes hayan sido recibidos (bloque [MEDIA] + submit_digital_evidence).",
    );
  }

  lines.push("[/STAGE_CONTEXT]");

  return lines.join(" ");
}
