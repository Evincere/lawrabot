package com.lawrabot.divorce_mcp_server.infrastructure.rest.dto;

import com.lawrabot.divorce_mcp_server.domain.enums.ObservationSeverityEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.TaskTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateObservationRequest {

    private UUID expedienteId;
    private String fieldName;
    private ObservationSeverityEnum severity;
    private String message;

    private String suggestedValue;

    @Builder.Default
    private boolean createTask = true;

    private TaskTypeEnum taskType;

    @Builder.Default
    @JsonProperty("isImmediate")
    private boolean immediate = false;

    private String operatorId;
}