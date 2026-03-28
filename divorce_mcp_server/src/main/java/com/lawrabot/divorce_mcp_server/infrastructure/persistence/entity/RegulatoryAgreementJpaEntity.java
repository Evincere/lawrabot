package com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity;

import com.lawrabot.divorce_mcp_server.domain.enums.*;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.embeddable.AlimonyAmountEmbeddable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Entidad JPA para la tabla 'regulatory_agreements'.
 * Los sub-modelos del convenio se persisten como columnas Embedded (estrategia "single table").
 */
@Entity
@Table(name = "regulatory_agreements")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegulatoryAgreementJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private AgreementStatusEnum status;

    // ============================================
    // ALIMONY PROVISION (cuota alimentaria)
    // ============================================
    @Enumerated(EnumType.STRING)
    @Column(name = "alimony_provision_type", length = 30)
    private ProvisionTypeEnum alimonyProvisionType;

    @Column(name = "alimony_custom_provision_type", length = 200)
    private String alimonyCustomProvisionType;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value",               column = @Column(name = "alimony_amount_value")),
        @AttributeOverride(name = "currencyOrParameter", column = @Column(name = "alimony_amount_currency")),
        @AttributeOverride(name = "customParameter",     column = @Column(name = "alimony_amount_custom"))
    })
    private AlimonyAmountEmbeddable alimonyAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "alimony_payment_frequency", length = 30)
    private PaymentFrequencyEnum alimonyPaymentFrequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "alimony_payment_method", length = 30)
    private PaymentMethodEnum alimonyPaymentMethod;

    @Column(name = "alimony_payment_details", length = 300)
    private String alimonyPaymentDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "alimony_update_mechanism", length = 30)
    private UpdateMechanismEnum alimonyUpdateMechanism;

    // ============================================
    // PERSONAL CARE (cuidado personal)
    // ============================================
    @Enumerated(EnumType.STRING)
    @Column(name = "personal_care_type", length = 40)
    private PersonalCareTypeEnum personalCareType;

    @Column(name = "personal_care_custom_type", length = 200)
    private String personalCareCustomType;

    @Enumerated(EnumType.STRING)
    @Column(name = "personal_care_main_residence", length = 40)
    private MainResidenceEnum personalCareMainResidence;

    // ============================================
    // COMMUNICATION REGIME (régimen comunicacional)
    // ============================================
    @Enumerated(EnumType.STRING)
    @Column(name = "comm_regime_type", length = 40)
    private CommunicationRegimeTypeEnum communicationRegimeType;

    @Column(name = "comm_regime_custom_type", length = 200)
    private String communicationRegimeCustomType;

    @Column(name = "comm_regime_regular_schedule", columnDefinition = "TEXT")
    private String communicationRegularSchedule;

    @Column(name = "comm_regime_holiday_schedule", columnDefinition = "TEXT")
    private String communicationHolidaySchedule;

    @Column(name = "comm_regime_pickup_location_desc", length = 300)
    private String communicationPickUpLocationDescription;

    @Column(name = "comm_regime_supervisor_name", length = 200)
    private String communicationSupervisorName;

    // ============================================
    // ASSET DISTRIBUTION (distribución de bienes)
    // ============================================
    @Enumerated(EnumType.STRING)
    @Column(name = "asset_home_attribution", length = 40)
    private HomeAttributionEnum assetHomeAttributionTo;

    @Column(name = "custom_asset_home_attribution", length = 200)
    private String customAssetHomeAttributionTo;

    @Column(name = "asset_home_attribution_term", length = 100)
    private String assetHomeAttributionTerm;

    @Column(name = "asset_summary", columnDefinition = "TEXT")
    private String assetsSummary;

    @Column(name = "asset_liabilities_summary", columnDefinition = "TEXT")
    private String liabilitiesSummary;

    // ============================================
    // ECONOMIC COMPENSATION (compensación económica - Art. 441 CCyC)
    // ============================================
    @Column(name = "ec_applies", nullable = false)
    private boolean appliesEconomicCompensation;

    @Enumerated(EnumType.STRING)
    @Column(name = "ec_beneficiary", length = 20)
    private SpouseRoleEnum ecBeneficiary;

    @Column(name = "ec_imbalance_justification", columnDefinition = "TEXT")
    private String ecImbalanceJustification;

    @Enumerated(EnumType.STRING)
    @Column(name = "ec_payment_method", length = 30)
    private CompensationPaymentEnum ecPaymentMethod;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "value",               column = @Column(name = "ec_amount_value")),
        @AttributeOverride(name = "currencyOrParameter", column = @Column(name = "ec_amount_currency")),
        @AttributeOverride(name = "customParameter",     column = @Column(name = "ec_amount_custom"))
    })
    private AlimonyAmountEmbeddable ecCompensationAmount;

    @Column(name = "ec_installments_count")
    private Integer ecInstallmentsCount;
}
