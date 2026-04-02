package com.lawrabot.divorce_mcp_server.infrastructure.mcp;

import com.lawrabot.divorce_mcp_server.application.port.in.*;
import com.lawrabot.divorce_mcp_server.domain.enums.BlsgScrapingResultEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.DataCollectionStageEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.HousingSituationEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Child;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.domain.valueobject.AddressVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.DNIVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.FullNameVO;
import com.lawrabot.divorce_mcp_server.application.service.AdvancedRagService;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.ExpedienteJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa.SpringDataExpedienteRepository;
import com.lawrabot.divorce_mcp_server.infrastructure.mcp.dto.*;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DivorceMcpController {

    private final CreateDivorceDossierUseCase createDivorceDossierUseCase;
    private final GetExpedienteCollectionStageUseCase getExpedienteCollectionStageUseCase;
    private final SubmitMarriageDetailsUseCase submitMarriageDetailsUseCase;
    private final SubmitChildrenInfoUseCase submitChildrenInfoUseCase;
    private final SubmitSocioEconomicEvaluationUseCase submitSocioEconomicEvaluationUseCase;
    private final ProcessBlsgScrapingResultUseCase processBlsgScrapingResultUseCase;
    private final ConsultarBlsgUseCase consultarBlsgUseCase;
    private final DraftRegulatoryAgreementUseCase draftRegulatoryAgreementUseCase;
    private final ValidateAgreementLegalityUseCase validateAgreementLegalityUseCase;
    private final AdvancedRagService ragService;
    private final SpringDataExpedienteRepository expedienteRepository;

    public DivorceMcpController(
            CreateDivorceDossierUseCase createDivorceDossierUseCase,
            GetExpedienteCollectionStageUseCase getExpedienteCollectionStageUseCase,
            SubmitMarriageDetailsUseCase submitMarriageDetailsUseCase,
            SubmitChildrenInfoUseCase submitChildrenInfoUseCase,
            SubmitSocioEconomicEvaluationUseCase submitSocioEconomicEvaluationUseCase,
            ProcessBlsgScrapingResultUseCase processBlsgScrapingResultUseCase,
            ConsultarBlsgUseCase consultarBlsgUseCase,
            DraftRegulatoryAgreementUseCase draftRegulatoryAgreementUseCase,
            ValidateAgreementLegalityUseCase validateAgreementLegalityUseCase,
            @Lazy AdvancedRagService ragService,
            SpringDataExpedienteRepository expedienteRepository) {
        this.createDivorceDossierUseCase = createDivorceDossierUseCase;
        this.getExpedienteCollectionStageUseCase = getExpedienteCollectionStageUseCase;
        this.submitMarriageDetailsUseCase = submitMarriageDetailsUseCase;
        this.submitChildrenInfoUseCase = submitChildrenInfoUseCase;
        this.submitSocioEconomicEvaluationUseCase = submitSocioEconomicEvaluationUseCase;
        this.processBlsgScrapingResultUseCase = processBlsgScrapingResultUseCase;
        this.consultarBlsgUseCase = consultarBlsgUseCase;
        this.draftRegulatoryAgreementUseCase = draftRegulatoryAgreementUseCase;
        this.validateAgreementLegalityUseCase = validateAgreementLegalityUseCase;
        this.ragService = ragService;
        this.expedienteRepository = expedienteRepository;
    }

    @Tool(name = "start_divorce_process", description = "Inicia un nuevo expediente de divorcio en el sistema.")
    public String startDivorceProcess(
            @JsonPropertyDescription("El número de teléfono del cliente en formato internacional") String phoneNumber,
            @JsonPropertyDescription("Primer nombre del cliente") String firstName,
            @JsonPropertyDescription("Apellido del cliente") String lastName) {
        Expediente exp = createDivorceDossierUseCase.execute(phoneNumber, firstName, lastName);
        return String.format("Expediente iniciado exitosamente. \nID: %s\nEtapa actual: %s", exp.getId(),
                exp.getCollectionStage());
    }

    @Tool(name = "get_dossier_stage", description = "Obtiene la etapa actual del trámite mediante el teléfono.")
    public String getDossierStage(
            @JsonPropertyDescription("El número de teléfono del cliente") String phoneNumber) {
        try {
            DataCollectionStageEnum stage = getExpedienteCollectionStageUseCase.queryByClientPhone(phoneNumber);
            return "La etapa actual del cliente es: " + stage.name();
        } catch (Exception e) {
            return "status: NOT_FOUND. No se encontró expediente activo para: " + phoneNumber;
        }
    }

    @Tool(name = "submit_marriage_details", description = "Guarda los detalles del matrimonio en el expediente.")
    public String submitMarriageDetails(
            @JsonPropertyDescription("UUID del expediente") UUID expedienteId,
            @JsonPropertyDescription("Fecha de matrimonio (YYYY-MM-DD)") String marriageDate,
            @JsonPropertyDescription("Fecha de separación (YYYY-MM-DD)") String separationDate,
            @JsonPropertyDescription("Dirección del último domicilio (Opcional)") AddressDto lastResidence) {

        AddressVO address = null;
        if (lastResidence != null) {
            address = AddressVO.builder()
                    .street(lastResidence.street())
                    .number(lastResidence.number())
                    .floorAppartment(lastResidence.floorAppartment())
                    .neighborhood(lastResidence.neighborhood())
                    .locality(lastResidence.locality())
                    .province(lastResidence.province())
                    .zipCode(lastResidence.zipCode())
                    .build();
        }

        LocalDate mDate = (marriageDate != null && !marriageDate.isBlank()) ? LocalDate.parse(marriageDate) : null;
        LocalDate sDate = (separationDate != null && !separationDate.isBlank()) ? LocalDate.parse(separationDate)
                : null;

        submitMarriageDetailsUseCase.execute(expedienteId, mDate, sDate, address);
        return "Detalles de matrimonio guardados correctamente.";
    }

    @Tool(name = "submit_children_info", description = "Carga la información de los hijos al expediente.")
    public String submitChildrenInfo(
            @JsonPropertyDescription("UUID del expediente") UUID expedienteId,
            @JsonPropertyDescription("Lista de hijos") List<ChildDto> childrenList) {
        List<Child> children = childrenList == null ? Collections.emptyList() : childrenList.stream().map(dto -> {
            FullNameVO name = new FullNameVO(dto.firstName(), dto.lastName());
            DNIVO dniVO = (dto.dni() != null && !dto.dni().isBlank()) ? DNIVO.of(dto.dni()) : null;
            LocalDate bDate = (dto.birthDate() != null && !dto.birthDate().isBlank()) ? LocalDate.parse(dto.birthDate())
                    : null;
            return Child.builder()
                    .name(name)
                    .dni(dniVO)
                    .birthDate(bDate)
                    .disabled(dto.disabled())
                    .build();
        }).collect(Collectors.toList());

        submitChildrenInfoUseCase.execute(expedienteId, children);
        return "Información de hijos guardada correctamente.";
    }

    @Tool(name = "submit_socioeconomic_info", description = "Completa los datos del análisis socioeconómico para el BLSG.")
    public String submitSocioEconomicInfo(
            @JsonPropertyDescription("UUID del expediente") UUID expedienteId,
            @JsonPropertyDescription("Ingreso mensual promedio en ARS") Double monthlyIncomeArs,
            @JsonPropertyDescription("Situación habitacional (Opción en español o inglés)") String housingSituation,
            @JsonPropertyDescription("Cantidad de vehículos registrados") Integer vehiclesRegistered,
            @JsonPropertyDescription("¿Posee empleo formal?") boolean hasFormalEmployment,
            @JsonPropertyDescription("Observaciones adicionales") String observations) {
        HousingSituationEnum housingEnum = null;
        try {
            if (housingSituation != null && !housingSituation.isBlank()) {
                String val = housingSituation.toUpperCase().trim();
                if (val.contains("ALQUILER") || val.contains("RENT")) {
                    housingEnum = HousingSituationEnum.RENTING;
                } else if (val.contains("PROPIA") || val.contains("OWNER") || val.contains("PROPIETARIO")) {
                    housingEnum = HousingSituationEnum.OWNER;
                } else if (val.contains("FAMILIAR") || val.contains("FAMILY")) {
                    housingEnum = HousingSituationEnum.FAMILY_HOME;
                } else if (val.contains("COMPARTE") || val.contains("SHARED")) {
                    housingEnum = HousingSituationEnum.SHARED_HOUSING;
                } else {
                    housingEnum = HousingSituationEnum.valueOf(val);
                }
            }
        } catch (IllegalArgumentException e) {
            return "Error: Situación habitacional inválida. Valores permitidos: RENTING, OWNER, FAMILY_HOME, SHARED_HOUSING, OTHER.";
        }

        BigDecimal income = (monthlyIncomeArs != null) ? BigDecimal.valueOf(monthlyIncomeArs) : BigDecimal.ZERO;

        submitSocioEconomicEvaluationUseCase.execute(
                expedienteId,
                income,
                housingEnum,
                vehiclesRegistered,
                hasFormalEmployment,
                observations);
        return "Evaluación socioeconómica preliminar guardada.";
    }

    @Tool(name = "process_scraping_result", description = "Registra los resultados de búsqueda de bienes.")
    public String processScrapingResult(
            @JsonPropertyDescription("UUID del expediente") UUID expedienteId,
            @JsonPropertyDescription("Estado del resultado") String resultStatus,
            @JsonPropertyDescription("Justificación del resultado") String justification) {
        BlsgScrapingResultEnum scrapingResult;
        try {
            scrapingResult = BlsgScrapingResultEnum.valueOf(resultStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Error: resultStatus inválido.";
        }

        processBlsgScrapingResultUseCase.execute(expedienteId, scrapingResult, justification);
        return "Resultado de búsqueda judicial integrado.";
    }

    @Tool(name = "consultar_blsg", description = "Consulta el Beneficio de Litigar Sin Gastos (BLSG) en el Poder Judicial de Mendoza.")
    public String consultarBlsg(
            @JsonPropertyDescription("Número de teléfono del ciudadano") String phoneNumber,
            @JsonPropertyDescription("DNI del ciudadano") String dni) {
        log.info("Tool MCP: consultar_blsg - DNI: {}", dni);
        ConsultarBlsgUseCase.ScrapingResult result = consultarBlsgUseCase.execute(phoneNumber, dni);
        
        if (!result.success()) {
            return "❌ Error en la consulta: " + result.benefitStatus();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("✅ **Resultado de la consulta BLSG**\n\n");
        sb.append("| Campo | Valor |\n");
        sb.append("|---|---|\n");
        sb.append("| **Nombre** | ").append(result.fullName()).append(" |\n");
        sb.append("| **DNI** | ").append(result.dni()).append(" |\n");
        sb.append("| **CUIL** | ").append(result.cuil()).append(" |\n");
        sb.append("| **Resultado** | **").append(result.benefitStatus()).append("** |\n");
        
        if (result.certificatePath() != null) {
            sb.append("\n📄 Constancia guardada en el sistema.");
        }
        
        return sb.toString();
    }

    @Tool(name = "draft_regulatory_agreement", description = "Guarda el borrador del convenio regulador.")
    public String draftRegulatoryAgreement(
            @JsonPropertyDescription("UUID del expediente") UUID expedienteId,
            @JsonPropertyDescription("Resumen de la propuesta acordada") String proposalSummary) {
        draftRegulatoryAgreementUseCase.draftAlimony(expedienteId, proposalSummary);
        return "Propuesta de Convenio Regulador guardada como borrador.";
    }

    @Tool(name = "validate_agreement_legality", description = "Ejecuta validaciones legales preventivas.")
    public String validateAgreementLegality(
            @JsonPropertyDescription("UUID del expediente en formato String") String expedienteIdStr) {
        UUID expedienteId = UUID.fromString(expedienteIdStr);
        List<String> validaciones = validateAgreementLegalityUseCase.executeSanityCheck(expedienteId);

        if (validaciones.isEmpty()) {
            return "El convenio y el expediente NO tienen alertas técnicas.";
        }

        StringBuilder sb = new StringBuilder("Atención - El convenio tiene las siguientes alertas legales:\n");
        validaciones.forEach(v -> sb.append("- ").append(v).append("\n"));
        return sb.toString();
    }

    @Tool(name = "consultar_normativa", description = "Consulta la base de conocimientos legal (CCyC) para responder dudas de derecho de familia.")
    public String consultarNormativa(
            @JsonPropertyDescription("El número de teléfono del usuario para obtener contexto del caso (Memoria)") String phoneNumber,
            @JsonPropertyDescription("La duda legal del usuario (coloquial o técnica)") String query) {
            
        log.info("Tool MCP: consultar_normativa - Query: {}", query);
        
        Optional<ExpedienteJpaEntity> expedienteOpt = expedienteRepository.findFirstByPhone(phoneNumber);
        String caseMemory = expedienteOpt.map(this::summarizeCase).orElse("Contexto general.");
        
        List<Document> legalArticles = ragService.searchLegalKnowledge(query);
        
        if (legalArticles.isEmpty()) {
            return "IMPORTANTE: No se han encontrado artículos pertinentes en la base de datos oficial para esta duda específica. Se recomienda consultar con un profesional del MPD.";
        }
        
        StringBuilder output = new StringBuilder();
        output.append("📖 ### BASE LEGAL ASOCIADA (Código Civil y Comercial)\n\n");
        
        for (Document doc : legalArticles) {
            output.append("#### ").append(doc.getMetadata().get("article_id")).append("\n");
            output.append("> ").append(doc.getText()).append("\n\n");
        }
        
        output.append("💡 ### ORIENTACIÓN SEGÚN TU CASO\n");
        output.append("Basado en tu situación: ").append(caseMemory).append("\n\n");
        
        output.append("---\n");
        output.append("⚠️ **ADVERTENCIA**: Esta información es generada por una IA con fines orientativos y no sustituye el asesoramiento legal de un abogado defensor.");
        
        return output.toString();
    }

    private String summarizeCase(ExpedienteJpaEntity e) {
        String base = (e.getDivorceType() != null ? e.getDivorceType().name() : "Desconocido") + " en proceso.";
        if (e.getChildren() != null && !e.getChildren().isEmpty()) {
            base += " Existen hijos menores registrados.";
        }
        return base;
    }
}