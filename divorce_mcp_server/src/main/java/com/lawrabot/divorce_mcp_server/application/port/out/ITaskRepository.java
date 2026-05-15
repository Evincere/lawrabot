package com.lawrabot.divorce_mcp_server.application.port.out;

import com.lawrabot.divorce_mcp_server.domain.enums.TaskStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Observation.ObservationTask;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de Salida para la persistencia de Tasks.
 */
public interface ITaskRepository {

    ObservationTask save(ObservationTask task);

    Optional<ObservationTask> findById(UUID id);

    List<ObservationTask> findByStatus(TaskStatusEnum status);

    List<ObservationTask> findByObservationExpedienteId(UUID expedienteId);
}