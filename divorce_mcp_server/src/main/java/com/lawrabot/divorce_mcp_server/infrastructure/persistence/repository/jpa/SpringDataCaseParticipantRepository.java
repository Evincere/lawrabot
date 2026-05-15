package com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa;

import com.lawrabot.divorce_mcp_server.application.port.out.ICaseParticipantRepository;
import com.lawrabot.divorce_mcp_server.domain.model.CaseParticipant;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.CaseParticipantJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.mapper.CaseParticipantMapper;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.CaseParticipantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SpringDataCaseParticipantRepository implements ICaseParticipantRepository {

    private final CaseParticipantJpaRepository jpaRepository;
    private final CaseParticipantMapper participantMapper;

    @Override
    public List<CaseParticipant> findByCitizenId(UUID citizenId) {
        Objects.requireNonNull(citizenId, "Citizen ID cannot be null");
        return jpaRepository.findByCitizenId(citizenId).stream()
                .map(participantMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CaseParticipant> findByExpedienteId(UUID expedienteId) {
        Objects.requireNonNull(expedienteId, "Expediente ID cannot be null");
        return jpaRepository.findByExpedienteId(expedienteId).stream()
                .map(participantMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @SuppressWarnings("null")
    public CaseParticipant save(CaseParticipant participant) {
        Objects.requireNonNull(participant, "Participant cannot be null");
        CaseParticipantJpaEntity entity = participantMapper.toEntity(participant);
        CaseParticipantJpaEntity saved = jpaRepository.save(entity);
        return participantMapper.toDomain(saved);
    }
}
