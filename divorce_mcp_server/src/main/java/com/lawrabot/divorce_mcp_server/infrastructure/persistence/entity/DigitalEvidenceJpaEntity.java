package com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "digital_evidences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DigitalEvidenceJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = false)
    @JsonIgnore
    private ExpedienteJpaEntity expediente;

    @JsonProperty("expedienteId")
    public UUID getExpedienteId() {
        return expediente != null ? expediente.getId() : null;
    }

    @Column(name = "document_type", nullable = false)
    private String documentType;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "is_approved")
    private boolean approved;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "child_full_name")
    private String childFullName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
