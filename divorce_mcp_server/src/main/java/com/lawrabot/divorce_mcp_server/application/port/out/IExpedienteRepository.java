package com.lawrabot.divorce_mcp_server.application.port.out;

import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de Salida (Out Port) para el Aggregate Root Expediente.
 * Define el contrato que la capa de Infraestructura (Base de Datos) debe cumplir.
 */
public interface IExpedienteRepository {

    /**
     * Guarda o actualiza un Expediente en el almacenamiento persistente.
     */
    Expediente save(Expediente expediente);

    /**
     * Busca un Expediente por su UUID único.
     */
    Optional<Expediente> findById(UUID id);

    /**
     * En el contexto de un Bot de WhatsApp, a menudo buscaremos el 
     * expediente activo ("en borrador") de un número de teléfono / usuario.
     */
    Optional<Expediente> findActiveByClientPhone(String phoneNumber);

    /**
     * Busca un expediente activo vinculado a un DNI específico (sea peticionante o socioeconómico).
     */
    Optional<Expediente> findActiveByDni(String dni);

    /**
     * Obtiene el listado completo de expedientes (trámites de divorcio).
     */
    java.util.List<Expediente> findAll();
}
