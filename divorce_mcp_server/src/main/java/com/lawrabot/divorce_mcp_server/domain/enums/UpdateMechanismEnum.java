package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Mecanismo pactado para la actualización de la cuota dineraria.
 * Escudo contra la depreciación monetaria en Argentina.
 */
public enum UpdateMechanismEnum {
    IPC_INDEX,           // Basado en el Índice de Precios al Consumidor (INDEC)
    SALARY_PARITY,       // Acorde a los aumentos paritarios del gremio del alimentante
    SMVM_PERCENTAGE,     // Al estar atado a % SMVM, se actualiza automáticamente con este
    NONE,                // Sin actualización (Monto Fijo Absoluto - Riesgoso)
    OTHER                // Otro mecanismo (ej: Canasta Básica Total)
}
