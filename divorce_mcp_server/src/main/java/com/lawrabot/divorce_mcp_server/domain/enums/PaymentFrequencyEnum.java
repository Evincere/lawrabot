package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Frecuencia del pago de la cuota alimentaria.
 */
public enum PaymentFrequencyEnum {
    MONTHLY,      // Mensual (del 1 al 10 generalmente)
    FORTNIGHTLY,  // Quincenal
    WEEKLY,       // Semanal
    ONE_OFF,      // Pago único excepcional
    OTHER         // Otra frecuencia
}
