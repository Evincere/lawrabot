package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Define la modalidad del Cuidado Personal de los hijos (Art. 648 CCyC).
 */
public enum PersonalCareTypeEnum {
    SHARED_INDISTINCT,      // Compartido Indistinto (la gran regla general).
    SHARED_ALTERNATED,      // Compartido Alternado (mitad de tiempo con cada progenitor).
    UNILATERAL_PETITIONER,  // Unilateral a favor del que pide el divorcio (Excepcional).
    UNILATERAL_RESPONDENT,  // Unilateral a favor del cónyuge demandado (Excepcional).
    OTHER                   // Otra modalidad jurídica.
}
