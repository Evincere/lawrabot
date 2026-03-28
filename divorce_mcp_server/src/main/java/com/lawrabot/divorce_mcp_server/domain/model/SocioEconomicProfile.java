package com.lawrabot.divorce_mcp_server.domain.model;

import com.lawrabot.divorce_mcp_server.domain.enums.BlsgScrapingResultEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.HousingSituationEnum;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidad que captura el Perfil Socioeconómico para evaluar el
 * Beneficio de Litigar Sin Gastos (BLSG).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SocioEconomicProfile {

    private UUID id;

    // FASE 1: Scraping Externo (Poder Judicial)
    private BlsgScrapingResultEnum scrapingResult;
    @Nullable
    private String scrapingJustification;

    // FASE 2: Recolección Activa (WhatsApp)
    @Nullable
    private BigDecimal monthlyIncomeArs;
    @Nullable
    private HousingSituationEnum housingSituation;
    @Nullable
    private Integer vehiclesRegistered;
    @Builder.Default
    private boolean hasFormatEmployment = false;

    // Fase 3: Evaluación Defensoría
    @Nullable
    private String defensoriaObservations;
    @Nullable
    private Boolean blsgApprovedByDefensoria;

    /**
     * Factory method para inicializar un perfil vacío listo para el scraping.
     */
    public static SocioEconomicProfile createForScraping() {
        return SocioEconomicProfile.builder()
                .id(UUID.randomUUID())
                .scrapingResult(BlsgScrapingResultEnum.INCONCLUSIVE)
                .hasFormatEmployment(false)
                .build();
    }

    public void updateScrapingResult(BlsgScrapingResultEnum result, String justification) {
        this.scrapingResult = result;
        this.scrapingJustification = justification;
    }

    public void submitEvaluation(BigDecimal income, HousingSituationEnum housing, Integer vehicles, boolean employment) {
        this.monthlyIncomeArs = income;
        this.housingSituation = housing;
        this.vehiclesRegistered = vehicles;
        this.hasFormatEmployment = employment;
    }

    // Reglas de Negocio para el proceso interactivo
    
    public boolean exceedsIncomeThreshold(BigDecimal threshold) {
        return monthlyIncomeArs != null && monthlyIncomeArs.compareTo(threshold) > 0;
    }

    public boolean hasSignificantAssets() {
        // Regla simplificada: Vivienda propia o más de 1 vehículo
        return housingSituation == HousingSituationEnum.OWNER || (vehiclesRegistered != null && vehiclesRegistered > 1);
    }
}
