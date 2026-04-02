package com.lawrabot.divorce_mcp_server.infrastructure.mcp.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record CreateDossierRequest(
    @JsonPropertyDescription("Teléfono del cliente en formato internacional sin +. Ejemplo: 5492611234567")
    String phoneNumber,
    
    @JsonPropertyDescription("Nombre completo o nombre de pila del solicitante.")
    String firstName,
    
    @JsonPropertyDescription("Apellido del solicitante.")
    String lastName
) {}
