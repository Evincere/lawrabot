package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Representa el estado de recolección de datos en un entorno conversacional (Bot de WhatsApp).
 * El orden de las constantes refleja el flujo procesal del Bot de izquierda a derecha.
 */
public enum DataCollectionStageEnum {
    PENDING_BLSG_SCRAPING,             // Esperando resultado scraping judicial
    PENDING_MODALITY_SELECTION,        // Elegir unilateral o conjunto
    PENDING_RESPONDENT_BLSG,           // (Solo conjunto) BLSG del 2do cónyuge
    PENDING_PERSONAL_DATA,             // Datos personales de las partes
    PENDING_SOCIOECONOMIC_EVALUATION,  // Perfil económico (criterios Defensoría)
    PENDING_INCOME_PROOF,              // Esperando documento de ingresos (bono sueldo / cert. negativo ANSES)
    PENDING_MARRIAGE_DETAILS,          // Matrimonio y último domicilio conyugal
    PENDING_CHILDREN_INFO,             // Hijos menores o vulnerables
    PENDING_REGULATORY_AGREEMENT,      // Convenio regulador
    COMPLETED,                         // Listo para revisión y PDF
    REJECTED                           // Evaluación fallida → proceso detenido
}
