package com.lawrabot.divorce_mcp_server.application.port.in;

import com.lawrabot.divorce_mcp_server.domain.enums.ObservationSeverityEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.ObservationStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.TaskTypeEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Observation;

import java.util.List;
import java.util.UUID;

/**
 * Puerto de Entrada (Use Case) para la gestión de observaciones del operador.
 */
public interface ManageObservationsUseCase {

    /**
     * Crea una nueva observación en un expediente.
     * Opcionalmente genera una tarea para LawraBot si createTask = true.
     */
    Observation createObservation(CreateObservationCommand command);

    /**
     * Obtiene todas las observaciones de un expediente.
     */
    List<Observation> getObservationsByExpediente(UUID expedienteId);

    /**
     * Obtiene las observaciones filtradas por estado.
     */
    List<Observation> getObservationsByExpedienteAndStatus(UUID expedienteId, ObservationStatusEnum status);

    /**
     * Cuenta observaciones por estado en un expediente.
     */
    long countByExpedienteIdAndStatusIn(UUID expedienteId, List<ObservationStatusEnum> statuses);

    /**
     * Resuelve una observación (operador validó la respuesta del ciudadano).
     */
    Observation resolveObservation(UUID observationId, String notes, String responseData);

    /**
     * Descarta una observación (operador decidió ignorarla).
     */
    Observation dismissObservation(UUID observationId);

    /**
     * Elimina físicamente una observación de la base de datos.
     */
    void deleteObservation(UUID observationId);

    /**
     * Marca una tarea como completada (llamado por LawraBot cuando el ciudadano responde).
     */
    void markTaskAsCompleted(UUID taskId, String responseData);

    /**
     * Comando para crear una observación.
     */
    record CreateObservationCommand(
            UUID expedienteId,
            String fieldName,
            ObservationSeverityEnum severity,
            String message,
            String suggestedValue,
            boolean createTask,
            TaskTypeEnum taskType,
            boolean isImmediate,
            UUID operatorId
    ) {}
}