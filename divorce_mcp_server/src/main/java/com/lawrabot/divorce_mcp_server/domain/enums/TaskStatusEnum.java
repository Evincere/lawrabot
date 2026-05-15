package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Estado de una tarea asignada a LawraBot.
 */
public enum TaskStatusEnum {
    PENDING,      // Creada, esperando envío
    SENT_TO_BOT,  // Enviada a LawraBot vía MCP
    IN_PROGRESS,  // LawraBot procesando (esperando respuesta ciudadano)
    COMPLETED,    // Ciudadano respondió, tarea completada
    FAILED        // Error en envío o respuesta
}
