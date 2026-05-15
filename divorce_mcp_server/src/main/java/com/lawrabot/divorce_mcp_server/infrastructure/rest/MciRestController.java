package com.lawrabot.divorce_mcp_server.infrastructure.rest;

import com.lawrabot.divorce_mcp_server.application.port.out.ICaseParticipantRepository;
import com.lawrabot.divorce_mcp_server.application.port.out.ICitizenRepository;
import com.lawrabot.divorce_mcp_server.application.port.out.ICorrectionFeedbackRepository;
import com.lawrabot.divorce_mcp_server.domain.model.CaseParticipant;
import com.lawrabot.divorce_mcp_server.domain.model.Citizen;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.CorrectionFeedbackJpaEntity;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/mci")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // Allow dashboard to connect
public class MciRestController {

    private final ICitizenRepository citizenRepository;
    private final ICaseParticipantRepository participantRepository;
    private final ICorrectionFeedbackRepository feedbackRepository;

    @GetMapping("/citizen/{dni}")
    public ResponseEntity<CitizenHistoryResponse> getCitizenHistory(@PathVariable String dni) {
        log.info("REST: Buscando ciudadano por DNI: {}", dni);
        Optional<Citizen> citizenOpt = citizenRepository.findByDni(dni);
        
        if (citizenOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Citizen citizen = citizenOpt.get();
        List<CaseParticipant> participations = participantRepository.findByCitizenId(citizen.getId());

        // Mapeo manual a DTOs limpios para evitar problemas de serialización de VOs o RECURSIÓN
        List<HistoryItemDTO> history = participations.stream()
                .map(this::mapToHistoryItem)
                .collect(Collectors.toList());

        var fn = citizen.getFullName();
        var cn = citizen.getCuil();
        var pn = citizen.getPhoneNumber();
        var ad = citizen.getAddress();

        String fullName = fn != null ? fn.getFullName() : "S/D";
        String cuil = cn != null ? cn.getValue() : "S/D";
        String phoneStr = pn != null ? pn.getValue() : "S/D";
        String address = ad != null ? ad.toLegalString() : "N/A";

        return ResponseEntity.ok(CitizenHistoryResponse.builder()
                .id(citizen.getId().toString())
                .fullName(fullName)
                .dni(citizen.getDni())
                .cuil(cuil)
                .phoneNumber(phoneStr)
                .email(citizen.getEmail())
                .address(address)
                .history(history)
                .build());
    }

    private HistoryItemDTO mapToHistoryItem(CaseParticipant participant) {
        String dateStr = "N/A";
        if (participant.getCreatedAt() != null) {
            dateStr = participant.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }

        String pId = "ID-" + (participant.getId() != null ? participant.getId().toString().substring(0, 8).toUpperCase() : "TEMP");
        String expId = participant.getExpedienteId() != null 
            ? "EXP-" + participant.getExpedienteId().toString().substring(0, 8).toUpperCase() 
            : pId;

        return HistoryItemDTO.builder()
                .id(expId)
                .type(participant.getExpedienteType() != null ? participant.getExpedienteType() : "Trámite Legal")
                .role(participant.getRole() != null ? participant.getRole().getValue() : "Interviniente")
                .status(participant.getExpedienteStatus() != null ? participant.getExpedienteStatus() : "EN PROCURACIÓN")
                .date(dateStr)
                .build();
    }

    @PostMapping("/correction")
    public ResponseEntity<String> saveCorrection(@RequestBody CorrectionRequest request) {
        log.info("REST: Registrando corrección para campo: {}", request.getFieldName());

        UUID citizenId = (request.getCitizenId() != null) ? UUID.fromString(request.getCitizenId()) : null;
        UUID caseId = (request.getCaseId() != null) ? UUID.fromString(request.getCaseId()) : null;

        CorrectionFeedbackJpaEntity feedback = CorrectionFeedbackJpaEntity.builder()
                .fieldName(request.getFieldName())
                .originalText(request.getOriginalText())
                .aiValue(request.getAiValue())
                .humanValue(request.getHumanValue())
                .citizenId(citizenId)
                .caseId(caseId)
                .isProcessed(false)
                .build();

        feedbackRepository.save(feedback);
        
        return ResponseEntity.ok("Corrección registrada en el Learning Loop.");
    }

    // --- DTOs ---

    @Data
    @Builder
    public static class CitizenHistoryResponse {
        private String id;
        private String fullName;
        private String dni;
        private String cuil;
        private String phoneNumber;
        private String email;
        private String address;
        private List<HistoryItemDTO> history;
    }

    @Data
    @Builder
    public static class HistoryItemDTO {
        private String id;
        private String type;
        private String role;
        private String status;
        private String date;
    }

    @Data
    public static class CorrectionRequest {
        private String fieldName;
        private String originalText;
        private String aiValue;
        private String humanValue;
        private String citizenId;
        private String caseId;
    }
}
