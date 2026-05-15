package com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa;

import com.lawrabot.divorce_mcp_server.domain.enums.TaskStatusEnum;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.TaskJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataTaskRepository extends JpaRepository<TaskJpaEntity, UUID> {

    List<TaskJpaEntity> findByStatus(TaskStatusEnum status);

    List<TaskJpaEntity> findByObservationExpedienteId(UUID expedienteId);
}