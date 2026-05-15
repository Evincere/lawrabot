package com.lawrabot.divorce_mcp_server.domain.model;

import com.lawrabot.divorce_mcp_server.domain.enums.ObservationSeverityEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.ObservationStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.TaskStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.TaskTypeEnum;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modelo de dominio que representa una observación del operador sobre un expediente.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Observation {

    private UUID id;
    private UUID expedienteId;
    private String fieldName;
    private ObservationSeverityEnum severity;
    private String message;

    @Nullable
    private String suggestedValue;

    @Builder.Default
    private ObservationStatusEnum status = ObservationStatusEnum.PENDING;

    @Nullable
    private UUID createdBy;

    @Nullable
    private ObservationTask task;

    private LocalDateTime createdAt;

    @Nullable
    private LocalDateTime resolvedAt;

    @Nullable
    private String resolutionNotes;

    /**
     * Método fábrica para crear una nueva observación.
     */
    public static Observation create(UUID expedienteId, String fieldName, ObservationSeverityEnum severity,
                                      String message, @Nullable String suggestedValue, @Nullable UUID createdBy) {
        return Observation.builder()
                .id(UUID.randomUUID())
                .expedienteId(expedienteId)
                .fieldName(fieldName)
                .severity(severity)
                .message(message)
                .suggestedValue(suggestedValue)
                .status(ObservationStatusEnum.PENDING)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Crea una tarea asociada a esta observación y la asigna a LawraBot.
     */
    public ObservationTask createTask(TaskTypeEnum type, boolean isImmediate) {
        ObservationTask newTask = ObservationTask.create(this.id, type, buildMessageTemplate(type), isImmediate);
        this.task = newTask;
        this.status = ObservationStatusEnum.ASSIGNED_TO_BOT;
        return newTask;
    }

    public void resolve(@Nullable String notes, @Nullable String responseData) {
        this.status = ObservationStatusEnum.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        this.resolutionNotes = notes;
        var currentTask = this.task;
        if (currentTask != null) {
            currentTask.complete(responseData);
        }
    }

    public void dismiss() {
        this.status = ObservationStatusEnum.DISMISSED;
        this.resolvedAt = LocalDateTime.now();
    }

    private String buildMessageTemplate(TaskTypeEnum type) {
        return switch (type) {
            case CORRECT_ERROR -> String.format(
                    "Hola! Desde el centro de operaciones han detectado una observación que debes revisar: %s. %sPor favor, indícame la información correcta.",
                    message,
                    suggestedValue != null ? "¿Podría ser que el dato correcto sea '" + suggestedValue + "'? " : "");
            case CLARIFY_DATA -> String.format(
                    "Hola! Necesito que por favor me aclares la siguiente observación: %s",
                    message);
            case REQUEST_DOCUMENT -> String.format(
                    "Hola! Para poder avanzar con tu trámite se requiere documentación adicional: %s",
                    message);
            case NOTIFY_APPOINTMENT -> String.format(
                    "Tengo una notificación importante sobre tu trámite: %s",
                    message);
        };
    }

    /**
     * Modelo anidado que representa una tarea asignada a LawraBot.
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class ObservationTask {

        private UUID id;
        private UUID observationId;
        private TaskTypeEnum type;
        private String messageTemplate;

        @Builder.Default
        private TaskStatusEnum status = TaskStatusEnum.PENDING;

        @Builder.Default
        private final String assignedTo = "LAWRA_BOT";

        @Builder.Default
        private final boolean isImmediate = false;

        private LocalDateTime createdAt;

        @Nullable
        private LocalDateTime sentAt;

        @Nullable
        private LocalDateTime completedAt;

        @Nullable
        private String responseData;

        public static ObservationTask create(UUID observationId, TaskTypeEnum type, String messageTemplate, boolean isImmediate) {
            return ObservationTask.builder()
                    .id(UUID.randomUUID())
                    .observationId(observationId)
                    .type(type)
                    .messageTemplate(messageTemplate)
                    .status(TaskStatusEnum.PENDING)
                    .isImmediate(isImmediate)
                    .assignedTo("LAWRA_BOT")
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        public void markAsSent() {
            this.status = TaskStatusEnum.SENT_TO_BOT;
            this.sentAt = LocalDateTime.now();
        }

        public void complete(@Nullable String responseData) {
            this.status = TaskStatusEnum.COMPLETED;
            this.completedAt = LocalDateTime.now();
            this.responseData = responseData;
        }

        public void fail() {
            this.status = TaskStatusEnum.FAILED;
        }
    }
}