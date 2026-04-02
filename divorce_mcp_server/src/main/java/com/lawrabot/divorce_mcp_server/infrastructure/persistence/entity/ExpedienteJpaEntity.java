package com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity;

import com.lawrabot.divorce_mcp_server.domain.enums.DataCollectionStageEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.DivorceTypeEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.ExpedienteStatusEnum;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.embeddable.AddressEmbeddable;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.embeddable.PhoneNumberEmbeddable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad JPA central para la tabla 'expedientes'.
 * Aggregate Root del lado de infraestructura: orquesta todas las relaciones con @OneToOne y @OneToMany.
 */
@Entity
@Table(name = "expedientes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpedienteJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "phoneNumber", column = @Column(name = "contact_phone_number"))
    })
    private PhoneNumberEmbeddable contactPhoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private ExpedienteStatusEnum status;

    @Enumerated(EnumType.STRING)
    @Column(name = "collection_stage", length = 50)
    private DataCollectionStageEnum collectionStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "divorce_type", length = 20)
    private DivorceTypeEnum divorceType;

    // Último domicilio conyugal (para competencia territorial - Art. 717 CCyC)
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street",          column = @Column(name = "conj_street")),
        @AttributeOverride(name = "number",          column = @Column(name = "conj_number")),
        @AttributeOverride(name = "floorAppartment", column = @Column(name = "conj_floor_apartment")),
        @AttributeOverride(name = "neighborhood",    column = @Column(name = "conj_neighborhood")),
        @AttributeOverride(name = "locality",        column = @Column(name = "conj_locality")),
        @AttributeOverride(name = "province",        column = @Column(name = "conj_province")),
        @AttributeOverride(name = "zipCode",         column = @Column(name = "conj_zip_code"))
    })
    private AddressEmbeddable lastConjugalResidence;

    @Column(name = "marriage_date")
    private LocalDate marriageDate;

    @Column(name = "defacto_separation_date")
    private LocalDate deFactoSeparationDate;

    // Cónyuge Peticionante (FK hacia tabla spouses)
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "petitioner_id", referencedColumnName = "id")
    private SpouseJpaEntity petitioner;

    // Cónyuge Demandado/a
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "respondent_id", referencedColumnName = "id")
    private SpouseJpaEntity respondent;

    // Hijos: se guardan con referencia al expediente padre
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "expediente_id")
    @Builder.Default
    private List<ChildJpaEntity> children = new ArrayList<>();

    // Perfil socioeconómico (BLSG)
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "socioeconomic_profile_id", referencedColumnName = "id")
    private SocioEconomicProfileJpaEntity socioEconomicProfile;

    // Convenio Regulador
    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "regulatory_agreement_id", referencedColumnName = "id")
    private RegulatoryAgreementJpaEntity regulatoryAgreement;

    // --- NUEVO: Master Client Index Integration ---
    @OneToMany(mappedBy = "expediente", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CaseParticipantJpaEntity> participants = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
