package com.lawrabot.divorce_mcp_server.application.port.in;

public interface GenerateReferralPdfUseCase {
    /**
     * Genera un resumen en PDF del expediente para atención presencial.
     * 
     * @param phoneNumber El número de teléfono del usuario.
     * @return La ruta absoluta del archivo PDF generado.
     */
    String execute(String phoneNumber);
}
