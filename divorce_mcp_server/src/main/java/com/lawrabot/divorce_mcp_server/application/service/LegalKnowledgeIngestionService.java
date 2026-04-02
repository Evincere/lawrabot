package com.lawrabot.divorce_mcp_server.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Servicio encargado de ingerir la normativa legal al iniciar el servidor.
 * F5: Inteligencia Jurídica con Estructura Jerárquica.
 */
@Service
@Slf4j
public class LegalKnowledgeIngestionService implements CommandLineRunner {

    private final VectorStore vectorStore;

    @Value("classpath:knowledge/normativa_divorcio_mendoza.md")
    private Resource resource;

    public LegalKnowledgeIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) throws Exception {
        ingestLegalKnowledge();
    }

    public void ingestLegalKnowledge() {
        try {
            log.info("Iniciando ingesta de conocimiento legal desde: {}", resource.getFilename());

            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            List<Document> documents = parseHierarchicalMarkdown(content);

            if (!documents.isEmpty()) {
                vectorStore.add(documents);
                log.info("Exito: {} fragmentos legales ingeridos en el almacén de vectores.", documents.size());
            } else {
                log.warn("Advertencia: No se encontraron fragmentos válidos en la normativa.");
            }

        } catch (Exception e) {
            log.error("Error durante la ingesta legal: {}", e.getMessage(), e);
        }
    }

    /**
     * Parsea el MD buscando patrones (ARTÍCULO X) y fragmenta en párrafos
     * (Incisos).
     * Implementación Pilar 3: Search Small.
     */
    private List<Document> parseHierarchicalMarkdown(String content) {
        List<Document> docs = new ArrayList<>();

        String currentParent = "General";
        StringBuilder currentParagraph = new StringBuilder();

        try (Scanner scanner = new Scanner(content)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                if (line.startsWith("# (ARTÍCULO") || line.startsWith("# (JURISDICCIÓN")) {
                    // Guardar último párrafo del bloque anterior
                    if (!currentParagraph.toString().isBlank()) {
                        docs.add(createDocument(currentParent, currentParagraph.toString()));
                    }
                    currentParent = line.replace("#", "").trim();
                    currentParagraph = new StringBuilder();
                } else if (line.isBlank()) {
                    // Cambio de párrafo: Ingerir fragmento pequeño
                    if (!currentParagraph.toString().isBlank()) {
                        docs.add(createDocument(currentParent, currentParagraph.toString()));
                        currentParagraph = new StringBuilder();
                    }
                } else {
                    currentParagraph.append(line).append(" ");
                }
            }
        }

        // El último remanente
        if (!currentParagraph.toString().isBlank()) {
            docs.add(createDocument(currentParent, currentParagraph.toString()));
        }

        return docs;
    }

    private Document createDocument(String parent, String text) {
        return new Document(
            java.util.Objects.requireNonNull(java.util.UUID.randomUUID().toString()),
            text != null ? text : "",
            java.util.Objects.requireNonNull(Map.of(
                "parent_id", parent != null ? parent : "General",
                "source", "normativa_divorcio_mendoza.md",
                "jurisdiccion", "Mendoza",
                "tipo_parent", (parent != null && parent.contains("ARTÍCULO")) ? "ARTÍCULO" : "ANOTACIÓN"
            ))
        );
    }
}
