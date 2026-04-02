package com.lawrabot.divorce_mcp_server.application.port.out;

import com.lawrabot.divorce_mcp_server.domain.model.CaseParticipant;
import java.util.List;
import java.util.UUID;

public interface ICaseParticipantRepository {
    List<CaseParticipant> findByCitizenId(UUID citizenId);
    List<CaseParticipant> findByExpedienteId(UUID expedienteId);
    CaseParticipant save(CaseParticipant participant);
}
