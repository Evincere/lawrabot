package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Resultado del Scraping del Poder Judicial para la evaluación "A Priori" del BLSG.
 * El resultado NO es definitivo. Es una señal automatizada inicial.
 */
public enum BlsgScrapingResultEnum {
    /** El Poder Judicial no registra bienes, ingresos ni restricciones. Favorable para continuar. */
    PROVISIONALLY_APPROVED,
    /** El sistema registra bienes o ingresos que sugieren que NO corresponde el beneficio. Proceso termina. */
    PROVISIONALLY_REJECTED,
    /** El sistema del Poder Judicial no está disponible o el DNI no devolvió resultado concluyente. */
    INCONCLUSIVE
}
