package com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa;

import com.lawrabot.divorce_mcp_server.application.port.out.ICorrectionFeedbackRepository;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.CorrectionFeedbackJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.CorrectionFeedbackJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SpringDataCorrectionFeedbackRepository implements ICorrectionFeedbackRepository {

    private final CorrectionFeedbackJpaRepository jpaRepository;

    @Override
    public void save(CorrectionFeedbackJpaEntity feedback) {
        jpaRepository.save(feedback);
    }

    @Override
    public List<CorrectionFeedbackJpaEntity> findUnprocessed() {
        return jpaRepository.findByIsProcessedFalse();
    }
}
