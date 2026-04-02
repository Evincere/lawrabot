package com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa;

import com.lawrabot.divorce_mcp_server.application.port.out.ICaseParticipantRepository;
import com.lawrabot.divorce_mcp_server.domain.model.CaseParticipant;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.CaseParticipantJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.mapper.CaseParticipantMapper;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.CaseParticipantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SpringDataCaseParticipantRepository implements ICaseParticipantRepository {

    private final CaseParticipantJpaRepository jpaRepository;
    private final CaseParticipantMapper participantMapper;

    @Override
    public List<CaseParticipant> findByCitizenId(UUID citizenId) {
        return jpaRepository.findByCitizenId(citizenId).stream()
                .map(participantMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CaseParticipant> findByExpedienteId(UUID expedienteId) {
        return jpaRepository.findByExpedienteId(expedienteId).stream()
                .map(participantMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public CaseParticipant save(CaseParticipant participant) {
        CaseParticipantJpaEntity entity = participantMapper.toEntity(participant);
        CaseParticipantJpaEntity saved = jpaRepository.save(entity);
        return participantMapper.toDomain(saved);
    }
}
