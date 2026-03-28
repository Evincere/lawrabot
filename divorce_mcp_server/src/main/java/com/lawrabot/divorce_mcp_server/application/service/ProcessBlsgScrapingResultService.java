package com.lawrabot.divorce_mcp_server.application.service;

import com.lawrabot.divorce_mcp_server.application.port.in.ProcessBlsgScrapingResultUseCase;
import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.enums.BlsgScrapingResultEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;

import java.util.UUID;

public class ProcessBlsgScrapingResultService implements ProcessBlsgScrapingResultUseCase {

    private final IExpedienteRepository repository;

    public ProcessBlsgScrapingResultService(IExpedienteRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID expedienteId, BlsgScrapingResultEnum result, String justification) {
        Expediente expediente = repository.findById(expedienteId)
                .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado: " + expedienteId));

        // La lógica de negocio (rechazo, avance de etapa) vive en el Dominio.
        expediente.processScrapingResult(result, justification);

        repository.save(expediente);
    }
}
