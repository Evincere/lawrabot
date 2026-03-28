package com.lawrabot.divorce_mcp_server.infrastructure.persistence.mapper;

import com.lawrabot.divorce_mcp_server.domain.model.RegulatoryAgreement;
import com.lawrabot.divorce_mcp_server.domain.model.agreement.AlimonyProvision;
import com.lawrabot.divorce_mcp_server.domain.model.agreement.AssetDistribution;
import com.lawrabot.divorce_mcp_server.domain.model.agreement.CommunicationRegime;
import com.lawrabot.divorce_mcp_server.domain.model.agreement.EconomicCompensation;
import com.lawrabot.divorce_mcp_server.domain.model.agreement.PersonalCare;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.embeddable.AlimonyAmountEmbeddable;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.RegulatoryAgreementJpaEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * Mapper para RegulatoryAgreement ↔ Entity.
 * Aplica lógica de aplanamiento para el modelo relacional.
 */
@Component
public class RegulatoryAgreementMapper {

    public RegulatoryAgreementJpaEntity toEntity(RegulatoryAgreement domain) {
        if (domain == null) return null;

        RegulatoryAgreementJpaEntity entity = RegulatoryAgreementJpaEntity.builder()
                .id(domain.getId())
                .status(domain.getStatus())
                .build();

        // Aplanando sub-objetos del dominio a columnas de la tabla JPA
        mapAlimonyToEntity(domain.getAlimonyProvision(), entity);
        mapPersonalCareToEntity(domain.getPersonalCare(), entity);
        mapCommunicationRegimeToEntity(domain.getCommunicationRegime(), entity);
        mapAssetDistributionToEntity(domain.getAssetDistribution(), entity);
        mapEconomicCompensationToEntity(domain.getEconomicCompensation(), entity);

        return entity;
    }

    public RegulatoryAgreement toDomain(RegulatoryAgreementJpaEntity entity) {
        if (entity == null) return null;

        return RegulatoryAgreement.builder()
                .id(entity.getId())
                .status(entity.getStatus())
                .alimonyProvision(mapAlimonyToDomain(entity))
                .personalCare(mapPersonalCareToDomain(entity))
                .communicationRegime(mapCommunicationRegimeToDomain(entity))
                .assetDistribution(mapAssetDistributionToDomain(entity))
                .economicCompensation(mapEconomicCompensationToDomain(entity))
                .build();
    }

    // ============================================
    // HELPERS: Domain → Entity (Flattening)
    // ============================================

    private void mapAlimonyToEntity(@Nullable AlimonyProvision prov, RegulatoryAgreementJpaEntity entity) {
        if (prov == null) return;
        entity.setAlimonyProvisionType(prov.getProvisionType());
        
        if (prov.getAmount() != null) {
            entity.setAlimonyAmount(new AlimonyAmountEmbeddable(
                prov.getAmount().getValue(), 
                prov.getAmount().getCurrencyOrParameter(),
                prov.getAmount().getCustomParameter()
            ));
        }
        entity.setAlimonyPaymentFrequency(prov.getPaymentFrequency());
        entity.setAlimonyPaymentMethod(prov.getPaymentMethod());
        entity.setAlimonyPaymentDetails(prov.getPaymentDetails());
        entity.setAlimonyUpdateMechanism(prov.getUpdateMechanism());
    }

    private void mapPersonalCareToEntity(@Nullable PersonalCare prov, RegulatoryAgreementJpaEntity entity) {
        if (prov == null) return;
        entity.setPersonalCareType(prov.getCareType());
        entity.setPersonalCareMainResidence(prov.getMainResidence());
    }

    private void mapCommunicationRegimeToEntity(@Nullable CommunicationRegime prov, RegulatoryAgreementJpaEntity entity) {
        if (prov == null) return;
        entity.setCommunicationRegimeType(prov.getRegimeType());
        entity.setCommunicationRegularSchedule(prov.getRegularSchedule());
        entity.setCommunicationHolidaySchedule(prov.getHolidaySchedule());
    }

    private void mapAssetDistributionToEntity(@Nullable AssetDistribution prov, RegulatoryAgreementJpaEntity entity) {
        if (prov == null) return;
        entity.setAssetHomeAttributionTo(prov.getHomeAttributionTo());
        entity.setAssetsSummary(prov.getAssetsSummary());
        entity.setLiabilitiesSummary(prov.getLiabilitiesSummary());
    }

    private void mapEconomicCompensationToEntity(@Nullable EconomicCompensation prov, RegulatoryAgreementJpaEntity entity) {
        if (prov == null) {
            entity.setAppliesEconomicCompensation(false);
            return;
        }
        entity.setAppliesEconomicCompensation(prov.isAppliesEconomicCompensation());
        entity.setEcBeneficiary(prov.getBeneficiary());
        entity.setEcImbalanceJustification(prov.getImbalanceJustification());
        entity.setEcPaymentMethod(prov.getPaymentMethod());
        if (prov.getCompensationAmount() != null) {
            entity.setEcCompensationAmount(new AlimonyAmountEmbeddable(
                prov.getCompensationAmount().getValue(), 
                prov.getCompensationAmount().getCurrencyOrParameter(),
                prov.getCompensationAmount().getCustomParameter()
            ));
        }
        entity.setEcInstallmentsCount(prov.getInstallmentsCount());
    }

    // ============================================
    // HELPERS: Entity → Domain (Expanding)
    // ============================================

    private AlimonyProvision mapAlimonyToDomain(RegulatoryAgreementJpaEntity entity) {
        if (entity.getAlimonyProvisionType() == null) return null;
        return AlimonyProvision.builder()
                .provisionType(entity.getAlimonyProvisionType())
                .amount(entity.getAlimonyAmount() != null ? 
                        com.lawrabot.divorce_mcp_server.domain.valueobject.AlimonyAmountVO.of(
                            entity.getAlimonyAmount().getValue(), 
                            entity.getAlimonyAmount().getCurrencyOrParameter(),
                            entity.getAlimonyAmount().getCustomParameter()
                        ) : null)
                .paymentFrequency(entity.getAlimonyPaymentFrequency())
                .paymentMethod(entity.getAlimonyPaymentMethod())
                .paymentDetails(entity.getAlimonyPaymentDetails())
                .updateMechanism(entity.getAlimonyUpdateMechanism())
                .build();
    }

    private PersonalCare mapPersonalCareToDomain(RegulatoryAgreementJpaEntity entity) {
        if (entity.getPersonalCareType() == null) return null;
        return PersonalCare.builder()
                .careType(entity.getPersonalCareType())
                .mainResidence(entity.getPersonalCareMainResidence())
                .build();
    }

    private CommunicationRegime mapCommunicationRegimeToDomain(RegulatoryAgreementJpaEntity entity) {
        if (entity.getCommunicationRegimeType() == null) return null;
        return CommunicationRegime.builder()
                .regimeType(entity.getCommunicationRegimeType())
                .regularSchedule(entity.getCommunicationRegularSchedule())
                .holidaySchedule(entity.getCommunicationHolidaySchedule())
                .build();
    }

    private AssetDistribution mapAssetDistributionToDomain(RegulatoryAgreementJpaEntity entity) {
        if (entity.getAssetHomeAttributionTo() == null) return null;
        return AssetDistribution.builder()
                .homeAttributionTo(entity.getAssetHomeAttributionTo())
                .assetsSummary(entity.getAssetsSummary())
                .liabilitiesSummary(entity.getLiabilitiesSummary())
                .build();
    }

    private EconomicCompensation mapEconomicCompensationToDomain(RegulatoryAgreementJpaEntity entity) {
        if (!entity.isAppliesEconomicCompensation()) return null;
        return EconomicCompensation.builder()
                .appliesEconomicCompensation(true)
                .beneficiary(entity.getEcBeneficiary())
                .imbalanceJustification(entity.getEcImbalanceJustification())
                .paymentMethod(entity.getEcPaymentMethod())
                .compensationAmount(entity.getEcCompensationAmount() != null ? 
                        com.lawrabot.divorce_mcp_server.domain.valueobject.AlimonyAmountVO.of(
                            entity.getEcCompensationAmount().getValue(), 
                            entity.getEcCompensationAmount().getCurrencyOrParameter(),
                            entity.getEcCompensationAmount().getCustomParameter()
                        ) : null)
                .installmentsCount(entity.getEcInstallmentsCount())
                .build();
    }
}
