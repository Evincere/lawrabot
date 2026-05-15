package com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa;

import com.lawrabot.divorce_mcp_server.application.port.out.ICitizenRepository;
import com.lawrabot.divorce_mcp_server.domain.model.Citizen;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.CitizenJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.mapper.CitizenMapper;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.CitizenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SpringDataCitizenRepository implements ICitizenRepository {

    private final CitizenJpaRepository jpaRepository;
    private final CitizenMapper citizenMapper;

    @Override
    public Optional<Citizen> findById(UUID id) {
        Objects.requireNonNull(id, "ID cannot be null");
        return jpaRepository.findById(id).map(citizenMapper::toDomain);
    }

    @Override
    public Optional<Citizen> findByDni(String dni) {
        Objects.requireNonNull(dni, "DNI cannot be null");
        return jpaRepository.findByDni(dni).map(citizenMapper::toDomain);
    }

    @Override
    @SuppressWarnings("null")
    public Citizen save(Citizen citizen) {
        Objects.requireNonNull(citizen, "Citizen cannot be null");
        CitizenJpaEntity entity = citizenMapper.toEntity(citizen);
        CitizenJpaEntity saved = jpaRepository.save(entity);
        return citizenMapper.toDomain(saved);
    }
}
