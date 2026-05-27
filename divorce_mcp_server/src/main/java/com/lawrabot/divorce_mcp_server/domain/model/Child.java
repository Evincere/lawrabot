package com.lawrabot.divorce_mcp_server.domain.model;

import com.lawrabot.divorce_mcp_server.domain.valueobject.DNIVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.FullNameVO;
import lombok.Builder;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

/**
 * Entidad que representa un hijo fruto del matrimonio.
 */
@Getter
@Builder
public class Child {

    private final UUID id;

    private final FullNameVO name;
    private final LocalDate birthDate;

    // Algunos hijos pueden no tener DNI aún, o no haber sido recolectado.
    @Nullable
    private final DNIVO dni;

    @Builder.Default
    private boolean disabled = false;   

    @Builder.Default
    private boolean student = false;

    @Nullable
    private final String birthCertificateId;

    public static Child create(FullNameVO name, LocalDate birthDate, @Nullable DNIVO dni, boolean disabled, boolean student, @Nullable String birthCertificateId) {
        return Child.builder()
                .id(UUID.randomUUID())
                .name(name)
                .birthDate(birthDate)
                .dni(dni)
                .disabled(disabled)
                .student(student)
                .birthCertificateId(birthCertificateId)
                .build();
    }

    public String getFullName() {
        return name != null ? name.getFullName() : "S/D";
    }

    public int getAge() {
        if (birthDate == null) return 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}
