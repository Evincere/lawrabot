package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Representa el estado procesal y negocial del Convenio Regulador
 * (Art. 438 y 439 CCyC).
 */
public enum AgreementStatusEnum {
    PROPOSED,    // Propuesta agregada a demanda unilateral (aún no evaluada por el demandado).
    ACCEPTED,    // Aprobado por ambas partes (Conjunto) pero aún no homologado por el juez.
    REJECTED,    // Desestimado total o parcialmente. Deriva en incidentes o juicios separados.
    HOMOLOGATED  // Sentencia firme del Juez que aprueba legalmente las cláusulas.
}
