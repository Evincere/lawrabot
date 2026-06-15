package com.lawrabot.divorce_mcp_server.application.service;

import com.lawrabot.divorce_mcp_server.application.port.in.CreateDivorceDossierUseCase;
import com.lawrabot.divorce_mcp_server.application.port.out.ICitizenRepository;
import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.enums.CaseRole;
import com.lawrabot.divorce_mcp_server.domain.enums.ExpedienteStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Citizen;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.domain.model.Spouse;
import com.lawrabot.divorce_mcp_server.domain.valueobject.DNIVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.FullNameVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.PhoneNumberVO;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del caso de uso para iniciar un expediente, ahora integrado
 * con el Master Client Index (MCI).
 */
public class CreateDivorceDossierService implements CreateDivorceDossierUseCase {

    private final IExpedienteRepository expedienteRepository;
    private final ICitizenRepository citizenRepository;

    public CreateDivorceDossierService(IExpedienteRepository expedienteRepository,
            ICitizenRepository citizenRepository) {
        this.expedienteRepository = expedienteRepository;
        this.citizenRepository = citizenRepository;
    }

    @Override
    @Transactional
    public Expediente execute(String clientPhone, String firstName, String lastName, String dni) {
        // 1. Master Client Index: Buscar ciudadano por DNI (ya disponible del resultado BLSG)
        Citizen citizen = null;
        if (dni != null && !dni.isBlank()) {
            citizen = citizenRepository.findByDni(dni).orElse(null);
        }

        // Si no existe por DNI, lo creamos usando el factory de dominio que garantiza ID y timestamps
        if (citizen == null) {
            citizen = Citizen.create(dni, new FullNameVO(firstName, lastName));
            citizen.updateContactInfo(PhoneNumberVO.of(clientPhone), null, null, null, null);
            citizen = citizenRepository.save(citizen);
        }

        // 2. Gestión del Expediente
        Expediente expediente = expedienteRepository.findActiveByClientPhone(clientPhone).orElse(null);

        if (expediente != null) {
            ExpedienteStatusEnum status = expediente.getStatus();
            // Permitimos continuar si es borrador nuevo o si ya pasó el scraping pero aún está en recolección inicial
            if (status != ExpedienteStatusEnum.BLSG_PRECONSULTA && 
                status != ExpedienteStatusEnum.IN_DATA_COLLECTION_PROGRESS) {
                throw new IllegalStateException(
                        "Ya existe un proceso judicial de divorcio activo para este celular (" + expediente.getId() + ").");
            }
        } else {
            expediente = Expediente.createNew(PhoneNumberVO.of(clientPhone),
                    com.lawrabot.divorce_mcp_server.domain.enums.DivorceTypeEnum.UNILATERAL);
        }

        // 3. Vincular Ciudadano al Expediente como PETITIONER (MCI Participation)
        expediente.addParticipant(citizen, CaseRole.PETITIONER);

        // 4. Crear el Spouse (peticionante) con los datos disponibles del BLSG
        // Esto es esencial para que SubmitPersonalDataService pueda actualizar los datos más adelante
        if (expediente.getPetitioner() == null) {
            FullNameVO nameVO = new FullNameVO(firstName, lastName);
            DNIVO dniVO = (dni != null && !dni.isBlank()) ? DNIVO.of(dni) : null;
            PhoneNumberVO phoneVO = PhoneNumberVO.of(clientPhone);

            Spouse petitioner = Spouse.builder()
                    .id(java.util.UUID.randomUUID())
                    .name(nameVO)
                    .dni(dniVO)
                    .phoneNumber(phoneVO)
                    .build();

            expediente.setPetitioner(petitioner);
        }

        // 5. Actualizamos etapa de recolección y status.
        // En este punto del flujo, la consulta BLSG ya fue realizada exitosamente
        // (es prerrequisito para que el agente llame a start_divorce_process),
        // por lo que el status debe reflejar que estamos en recolección activa.
        expediente.updateCollectionStage(
                com.lawrabot.divorce_mcp_server.domain.enums.DataCollectionStageEnum.PENDING_MODALITY_SELECTION);
        expediente.updateStatus(
                com.lawrabot.divorce_mcp_server.domain.enums.ExpedienteStatusEnum.IN_DATA_COLLECTION_PROGRESS);

        return expedienteRepository.save(expediente);
    }
}
