package com.lawrabot.divorce_mcp_server.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Servicio de búsqueda legal avanzada.
 * F5: Implementación de HyDE y recuperación modular.
 */
@Service
@Slf4j
@org.springframework.context.annotation.Profile("!test")
public class AdvancedRagService {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;

    public AdvancedRagService(ChatModel chatModel, VectorStore vectorStore, JdbcTemplate jdbcTemplate) {
        this.chatModel = chatModel;
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Realiza una búsqueda legal completa usando HyDE, recuperación híbrida y RRF
     * Fusion.
     * Pilares 1 & 2 del diseño avanzado.
     */
    public Mono<List<Document>> searchLegalKnowledge(String userQuery) {
        return Mono.fromCallable(() -> {
            log.info("Iniciando búsqueda híbrida avanzada: {}", userQuery);
            long totalStart = System.currentTimeMillis();

            // 1. HyDE
            long hydeStart = System.currentTimeMillis();
            String hypotheticalDoc = generateHydeDocument(userQuery);
            log.info("Fase HyDE completada en {}ms", System.currentTimeMillis() - hydeStart);

            // 2. Rama 1: Búsqueda Semántica (Dense)
            long denseStart = System.currentTimeMillis();
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(java.util.Objects.requireNonNull(hypotheticalDoc != null ? hypotheticalDoc : userQuery))
                    .topK(20)
                    .similarityThreshold(0.60)
                    .build();
            List<Document> denseResults = vectorStore.similaritySearch(searchRequest);
            log.info("Fase Búsqueda Semántica (Dense) completada en {}ms", System.currentTimeMillis() - denseStart);

            // 3. Rama 2: Búsqueda Léxica (Lexical/BM25 similar) vía SQL
            long lexicalStart = System.currentTimeMillis();
            List<Document> lexicalResults = searchLexical(userQuery);
            log.info("Fase Búsqueda Léxica (SQL) completada en {}ms", System.currentTimeMillis() - lexicalStart);

            // 4. Pilar 2: Fusión de Rankings (RRF)
            List<Document> fusedResults = reciprocalRankFusion(denseResults, lexicalResults);

            // 5. Pilar 3: Expansión a Contexto Padre (Parent-Child Retrieval)
            List<Document> expandedResults = expandToParentContext(fusedResults);

            log.info("Búsqueda finalizada en {}ms. {} artículos consolidados.", 
                    System.currentTimeMillis() - totalStart, expandedResults.size());
            return expandedResults;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Expande los child chunks recuperados a su contexto completo de Artículo.
     * Pilar 3: Return Big.
     */
    private List<Document> expandToParentContext(List<Document> children) {
        if (children.isEmpty())
            return children;

        // 1. Identificar padres únicos (filtrando nulos)
        List<String> parentIds = children.stream()
                .map(doc -> (String) doc.getMetadata().get("parent_id"))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 2. Recuperar todos los hijos de esos padres para reconstruir el artículo
        List<Document> fullContexts = new ArrayList<>();

        for (String parentId : parentIds) {
            // Usamos DISTINCT para evitar repeticiones si hubo múltiples ingestas
            String sql = "SELECT DISTINCT content FROM vector_store WHERE metadata->>'parent_id' = ? ORDER BY content ASC";
            try {
                List<String> parts = jdbcTemplate.queryForList(sql, String.class, parentId);
                String fullArticle = (parts != null && !parts.isEmpty()) ? String.join("\n", parts) : "";
                
                fullContexts.add(new Document(
                    java.util.Objects.requireNonNull(parentId != null ? parentId : "unknown"), 
                    java.util.Objects.requireNonNull(fullArticle), 
                    java.util.Objects.requireNonNull(Map.of("is_parent", true, "article_id", parentId != null ? parentId : "unknown"))
                ));
            } catch (Exception e) {
                log.error("Error expandiendo contexto para {}: {}", parentId, e.getMessage());
            }
        }

        return fullContexts;
    }

    /**
     * Búsqueda léxica directa en PostgreSQL usando Full-Text Search sobre el
     * contenido y metadatos.
     */
    private List<Document> searchLexical(String query) {
        // Implementación simplificada usando ts_rank sobre el contenido
        // En producción, esto usaría columnas indexadas GIN.
        String sql = """
                    SELECT id, content, metadata->>'parent_id' as parent_id, ts_rank(to_tsvector('spanish', content), plainto_tsquery('spanish', ?)) as rank
                    FROM vector_store
                    WHERE to_tsvector('spanish', content) @@ plainto_tsquery('spanish', ?)
                    ORDER BY rank DESC
                    LIMIT 20
                """;

        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                String id = rs.getString("id");
                String content = rs.getString("content");
                String parentId = rs.getString("parent_id");
                return new Document(
                    java.util.Objects.requireNonNull(id != null ? id : java.util.UUID.randomUUID().toString()),
                    java.util.Objects.requireNonNull(content != null ? content : ""),
                    java.util.Objects.requireNonNull(Map.of("parent_id", parentId != null ? parentId : "General"))
                );
            }, query, query);
        } catch (Exception e) {
            log.error("Error en búsqueda léxica SQL: {}. Continuando solo con semántica.", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Algoritmo Reciprocal Rank Fusion (RRF).
     * Fusiona dos listas de ranking en una sola basada en sus posiciones.
     */
    private List<Document> reciprocalRankFusion(List<Document> list1, List<Document> list2) {
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, Document> documentMap = new HashMap<>();

        int k = 60; // Constante de suavizado estándar

        // Sumar scores de la primera lista
        for (int i = 0; i < list1.size(); i++) {
            Document doc = list1.get(i);
            documentMap.put(doc.getId(), doc);
            rrfScores.put(doc.getId(), rrfScores.getOrDefault(doc.getId(), 0.0) + (1.0 / (k + i + 1)));
        }

        // Sumar scores de la segunda lista
        for (int i = 0; i < list2.size(); i++) {
            Document doc = list2.get(i);
            documentMap.put(doc.getId(), doc);
            rrfScores.put(doc.getId(), rrfScores.getOrDefault(doc.getId(), 0.0) + (1.0 / (k + i + 1)));
        }

        // Ordenar por el nuevo score acumulado
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .map(entry -> documentMap.get(entry.getKey()))
                .collect(Collectors.toList());
    }

    /**
     * Genera un texto técnico legal basado en la duda coloquial del usuario.
     * Prompt Estricto (Aislamiento jurídico).
     */
    private String generateHydeDocument(String userQuery) {
        String hydePrompt = """
                Eres un asistente de redacción técnico-jurídica experto en Derecho de Familia Argentino.

                OBJETIVO: Transforma la siguiente duda coloquial en un párrafo técnico formal que describa la situación
                usando terminología del Código Civil y Comercial de la Nación.

                REGLAS:
                - NO respondas la duda. Solo redáctala técnicamente.
                - Usa términos como 'obligación alimentaria', 'convenio regulador', 'atribución de vivienda', 'responsabilidad parental'.
                - NO inventes leyes ni artículos.
                - El texto generado será usado para buscar en una base de datos vectorial legal.

                DUDA COLOQUIAL: %s

                ANÁLISIS TÉCNICO HIPOTÉTICO:
                """
                .formatted(userQuery);

        try {
            return chatModel.call(hydePrompt);
        } catch (Exception e) {
            log.error("Error en HyDE Generator: {}. Usando query original.", e.getMessage());
            return userQuery;
        }
    }
}
