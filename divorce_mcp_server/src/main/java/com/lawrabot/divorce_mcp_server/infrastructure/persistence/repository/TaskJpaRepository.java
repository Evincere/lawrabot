package com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository;

import com.lawrabot.divorce_mcp_server.application.port.out.ITaskRepository;
import com.lawrabot.divorce_mcp_server.domain.enums.TaskStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Observation.ObservationTask;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.TaskJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.mapper.TaskMapper;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa.SpringDataTaskRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TaskJpaRepository implements ITaskRepository {

    private final SpringDataTaskRepository springDataRepository;
    private final TaskMapper mapper;

    public TaskJpaRepository(SpringDataTaskRepository springDataRepository, TaskMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ObservationTask save(ObservationTask task) {
        TaskJpaEntity entity = mapper.toEntity(task);
        TaskJpaEntity saved = springDataRepository.save(java.util.Objects.requireNonNull(entity));
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ObservationTask> findById(UUID id) {
        return springDataRepository.findById(java.util.Objects.requireNonNull(id)).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObservationTask> findByStatus(TaskStatusEnum status) {
        return springDataRepository.findByStatus(status).stream()
                .map(mapper::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObservationTask> findByObservationExpedienteId(UUID expedienteId) {
        return springDataRepository.findByObservationExpedienteId(expedienteId).stream()
                .map(mapper::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }
}