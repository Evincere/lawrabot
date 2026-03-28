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
                .monthlyIncomeArs(entity.getMonthlyIncomeArs())
                .housingSituation(entity.getHousingSituation())
                .vehiclesRegistered(entity.getVehiclesRegistered())
                .hasFormatEmployment(entity.getHasFormalEmployment() != null && entity.getHasFormalEmployment())
                .defensoriaObservations(entity.getDefensoriaObservations())
                .blsgApprovedByDefensoria(entity.getBlsgApprovedByDefensoria())
                .build();
    }
}
