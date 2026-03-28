package com.lawrabot.divorce_mcp_server.domain.model;

import com.lawrabot.divorce_mcp_server.domain.enums.BlsgScrapingResultEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.DataCollectionStageEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.DivorceTypeEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.ExpedienteStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.valueobject.AddressVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.PhoneNumberVO;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad de dominio central (Aggregate Root) que representa un caso de divorcio.
 * Coordina el ciclo de vida, los estados y agrupa a conyugés, hijos y acuerdos.
 */
@Slf4j
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Expediente {

    private UUID id;
    private PhoneNumberVO contactPhoneNumber;
    private ExpedienteStatusEnum status;
    private DataCollectionStageEnum collectionStage; // Para guiar al LLM/Bot
    private DivorceTypeEnum divorceType; // UNILATERAL o JOINT

    // ============================================
    // DATOS DE COMPETENCIA Y PROCESALES
    // ============================================
    
    // Último domicilio conyugal (Art. 717 CCyC: determina competencia territorial)
    @Setter(AccessLevel.NONE)
    private AddressVO lastConjugalResidence;

    // Fechas Críticas para el Régimen Patrimonial (Bienes)
    @Setter(AccessLevel.NONE)
    private LocalDate marriageDate;
    @Setter(AccessLevel.NONE)
    private LocalDate deFactoSeparationDate; // Marca el cese de ganancialidad (Art. 480)

    // Requisitos de admisibilidad (Art. 438 y 439 CCyC)
    // Propuesta (si es unilateral) o Convenio (si es conjunto)
    private RegulatoryAgreement regulatoryAgreement;

    // Requisitos de patrocinio público.
    // El perfil completo reemplaza al flag booleano simple.
    @Setter(AccessLevel.NONE)
    private SocioEconomicProfile socioEconomicProfile;

    // ============================================
    // RELACIONES (Participantes)
    // ============================================
    private Spouse petitioner;
    private Spouse respondent;
    @Builder.Default
    private List<Child> children = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Método fábrica para iniciar un nuevo caso de recolección de datos por WhatsApp.
     */
    public static Expediente createNew(PhoneNumberVO phoneNumber, DivorceTypeEnum divorceType) {
        LocalDateTime now = LocalDateTime.now();
        return Expediente.builder()
                .id(UUID.randomUUID())
                .contactPhoneNumber(phoneNumber)
                .divorceType(divorceType)
                .status(ExpedienteStatusEnum.BLSG_PRECONSULTA)
                .collectionStage(DataCollectionStageEnum.PENDING_BLSG_SCRAPING) // Empieza siempre por el filtro BLSG
                .socioEconomicProfile(SocioEconomicProfile.createForScraping()) // Perfil listo para recibir resultado
                .children(new ArrayList<>())
                .regulatoryAgreement(null)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    // ============================================
    // LÓGICA DE NEGOCIO (Filtro BLSG)
    // ============================================

    /**
     * Registra el resultado del Scraping del Poder Judicial (Fase 1 del BLSG).
     * Si el resultado es un rechazo definitivo, cierra el expediente.
     * Si es aprobado o inconcluyente, avanza a la etapa de evaluación profunda.
     *
     * @param result Resultado del scraping automatizado.
     * @param justification Texto descriptivo del resultado (puede ser nulo).
     */
    public void processScrapingResult(BlsgScrapingResultEnum result, String justification) {
        if (this.socioEconomicProfile == null) {
            this.socioEconomicProfile = SocioEconomicProfile.createForScraping();
        }
        // Usamos el builder del perfil existente con los nuevos datos del scraping
        this.socioEconomicProfile = SocioEconomicProfile.builder()
                .id(this.socioEconomicProfile.getId())
                .scrapingResult(result)
                .scrapingJustification(justification)
                .build();

        if (result == BlsgScrapingResultEnum.PROVISIONALLY_REJECTED) {
            // Rechazo inmediato: el Poder Judicial registra bienes o ingresos incompatibles.
            this.status = ExpedienteStatusEnum.BLSG_RECHAZADO;
            log.warn("Expediente {} rechazado por scraping BLSG: {}", id, justification);
        } else {
            // Aprobado o Inconcluyente: la duda favorece al solicitante, continua con evaluación interna.
            this.collectionStage = DataCollectionStageEnum.PENDING_SOCIOECONOMIC_EVALUATION;
            log.info("Expediente {} avanza a evaluación socioeconómica. Resultado scraping: {}", id, result);
        }
        updateTimestamp();
    }

    /**
     * Registra la evaluación socioeconómica interna de la Defensoría (Fase 2 del BLSG).
     * Si el solicitante supera los umbrales o tiene activos significativos, se rechaza.
     *
     * @param updatedProfile El perfil completo con todos los datos declarados por el cliente.
     * @param approved Decisión final del Defensor/Bot basada en criterios internos.
     */
    public void evaluateDefensoriaCriteria(SocioEconomicProfile updatedProfile, boolean approved) {
        this.socioEconomicProfile = updatedProfile;

        if (!approved) {
            this.status = ExpedienteStatusEnum.BLSG_RECHAZADO;
            log.warn("Expediente {} rechazado por criterios internos de la Defensoría.", id);
        } else {
            // BLSG aprobado: ahora sí arranca el cuestionario de divorcio.
            this.status = ExpedienteStatusEnum.IN_DATA_COLLECTION_PROGRESS;
            this.collectionStage = DataCollectionStageEnum.PENDING_BASIC_INFO;
            log.info("Expediente {} BLSG aprobado. Iniciando recopilación de datos del divorcio.", id);
        }
        updateTimestamp();
    }

    // ============================================
    // LÓGICA DE NEGOCIO (Recolección Conversacional)
    // ============================================

    /**
     * Ingresa la data central del matrimonio, usualmente la segunda etapa conversacional.
     */
    public void provideMarriageDetails(LocalDate marriageDate, LocalDate separationDate, AddressVO lastResidence) {
        if (marriageDate == null) {
            throw new IllegalArgumentException("La fecha de matrimonio es obligatoria.");
        }
        if (separationDate != null && separationDate.isBefore(marriageDate)) {
            throw new IllegalArgumentException("La fecha de separación de hecho no puede ser anterior al matrimonio.");
        }
        
        this.marriageDate = marriageDate;
        this.deFactoSeparationDate = separationDate;
        this.lastConjugalResidence = lastResidence;
        
        // Si estábamos esperando estos detalles, avanzamos el "cursor" conversacional
        if (this.collectionStage == DataCollectionStageEnum.PENDING_MARRIAGE_DETAILS) {
            this.collectionStage = DataCollectionStageEnum.PENDING_CHILDREN_INFO;
        }
        updateTimestamp();
    }

    /**
     * Registra la existencia de hijos (tercera etapa conversacional).
     */
    public void registerChildren(List<Child> declaredChildren) {
        this.children = declaredChildren != null ? declaredChildren : new ArrayList<>();
        
        // Avanzamos el "cursor"
        if (this.collectionStage == DataCollectionStageEnum.PENDING_CHILDREN_INFO) {
            this.collectionStage = DataCollectionStageEnum.PENDING_REGULATORY_AGREEMENT;
        }
        updateTimestamp();
    }

    // ============================================
    // LÓGICA DE NEGOCIO (Máquina de Estados y Agregado)
    // ============================================

    /**
     * Adjunta o actualiza el Convenio/Propuesta Reguladora al Expediente.
     */
    public void attachRegulatoryAgreement(RegulatoryAgreement agreement) {
        this.regulatoryAgreement = agreement;
        updateTimestamp();
        log.info("Convenio Regulador adjuntado al expediente {}", id);
    }

    public void transitionToInProgress() {
        validateStatusTransition(ExpedienteStatusEnum.IN_DATA_COLLECTION_PROGRESS);
        this.status = ExpedienteStatusEnum.IN_DATA_COLLECTION_PROGRESS;
        updateTimestamp();
    }

    public void markDataComplete() {
        validateStatusTransition(ExpedienteStatusEnum.DATA_COMPLETE);
        this.status = ExpedienteStatusEnum.DATA_COMPLETE;
        updateTimestamp();
    }

    public void rejectDueToBLSG() {
        this.status = ExpedienteStatusEnum.BLSG_RECHAZADO;
        updateTimestamp();
    }

    private void validateStatusTransition(ExpedienteStatusEnum nextStatus) {
        // Implementar lógica compleja de transición aquí si es necesario
        log.info("Transicionando expediente {} de {} a {}", id, status, nextStatus);
    }

    private void updateTimestamp() {
        this.updatedAt = LocalDateTime.now();
    }
}
