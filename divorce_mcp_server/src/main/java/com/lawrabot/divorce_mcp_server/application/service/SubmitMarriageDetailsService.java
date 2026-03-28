package com.lawrabot.divorce_mcp_server.application.service;

import com.lawrabot.divorce_mcp_server.application.port.in.SubmitMarriageDetailsUseCase;
import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.domain.valueobject.AddressVO;

import java.time.LocalDate;
import java.util.UUID;

public class SubmitMarriageDetailsService implements SubmitMarriageDetailsUseCase {

    private final IExpedienteRepository repository;

    public SubmitMarriageDetailsService(IExpedienteRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID expedienteId, LocalDate marriageDate, LocalDate separationDate, AddressVO lastResidence) {
        Expediente expediente = repository.findById(expedienteId)
                .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado."));

        // Lógica de dominio pura: setea los campos, valida fechas, y avanza de Etapa al 3.
        expediente.provideMarriageDetails(marriageDate, separationDate, lastResidence);

        repository.save(expediente);
    }
}
