package com.lawrabot.divorce_mcp_server.application.port.in;

import com.lawrabot.divorce_mcp_server.domain.model.Expediente;

/**
 * Puerto de Entrada (Use Case) que el controlador (ej. REST/WebHook Node.js) 
 * invocará cuando un cliente pide iniciar un divorcio en WhatsApp.
 */
public interface CreateDivorceDossierUseCase {

    /**
     * Orquesta la creación inical de un expediente tomando el WhatsApp 
     * y los datos primarios del solicitante.
     * 
     * @param clientPhone Teléfono del solicitante (formato internacional)
     * @param firstName Nombre de pila
     * @param lastName Apellido
     */
    Expediente execute(String clientPhone, String firstName, String lastName);
}
