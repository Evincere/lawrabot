package com.lawrabot.divorce_mcp_server.domain.model;

import com.lawrabot.divorce_mcp_server.domain.enums.DivorceTypeEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.ExpedienteStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.DataCollectionStageEnum;
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

    // Requisitos de patrocinio público
    private boolean requiresBLSG; // Beneficio de Litigar Sin Gastos

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
                .status(ExpedienteStatusEnum.BLSG_PRECONSULTA) // Todo caso entra primero por filtro BLSG
                .collectionStage(DataCollectionStageEnum.PENDING_BASIC_INFO) // Inicia en la etapa más básica
                .children(new ArrayList<>())
                .requiresBLSG(true) // En Defensoría casi el 100% lo requiere por defecto
                // El convenio inicia vacío o nulo hasta que el bot haga las preguntas patrimoniales
                .regulatoryAgreement(null) 
                .createdAt(now)
                .updatedAt(now)
                .build();
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
