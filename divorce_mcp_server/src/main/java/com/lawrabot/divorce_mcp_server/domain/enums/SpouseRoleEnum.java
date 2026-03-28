package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Identifica el rolprocesal de uno de los cónyuges en el expediente de divorcio.
 * Principalmente usado para designar beneficiarios de derecho (ej. Compensación económica).
 */
public enum SpouseRoleEnum {
    PETITIONER,   // El que inicia la demanda (Acuerdo unilateral o Conjunto - cónyuge 1)
    RESPONDENT,   // El demandado (o Conjunto - cónyuge 2)
    BOTH,         // Ambos (para obligaciones compartidas)
    OTHER         // Otra configuración anómala
}
