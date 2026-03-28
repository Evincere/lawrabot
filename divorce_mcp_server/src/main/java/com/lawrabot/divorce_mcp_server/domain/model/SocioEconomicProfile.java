package com.lawrabot.divorce_mcp_server.domain.model;

import com.lawrabot.divorce_mcp_server.domain.enums.BlsgScrapingResultEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.HousingSituationEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.AccessLevel;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidad que concentra toda la información socioeconómica del solicitante.
 * Es el modelo que permite a la Defensoría evaluar la procedencia del
 * Beneficio de Litigar Sin Gastos (BLSG) según sus propios criterios.
 *
 * <p>El proceso de evaluación tiene dos instancias:
 * <ol>
 *   <li>Scraping "A Priori" del Poder Judicial (resultado automático, no definitivo).</li>
 *   <li>Evaluación manual/conversacional de criterios propios de la Defensoría.</li>
 * </ol>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SocioEconomicProfile {

    private UUID id;

    // ============================================
    // FASE 1: RESULTADO DEL SCRAPING JUDICIAL
    // ============================================

    /** Resultado del scraping judicial automatizado. */
    private BlsgScrapingResultEnum scrapingResult;

    /** Detalle textual devuelto por el sistema del Poder Judicial (motivo de rechazo o notas). */
    private String scrapingJustification;

    // ============================================
    // FASE 2: CRITERIOS PROPIOS DE LA DEFENSORÍA
    // ============================================

    /**
     * Ingresos mensuales declarados (en pesos argentinos).
     * Se compararán contra el umbral de la "Canasta Básica Total" como referencia.
     */
    private BigDecimal monthlyIncomeArs;

    /** Situación habitacional del solicitante (Art. 8, Ley 1893). */
    private HousingSituationEnum housingSituation;

    /** Cantidad de vehículos registrados a nombre del titular. */
    private Integer vehiclesRegistered;

    /** Indica si el solicitante es actualmente empleado en relación de dependencia. */
    private boolean hasFormatEmployment;

    /** Observaciones adicionales del abogado defensor complementando el análisis. */
    private String defensoriaObservations;

    // ============================================
    // RESULTADO DE LA EVALUACIÓN INTEGRADA
    // ============================================

    /** Aprobación definitiva del BLSG por parte de la Defensoría (evaluación humana final). */
    private Boolean blsgApprovedByDefensoria;

    // ============================================
    // LÓGICA DE NEGOCIO (Reglas de Defensoría)
    // ============================================

    /**
     * Fábrica para la etapa inicial: se acaba de lanzar el scraping.
     */
    public static SocioEconomicProfile createForScraping() {
        return SocioEconomicProfile.builder()
                .id(UUID.randomUUID())
                .build();
    }

    /**
     * Regla de negocio CLAVE: evalúa si el solicitante supera el umbral de ingresos.
     * Umbral aproximado: 2.5 veces la Canasta Básica Total (CBT) del INDEC.
     * Este valor debe externalizarse a configuración en una implementación futura.
     *
     * @param cbtReferenceValue Valor de la Canasta Básica Total vigente.
     * @return true si los ingresos exceden el umbral y el BLSG NO debería proceder.
     */
    public boolean exceedsIncomeThreshold(BigDecimal cbtReferenceValue) {
        if (monthlyIncomeArs == null || cbtReferenceValue == null) return false;
        BigDecimal threshold = cbtReferenceValue.multiply(new BigDecimal("2.5"));
        return monthlyIncomeArs.compareTo(threshold) > 0;
    }

    /**
     * Indica si el perfil tiene propiedades o vehículos que dificultan el BLSG.
     * Un vehículo puede no ser impedimento si justifica movilidad laboral.
     */
    public boolean hasSignificantAssets() {
        return vehiclesRegistered != null && vehiclesRegistered > 1
            || HousingSituationEnum.OWNER.equals(housingSituation);
    }

    /**
     * Indica si el scraping resultó positivo y el proceso puede avanzar a la etapa
     * de evaluación socioeconómica profunda.
     */
    public boolean isScrapingClearToAdvance() {
        return scrapingResult == BlsgScrapingResultEnum.PROVISIONALLY_APPROVED
            || scrapingResult == BlsgScrapingResultEnum.INCONCLUSIVE; // La duda favorece al solicitante
    }
}
