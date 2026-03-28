package com.lawrabot.divorce_mcp_server.application.port.in;

import com.lawrabot.divorce_mcp_server.domain.valueobject.AddressVO;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Puerto de Entrada (Use Case) invocado por el Bot en la etapa 2:
 * "Datos del Matrimonio".
 */
public interface SubmitMarriageDetailsUseCase {

    /**
     * @param expedienteId ID central del caso.
     * @param marriageDate Fecha de celebración del matrimonio civil.
     * @param separationDate Fecha (opcional o aproximada) de la separación de hecho.
     * @param lastResidence Estructura del último hogar conyugal.
     */
    void execute(UUID expedienteId, LocalDate marriageDate, LocalDate separationDate, AddressVO lastResidence);
}
