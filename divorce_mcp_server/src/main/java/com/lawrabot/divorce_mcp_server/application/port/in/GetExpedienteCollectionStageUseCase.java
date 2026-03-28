package com.lawrabot.divorce_mcp_server.application.port.in;

import com.lawrabot.divorce_mcp_server.domain.enums.DataCollectionStageEnum;

/**
 * Indispensable para que el servidor Node.js/WhatsApp pregunte:
 * "¿En qué andamos con este cliente que me acaba de enviar un mensaje?"
 */
public interface GetExpedienteCollectionStageUseCase {

    /**
     * Busca qué estaba respondiendo este número de teléfono activo.
     * Retorna NULL si el cliente nunca inició un caso.
     */
    DataCollectionStageEnum queryByClientPhone(String phoneNumber);
}
