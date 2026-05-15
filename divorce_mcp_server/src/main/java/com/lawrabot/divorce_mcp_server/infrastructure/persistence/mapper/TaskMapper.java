package com.lawrabot.divorce_mcp_server.infrastructure.persistence.mapper;

import com.lawrabot.divorce_mcp_server.domain.model.Observation.ObservationTask;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.TaskJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa.SpringDataObservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TaskMapper {
    
    private final SpringDataObservationRepository observationRepository;

    public TaskJpaEntity toEntity(ObservationTask domain) {
        if (domain == null) return null;

        var entity = TaskJpaEntity.builder()
                .id(domain.getId())
                .type(domain.getType())
                .messageTemplate(domain.getMessageTemplate())
                .status(domain.getStatus())
                .assignedTo(domain.getAssignedTo())
                .isImmediate(domain.isImmediate())
                .createdAt(domain.getCreatedAt())
                .sentAt(domain.getSentAt())
                .completedAt(domain.getCompletedAt())
                .responseData(domain.getResponseData())
                .build();
        
        var obsId = domain.getObservationId();
        if (obsId != null) {
            observationRepository.findById(java.util.Objects.requireNonNull(obsId))
                .ifPresent(entity::setObservation);
        }
        
        return entity;
    }

    public ObservationTask toDomain(TaskJpaEntity entity) {
        if (entity == null) return null;

        return ObservationTask.builder()
                .id(entity.getId())
                .observationId(entity.getObservation() != null ? entity.getObservation().getId() : null)
                .type(entity.getType())
                .messageTemplate(entity.getMessageTemplate())
                .status(entity.getStatus())
                .assignedTo(entity.getAssignedTo())
                .isImmediate(entity.isImmediate())
                .createdAt(entity.getCreatedAt())
                .sentAt(entity.getSentAt())
                .completedAt(entity.getCompletedAt())
                .responseData(entity.getResponseData())
                .build();
    }
}