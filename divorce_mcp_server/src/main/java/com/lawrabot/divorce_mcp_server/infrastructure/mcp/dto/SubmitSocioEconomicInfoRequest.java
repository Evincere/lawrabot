package com.lawrabot.divorce_mcp_server.infrastructure.mcp.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.math.BigDecimal;
import java.util.UUID;

public record SubmitSocioEconomicInfoRequest(
    @JsonPropertyDescription("ID UUID del Expediente.")
    UUID expedienteId,
    
    @JsonPropertyDescription("Ingreso mensual bruto aproximado en pesos argentinos (ARS). Si no tiene ingresos, enviar 0.")
    BigDecimal monthlyIncomeArs,
    
    @JsonPropertyDescription("Situación habitacional. Valores permitidos: RENTED (Alquilada), OWNED (Propia), BORROWED (Prestada), PRECARIOUS (Precaria), OTHER (Otro)")
    String housingSituation,
    
    @JsonPropertyDescription("Cantidad de vehículos (autos/motos) a su nombre. Si no tiene, enviar 0.")
    Integer vehiclesRegistered,
    
    @JsonPropertyDescription("¿Tiene trabajo formal en relación de dependencia? (true/false)")
    boolean hasFormalEmployment,
    
    @JsonPropertyDescription("Observaciones adicionales recolectadas durante la entrevista.")
    String observations
) {}
