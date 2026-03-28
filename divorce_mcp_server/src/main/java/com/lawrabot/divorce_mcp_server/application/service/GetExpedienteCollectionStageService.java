package com.lawrabot.divorce_mcp_server.application.service;

import com.lawrabot.divorce_mcp_server.application.port.in.GetExpedienteCollectionStageUseCase;
import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.enums.DataCollectionStageEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;

import java.util.Optional;

public class GetExpedienteCollectionStageService implements GetExpedienteCollectionStageUseCase {

    private final IExpedienteRepository repository;

    public GetExpedienteCollectionStageService(IExpedienteRepository repository) {
        this.repository = repository;
    }

    @Override
    public DataCollectionStageEnum queryByClientPhone(String phoneNumber) {
        Optional<Expediente> expedienteOpt = repository.findActiveByClientPhone(phoneNumber);
        
        // Si hay expediente activo (en borrador), retornamos su etapa conversacional.
        return expedienteOpt.map(Expediente::getCollectionStage).orElse(null);
    }
}
