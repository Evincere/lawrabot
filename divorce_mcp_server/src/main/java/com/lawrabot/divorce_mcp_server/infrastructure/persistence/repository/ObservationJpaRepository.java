package com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository;

import com.lawrabot.divorce_mcp_server.application.port.out.IObservationRepository;
import com.lawrabot.divorce_mcp_server.domain.enums.ObservationStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Observation;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.ObservationJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.mapper.ObservationMapper;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa.SpringDataObservationRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ObservationJpaRepository implements IObservationRepository {

    private final SpringDataObservationRepository springDataRepository;
    private final ObservationMapper mapper;

    public ObservationJpaRepository(SpringDataObservationRepository springDataRepository, ObservationMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Observation save(Observation observation) {
        ObservationJpaEntity entity = mapper.toEntity(observation);
        ObservationJpaEntity saved = springDataRepository.save(java.util.Objects.requireNonNull(entity));
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Observation> findById(UUID id) {
        return springDataRepository.findById(java.util.Objects.requireNonNull(id)).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Observation> findByExpedienteId(UUID expedienteId) {
        return mapper.toDomainList(springDataRepository.findByExpedienteId(expedienteId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Observation> findByExpedienteIdAndStatus(UUID expedienteId, ObservationStatusEnum status) {
        return mapper.toDomainList(springDataRepository.findByExpedienteIdAndStatus(expedienteId, status));
    }

    @Override
    @Transactional(readOnly = true)
    public long countByExpedienteIdAndStatusIn(UUID expedienteId, List<ObservationStatusEnum> statuses) {
        return springDataRepository.countByExpedienteIdAndStatusIn(expedienteId, statuses);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return springDataRepository.existsById(java.util.Objects.requireNonNull(id));
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        springDataRepository.deleteById(java.util.Objects.requireNonNull(id));
    }
}