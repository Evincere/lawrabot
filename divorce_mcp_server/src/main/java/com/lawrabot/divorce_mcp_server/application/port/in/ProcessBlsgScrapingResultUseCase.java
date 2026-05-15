package com.lawrabot.divorce_mcp_server.application.port.in;

import com.lawrabot.divorce_mcp_server.domain.enums.BlsgScrapingResultEnum;
import java.util.UUID;

/**
 * Puerto de Entrada invocado por el Agente Node.js una vez concluye el scraping
 * del Poder Judicial para un DNI determinado.
 * Es el puente entre la acción asíncrona del scraper y el estado persistido del Expediente.
 */
public interface ProcessBlsgScrapingResultUseCase {

    /**
     * @param expedienteId    ID del Expediente que desencadenó el scraping.
     * @param result          Resultado del scraping (aprobado, rechazado, inconcluyente).
     * @param justification   Texto descriptivo devuelto por el sistema judicial (puede ser nulo).
     */
    void execute(UUID expedienteId, BlsgScrapingResultEnum result, String justification, String fullName, String dni, String cuil, String birthDate, String province, String sex, String certificatePath);
}
