package com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa;

import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.DigitalEvidenceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataDigitalEvidenceRepository extends JpaRepository<DigitalEvidenceJpaEntity, UUID> {
    List<DigitalEvidenceJpaEntity> findByExpediente_IdOrderByCreatedAtDesc(UUID expedienteId);
}
