package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Define el tipo de presentación para la demanda de divorcio en Argentina.
 * Art. 437 CCyC.
 */
public enum DivorceTypeEnum {
    /**
     * Divorcio por presentación conjunta. Ambos cónyuges firman y presentan la demanda,
     * usualmente con un Convenio Regulador ya acordado.
     */
    JOINT,

    /**
     * Divorcio por presentación unilateral. Un solo cónyuge solicita el divorcio,
     * notificando a la contraparte. Requiere acompañar una Propuesta Reguladora.
     */
    UNILATERAL
}
