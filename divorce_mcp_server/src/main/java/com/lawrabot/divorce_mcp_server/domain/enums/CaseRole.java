package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Enum que define los roles que un ciudadano puede tomar en un expediente legal.
 * Soporta múltiples tipos de procesos (Divorcio, Sucesiones, etc.)
 */
public enum CaseRole {
    // Divorcio
    PETITIONER,   // Peticionante (Parte actora)
    RESPONDENT,   // Demandado / Otro cónyuge

    // Sucesiones
    CAUSANTE,     // Persona fallecida
    HEIR,         // Heredero
    TESTAMENTARIO, // Legatario / Heredero testamentario
    
    // Otros / General
    CLAIMANT,     // Requirente
    DEFENDANT,    // Requerido
    OTHER         // Otros intervinientes
}
