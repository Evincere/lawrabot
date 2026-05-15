package com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity;

import com.lawrabot.divorce_mcp_server.domain.enums.TaskStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.TaskTypeEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA para la tabla 'tasks'.
 * Representa tareas asignadas a LawraBot derivadas de observaciones del operador.
 */
@Entity
@Table(name = "tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "observation_id", nullable = false)
    private ObservationJpaEntity observation;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private TaskTypeEnum type;

    @Column(name = "message_template", columnDefinition = "TEXT")
    private String messageTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private TaskStatusEnum status = TaskStatusEnum.PENDING;

    @Column(name = "assigned_to", length = 50)
    @Builder.Default
    private String assignedTo = "LAWRA_BOT";

    @Column(name = "is_immediate")
    private boolean isImmediate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "response_data", columnDefinition = "TEXT")
    private String responseData;
}