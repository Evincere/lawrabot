package com.lawrabot.divorce_mcp_server.application.service;

import com.lawrabot.divorce_mcp_server.application.port.in.SubmitSocioEconomicEvaluationUseCase;
import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.enums.HousingSituationEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.domain.model.SocioEconomicProfile;

import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Transactional
public class SubmitSocioEconomicEvaluationService implements SubmitSocioEconomicEvaluationUseCase {

    private final IExpedienteRepository repository;

    /**
     * Valor de referencia de la Canasta Básica Total (CBT) del INDEC.
     * En producción este valor se externalizaría a un servicio de configuración 
     * actualizable dinámicamente (ej. tabla de BD, archivo properties, etc.).
     */
    private static final BigDecimal CBT_REFERENCE_ARS = new BigDecimal("350000");

    public SubmitSocioEconomicEvaluationService(IExpedienteRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean execute(UUID expedienteId,
                           BigDecimal monthlyIncomeArs,
                           HousingSituationEnum housingSituation,
                           String occupation,
                           Integer vehiclesRegistered,
                           boolean hasFormalEmployment,
                           String observations) {

        Expediente expediente = repository.findById(expedienteId)
                .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado: " + expedienteId));

        // Construir el perfil socioeconómico completo conservando el resultado del scraping previo
        SocioEconomicProfile existingProfile = expediente.getSocioEconomicProfile();

        SocioEconomicProfile updatedProfile = SocioEconomicProfile.builder()
                .id(existingProfile != null ? existingProfile.getId() : UUID.randomUUID())
                .scrapingResult(existingProfile != null ? existingProfile.getScrapingResult() : null)
                .scrapingJustification(existingProfile != null ? existingProfile.getScrapingJustification() : null)
                .monthlyIncomeArs(monthlyIncomeArs)
                .housingSituation(housingSituation)
                .occupation(occupation)
                .vehiclesRegistered(vehiclesRegistered)
                .hasFormalEmployment(hasFormalEmployment)
                .defensoriaObservations(observations)
                .build();

        // Aplicar reglas de negocio de la Defensoría para decidir si aprueba el BLSG.
        boolean approved = !updatedProfile.exceedsIncomeThreshold(CBT_REFERENCE_ARS)
                        && !updatedProfile.hasSignificantAssets();

        // La transición de estado vive en el Dominio.
        expediente.evaluateDefensoriaCriteria(updatedProfile, approved);

        repository.save(expediente);
        return approved;
    }
}
