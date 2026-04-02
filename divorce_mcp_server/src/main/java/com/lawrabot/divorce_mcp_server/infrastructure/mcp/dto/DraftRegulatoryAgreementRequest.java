package com.lawrabot.divorce_mcp_server.infrastructure.mcp.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.UUID;

public record DraftRegulatoryAgreementRequest(
    @JsonPropertyDescription("ID UUID del Expediente.")
    UUID expedienteId,
    
    @JsonPropertyDescription("Propuesta textual y resumida de la cuota alimentaria, monto, mecanismo de actualización, vivienda, comunicación, etc. Todo texto libre por ahora.")
    String proposalSummary
) {}
