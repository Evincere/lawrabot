package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Identifica con cuál de los progenitores el menor tiene su residencia habitual (centro de vida).
 * Fundamental para determinar el reclamo automático de Cuota Alimentaria y Asignaciones Familiares (SUAF/AUH).
 */
public enum MainResidenceEnum {
    PETITIONER,      // Cónyuge solicitante.
    RESPONDENT,      // Cónyuge demandado.
    BOTH_EQUITABLE,  // Ambos (tiempo hiper-equitativo). Desplazaría la obligación alimentaria según ingresos.
    OTHER            // Otra situación (ej: vive con la abuela).
}
