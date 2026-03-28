package com.lawrabot.divorce_mcp_server.application.service;

import com.lawrabot.divorce_mcp_server.application.port.in.SubmitChildrenInfoUseCase;
import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.model.Child;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;

import java.util.List;
import java.util.UUID;

public class SubmitChildrenInfoService implements SubmitChildrenInfoUseCase {

    private final IExpedienteRepository repository;

    public SubmitChildrenInfoService(IExpedienteRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID expedienteId, List<Child> children) {
        Expediente expediente = repository.findById(expedienteId)
                .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado."));

        // Avanza el cursor a Etapa 4 o gestiona la semántica
        expediente.registerChildren(children);

        repository.save(expediente);
    }
}
