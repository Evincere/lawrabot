package com.lawrabot.divorce_mcp_server.domain.repository;

import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import java.util.Optional;
import java.util.UUID;

/**
 * Interfaz de repositorio para Expediente (Puerto).
 * Arquitectura Limpia: El Dominio define la interfaz, la Infraestructura la implementa.
 */
public interface IExpedienteRepository {
    Expediente save(Expediente expediente);
    Optional<Expediente> findById(UUID id);
    Optional<Expediente> findByContactPhoneNumber(String phoneNumber);
    boolean existsByContactPhoneNumber(String phoneNumber);
}
