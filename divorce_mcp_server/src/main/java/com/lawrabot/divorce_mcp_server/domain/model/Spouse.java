package com.lawrabot.divorce_mcp_server.domain.model;

import com.lawrabot.divorce_mcp_server.domain.valueobject.AddressVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.CuilVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.FullNameVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.PhoneNumberVO;
import lombok.Builder;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.util.UUID;

/**
 * Entidad que representa a uno de los cónyuges.
 */
@Getter
@Builder
public class Spouse {

    private final UUID id;

    // Datos obligatorios
    private FullNameVO name;
    
    // El teléfono es obligatorio para el peticionante pero opcional para el demandado inicial.
    @Nullable
    private PhoneNumberVO phoneNumber;

    // Datos opcionales (se recolectan después)
    @Nullable
    private CuilVO cuil;
    @Nullable
    private AddressVO address;
    @Nullable
    private String profession;

    /**
     * Fábrica para crear un cónyuge a partir de su nombre.
     */
    public static Spouse create(FullNameVO name, @Nullable PhoneNumberVO phoneNumber) {
        return Spouse.builder()
                .id(UUID.randomUUID())
                .name(name)
                .phoneNumber(phoneNumber)
                .build();
    }
}
