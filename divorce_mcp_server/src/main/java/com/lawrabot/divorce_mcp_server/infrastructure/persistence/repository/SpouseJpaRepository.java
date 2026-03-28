package com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository;

import com.lawrabot.divorce_mcp_server.application.port.out.ISpouseRepository;
import com.lawrabot.divorce_mcp_server.domain.model.Spouse;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.SpouseJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.mapper.SpouseMapper;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa.SpringDataSpouseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Implementación JPA del puerto de salida para Cónyuges.
 */
@Repository
@SuppressWarnings("null") // Spring Data JPA methods lack @NonNull annotations
public class SpouseJpaRepository implements ISpouseRepository {

    private final SpringDataSpouseRepository springDataRepository;
    private final SpouseMapper mapper;

    public SpouseJpaRepository(SpringDataSpouseRepository springDataRepository, SpouseMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public Spouse save(Spouse spouse) {
        SpouseJpaEntity entity = mapper.toEntity(spouse);
        SpouseJpaEntity saved = springDataRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Spouse> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }
}
