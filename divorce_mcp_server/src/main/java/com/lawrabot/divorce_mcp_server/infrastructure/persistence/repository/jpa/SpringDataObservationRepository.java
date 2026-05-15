package com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa;

import com.lawrabot.divorce_mcp_server.domain.enums.ObservationStatusEnum;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.ObservationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataObservationRepository extends JpaRepository<ObservationJpaEntity, UUID> {

    List<ObservationJpaEntity> findByExpedienteId(UUID expedienteId);

    List<ObservationJpaEntity> findByExpedienteIdAndStatus(UUID expedienteId, ObservationStatusEnum status);

    long countByExpedienteIdAndStatusIn(UUID expedienteId, List<ObservationStatusEnum> statuses);
}