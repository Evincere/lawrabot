package com.lawrabot.divorce_mcp_server.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;
import com.lawrabot.divorce_mcp_server.domain.model.CaseParticipant;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.CaseParticipantJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.ExpedienteJpaEntity;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CaseParticipantMapper {

    private final CitizenMapper citizenMapper;

    public CaseParticipant toDomain(CaseParticipantJpaEntity entity) {
        if (entity == null)
            return null;

        ExpedienteJpaEntity exp = entity.getExpediente();
        String type = "Divorcio";
        if (exp != null && exp.getDivorceType() != null) {
            type = "Divorcio " + exp.getDivorceType();
        }

        return CaseParticipant.builder()
                .id(entity.getId())
                .citizen(citizenMapper.toDomain(entity.getCitizen()))
                .role(entity.getRole())
                .interventionSummary(entity.getInterventionSummary())
                .expedienteId(exp != null ? exp.getId() : null)
                .expedienteStatus(exp != null ? exp.getStatus().name() : "N/A")
                .expedienteType(type)
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public CaseParticipantJpaEntity toEntity(CaseParticipant domain) {
        if (domain == null)
            return null;

        return CaseParticipantJpaEntity.builder()
                .id(domain.getId())
                .citizen(citizenMapper.toEntity(domain.getCitizen()))
                .role(domain.getRole())
                .interventionSummary(domain.getInterventionSummary())
                // Nota: El link al Expediente suele setearse en el Agregado Raíz (Expediente)
                .build();
    }
}
