package com.lawrabot.divorce_mcp_server.domain.model;

import com.lawrabot.divorce_mcp_server.domain.enums.CaseRole;
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
@AllArgsConstructor(access = AccessLevel.PACKAGE)
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

    // Datos del Acta de Matrimonio — completados por el operador humano al visar el documento
    @Nullable
    private String marriageCertificateNumber; // Número de Acta (ej: "42")
    @Nullable
    private String marriageRegistryBook;      // Libro Registro (ej: "9485")
    @Nullable
    private String marriageRegistryPage;      // Foja (ej: "43")
    @Nullable
    private String marriageRegistryOffice;    // Oficina denominada (ej: "Monte Coman")
    @Nullable
    private String marriagePlace;             // Lugar del matrimonio (ej: "San Rafael, Provincia de Mendoza")

    // Vínculo con el archivo de evidencia digital oficial
    @Nullable
    private UUID marriageCertificateId;
    @Nullable
    private LocalDate marriageCertificateIssuanceDate;

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

    // Texto crudo extraído de la conversación de WhatsApp para revisión humana
    @Nullable
    private String rawAgreementText;

    // --- NUEVO: Master Client Index Integration ---
    @Builder.Default
    private List<CaseParticipant> participants = new ArrayList<>();

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

    /**
     * Completar datos del acta de matrimonio — operación exclusiva del operador humano
     * al visar el documento cargado como evidencia digital.
     */
    public void provideMarriageCertificateDetails(@Nullable String certificateNumber,
                                                    @Nullable String registryBook,
                                                    @Nullable String registryPage,
                                                    @Nullable String registryOffice,
                                                    @Nullable String place,
                                                    @Nullable UUID certificateId,
                                                    @Nullable LocalDate issuanceDate) {
        this.marriageCertificateNumber = certificateNumber;
        this.marriageRegistryBook = registryBook;
        this.marriageRegistryPage = registryPage;
        this.marriageRegistryOffice = registryOffice;
        this.marriagePlace = place;
        this.marriageCertificateId = certificateId;
        this.marriageCertificateIssuanceDate = issuanceDate;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateDivorceType(DivorceTypeEnum divorceType) {
        this.divorceType = divorceType;
        this.updatedAt = LocalDateTime.now();
    }

    public void setRegulatoryAgreement(@Nullable RegulatoryAgreement agreement) {
        this.regulatoryAgreement = agreement;
        this.updatedAt = LocalDateTime.now();
    }

    public void setRawAgreementText(@Nullable String rawText) {
        this.rawAgreementText = rawText;
        this.updatedAt = LocalDateTime.now();
    }

    public void evaluateDefensoriaCriteria(SocioEconomicProfile updatedProfile, boolean approved) {
        this.socioEconomicProfile = updatedProfile;
        
        if (approved) {
            this.status = ExpedienteStatusEnum.IN_DATA_COLLECTION_PROGRESS;
            // Solo retrocedemos a PENDING_MARRIAGE_DETAILS si aún no se han cargado esos datos.
            if (this.collectionStage == DataCollectionStageEnum.PENDING_SOCIOECONOMIC_EVALUATION) {
                this.collectionStage = (this.marriageDate != null) 
                    ? DataCollectionStageEnum.COMPLETED 
                    : DataCollectionStageEnum.PENDING_INCOME_PROOF;
            }
        } else {
            this.status = ExpedienteStatusEnum.BLSG_RECHAZADO;
            this.collectionStage = DataCollectionStageEnum.REJECTED;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void processScrapingResult(BlsgScrapingResultEnum result, String observations) {
        this.processScrapingResult(result, observations, null, null, null, null, null, null, null);
    }

    public void processScrapingResult(BlsgScrapingResultEnum result, String observations, String fullName, String dni, String cuil, String birthDate, String province, String sex, String certificatePath) {
        SocioEconomicProfile profile = this.socioEconomicProfile;
        if (profile == null) {
            profile = SocioEconomicProfile.createForScraping();
            this.socioEconomicProfile = profile;
        }
        profile.updateScrapingResult(result, observations, fullName, dni, cuil, birthDate, province, sex, certificatePath);
        
        if (result == BlsgScrapingResultEnum.PROVISIONALLY_REJECTED) {
            this.status = ExpedienteStatusEnum.BLSG_RECHAZADO;
        } else {
            // PROVISIONALLY_APPROVED e INCONCLUSIVE avanzan a la selección de modalidad
            this.status = ExpedienteStatusEnum.IN_DATA_COLLECTION_PROGRESS;
            this.collectionStage = DataCollectionStageEnum.PENDING_MODALITY_SELECTION;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void setChildren(List<Child> childrenList) {
        this.children = new ArrayList<>(childrenList);
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

    // --- MÉTODOS MCI ---

    public void addParticipant(Citizen citizen, CaseRole role) {
        this.addParticipant(citizen, role, null);
    }

    public void addParticipant(Citizen citizen, CaseRole role, String summary) {
        if (this.participants == null) {
            this.participants = new ArrayList<>();
        }
        this.participants.add(CaseParticipant.create(citizen, role, summary));
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isDataComplete() {
        return status == ExpedienteStatusEnum.DATA_COMPLETE;
    }

    public void archive() {
        this.status = ExpedienteStatusEnum.ARCHIVED;
        this.updatedAt = LocalDateTime.now();
    }
}
