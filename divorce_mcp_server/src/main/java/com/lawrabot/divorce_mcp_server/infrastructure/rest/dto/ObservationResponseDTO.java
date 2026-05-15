package com.lawrabot.divorce_mcp_server.infrastructure.rest.dto;

import com.lawrabot.divorce_mcp_server.domain.enums.ObservationSeverityEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.ObservationStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.TaskStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.TaskTypeEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Observation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObservationResponseDTO {

    private UUID id;
    private UUID expedienteId;
    private String fieldName;
    private ObservationSeverityEnum severity;
    private String message;
    private String suggestedValue;
    private ObservationStatusEnum status;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String resolutionNotes;
    private TaskDTO task;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TaskDTO {
        private UUID id;
        private TaskTypeEnum type;
        private TaskStatusEnum status;
        private String assignedTo;
        private boolean isImmediate;
        private String messageTemplate;
        private LocalDateTime createdAt;
        private LocalDateTime sentAt;
        private LocalDateTime completedAt;
        private String responseData;
    }

    public static ObservationResponseDTO fromDomain(Observation observation) {
        ObservationResponseDTO dto = ObservationResponseDTO.builder()
                .id(observation.getId())
                .expedienteId(observation.getExpedienteId())
                .fieldName(observation.getFieldName())
                .severity(observation.getSeverity())
                .message(observation.getMessage())
                .suggestedValue(observation.getSuggestedValue())
                .status(observation.getStatus())
                .createdBy(observation.getCreatedBy())
                .createdAt(observation.getCreatedAt())
                .resolvedAt(observation.getResolvedAt())
                .resolutionNotes(observation.getResolutionNotes())
                .build();

        var task = observation.getTask();
        if (task != null) {
            var nnTask = java.util.Objects.requireNonNull(task);
            dto.setTask(TaskDTO.builder()
                    .id(nnTask.getId())
                    .type(nnTask.getType())
                    .status(nnTask.getStatus())
                    .assignedTo(nnTask.getAssignedTo())
                    .isImmediate(nnTask.isImmediate())
                    .messageTemplate(nnTask.getMessageTemplate())
                    .createdAt(nnTask.getCreatedAt())
                    .sentAt(nnTask.getSentAt())
                    .completedAt(nnTask.getCompletedAt())
                    .responseData(nnTask.getResponseData())
                    .build());
        }

        return dto;
    }
}