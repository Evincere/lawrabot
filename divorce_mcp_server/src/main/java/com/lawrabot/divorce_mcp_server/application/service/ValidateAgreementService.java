package com.lawrabot.divorce_mcp_server.application.service;

import com.lawrabot.divorce_mcp_server.application.port.in.ValidateAgreementLegalityUseCase;
import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.domain.model.RegulatoryAgreement;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ValidateAgreementService implements ValidateAgreementLegalityUseCase {

    private final IExpedienteRepository expedienteRepository;

    public ValidateAgreementService(IExpedienteRepository expedienteRepository) {
        this.expedienteRepository = expedienteRepository;
    }

    @Override
    public List<String> executeSanityCheck(UUID expedienteId) {
        Expediente expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado."));

        List<String> alertsToBot = new ArrayList<>();

        RegulatoryAgreement agreement = expediente.getRegulatoryAgreement();
        if (agreement == null) {
            alertsToBot.add("FALTA_CONVENIO");
            return alertsToBot;
        }

        // --- Alimentos ---
        if (agreement.getAlimonyProvision() != null) {
            if (agreement.getAlimonyProvision().isIndexationMissingWarning()) {
                alertsToBot.add("ALIMONY_WARNING_LACKS_UPDATE_MECHANISM");
            }
        }

        // --- Cuidado Personal ---
        if (agreement.getPersonalCare() != null) {
            if (agreement.getPersonalCare().isUnilateralWarning()) {
                alertsToBot.add("PERSONAL_CARE_UNILATERAL_REQUIRES_ALIMONY_JUSTIFICATION");
            }
            if (agreement.getPersonalCare().requiresAlimonyWarning()) {
                alertsToBot.add("NO_MINOR_CHILDREN_NO_CARE_NEEDED");
            }
        }

        // --- Distribución de Bienes ---
        if (agreement.getAssetDistribution() != null && agreement.getAssetDistribution().requiresTaxWarning()) {
            alertsToBot.add("ASSET_DISTRIBUTION_TAX_WARNING");
        }

        // --- Compensación Económica ---
        if (agreement.getEconomicCompensation() != null) {
             if (agreement.getEconomicCompensation().requiresExpressWaiver()) {
                 alertsToBot.add("EC_REQUIRES_EXPRESS_WAIVER");
             }
             if (agreement.getEconomicCompensation().isMissingJustificationWarning()) {
                 alertsToBot.add("EC_MISSING_JUSTIFICATION");
             }
        }
        
        // --- Cónyuges ---
        if (expediente.getPetitioner() == null || expediente.getRespondent() == null) {
            alertsToBot.add("MISSING_SPOUSE_DATA");
        }

        return alertsToBot;
    }
}
