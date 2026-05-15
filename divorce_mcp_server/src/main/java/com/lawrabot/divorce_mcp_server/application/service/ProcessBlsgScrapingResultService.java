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
    public void execute(UUID expedienteId, BlsgScrapingResultEnum result, String justification, String fullName, String dni, String cuil, String birthDate, String province, String sex, String certificatePath) {
        Expediente expediente = repository.findById(expedienteId)
                .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado: " + expedienteId));

        // Delegamos la lógica de actualización multivariable al Dominio
        expediente.processScrapingResult(result, justification, fullName, dni, cuil, birthDate, province, sex, certificatePath);

        repository.save(expediente);
    }
}
