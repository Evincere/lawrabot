package com.lawrabot.divorce_mcp_server.infrastructure.mcp.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record AddressDto(
    @JsonPropertyDescription("CAMPO 'street': Solo el nombre de la calle, SIN número. Ej: Si el usuario dice 'Alem 456', aquí va solo 'Alem'. PROHIBIDO usar 'calle' como nombre de campo.") String street,
    @JsonPropertyDescription("CAMPO 'number': Solo el número de puerta/altura, separado de la calle. Ej: Si el usuario dice 'Alem 456', aquí va solo '456'. PROHIBIDO usar 'numero' como nombre de campo.") String number,
    @JsonPropertyDescription("CAMPO 'floorAppartment': Piso y departamento, si aplica. Puede ser null.") String floorAppartment,
    @JsonPropertyDescription("CAMPO 'neighborhood': Barrio, si aplica. Puede ser null.") String neighborhood,
    @JsonPropertyDescription("CAMPO 'locality': Localidad o ciudad. PROHIBIDO usar 'localidad' como nombre de campo.") String locality,
    @JsonPropertyDescription("CAMPO 'province': Provincia. Ej: 'Mendoza'. PROHIBIDO usar 'provincia' como nombre de campo.") String province,
    @JsonPropertyDescription("CAMPO 'zipCode': Código postal, si se conoce. Puede ser null.") String zipCode
) {}
