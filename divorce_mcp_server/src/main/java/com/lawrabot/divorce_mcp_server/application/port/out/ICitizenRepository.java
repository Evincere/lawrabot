package com.lawrabot.divorce_mcp_server.application.port.out;

import com.lawrabot.divorce_mcp_server.domain.model.Citizen;
import java.util.Optional;
import java.util.UUID;

public interface ICitizenRepository {
    Optional<Citizen> findById(UUID id);
    Optional<Citizen> findByDni(String dni);
    Citizen save(Citizen citizen);
}
