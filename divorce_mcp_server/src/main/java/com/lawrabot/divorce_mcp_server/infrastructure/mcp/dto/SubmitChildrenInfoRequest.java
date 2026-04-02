package com.lawrabot.divorce_mcp_server.infrastructure.mcp.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;
import java.util.UUID;

public record SubmitChildrenInfoRequest(
    @JsonPropertyDescription("ID UUID del Expediente de divorcio.")
    UUID expedienteId,
    
    @JsonPropertyDescription("Lista de hijos de la pareja. Si no tienen hijos en común, este array debe estar vacío.")
    List<ChildDto> children
) {}
