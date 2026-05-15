package com.lawrabot.divorce_mcp_server.domain.model;

import com.lawrabot.divorce_mcp_server.domain.valueobject.AddressVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.CuilVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.FullNameVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.PhoneNumberVO;
import lombok.Builder;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Objeto de Dominio que representa a un ciudadano en el sistema.
 * Es la entidad central del Master Client Index (MCI).
 */
@Getter
@Builder
public class Citizen {
    private final UUID id;
    private final String dni;
    
    @Nullable
    private CuilVO cuil;
    @Nullable
    private FullNameVO fullName;
    @Nullable
    private PhoneNumberVO phoneNumber;
    @Nullable
    private String email;
    @Nullable
    private String nationality;
    @Nullable
    private String occupation;
    @Nullable
    private AddressVO address;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Citizen create(String dni, FullNameVO name) {
        return Citizen.builder()
                .id(UUID.randomUUID())
                .dni(dni)
                .fullName(name)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public void updateContactInfo(@Nullable PhoneNumberVO phone, @Nullable String email, @Nullable AddressVO address, @Nullable String nationality, @Nullable String occupation) {
        this.phoneNumber = phone;
        this.email = email;
        this.address = address;
        this.nationality = nationality;
        this.occupation = occupation;
        this.updatedAt = LocalDateTime.now();
    }
}
