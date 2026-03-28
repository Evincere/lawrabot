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
        // 1. Verificamos que no haya ya un expediente activo para este número
        // Esto previene que el chatbot cree 10 expedientes si el usuario dice "Hola" 10 veces.
        expedienteRepository.findActiveByClientPhone(clientPhone)
                .ifPresent(exp -> {
                    throw new IllegalStateException("Ya existe un trámite de divorcio activo para este número de WhatsApp (" + exp.getId() + ").");
                });

        // 2. Usando la fábrica de Dominio para el Expediente
        // Por defecto asumimos UNILATERAL hasta que el bot determine lo contrario
        Expediente nuevoExpediente = Expediente.createNew(PhoneNumberVO.of(clientPhone), com.lawrabot.divorce_mcp_server.domain.enums.DivorceTypeEnum.UNILATERAL);

        // 3. Crear el Cónyuge Peticionante usando la lógica de Dominio
        Spouse petitioner = Spouse.builder()
                .id(java.util.UUID.randomUUID())
                .name(new FullNameVO(firstName, lastName))
                .phoneNumber(PhoneNumberVO.of(clientPhone))
                .build();
        
        // 4. Conectamos y guardamos (Idealmente dentro de una transacción en otra capa superior)
        nuevoExpediente.setPetitioner(petitioner);

        // Guardar primero el spouse por constraints de BD, o depender del cascade.
        // Asumiendo que Repository maneja esto, guardamos el agregate root.
        spouseRepository.save(petitioner);
        return expedienteRepository.save(nuevoExpediente);
    }
}
