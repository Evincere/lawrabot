package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Medio de pago para el cumplimiento de la cuota.
 */
public enum PaymentMethodEnum {
    BANK_TRANSFER,         // Transferencia bancaria directa
    JUDICIAL_DEPOSIT,      // Depósito en cuenta judicial (Banco Nación / Provincia)
    CASH,                  // Efectivo contra recibo
    EMPLOYER_WITHHOLDING,  // Retención directa por el empleador (embargo voluntario)
    OTHER                  // Otro medio acordado
}
