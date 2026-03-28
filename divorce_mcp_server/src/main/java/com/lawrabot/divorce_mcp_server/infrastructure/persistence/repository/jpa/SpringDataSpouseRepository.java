package com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa;

import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.SpouseJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataSpouseRepository extends JpaRepository<SpouseJpaEntity, UUID> {
}
