package com.lawrabot.divorce_mcp_server.domain.model;

import com.lawrabot.divorce_mcp_server.domain.enums.DataCollectionStageEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.DivorceTypeEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.ExpedienteStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.BlsgScrapingResultEnum;
import com.lawrabot.divorce_mcp_server.domain.valueobject.AddressVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.PhoneNumberVO;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Agregado Raíz (Aggregate Root) que representa un expediente de divorcio.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Expediente {

    private UUID id;

    // Identificación inicial
    private PhoneNumberVO contactPhoneNumber;

    // Estado del trámite
    private ExpedienteStatusEnum status;
    private DataCollectionStageEnum collectionStage;

    // Datos del Matrimonio
    private DivorceTypeEnum divorceType;
    @Nullable
    private AddressVO lastConjugalResidence;
    @Nullable
    private LocalDate marriageDate;
    @Nullable
    private LocalDate deFactoSeparationDate;

    // Cónyuges
    @Nullable
    private Spouse petitioner;
    @Nullable
    private Spouse respondent;

    // Hijos
    @Builder.Default
    private List<Child> children = new ArrayList<>();

    // Evaluación Socioeconómica (BLSG)
    @Nullable
    private SocioEconomicProfile socioEconomicProfile;

    // Convenio Regulador
    @Nullable
    private RegulatoryAgreement regulatoryAgreement;

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
                .collectionStage(DataCollectionStageEnum.PENDING_BLSG_SCRAPING)
                .socioEconomicProfile(SocioEconomicProfile.createForScraping())
                .children(new ArrayList<>())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    // ============================================
    // LÓGICA DE NEGOCIO
    // ============================================

    public void setPetitioner(@Nullable Spouse spouse) {
        this.petitioner = spouse;
        this.updatedAt = LocalDateTime.now();
    }

    public void setRespondent(@Nullable Spouse spouse) {
        this.respondent = spouse;
        this.updatedAt = LocalDateTime.now();
    }

    public void provideMarriageDetails(@Nullable LocalDate marriageDate, @Nullable LocalDate separationDate, @Nullable AddressVO residence) {
        this.marriageDate = marriageDate;
        this.deFactoSeparationDate = separationDate;
        this.lastConjugalResidence = residence;
        this.updatedAt = LocalDateTime.now();
    }

    public void setRegulatoryAgreement(@Nullable RegulatoryAgreement agreement) {
        this.regulatoryAgreement = agreement;
        this.updatedAt = LocalDateTime.now();
    }

    public void evaluateDefensoriaCriteria(SocioEconomicProfile updatedProfile, boolean approved) {
        this.socioEconomicProfile = updatedProfile;
        
        if (approved) {
            this.status = ExpedienteStatusEnum.IN_DATA_COLLECTION_PROGRESS;
            this.collectionStage = DataCollectionStageEnum.PENDING_MARRIAGE_DETAILS;
        } else {
            this.status = ExpedienteStatusEnum.BLSG_RECHAZADO;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void processScrapingResult(BlsgScrapingResultEnum result, String observations) {
        if (this.socioEconomicProfile == null) {
            this.socioEconomicProfile = SocioEconomicProfile.createForScraping();
        }
        this.socioEconomicProfile.updateScrapingResult(result, observations);
        
        if (result == BlsgScrapingResultEnum.PROVISIONALLY_REJECTED) {
            this.status = ExpedienteStatusEnum.BLSG_RECHAZADO;
        } else {
            this.collectionStage = DataCollectionStageEnum.PENDING_SOCIOECONOMIC_EVALUATION;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void registerChildren(List<Child> childrenList) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.addAll(childrenList);
        this.updatedAt = LocalDateTime.now();
    }

    public void updateStatus(ExpedienteStatusEnum newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateCollectionStage(DataCollectionStageEnum newStage) {
        this.collectionStage = newStage;
        this.updatedAt = LocalDateTime.now();
    }

    public void addChild(Child child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isDataComplete() {
        return status == ExpedienteStatusEnum.DATA_COMPLETE;
    }
}
