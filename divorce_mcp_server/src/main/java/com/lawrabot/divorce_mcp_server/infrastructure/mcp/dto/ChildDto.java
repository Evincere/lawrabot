package com.lawrabot.divorce_mcp_server.infrastructure.mcp.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record ChildDto(
    @JsonPropertyDescription("CAMPO 'dni': DNI del hijo SIN puntos. Ej: '51711299'. Si no lo sabe, usar null. PROHIBIDO usar 'document' o 'cedula'.")
    String dni,
    
    @JsonPropertyDescription("CAMPO 'fullName': Nombre y apellido completo del hijo. Ej: 'Gaston Maraval'. PROHIBIDO usar 'name', 'nombre' o 'apellido' como nombre de campo.")
    String fullName,
    
    @JsonPropertyDescription("CAMPO 'birthDate': Fecha de nacimiento en formato YYYY-MM-DD. Ej: '2009-09-20'. PROHIBIDO usar 'fecha' o 'nacimiento' como nombre de campo.")
    String birthDate,
    
    @JsonPropertyDescription("CAMPO 'disabled': true si el hijo tiene certificado de discapacidad, false en caso contrario.")
    boolean disabled,

    @JsonPropertyDescription("CAMPO 'isStudent': true si el hijo de 21 a 24 años se encuentra estudiando o capacitándose, false o null en caso contrario.")
    Boolean isStudent
) {}
