package com.lawrabot.divorce_mcp_server.application.service;

import com.lawrabot.divorce_mcp_server.application.port.in.CreateDivorceDossierUseCase;
import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.application.port.out.ISpouseRepository;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.domain.model.Spouse;
import com.lawrabot.divorce_mcp_server.domain.valueobject.FullNameVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.PhoneNumberVO;

/**
 * Implementación "Pure Java" del caso de uso. Sin anotaciones de Spring Boot,
 * garantizando portabilidad e independencia de framework.
 */
public class CreateDivorceDossierService implements CreateDivorceDossierUseCase {

    private final IExpedienteRepository expedienteRepository;
    private final ISpouseRepository spouseRepository;

    public CreateDivorceDossierService(IExpedienteRepository expedienteRepository, ISpouseRepository spouseRepository) {
        this.expedienteRepository = expedienteRepository;
        this.spouseRepository = spouseRepository;
    }

    @Override
    public Expediente execute(String clientPhone, String firstName, String lastName) {
        // 1. Buscamos si existe un expediente activo.
        // Si ya pasó la etapa de BLSG_PRECONSULTA, lanzamos error. Si está en BLSG_PRECONSULTA, lo reutilizamos.
        Expediente expediente = expedienteRepository.findActiveByClientPhone(clientPhone).orElse(null);

        if (expediente != null) {
            if (expediente.getStatus() != com.lawrabot.divorce_mcp_server.domain.enums.ExpedienteStatusEnum.BLSG_PRECONSULTA) {
                throw new IllegalStateException("Ya existe un trámite de divorcio activo para este celular (" + expediente.getId() + ").");
            }
        } else {
            // Si el BLSG no se usó por algún motivo, lo creamos de cero
            expediente = Expediente.createNew(PhoneNumberVO.of(clientPhone), com.lawrabot.divorce_mcp_server.domain.enums.DivorceTypeEnum.UNILATERAL);
        }

        // 2. Crear o actualizar el Cónyuge Peticionante usando la lógica de Dominio
        Spouse petitioner = Spouse.builder()
                .id(java.util.UUID.randomUUID())
                .name(new FullNameVO(firstName, lastName))
                .phoneNumber(PhoneNumberVO.of(clientPhone))
                // Si el DNI ya estuviera, lo pasaríamos, pero start_divorce_process no lo recibe. El DNI queda en el SocioEconomicProfile.
                .build();
        
        // 3. Conectamos y guardamos
        expediente.setPetitioner(petitioner);
        
        // Actualizamos estado a la siguiente fase
        expediente.updateCollectionStage(com.lawrabot.divorce_mcp_server.domain.enums.DataCollectionStageEnum.PENDING_SOCIOECONOMIC_EVALUATION);

        spouseRepository.save(petitioner);
        return expedienteRepository.save(expediente);

    }
}
