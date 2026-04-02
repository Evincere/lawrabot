package com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository;

import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.CitizenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CitizenJpaRepository extends JpaRepository<CitizenJpaEntity, UUID> {
    Optional<CitizenJpaEntity> findByDni(String dni);
    Optional<CitizenJpaEntity> findByCuil(String cuil);
}
