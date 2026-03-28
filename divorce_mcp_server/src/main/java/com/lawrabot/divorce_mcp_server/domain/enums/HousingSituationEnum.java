package com.lawrabot.divorce_mcp_server.domain.enums;

/**
 * Situación habitacional del solicitante, un criterio clave para la
 * evaluación socioeconómica de la Defensoría.
 */
public enum HousingSituationEnum {
    RENTING,          // Alquila
    OWNER,            // Propietario
    FAMILY_HOME,      // Vive con familiares (sin costo)
    SHARED_HOUSING,   // Comparte vivienda con terceros
    OTHER             // Otro
}
