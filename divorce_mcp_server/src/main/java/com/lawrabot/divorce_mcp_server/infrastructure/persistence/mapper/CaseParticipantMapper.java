package com.lawrabot.divorce_mcp_server.infrastructure.persistence.mapper;

import com.lawrabot.divorce_mcp_server.domain.model.CaseParticipant;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.CaseParticipantJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CaseParticipantMapper {

    private final CitizenMapper citizenMapper;

    public CaseParticipant toDomain(CaseParticipantJpaEntity entity) {
        if (entity == null) return null;

        return CaseParticipant.builder()
                .id(entity.getId())
                .citizen(citizenMapper.toDomain(entity.getCitizen()))
                .role(entity.getRole())
                .interventionSummary(entity.getInterventionSummary())
                .build();
    }

    public CaseParticipantJpaEntity toEntity(CaseParticipant domain) {
        if (domain == null) return null;

        return CaseParticipantJpaEntity.builder()
                .id(domain.getId())
                .citizen(citizenMapper.toEntity(domain.getCitizen()))
                .role(domain.getRole())
                .interventionSummary(domain.getInterventionSummary())
                .build();
    }
}
