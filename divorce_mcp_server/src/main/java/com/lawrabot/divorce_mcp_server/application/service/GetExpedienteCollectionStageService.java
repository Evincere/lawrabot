package com.lawrabot.divorce_mcp_server.application.service;

import com.lawrabot.divorce_mcp_server.application.port.in.GetExpedienteCollectionStageUseCase;
import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.enums.DataCollectionStageEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;

/**
 * Servicio encargado de determinar en qué fase de recolección de datos se encuentra un cliente.
 */
public class GetExpedienteCollectionStageService implements GetExpedienteCollectionStageUseCase {

    private final IExpedienteRepository repository;

    public GetExpedienteCollectionStageService(IExpedienteRepository repository) {
        this.repository = repository;
    }

    @Override
    public DataCollectionStageEnum queryByClientPhone(String phoneNumber) {
        return repository.findActiveByClientPhone(phoneNumber)
                .map(Expediente::getCollectionStage)
                .orElse(null);
    }
}
