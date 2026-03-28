package com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository;

import com.lawrabot.divorce_mcp_server.application.port.out.IChildRepository;
import com.lawrabot.divorce_mcp_server.domain.model.Child;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.ChildJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.mapper.ChildMapper;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa.SpringDataChildRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación JPA del puerto de salida para Hijos.
 */
@Repository
@SuppressWarnings("null") // Spring Data JPA methods lack @NonNull annotations
public class ChildJpaRepository implements IChildRepository {

    private final SpringDataChildRepository springDataRepository;
    private final ChildMapper mapper;

    public ChildJpaRepository(SpringDataChildRepository springDataRepository, ChildMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public Child save(Child child) {
        // Al guardar individualmente, el expedienteId debería estar en el dominio o manejarse por el agregado raíz.
        // Como IChildRepository.save(Child) no lo tiene, usamos null o recuperamos si es necesario.
        // En este diseño, los hijos suelen ser guardados vía Expediente, pero este puerto permite guardado individual.
        ChildJpaEntity entity = mapper.toEntity(child, null);
        ChildJpaEntity saved = springDataRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Child> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Child> findByExpedienteId(UUID expedienteId) {
        return springDataRepository.findAllByExpedienteId(expedienteId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
