package com.lawrabot.divorce_mcp_server.application.port.out;

import com.lawrabot.divorce_mcp_server.domain.model.Child;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

/**
 * Puerto de Salida para el almacenamiento de los Hijos.
 */
public interface IChildRepository {

    Child save(Child child);

    Optional<Child> findById(UUID id);

    List<Child> findByExpedienteId(UUID expedienteId);
}
