package com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity;

import com.lawrabot.divorce_mcp_server.domain.enums.ObservationSeverityEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.ObservationStatusEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA para la tabla 'observations'.
 * Registra observaciones creadas por el operador humano sobre datos de un expediente.
 */
@Entity
@Table(name = "observations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObservationJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "expediente_id", nullable = false)
    private UUID expedienteId;

    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private ObservationSeverityEnum severity;

    @Column(name = "message", columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "suggested_value", columnDefinition = "TEXT")
    private String suggestedValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ObservationStatusEnum status = ObservationStatusEnum.PENDING;

    @Column(name = "created_by")
    private UUID createdBy;

    @OneToOne(mappedBy = "observation", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    private TaskJpaEntity task;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
}