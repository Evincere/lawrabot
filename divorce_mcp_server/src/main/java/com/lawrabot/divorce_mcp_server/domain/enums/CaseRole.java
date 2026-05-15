package com.lawrabot.divorce_mcp_server.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum que define los roles que un ciudadano puede tomar en un expediente legal.
 * Soporta múltiples tipos de procesos (Divorcio, Sucesiones, etc.)
 */
@Getter
@RequiredArgsConstructor
public enum CaseRole {
    // Divorcio
    PETITIONER("Peticionante"),   // Peticionante (Parte actora)
    RESPONDENT("Otro Cónyuge"),   // Demandado / Otro cónyuge

    // Sucesiones
    CAUSANTE("Causante"),     // Persona fallecida
    HEIR("Heredero"),         // Heredero
    TESTAMENTARIO("Testamentario"), // Legatario / Heredero testamentario
    
    // Otros / General
    CLAIMANT("Requirente"),     // Requirente
    DEFENDANT("Requerido"),    // Requerido
    OTHER("Interviniente");         // Otros intervinientes

    private final String value;
}
