package com.lawrabot.divorce_mcp_server.infrastructure.rest;

import com.lawrabot.divorce_mcp_server.application.service.ObservationService.ObservationNotFoundException;
import com.lawrabot.divorce_mcp_server.application.service.ObservationService.TaskNotFoundException;
import com.lawrabot.divorce_mcp_server.infrastructure.rest.dto.ApiErrorResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Manejador global de excepciones para todos los controladores REST.
 *
 * PRINCIPIO DE DISEÑO:
 * Los controladores NO deben capturar excepciones internamente para construir
 * respuestas de error. Deben dejar que las excepciones se propaguen naturalmente
 * y este advice las transforma en respuestas HTTP estandarizadas (ApiErrorResponse).
 *
 * Excepciones manejadas:
 * - IllegalArgumentException → 400 Bad Request (validación de Value Objects)
 * - ObservationNotFoundException → 404 Not Found
 * - TaskNotFoundException → 404 Not Found
 * - MethodArgumentTypeMismatchException → 400 Bad Request (parámetros de URL inválidos)
 * - Exception → 500 Internal Server Error (catch-all de seguridad)
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // -------------------------------------------------------------------
    // 400 — Errores de validación de dominio (Value Objects, reglas de negocio)
    // -------------------------------------------------------------------

    /**
     * Captura excepciones lanzadas por los Value Objects (DNIVO, CuilVO, PhoneNumberVO, etc.)
     * cuando reciben datos inválidos. Estas se propagan naturalmente desde los servicios
     * o desde los métodos de mapeo del controlador sin necesidad de try-catch.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, ServerHttpRequest request) {

        log.warn("Validación fallida en {}: {}", request.getURI().getPath(), ex.getMessage());

        ApiErrorResponse body = ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Datos Inválidos")
                .message(ex.getMessage())
                .path(request.getURI().getPath())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // -------------------------------------------------------------------
    // 404 — Recursos no encontrados
    // -------------------------------------------------------------------

    /**
     * Observaciones inexistentes.
     */
    @ExceptionHandler(ObservationNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleObservationNotFound(
            ObservationNotFoundException ex, ServerHttpRequest request) {

        log.info("Observación no encontrada en {}: {}", request.getURI().getPath(), ex.getMessage());

        ApiErrorResponse body = ApiErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Recurso No Encontrado")
                .message(ex.getMessage())
                .path(request.getURI().getPath())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    /**
     * Tareas inexistentes.
     */
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTaskNotFound(
            TaskNotFoundException ex, ServerHttpRequest request) {

        log.info("Tarea no encontrada en {}: {}", request.getURI().getPath(), ex.getMessage());

        ApiErrorResponse body = ApiErrorResponse.builder()
                .status(HttpStatus.NOT_FOUND.value())
                .error("Recurso No Encontrado")
                .message(ex.getMessage())
                .path(request.getURI().getPath())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // -------------------------------------------------------------------
    // 400 — Parámetros de ruta/query malformados
    // -------------------------------------------------------------------

    /**
     * Captura errores de conversión de tipos en parámetros de URL
     * (ej: UUID inválido en un @PathVariable).
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, ServerHttpRequest request) {

        String paramName = ex.getName();
        String message = String.format("El parámetro '%s' tiene un formato inválido: %s", paramName, ex.getValue());
        log.warn("Error de tipo en {}: {}", request.getURI().getPath(), message);

        ApiErrorResponse body = ApiErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Parámetro Inválido")
                .message(message)
                .path(request.getURI().getPath())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // -------------------------------------------------------------------
    // 500 — Catch-all de seguridad (errores no previstos)
    // -------------------------------------------------------------------

    /**
     * Última línea de defensa. Si una excepción no fue capturada por ningún
     * handler específico, se devuelve un 500 genérico SIN exponer detalles
     * internos al cliente.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex, ServerHttpRequest request) {

        log.error("Error no manejado en {}: {}", request.getURI().getPath(), ex.getMessage(), ex);

        ApiErrorResponse body = ApiErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Error Interno")
                .message("Ha ocurrido un error interno en el servidor. Contacte al administrador.")
                .path(request.getURI().getPath())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
