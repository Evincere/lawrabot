package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Estado de una observación en el flujo de trabajo.
 */
public enum ObservationStatusEnum {
    PENDING,          // Creada, esperando asignación
    ASSIGNED_TO_BOT,  // Enviada a LawraBot para contactar al ciudadano
    RESOLVED,         // Ciudadano respondió, operador validó
    DISMISSED         // Operador decidió ignorar la observación
}
