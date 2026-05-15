package com.lawrabot.divorce_mcp_server.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * DTO estandarizado para respuestas de error de la API REST.
 * Utilizado por el GlobalExceptionHandler para garantizar un formato de error
 * consistente en todos los endpoints del sistema.
 *
 * Formato alineado con RFC 7807 (Problem Details for HTTP APIs).
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    /** Código HTTP numérico (ej. 400, 404, 500). */
    int status;

    /** Categoría del error. */
    String error;

    /** Descripción legible del problema. */
    String message;

    /** Ruta del endpoint que generó el error. */
    String path;

    /** Timestamp ISO-8601 del momento del error. */
    @Builder.Default
    String timestamp = LocalDateTime.now().toString();
}
