package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Severidad de una observación creada por el operador humano.
 * Determina el impacto en la generación de documentos.
 */
public enum ObservationSeverityEnum {
    ERROR,    // Bloquea generación de documentos hasta resolverse
    WARNING,  // Advertencia pero permite continuar
    INFO      // Informativo, no afecta el flujo
}
