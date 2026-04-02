package com.lawrabot.divorce_mcp_server.infrastructure.mcp.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.UUID;

public record SubmitMarriageDetailsRequest(
    @JsonPropertyDescription("ID UUID del Expediente de divorcio.")
    UUID expedienteId,
    
    @JsonPropertyDescription("Fecha de celebración del matrimonio en formato ISO YYYY-MM-DD. Si no la recuerda, usar la más aproximada.")
    String marriageDate,
    
    @JsonPropertyDescription("Fecha aproximada en la que dejaron de convivir en formato ISO YYYY-MM-DD. Si siguen conviviendo, ignorar o enviar null.")
    String separationDate,
    
    @JsonPropertyDescription("El último domicilio donde convivieron los cónyuges. Fundamental para determinar la competencia del juez.")
    AddressDto lastResidence
) {}
