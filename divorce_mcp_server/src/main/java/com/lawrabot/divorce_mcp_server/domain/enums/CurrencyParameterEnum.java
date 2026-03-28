package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Parámetro o moneda del monto alimentario.
 */
public enum CurrencyParameterEnum {
    ARS,                    // Pesos Argentinos (Monto Fijo)
    USD,                    // Dólares Estadounidenses
    SALARY_PERCENTAGE,      // Porcentaje del salario del alimentante
    SMVM_PERCENTAGE,        // Porcentaje del Salario Mínimo Vital y Móvil
    JUS_PERCENTAGE,         // Porcentaje de la unidad arancelaria (JUS)
    OTHER                   // Otro parámetro acordado libremente
}
