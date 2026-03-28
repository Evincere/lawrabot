package com.lawrabot.divorce_mcp_server.application.service;

import com.lawrabot.divorce_mcp_server.application.port.in.DraftRegulatoryAgreementUseCase;
import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.enums.AgreementStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.domain.model.RegulatoryAgreement;

import java.util.UUID;

public class RegulatoryAgreementService implements DraftRegulatoryAgreementUseCase {

    private final IExpedienteRepository expedienteRepository;

    public RegulatoryAgreementService(IExpedienteRepository expedienteRepository) {
        this.expedienteRepository = expedienteRepository;
    }

    @Override
    public RegulatoryAgreement draftAlimony(UUID expedienteId, Object alimonyDataDTO) {
        // Aquí recuperaríamos el expediente de la DB
        Expediente expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado."));

        // Lógica de validación cruzada: si tiene hijos o no.
        
        // Se instanciarían Value Objects usando AlimonyAmountVO.of(...)
        // Se inyectaría al RegulatoryAgreement del expediente.
        
        expedienteRepository.save(expediente);
        return expediente.getRegulatoryAgreement();
    }

    @Override
    public void markAsAcceptedByBothParties(UUID expedienteId) {
        Expediente expediente = expedienteRepository.findById(expedienteId)
                .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado."));

        if (expediente.getRegulatoryAgreement() == null) {
            throw new IllegalStateException("No existe un Convenio redactado para aceptar.");
        }

        // MÁQUINA DE ESTADOS:
        // Transiciona de PROPOSED a ACCEPTED
        expediente.getRegulatoryAgreement().setStatus(AgreementStatusEnum.ACCEPTED);

        expedienteRepository.save(expediente);
    }
}
