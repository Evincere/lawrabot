package com.lawrabot.divorce_mcp_server.application.port.in;

/**
 * Puerto de entrada para la consulta del BLSG (Beneficio de Litigar Sin
 * Gastos).
 */
public interface ConsultarBlsgUseCase {

    /**
     * Ejecuta el proceso de scraping y validación de BLSG para un expediente.
     * 
     * @param phoneNumber El ID del expediente (uuid) o teléfono para identificar el
     *                    proceso.
     * @param dni         El DNI del ciudadano a consultar.
     * @return Un objeto con el resultado cargado del scraping.
     */
    ScrapingResult execute(String phoneNumber, String dni);

    record ScrapingResult(
            String fullName,
            String dni,
            String cuil,
            String birthDate,
            String province,
            String sex,
            String benefitStatus,
            String certificatePath,
            boolean success) {
    }
}
