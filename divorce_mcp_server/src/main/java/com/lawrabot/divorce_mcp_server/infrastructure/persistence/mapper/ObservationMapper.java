package com.lawrabot.divorce_mcp_server.infrastructure.persistence.mapper;

import com.lawrabot.divorce_mcp_server.domain.model.Observation;
import com.lawrabot.divorce_mcp_server.domain.model.Observation.ObservationTask;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.ObservationJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.TaskJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ObservationMapper {

    private final TaskMapper taskMapper;

    public ObservationMapper(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public ObservationJpaEntity toEntity(Observation domain) {
        if (domain == null) return null;

        ObservationJpaEntity entity = ObservationJpaEntity.builder()
                .id(domain.getId())
                .expedienteId(domain.getExpedienteId())
                .fieldName(domain.getFieldName())
                .severity(domain.getSeverity())
                .message(domain.getMessage())
                .suggestedValue(domain.getSuggestedValue())
                .status(domain.getStatus())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .resolvedAt(domain.getResolvedAt())
                .resolutionNotes(domain.getResolutionNotes())
                .build();

        if (domain.getTask() != null) {
            TaskJpaEntity taskEntity = taskMapper.toEntity(domain.getTask());
            taskEntity.setObservation(entity);
            entity.setTask(taskEntity);
        }

        return entity;
    }

    public Observation toDomain(ObservationJpaEntity entity) {
        if (entity == null) return null;

        ObservationTask task = taskMapper.toDomain(entity.getTask());

        return Observation.builder()
                .id(entity.getId())
                .expedienteId(entity.getExpedienteId())
                .fieldName(entity.getFieldName())
                .severity(entity.getSeverity())
                .message(entity.getMessage())
                .suggestedValue(entity.getSuggestedValue())
                .status(entity.getStatus())
                .createdBy(entity.getCreatedBy())
                .task(task)
                .createdAt(entity.getCreatedAt())
                .resolvedAt(entity.getResolvedAt())
                .resolutionNotes(entity.getResolutionNotes())
                .build();
    }

    public List<Observation> toDomainList(List<ObservationJpaEntity> entities) {
        return entities.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
}