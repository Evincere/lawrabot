package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Define el tipo de cuota alimentaria pactada.
 */
public enum ProvisionTypeEnum {
    MONETARY,     // Dinero en efectivo o transferencia
    IN_KIND,      // En especie (ej. pago directo de colegio, prepaga)
    MIXED,        // Mixta (una parte en dinero, otra en especie)
    OTHER         // Otro tipo acordado
}
