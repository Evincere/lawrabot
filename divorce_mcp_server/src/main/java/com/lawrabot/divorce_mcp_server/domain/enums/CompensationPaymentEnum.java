package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Modos válidos de pago o cumplimiento para la Compensación Económica (Art. 441 CCyC).
 */
public enum CompensationPaymentEnum {
    SINGLE_PAYMENT, // Pago único en dinero (Efectivo, transferencia)
    INSTALLMENTS,   // Pago fraccionado en cuotas mensuales/anuales
    USUFRUCT,       // Pago mediante el usufructo o goce de bienes (ej: quedarse a vivir en la casa paterna sin pagar alquiler)
    OTHER           // Modalidades mixtas u otras (cesión de derechos, fondo de comercio)
}
