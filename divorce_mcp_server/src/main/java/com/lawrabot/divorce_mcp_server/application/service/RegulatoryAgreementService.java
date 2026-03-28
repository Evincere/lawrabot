package com.lawrabot.divorce_mcp_server.application.service;

import com.lawrabot.divorce_mcp_server.application.port.in.DraftRegulatoryAgreementUseCase;
import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.enums.AgreementStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.domain.model.RegulatoryAgreement;

import java.util.UUID;

/**
 * Servicio que orquesta la redacción del convenio regulador.
 */
public class RegulatoryAgreementService implements DraftRegulatoryAgreementUseCase {

    private final IExpedienteRepository expedienteRepository;

    public RegulatoryAgreementService(IExpedienteRepository expedienteRepository) {
        this.expedienteRepository = expedienteRepository;
    }

    @Override
    public RegulatoryAgreement draftAlimony(UUID expedienteId, Object alimonyDataDTO) {
        Expediente expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado."));

        // Si no hay convenio, lo creamos
        if (expediente.getRegulatoryAgreement() == null) {
            expediente.setRegulatoryAgreement(RegulatoryAgreement.createEmpty());
        }
        
        // Aquí se procesaría el DTO y se actualizaría el convenio...
        // RegulatoryAgreement agreement = expediente.getRegulatoryAgreement();
        // agreement.updateAlimony(alimonyData);
        
        expedienteRepository.save(expediente);
        
        RegulatoryAgreement result = expediente.getRegulatoryAgreement();
        if (result == null) {
            throw new IllegalStateException("Error al generar el convenio regulador.");
        }
        return result;
    }

    @Override
    public void markAsAcceptedByBothParties(UUID expedienteId) {
        Expediente expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado."));

        RegulatoryAgreement agreement = expediente.getRegulatoryAgreement();
        if (agreement == null) {
            throw new IllegalStateException("No existe un Convenio redactado para aceptar.");
        }

        // Transiciona de PROPOSED a ACCEPTED
        // Nota: Deberíamos usar un método en el Dominio para esto, pero por simplicidad:
        // agreement.accept();
        expediente.setRegulatoryAgreement(
            RegulatoryAgreement.builder()
                .id(agreement.getId())
                .status(AgreementStatusEnum.ACCEPTED)
                .includesChildrenProvisions(agreement.isIncludesChildrenProvisions())
                .alimonyProvision(agreement.getAlimonyProvision())
                .personalCare(agreement.getPersonalCare())
                .communicationRegime(agreement.getCommunicationRegime())
                .assetDistribution(agreement.getAssetDistribution())
                .economicCompensation(agreement.getEconomicCompensation())
                .build()
        );

        expedienteRepository.save(expediente);
    }
}
