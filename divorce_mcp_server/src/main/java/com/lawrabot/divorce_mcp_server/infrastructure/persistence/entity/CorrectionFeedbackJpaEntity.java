package com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad para registrar correcciones manuales realizadas por el operador humano.
 * Alimenta el Learning Loop para mejorar la extracción del agente.
 */
@Entity
@Table(name = "correction_feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CorrectionFeedbackJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "field_name", nullable = false)
    private String fieldName; // ej: "conyuge_1_dni"

    @Column(name = "original_text", columnDefinition = "TEXT")
    private String originalText; // Texto crudo de la conversación

    @Column(name = "ai_value", columnDefinition = "TEXT")
    private String aiValue; // Valor extraído incorrectamente por la AI

    @Column(name = "human_value", columnDefinition = "TEXT")
    private String humanValue; // Valor corregido por el humano

    @Column(name = "citizen_id")
    private UUID citizenId; // Opcional: link al ciudadano si aplica

    @Column(name = "case_id")
    private UUID caseId; // Opcional: link al expediente si aplica

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // Política de retención (6 meses)

    @Builder.Default
    @Column(name = "is_processed")
    private boolean isProcessed = false; // Flag para saber si ya se usó para few-shot

    @PrePersist
    protected void onCreate() {
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMonths(6);
        }
    }
}
