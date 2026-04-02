package com.lawrabot.divorce_mcp_server.infrastructure.mcp.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ChildDto(
    @JsonPropertyDescription("DNI del hijo en formato sin puntos. Ej: '30123456'. Si no lo sabe, usar null.")
    String dni,
    
    @JsonPropertyDescription("Nombres del hijo")
    String firstName,
    
    @JsonPropertyDescription("Apellidos del hijo")
    String lastName,
    
    @JsonPropertyDescription("Fecha de nacimiento en formato YYYY-MM-DD")
    String birthDate,
    
    @JsonPropertyDescription("Indica si el hijo posee algún certificado de discapacidad (true/false)")
    boolean disabled
) {}
