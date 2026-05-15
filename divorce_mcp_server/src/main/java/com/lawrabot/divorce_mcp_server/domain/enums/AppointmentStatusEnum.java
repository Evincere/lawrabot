package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Estados posibles de un turno de firma presencial.
 */
public enum AppointmentStatusEnum {
    SCHEDULED,    // Turno agendado por el sistema/operador
    CONFIRMED,    // Interesado confirmó asistencia vía LawraBot
    COMPLETED,    // Interesado se presentó y firmó
    CANCELLED,    // Turno cancelado
    NO_SHOW       // Interesado no se presentó
}
