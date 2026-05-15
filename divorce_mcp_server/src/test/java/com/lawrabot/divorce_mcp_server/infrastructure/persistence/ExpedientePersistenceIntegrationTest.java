package com.lawrabot.divorce_mcp_server.infrastructure.persistence;

import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.enums.DataCollectionStageEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.ExpedienteStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.domain.valueobject.PhoneNumberVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "spring.autoconfigure.exclude=org.springframework.ai.autoconfigure.vectorstore.pgvector.PgVectorStoreAutoConfiguration")
@ActiveProfiles("test")
@Transactional
class ExpedientePersistenceIntegrationTest {

    @Autowired
    private IExpedienteRepository expedienteRepository;

    @Test
    void testSaveAndFindExpediente() {
        // 1. Create a minimal domain object
        PhoneNumberVO phone = PhoneNumberVO.of("2614123456");
        Expediente expediente = Expediente.builder()
                .id(java.util.UUID.randomUUID())
                .contactPhoneNumber(phone)
                .status(ExpedienteStatusEnum.IN_DATA_COLLECTION_PROGRESS)
                .collectionStage(DataCollectionStageEnum.PENDING_PERSONAL_DATA)
                .createdAt(java.time.LocalDateTime.now())
                .build();

        // 2. Save using the repository (Outport implementation)
        Expediente savedExpediente = expedienteRepository.save(expediente);
        assertNotNull(savedExpediente);
        assertEquals(expediente.getId(), savedExpediente.getId());

        // 3. Retrieve and verify
        Optional<Expediente> found = expedienteRepository.findById(expediente.getId());
        assertTrue(found.isPresent());
        assertEquals("2614123456", found.get().getContactPhoneNumber().getValue());
        assertEquals(ExpedienteStatusEnum.IN_DATA_COLLECTION_PROGRESS, found.get().getStatus());
    }
}
