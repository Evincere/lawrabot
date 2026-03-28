package com.lawrabot.divorce_mcp_server.application.port.in;

import com.lawrabot.divorce_mcp_server.domain.model.RegulatoryAgreement;
import java.util.UUID;

/**
 * Puerto de entrada para ir poblando o validando un Convenio Regulador en gestación.
 * El Bot lo llamará a medida que recolecte respuestas del cliente en WhatsApp.
 */
public interface DraftRegulatoryAgreementUseCase {

    /**
     * Completa o reemplaza la sección de Cuota Alimentaria (AlimonyProvision) 
     * en el expediente solicitado.
     */
    RegulatoryAgreement draftAlimony(UUID expedienteId, /* DTO de entrada simplificado en una vista real, aquí modelamos la firma abstracta */ Object alimonyDataDTO);

    /**
     * Avanza el estado del convenio si ambas partes dan su "OK" verbal por WhatsApp.
     */
    void markAsAcceptedByBothParties(UUID expedienteId);
}
