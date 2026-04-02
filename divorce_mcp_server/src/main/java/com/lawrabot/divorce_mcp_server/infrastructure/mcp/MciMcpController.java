package com.lawrabot.divorce_mcp_server.infrastructure.mcp;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.lawrabot.divorce_mcp_server.application.port.out.ICaseParticipantRepository;
import com.lawrabot.divorce_mcp_server.application.port.out.ICitizenRepository;
import com.lawrabot.divorce_mcp_server.application.port.out.ICorrectionFeedbackRepository;
import com.lawrabot.divorce_mcp_server.domain.model.CaseParticipant;
import com.lawrabot.divorce_mcp_server.domain.model.Citizen;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.CorrectionFeedbackJpaEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller MCP para el Master Client Index (MCI) y el Learning Loop.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MciMcpController {

    private final ICitizenRepository citizenRepository;
    private final ICaseParticipantRepository participantRepository;
    private final ICorrectionFeedbackRepository feedbackRepository;

    @Tool(name = "mci:get_citizen_history", description = "Obtiene la ficha del ciudadano y su historial de expedientes en el MPD.")
    public String getCitizenHistory(
            @JsonPropertyDescription("DNI del ciudadano para buscar en el MCI") String dni) {
        
        Optional<Citizen> citizenOpt = citizenRepository.findByDni(dni);
        if (citizenOpt.isEmpty()) {
            return "Ciudadano no encontrado en el Master Client Index.";
        }

        Citizen citizen = citizenOpt.get();
        List<CaseParticipant> participations = participantRepository.findByCitizenId(citizen.getId());

        StringBuilder sb = new StringBuilder();
        sb.append("👤 **Ficha del Ciudadano (MCI)**\n\n");
        sb.append("| Campo | Valor |\n");
        sb.append("|---|---|\n");
        sb.append("| **ID Único** | ").append(citizen.getId()).append(" |\n");
        sb.append("| **Nombre** | ").append(citizen.getFullName()).append(" |\n");
        sb.append("| **DNI** | ").append(citizen.getDni()).append(" |\n");
        sb.append("| **CUIL** | ").append(citizen.getCuil()).append(" |\n");
        sb.append("| **Teléfono** | ").append(citizen.getPhoneNumber()).append(" |\n");
        sb.append("\n⚖️ **Historial de Intervenciones**\n");
        
        if (participations.isEmpty()) {
            sb.append("- Sin antecedentes registrados.");
        } else {
            participations.forEach(p -> {
                sb.append("- Role: **").append(p.getRole()).append("** en Expediente ID: ").append(p.getId()).append("\n");
            });
        }

        return sb.toString();
    }

    @Tool(name = "mci:save_data_correction", description = "Registra una corrección manual. Alimenta el Learning Loop.")
    public String saveDataCorrection(
            @JsonPropertyDescription("Nombre del campo corregido") String fieldName,
            @JsonPropertyDescription("Texto original / crudo") String originalText,
            @JsonPropertyDescription("Valor que extrajo la AI") String aiValue,
            @JsonPropertyDescription("Valor correcto según el humano") String humanValue,
            @JsonPropertyDescription("ID del ciudadano (opcional)") String citizenIdStr,
            @JsonPropertyDescription("ID del expediente (opcional)") String caseIdStr) {

        UUID citizenId = (citizenIdStr != null && !citizenIdStr.isEmpty()) ? UUID.fromString(citizenIdStr) : null;
        UUID caseId = (caseIdStr != null && !caseIdStr.isEmpty()) ? UUID.fromString(caseIdStr) : null;

        CorrectionFeedbackJpaEntity feedback = CorrectionFeedbackJpaEntity.builder()
                .fieldName(fieldName)
                .originalText(originalText)
                .aiValue(aiValue)
                .humanValue(humanValue)
                .citizenId(citizenId)
                .caseId(caseId)
                .isProcessed(false)
                .build();

        feedbackRepository.save(feedback);
        log.info("Learning Loop: Corrección registrada para el campo {}", fieldName);

        return "✅ Corrección registrada exitosamente. Esta información ayudará a mejorar la precisión del bot.";
    }
}
