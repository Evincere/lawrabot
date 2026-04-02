package com.lawrabot.divorce_mcp_server.infrastructure.mcp.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.UUID;

public record ProcessScrapingResultRequest(
    @JsonPropertyDescription("ID UUID del Expediente.")
    UUID expedienteId,
    
    @JsonPropertyDescription("Resultado arrojado por el webhook de scraping judicial. Valores esperados: PROVISIONALLY_APPROVED, PROVISIONALLY_REJECTED, INCONCLUSIVE")
    String resultStatus,
    
    @JsonPropertyDescription("Observaciones, capturas de pantalla o texto del scraper en caso de haber encontrado bienes a su nombre. Puede ser null.")
    String justification
) {}
