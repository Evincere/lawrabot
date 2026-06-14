package com.lawrabot.divorce_mcp_server.infrastructure.rest.dto;

import com.lawrabot.divorce_mcp_server.domain.enums.*;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Request DTO para la actualización estructurada del Convenio Regulador.
 */
@Data
public class UpdateRegulatoryAgreementRequest {
    private PersonalCareUpdateDTO personalCare;
    private CommunicationRegimeUpdateDTO communicationRegime;
    private AlimonyProvisionUpdateDTO alimonyProvision;
    private AssetDistributionUpdateDTO assetDistribution;
    private EconomicCompensationUpdateDTO economicCompensation;

    @Data
    public static class PersonalCareUpdateDTO {
        private String careType;
        private String mainResidence;
    }

    @Data
    public static class CommunicationRegimeUpdateDTO {
        private String regimeType;
        private String regularSchedule;
        private String holidaySchedule;
    }

    @Data
    public static class AlimonyProvisionUpdateDTO {
        private String provisionType;
        private BigDecimal amountValue;
        private String amountCurrency;
        private String customParameter;
        private String paymentFrequency;
        private String paymentMethod;
        private String paymentDetails;
        private String updateMechanism;
    }

    @Data
    public static class AssetDistributionUpdateDTO {
        private String homeAttributionTo;
        private String assetsSummary;
        private String liabilitiesSummary;
    }

    @Data
    public static class EconomicCompensationUpdateDTO {
        private boolean appliesEconomicCompensation;
        private String beneficiary;
        private String imbalanceJustification;
        private String paymentMethod;
        private BigDecimal compensationAmountValue;
        private String compensationAmountCurrency;
        private String compensationCustomParameter;
        private Integer installmentsCount;
    }
}
