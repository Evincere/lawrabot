package com.lawrabot.divorce_mcp_server.application.port.out;

import com.lawrabot.divorce_mcp_server.domain.model.Spouse;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de Salida para el almacenamiento de los Cónyuges del Expediente.
 */
public interface ISpouseRepository {
    
    Spouse save(Spouse spouse);
    
    Optional<Spouse> findById(UUID id);
}
