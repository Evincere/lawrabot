package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Representa el estado de recolección de datos en un entorno conversacional (Bot de WhatsApp).
 * Permite al bot saber dónde retomó la charla el usuario si se desconecta.
 */
public enum DataCollectionStageEnum {
    PENDING_BASIC_INFO,         // Solo tenemos el número y la intención. Faltan nombres, DNI de partes.
    PENDING_MARRIAGE_DETAILS,   // Faltan fechas de matrimonio, separación y domicilio último.
    PENDING_CHILDREN_INFO,      // Falta saber si hay hijos menores o vulnerables y sus edades.
    PENDING_REGULATORY_AGREEMENT, // Falta negociar las partes del convenio (Alimentos, Cuidado, Bienes).
    COMPLETED                   // Todos los datos duros recolectados. Listo para revisión final y PDF.
}
