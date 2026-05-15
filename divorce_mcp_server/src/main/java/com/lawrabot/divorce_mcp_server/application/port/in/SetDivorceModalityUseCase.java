package com.lawrabot.divorce_mcp_server.application.port.in;

import com.lawrabot.divorce_mcp_server.domain.enums.DivorceTypeEnum;

/**
 * Puerto de Entrada (Use Case) para definir la modalidad del divorcio (Unilateral o Conjunta).
 */
public interface SetDivorceModalityUseCase {

    /**
     * Establece la modalidad de divorcio para un expediente activo.
     *
     * @param phoneNumber Número de teléfono del cliente
     * @param modality Modalidad elegida (JOINT o UNILATERAL)
     */
    void execute(String phoneNumber, DivorceTypeEnum modality);
}
