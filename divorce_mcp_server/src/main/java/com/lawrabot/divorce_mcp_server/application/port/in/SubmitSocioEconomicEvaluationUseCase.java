package com.lawrabot.divorce_mcp_server.application.port.in;

import com.lawrabot.divorce_mcp_server.domain.enums.HousingSituationEnum;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Puerto de Entrada invocado después del scraping positivo / inconcluyente.
 * El Bot ha completado el cuestionario socioeconómico según criterios de la Defensoría.
 */
public interface SubmitSocioEconomicEvaluationUseCase {

    /**
     * @param expedienteId       ID del Expediente.
     * @param monthlyIncomeArs   Ingresos mensuales declarados (ARS).
     * @param housingSituation   Situación habitacional.
     * @param vehiclesRegistered Cantidad de vehículos a su nombre.
     * @param hasFormalEmployment Si tiene empleo formal en relación de dependencia.
     * @param observations       Observaciones adicionales del defensor.
     */
    void execute(UUID expedienteId,
                 BigDecimal monthlyIncomeArs,
                 HousingSituationEnum housingSituation,
                 Integer vehiclesRegistered,
                 boolean hasFormalEmployment,
                 String observations);
}
