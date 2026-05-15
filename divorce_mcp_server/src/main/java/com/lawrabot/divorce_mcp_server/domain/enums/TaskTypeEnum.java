package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Tipos de tareas que pueden asignarse a LawraBot para ejecutar vía WhatsApp.
 */
public enum TaskTypeEnum {
    CLARIFY_DATA,       // Solicitar aclaración sobre dato confuso
    REQUEST_DOCUMENT,   // Pedir envío de documentación faltante
    NOTIFY_APPOINTMENT, // Convocar a firma de documentos
    CORRECT_ERROR       // Notificar error detectado con sugerencia de corrección
}
