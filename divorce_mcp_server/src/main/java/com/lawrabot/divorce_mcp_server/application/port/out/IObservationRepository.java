package com.lawrabot.divorce_mcp_server.application.port.out;

import com.lawrabot.divorce_mcp_server.domain.enums.ObservationStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Observation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de Salida para la persistencia de Observaciones.
 */
public interface IObservationRepository {

    Observation save(Observation observation);

    Optional<Observation> findById(UUID id);

    List<Observation> findByExpedienteId(UUID expedienteId);

    List<Observation> findByExpedienteIdAndStatus(UUID expedienteId, ObservationStatusEnum status);

    long countByExpedienteIdAndStatusIn(UUID expedienteId, List<ObservationStatusEnum> statuses);

    boolean existsById(UUID id);

    void deleteById(UUID id);
}