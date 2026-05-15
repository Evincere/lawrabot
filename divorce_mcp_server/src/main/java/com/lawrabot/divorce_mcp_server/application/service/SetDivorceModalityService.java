package com.lawrabot.divorce_mcp_server.application.service;

import com.lawrabot.divorce_mcp_server.application.port.in.SetDivorceModalityUseCase;
import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.enums.DataCollectionStageEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.DivorceTypeEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetDivorceModalityService implements SetDivorceModalityUseCase {

    private final IExpedienteRepository expedienteRepository;

    public SetDivorceModalityService(IExpedienteRepository expedienteRepository) {
        this.expedienteRepository = expedienteRepository;
    }

    @Override
    @Transactional
    public void execute(String phoneNumber, DivorceTypeEnum modality) {
        Expediente expediente = expedienteRepository.findActiveByClientPhone(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró expediente activo para: " + phoneNumber));

        expediente.updateDivorceType(modality);
        
        // Si es CONJUNTO, pasamos a esperar el BLSG del respondido. 
        // Si es UNILATERAL, pasamos directo a datos personales.
        DataCollectionStageEnum nextStage = (modality == DivorceTypeEnum.JOINT) 
                ? DataCollectionStageEnum.PENDING_RESPONDENT_BLSG 
                : DataCollectionStageEnum.PENDING_PERSONAL_DATA;
                
        expediente.updateCollectionStage(nextStage);
        expedienteRepository.save(expediente);
    }
}
