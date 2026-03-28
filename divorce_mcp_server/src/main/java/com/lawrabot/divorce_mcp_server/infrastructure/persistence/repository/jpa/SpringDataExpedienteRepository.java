package com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa;

import com.lawrabot.divorce_mcp_server.domain.enums.ExpedienteStatusEnum;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.ExpedienteJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataExpedienteRepository extends JpaRepository<ExpedienteJpaEntity, UUID> {

    @Query("SELECT e FROM ExpedienteJpaEntity e WHERE e.contactPhoneNumber.phoneNumber = :phone AND e.status IN :statuses ORDER BY e.createdAt DESC LIMIT 1")
    Optional<ExpedienteJpaEntity> findFirstByContactPhoneNumberPhoneNumberAndStatusIn(
            @Param("phone") String phone,
            @Param("statuses") List<ExpedienteStatusEnum> statuses);
}
