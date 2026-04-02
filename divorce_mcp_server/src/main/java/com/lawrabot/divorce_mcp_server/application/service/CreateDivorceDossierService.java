package com.lawrabot.divorce_mcp_server.application.service;

import com.lawrabot.divorce_mcp_server.application.port.in.CreateDivorceDossierUseCase;
import com.lawrabot.divorce_mcp_server.application.port.out.ICitizenRepository;
import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.enums.CaseRole;
import com.lawrabot.divorce_mcp_server.domain.model.CaseParticipant;
import com.lawrabot.divorce_mcp_server.domain.model.Citizen;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.domain.valueobject.FullNameVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.PhoneNumberVO;

/**
 * Implementación del caso de uso para iniciar un expediente, ahora integrado con el Master Client Index (MCI).
 */
public class CreateDivorceDossierService implements CreateDivorceDossierUseCase {

    private final IExpedienteRepository expedienteRepository;
    private final ICitizenRepository citizenRepository;

    public CreateDivorceDossierService(IExpedienteRepository expedienteRepository, ICitizenRepository citizenRepository) {
        this.expedienteRepository = expedienteRepository;
        this.citizenRepository = citizenRepository;
    }

    @Override
    public Expediente execute(String clientPhone, String firstName, String lastName) {
        // 1. Master Client Index: Buscar o crear el Ciudadano
        Citizen citizen = citizenRepository.findByDni(null) // Por ahora no tenemos DNI en el inicio del bot
                .orElse(null);

        // Fallback por teléfono si no hay DNI (muy común en el inicio del flujo de WhatsApp)
        if (citizen == null) {
            // Buscamos si ya existe alguien con este teléfono
            // Nota: El repositorio debería soportar búsqueda por teléfono si queremos ser precisos.
            // Por simplicidad en este paso, creamos uno nuevo si no hay match claro.
            citizen = Citizen.builder()
                    .fullName(firstName + " " + lastName)
                    .phoneNumber(clientPhone)
                    .build();
            citizen = citizenRepository.save(citizen);
        }

        // 2. Gestión del Expediente
        Expediente expediente = expedienteRepository.findActiveByClientPhone(clientPhone).orElse(null);

        if (expediente != null) {
            if (expediente.getStatus() != com.lawrabot.divorce_mcp_server.domain.enums.ExpedienteStatusEnum.BLSG_PRECONSULTA) {
                throw new IllegalStateException("Ya existe un trámite de divorcio activo para este celular (" + expediente.getId() + ").");
            }
        } else {
            expediente = Expediente.createNew(PhoneNumberVO.of(clientPhone), com.lawrabot.divorce_mcp_server.domain.enums.DivorceTypeEnum.UNILATERAL);
        }

        // 3. Vincular Ciudadano al Expediente como PETITIONER (MCI Participation)
        CaseParticipant participant = CaseParticipant.builder()
                .citizen(citizen)
                .role(CaseRole.PETITIONER)
                .build();
        
        expediente.addParticipant(participant);
        
        // Actualizamos etapa de recolección
        expediente.updateCollectionStage(com.lawrabot.divorce_mcp_server.domain.enums.DataCollectionStageEnum.PENDING_SOCIOECONOMIC_EVALUATION);

        return expedienteRepository.save(expediente);
    }
}
