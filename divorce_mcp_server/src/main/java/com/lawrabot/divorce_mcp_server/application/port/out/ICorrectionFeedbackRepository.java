package com.lawrabot.divorce_mcp_server.application.port.out;

import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.CorrectionFeedbackJpaEntity;
import java.util.List;

public interface ICorrectionFeedbackRepository {
    void save(CorrectionFeedbackJpaEntity feedback);
    List<CorrectionFeedbackJpaEntity> findUnprocessed();
}
