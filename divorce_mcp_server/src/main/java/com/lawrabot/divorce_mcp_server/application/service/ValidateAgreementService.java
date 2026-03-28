package com.lawrabot.divorce_mcp_server.application.service;

import com.lawrabot.divorce_mcp_server.application.port.in.ValidateAgreementLegalityUseCase;
import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.domain.model.RegulatoryAgreement;
import com.lawrabot.divorce_mcp_server.domain.model.agreement.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Servicio encargado de la validación preventiva de un convenio regulador contra
 * reglas de buena práctica jurídica y normas básicas del Código Civil.
 */
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
        AlimonyProvision alimony = agreement.getAlimonyProvision();
        if (alimony != null) {
            if (alimony.isIndexationMissingWarning()) {
                alertsToBot.add("ALIMONY_WARNING_LACKS_UPDATE_MECHANISM");
            }
        }

        // --- Cuidado Personal ---
        PersonalCare care = agreement.getPersonalCare();
        if (care != null) {
            if (care.isUnilateralWarning()) {
                alertsToBot.add("PERSONAL_CARE_UNILATERAL_REQUIRES_ALIMONY_JUSTIFICATION");
            }
            if (care.requiresAlimonyWarning()) {
                alertsToBot.add("NO_MINOR_CHILDREN_NO_CARE_NEEDED");
            }
        }

        // --- Distribución de Bienes ---
        AssetDistribution assets = agreement.getAssetDistribution();
        if (assets != null) {
            if (assets.requiresTaxWarning()) {
                alertsToBot.add("ASSET_DISTRIBUTION_TAX_WARNING");
            }
        }

        // --- Compensación Económica ---
        EconomicCompensation ec = agreement.getEconomicCompensation();
        if (ec != null) {
            if (ec.requiresExpressWaiver()) {
                alertsToBot.add("EC_REQUIRES_EXPRESS_WAIVER");
            }
            if (ec.isMissingJustificationWarning()) {
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
