package com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository;

import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.CorrectionFeedbackJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CorrectionFeedbackJpaRepository extends JpaRepository<CorrectionFeedbackJpaEntity, UUID> {
    
    List<CorrectionFeedbackJpaEntity> findByIsProcessedFalse();
    
    List<CorrectionFeedbackJpaEntity> findByFieldName(String fieldName);
    
    @Query("SELECT f FROM CorrectionFeedbackJpaEntity f WHERE f.expiresAt > ?1")
    List<CorrectionFeedbackJpaEntity> findAllActive(LocalDateTime now);

    void deleteByExpiresAtBefore(LocalDateTime now);
}
