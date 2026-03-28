package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Representa el estado de recolección de datos en un entorno conversacional (Bot de WhatsApp).
 * El orden de las constantes refleja el flujo procesal del Bot de izquierda a derecha.
 */
public enum DataCollectionStageEnum {
    PENDING_BLSG_SCRAPING,           // El bot tiene el DNI y está esperando el resultado del scraping judicial.
    PENDING_SOCIOECONOMIC_EVALUATION, // El scraping fue positivo. Ahora se aplican los criterios propios de la Defensoría.
    PENDING_BASIC_INFO,              // BLSG aprobado. Faltan nombres y datos básicos del proceso.
    PENDING_MARRIAGE_DETAILS,        // Faltan fechas de matrimonio, separación y domicilio último.
    PENDING_CHILDREN_INFO,           // Falta saber si hay hijos menores o vulnerables y sus edades.
    PENDING_REGULATORY_AGREEMENT,    // Falta negociar las partes del convenio (Alimentos, Cuidado, Bienes).
    COMPLETED                        // Todos los datos duros recolectados. Listo para revisión final y PDF.
}
