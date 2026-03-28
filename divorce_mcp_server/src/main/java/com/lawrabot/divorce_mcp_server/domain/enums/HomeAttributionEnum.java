package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Designación contractual sobre el inmueble que fue sede del hogar conyugal (Art. 443 CCyC).
 */
public enum HomeAttributionEnum {
    PETITIONER,     // Atribuido al que solicita
    RESPONDENT,     // Atribuido al demandado
    BOTH_SALE,      // Se atribuye temporalmente a ambos para venta rápida
    OTHER           // Ej: alquiler para dividir las ganancias
}
