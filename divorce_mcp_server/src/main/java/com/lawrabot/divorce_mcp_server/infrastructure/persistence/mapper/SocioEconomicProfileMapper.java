package com.lawrabot.divorce_mcp_server.infrastructure.persistence.mapper;

import com.lawrabot.divorce_mcp_server.domain.model.SocioEconomicProfile;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.SocioEconomicProfileJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper para SocioEconomicProfile ↔ SocioEconomicProfileJpaEntity.
 */
@Component
public class SocioEconomicProfileMapper {

    public SocioEconomicProfileJpaEntity toEntity(SocioEconomicProfile domain) {
        if (domain == null) return null;
        return SocioEconomicProfileJpaEntity.builder()
                .id(domain.getId())
                .scrapingResult(domain.getScrapingResult())
                .scrapingJustification(domain.getScrapingJustification())
                .fullName(domain.getFullName())
                .dni(domain.getDni())
                .cuil(domain.getCuil())
                .birthDate(domain.getBirthDate())
                .province(domain.getProvince())
                .sex(domain.getSex())
                .certificatePath(domain.getCertificatePath())
                .monthlyIncomeArs(domain.getMonthlyIncomeArs())
                .housingSituation(domain.getHousingSituation())
                .vehiclesRegistered(domain.getVehiclesRegistered())
                .hasFormalEmployment(domain.isHasFormatEmployment())
                .defensoriaObservations(domain.getDefensoriaObservations())
                .blsgApprovedByDefensoria(domain.getBlsgApprovedByDefensoria())
                .build();
    }

    public SocioEconomicProfile toDomain(SocioEconomicProfileJpaEntity entity) {
        if (entity == null) return null;
        return SocioEconomicProfile.builder()
                .id(entity.getId())
                .scrapingResult(entity.getScrapingResult())
                .scrapingJustification(entity.getScrapingJustification())
                .fullName(entity.getFullName())
                .dni(entity.getDni())
                .cuil(entity.getCuil())
                .birthDate(entity.getBirthDate())
                .province(entity.getProvince())
                .sex(entity.getSex())
                .certificatePath(entity.getCertificatePath())
                .monthlyIncomeArs(entity.getMonthlyIncomeArs())
                .housingSituation(entity.getHousingSituation())
                .vehiclesRegistered(entity.getVehiclesRegistered())
                .hasFormatEmployment(entity.getHasFormalEmployment() != null && entity.getHasFormalEmployment())
                .defensoriaObservations(entity.getDefensoriaObservations())
                .blsgApprovedByDefensoria(entity.getBlsgApprovedByDefensoria())
                .build();
    }
}
