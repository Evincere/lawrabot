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
        // Normalización: aseguramos que buscamos por el valor de 10 dígitos (ej: 261XXXXXXX)
        // ya que así lo guarda PhoneNumberVO.getValue()
        String normalizedPhone = com.lawrabot.divorce_mcp_server.domain.valueobject.PhoneNumberVO.of(phoneNumber).getValue();

        List<ExpedienteStatusEnum> activeStatuses = Arrays.asList(
            ExpedienteStatusEnum.BLSG_PRECONSULTA,
            ExpedienteStatusEnum.IN_DATA_COLLECTION_PROGRESS,
            ExpedienteStatusEnum.DATA_COMPLETE,
            ExpedienteStatusEnum.BLSG_RECHAZADO,
            ExpedienteStatusEnum.WAITING_SIGNATURE,
            ExpedienteStatusEnum.READY_FOR_PORTAL,
            ExpedienteStatusEnum.OBSERVATIONS_PENDING,
            ExpedienteStatusEnum.DOCUMENTS_GENERATED
        );
        
        return springDataRepository.findFirstByContactPhoneNumberPhoneNumberAndStatusIn(normalizedPhone, activeStatuses)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Expediente> findActiveByDni(String dni) {
        List<ExpedienteStatusEnum> activeStatuses = Arrays.asList(
            ExpedienteStatusEnum.BLSG_PRECONSULTA,
            ExpedienteStatusEnum.IN_DATA_COLLECTION_PROGRESS,
            ExpedienteStatusEnum.DATA_COMPLETE,
            ExpedienteStatusEnum.BLSG_RECHAZADO,
            ExpedienteStatusEnum.WAITING_SIGNATURE,
            ExpedienteStatusEnum.READY_FOR_PORTAL,
            ExpedienteStatusEnum.OBSERVATIONS_PENDING,
            ExpedienteStatusEnum.DOCUMENTS_GENERATED
        );
        
        return springDataRepository.findFirstByDniAndStatusIn(dni, activeStatuses)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Expediente> findAll() {
        return springDataRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(mapper::toDomain)
                .collect(java.util.stream.Collectors.toList());
    }
}
