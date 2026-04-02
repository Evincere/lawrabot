package com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository;

import com.lawrabot.divorce_mcp_server.domain.enums.CaseRole;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.CaseParticipantJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CaseParticipantJpaRepository extends JpaRepository<CaseParticipantJpaEntity, UUID> {
    List<CaseParticipantJpaEntity> findByCitizenId(UUID citizenId);
    List<CaseParticipantJpaEntity> findByExpedienteId(UUID expedienteId);
    List<CaseParticipantJpaEntity> findByCitizenIdAndRole(UUID citizenId, CaseRole role);
}
