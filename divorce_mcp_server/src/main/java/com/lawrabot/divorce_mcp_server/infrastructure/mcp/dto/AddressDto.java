package com.lawrabot.divorce_mcp_server.infrastructure.mcp.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record AddressDto(
    @JsonPropertyDescription("Calle del domicilio") String street,
    @JsonPropertyDescription("Número de puerta") String number,
    @JsonPropertyDescription("Piso y departamento, si aplica") String floorAppartment,
    @JsonPropertyDescription("Barrio") String neighborhood,
    @JsonPropertyDescription("Localidad o ciudad") String locality,
    @JsonPropertyDescription("Provincia (Ej: Mendoza)") String province,
    @JsonPropertyDescription("Código Postal") String zipCode
) {}
