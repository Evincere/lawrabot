package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Define la naturaleza del régimen de comunicación con los hijos
 * (Art. 652 CCyC).
 */
public enum CommunicationRegimeTypeEnum {
    BROAD_AND_FLEXIBLE,       // Amplio y flexible (acuerdo fluido cotidiano). Es preferido.
    SPECIFIC_SCHEDULE,        // Régimen específico (horarios fijos y días determinados).
    RESTRICTED_SUPERVISED,    // Régimen restringido o supervisado (violencia o conflictividad extrema).
    OTHER                     // Otra modalidad particular acordada.
}
