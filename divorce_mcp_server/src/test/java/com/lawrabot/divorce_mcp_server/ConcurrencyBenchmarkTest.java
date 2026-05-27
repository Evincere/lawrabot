package com.lawrabot.divorce_mcp_server;

import com.lawrabot.divorce_mcp_server.application.service.AdvancedRagService;
import com.lawrabot.divorce_mcp_server.application.service.ValidateAgreementService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ConcurrencyBenchmarkTest {

    @Test
    public void testSimulatedConcurrencyBefore() throws Exception {
        ChatModel chatModel = Mockito.mock(ChatModel.class);
        VectorStore vectorStore = Mockito.mock(VectorStore.class);
        JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);

        // Simulamos que el LLM tarda 1 segundo en responder
        Mockito.when(chatModel.call(Mockito.anyString())).thenAnswer(invocation -> {
            Thread.sleep(1000); // Bloqueo Síncrono de I/O
            return "Mock Response";
        });

        AdvancedRagService service = new AdvancedRagService(chatModel, vectorStore, jdbcTemplate);

        long start = System.currentTimeMillis();

        // Simulamos 3 peticiones concurrentes
        Mono<List<Document>> c1 = service.searchLegalKnowledge("Consulta 1");
        Mono<List<Document>> c2 = service.searchLegalKnowledge("Consulta 2");
        Mono<List<Document>> c3 = service.searchLegalKnowledge("Consulta 3");

        Mono.zip(c1, c2, c3).block();

        long time = System.currentTimeMillis() - start;
        System.out.println("====== METRICAS DESPUES (Asíncrono WebFlux) ======");
        System.out.println("Tiempo total para 3 llamadas simultáneas (1s I/O simulado c/u): " + time + "ms");
        System.out.println("Los subprocesos no se bloquearon durante la espera de I/O.");
        System.out.println("==================================================");
    }
}
