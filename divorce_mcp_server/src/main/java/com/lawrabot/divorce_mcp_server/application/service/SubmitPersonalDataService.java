package com.lawrabot.divorce_mcp_server.application.service;

import com.lawrabot.divorce_mcp_server.application.port.in.SubmitPersonalDataUseCase;
import com.lawrabot.divorce_mcp_server.application.port.out.ICitizenRepository;
import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.enums.CaseRole;
import com.lawrabot.divorce_mcp_server.domain.enums.DataCollectionStageEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.DivorceTypeEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Citizen;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.domain.model.Spouse;
import com.lawrabot.divorce_mcp_server.domain.valueobject.AddressVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.DNIVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.FullNameVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Service
public class SubmitPersonalDataService implements SubmitPersonalDataUseCase {

    private final IExpedienteRepository expedienteRepository;
    private final ICitizenRepository citizenRepository;

    public SubmitPersonalDataService(IExpedienteRepository expedienteRepository,
            ICitizenRepository citizenRepository) {
        this.expedienteRepository = expedienteRepository;
        this.citizenRepository = citizenRepository;
    }

    @Override
    @Transactional
    public void execute(String phoneNumber, CaseRole role, String fullName, String dni, String participantPhoneNumber, String nationality,
            String occupation, String email, String birthDate, AddressVO address) {
        Expediente expediente = expedienteRepository.findActiveByClientPhone(phoneNumber)
                .orElseThrow(
                        () -> new IllegalArgumentException("No se encontró expediente activo para: " + phoneNumber));

        // Parsear fecha de nacimiento si viene informada
        LocalDate parsedBirthDate = parseBirthDate(birthDate);

        if (role == CaseRole.PETITIONER) {
            updatePetitioner(expediente, participantPhoneNumber, nationality, occupation, email, parsedBirthDate, address);
        } else {
            updateRespondent(expediente, fullName, dni, participantPhoneNumber, nationality, occupation, email, parsedBirthDate, address);
        }

        // Determinar siguiente etapa
        if (isPersonalDataComplete(expediente)) {
            expediente.updateCollectionStage(DataCollectionStageEnum.PENDING_SOCIOECONOMIC_EVALUATION);
        }

        expedienteRepository.save(expediente);
    }

    /**
     * Parsea una fecha de nacimiento en formato YYYY-MM-DD.
     * Retorna null si el valor es nulo, vacío o no puede parsearse.
     */
    private LocalDate parseBirthDate(String birthDateStr) {
        if (birthDateStr == null || birthDateStr.isBlank()) return null;
        try {
            return LocalDate.parse(birthDateStr);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void updatePetitioner(Expediente exp, String phoneNumber, String nationality, String occupation, String email,
            LocalDate birthDate, AddressVO address) {
        Spouse p = exp.getPetitioner();

        var contactPhone = exp.getContactPhoneNumber();

        // Resiliencia: si el Spouse no existe (expediente legacy/huérfano), lo creamos con datos mínimos
        if (p == null) {
            p = Spouse.builder()
                    .id(java.util.UUID.randomUUID())
                    .phoneNumber(contactPhone)
                    .build();
        }

        var phoneVO = (phoneNumber != null && !phoneNumber.isBlank()) 
            ? com.lawrabot.divorce_mcp_server.domain.valueobject.PhoneNumberVO.of(phoneNumber) 
            : p.getPhoneNumber();

        // Si viene birthDate desde el tool, usarlo; sino preservar el existente 
        // (puede venir del scraping BLSG o de una edición anterior)
        LocalDate effectiveBirthDate = birthDate != null ? birthDate : p.getBirthDate();

        Spouse updated = Spouse.builder()
                .id(p.getId())
                .name(p.getName())
                .dni(p.getDni())
                .cuil(p.getCuil())
                .birthDate(effectiveBirthDate)
                .phoneNumber(phoneVO)
                .address(address != null ? address : p.getAddress())
                .nationality(nationality != null ? nationality : p.getNationality())
                .email(email != null ? email : p.getEmail())
                .profession(occupation != null ? occupation : p.getProfession())
                .build();

        exp.setPetitioner(updated);

        // Sincronizar con MCI
        DNIVO petitionerDni = updated.getDni();
        if (petitionerDni != null) {
            final String dniValue = petitionerDni.getValue();
            final Spouse finalUpdated = updated;
            citizenRepository.findByDni(dniValue).ifPresent(c -> {
                c.updateContactInfo(finalUpdated.getPhoneNumber(), finalUpdated.getEmail(), finalUpdated.getAddress(), finalUpdated.getNationality(), finalUpdated.getProfession());
                citizenRepository.save(c);
            });
        }
    }

    private void updateRespondent(Expediente exp, String fullName, String dni, String phoneNumber, String nationality, String occupation,
            String email, LocalDate birthDate, AddressVO address) {
        Spouse r = exp.getRespondent();

        // Si ya existe (ej: de BLSG), preservamos datos base
        FullNameVO nameVO = (fullName != null && !fullName.isBlank()) ? FullNameVO.fromFullString(fullName)
                : (r != null ? r.getName() : null);
        DNIVO dniVO = (dni != null && !dni.isBlank()) ? DNIVO.of(dni) : (r != null ? r.getDni() : null);

        if (r == null) {
            r = Spouse.builder().id(java.util.UUID.randomUUID()).build();
        }

        var phoneVO = (phoneNumber != null && !phoneNumber.isBlank()) 
            ? com.lawrabot.divorce_mcp_server.domain.valueobject.PhoneNumberVO.of(phoneNumber) 
            : r.getPhoneNumber();

        // Si viene birthDate desde el tool, usarlo; sino preservar el existente
        LocalDate effectiveBirthDate = birthDate != null ? birthDate : r.getBirthDate();

        Spouse updated = Spouse.builder()
                .id(r.getId())
                .name(nameVO)
                .dni(dniVO)
                .cuil(r.getCuil())
                .birthDate(effectiveBirthDate)
                .phoneNumber(phoneVO)
                .address(address != null ? address : r.getAddress())
                .nationality(nationality != null ? nationality : r.getNationality())
                .email(email != null ? email : r.getEmail())
                .profession(occupation != null ? occupation : r.getProfession())
                .build();

        exp.setRespondent(updated);

        // Sincronizar con MCI para el respondent
        if (dniVO != null) {
            final String dniValue = dniVO.getValue();
            final FullNameVO finalName = nameVO;
            final Spouse finalUpdated = updated;

            Citizen c = citizenRepository.findByDni(dniValue)
                    .orElseGet(() -> Citizen.create(dniValue, finalName));

            c.updateContactInfo(phoneVO, finalUpdated.getEmail(), finalUpdated.getAddress(), finalUpdated.getNationality(),
                    finalUpdated.getProfession());
            citizenRepository.save(c);
        }
    }

    private boolean isPersonalDataComplete(Expediente exp) {
        // Regla básica: Petitioner siempre completo.
        // Respondent completo si es JOINT. Si es UNILATERAL, mínimos.
        if (exp.getPetitioner() == null)
            return false;

        if (exp.getDivorceType() == DivorceTypeEnum.JOINT) {
            Spouse respondent = exp.getRespondent();
            return respondent != null && respondent.getDni() != null;
        } else {
            return exp.getRespondent() != null;
        }
    }
}
