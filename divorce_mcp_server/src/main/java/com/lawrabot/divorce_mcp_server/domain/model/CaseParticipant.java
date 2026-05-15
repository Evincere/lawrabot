package com.lawrabot.divorce_mcp_server.domain.model;

import com.lawrabot.divorce_mcp_server.domain.enums.CaseRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa la participación de un ciudadano en un expediente legal específico.
 * Incluye metadatos básicos del expediente para facilitar la vista de historial.
 */
@Getter
@Builder
public class CaseParticipant {
    private final UUID id;
    private final Citizen citizen;
    private final CaseRole role;
    private final String interventionSummary;
    
    // Metadatos enriquecidos del expediente (para evitar recursión)
    private final UUID expedienteId;
    private final String expedienteStatus;
    private final String expedienteType; 
    private final LocalDateTime createdAt;

    public static CaseParticipant create(Citizen citizen, CaseRole role, String summary) {
        return CaseParticipant.builder()
                .id(UUID.randomUUID())
                .citizen(citizen)
                .role(role)
                .interventionSummary(summary)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
