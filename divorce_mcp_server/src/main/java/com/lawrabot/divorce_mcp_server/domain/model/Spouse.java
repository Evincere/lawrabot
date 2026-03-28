package com.lawrabot.divorce_mcp_server.domain.model;

import com.lawrabot.divorce_mcp_server.domain.valueobject.AddressVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.CuilVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.DNIVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.FullNameVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.PhoneNumberVO;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

/**
 * Entidad de dominio que representa a un cónyuge en un proceso de divorcio.
 * Contiene la información legal necesaria para la demanda y notificaciones.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Spouse {

    private UUID id;
    private FullNameVO name;
    private DNIVO dni;
    private CuilVO cuil;
    private LocalDate birthDate;
    
    // Direcciones legales estructuradas
    private AddressVO residentialAddress; // Domicilio real
    private AddressVO legalAddress;      // Domicilio legal
    private AddressVO specialAddress;    // Domicilio especial
    
    private String profession;
    private PhoneNumberVO phoneNumber;
    private String email; // Pendiente EmailVO

    /**
     * Método fábrica para crear un nuevo Cónyuge.
     */
    public static Spouse create(FullNameVO name, DNIVO dni) {
        return Spouse.builder()
                .id(UUID.randomUUID())
                .name(name)
                .dni(dni)
                .build();
    }

    // ============================================
    // LÓGICA DE NEGOCIO
    // ============================================

    /**
     * Calcula la edad actual.
     */
    public int getAge() {
        if (birthDate == null) return 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * Verifica mayoría de edad.
     */
    public boolean isAdult() {
        return getAge() >= 18;
    }

    /**
     * Verifica datos mínimos para la demanda judicial.
     */
    public boolean hasMinimumLegalData() {
        return name != null && 
               dni != null && 
               residentialAddress != null && 
               cuil != null;
    }
}
