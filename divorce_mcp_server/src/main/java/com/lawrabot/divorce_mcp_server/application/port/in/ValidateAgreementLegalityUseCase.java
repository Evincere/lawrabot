package com.lawrabot.divorce_mcp_server.application.port.in;

import java.util.List;
import java.util.UUID;

public interface ValidateAgreementLegalityUseCase {

    /**
     * Revisa todas las alertas legales del Convenio y del Expediente.
     * Si retorna una lista vacía, el expediente está listo para firmar.
     * Si no, lista los faltantes para que el Bot pida más info al cliente.
     */
    List<String> executeSanityCheck(UUID expedienteId);
}
