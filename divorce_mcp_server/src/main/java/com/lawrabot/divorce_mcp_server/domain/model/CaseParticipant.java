package com.lawrabot.divorce_mcp_server.domain.model;

import com.lawrabot.divorce_mcp_server.domain.enums.CaseRole;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * Representa la participación de un ciudadano en un expediente legal específico.
 */
@Getter
@Builder
public class CaseParticipant {
    private final UUID id;
    private final Citizen citizen;
    private final CaseRole role;
    private final String interventionSummary;

    public static CaseParticipant create(Citizen citizen, CaseRole role, String summary) {
        return CaseParticipant.builder()
                .id(UUID.randomUUID())
                .citizen(citizen)
                .role(role)
                .interventionSummary(summary)
                .build();
    }
}
