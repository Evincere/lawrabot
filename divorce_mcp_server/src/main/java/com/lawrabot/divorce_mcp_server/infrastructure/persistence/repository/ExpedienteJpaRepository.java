package com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository;

import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.enums.ExpedienteStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.ExpedienteJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.mapper.ExpedienteMapper;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa.SpringDataExpedienteRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementación JPA del puerto de salida para la gestión de Expedientes.
 */
@Repository
@SuppressWarnings("null") // Spring Data JPA methods lack @NonNull annotations
public class ExpedienteJpaRepository implements IExpedienteRepository {

    private final SpringDataExpedienteRepository springDataRepository;
    private final ExpedienteMapper mapper;

    public ExpedienteJpaRepository(SpringDataExpedienteRepository springDataRepository, ExpedienteMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Expediente save(Expediente expediente) {
        ExpedienteJpaEntity entity = mapper.toEntity(expediente);
        ExpedienteJpaEntity saved = springDataRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Expediente> findById(UUID id) {
        return springDataRepository.findByIdWithChildren(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Expediente> findActiveByClientPhone(String phoneNumber) {
        // Un expediente se considera "activo" si no está cerrado
        List<ExpedienteStatusEnum> activeStatuses = Arrays.asList(
            ExpedienteStatusEnum.BLSG_PRECONSULTA,
            ExpedienteStatusEnum.IN_DATA_COLLECTION_PROGRESS,
            ExpedienteStatusEnum.DATA_COMPLETE
        );
        
        return springDataRepository.findFirstByContactPhoneNumberPhoneNumberAndStatusIn(phoneNumber, activeStatuses)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Expediente> findActiveByDni(String dni) {
        List<ExpedienteStatusEnum> activeStatuses = Arrays.asList(
            ExpedienteStatusEnum.BLSG_PRECONSULTA,
            ExpedienteStatusEnum.IN_DATA_COLLECTION_PROGRESS,
            ExpedienteStatusEnum.DATA_COMPLETE
        );
        
        return springDataRepository.findFirstByDniAndStatusIn(dni, activeStatuses)
                .map(mapper::toDomain);
    }
}
