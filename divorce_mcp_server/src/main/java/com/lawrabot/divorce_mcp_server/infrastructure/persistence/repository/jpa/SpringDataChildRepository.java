package com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa;

import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.ChildJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataChildRepository extends JpaRepository<ChildJpaEntity, UUID> {
    List<ChildJpaEntity> findAllByExpedienteId(UUID expedienteId);
}
