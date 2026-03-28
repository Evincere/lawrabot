package com.lawrabot.divorce_mcp_server.domain.model;

import com.lawrabot.divorce_mcp_server.domain.valueobject.DNIVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.FullNameVO;
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
 * Entidad de dominio que representa a un hijo en un proceso de divorcio.
 * Contiene información vital para determinar responsabilidad parental, 
 * cuidado personal y cuota alimentaria.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Child {

    private UUID id;
    private FullNameVO name;
    private DNIVO dni;
    private LocalDate birthDate;
    
    // Indica si posee Certificado Único de Discapacidad (CUD)
    // Esencial para la extensión de la cuota alimentaria en Argentina
    private boolean hasDisability;

    // Estado de estudios (Art. 663 CCyC)
    // Extiende la cuota alimentaria hasta los 25 años si le impide proveerse
    private boolean isStudying;

    // Indica si el hijo puede mantenerse por sí mismo (medios propios/trabajo)
    // Excluyente para la cuota alimentaria entre los 21 y 25 años (Art. 663 CCyC)
    private boolean canSelfSustain;

    // Centro de Vida del niño/a
    // Lugar principal donde reside y se desarrolla (Ej: "Domicilio Materno", "Compartido")
    private String mainResidence;

    /**
     * Método fábrica para crear un nuevo Hijo.
     */
    public static Child create(FullNameVO name, DNIVO dni, LocalDate birthDate) {
        return Child.builder()
                .id(UUID.randomUUID())
                .name(name)
                .dni(dni)
                .birthDate(birthDate)
                .hasDisability(false)    // Por defecto falso
                .isStudying(false)       // Por defecto falso
                .canSelfSustain(false)   // Por defecto falso
                .build();
    }

    // ============================================
    // LÓGICA DE NEGOCIO Y LEGAL
    // ============================================

    /**
     * Calcula la edad actual del hijo.
     */
    public int getAge() {
        if (birthDate == null) return 0;
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * Categoriza al hijo según el Código Civil y Comercial de la Nación.
     * Crucial para determinar autonomía progresiva y derecho a ser oído.
     */
    public String getAgeGroupCategory() {
        int age = getAge();
        if (age < 13) return "NIÑO/A";
        if (age < 18) return "ADOLESCENTE";
        return "MAYOR_DE_EDAD";
    }

    /**
     * Lógica de negocio: verifica si el hijo es menor de edad (menor de 18 años).
     * Determina si está sujeto a responsabilidad parental plena.
     */
    public boolean isMinor() {
        return getAge() < 18;
    }

    /**
     * Verifica si el hijo tiene derecho presunto a cuota alimentaria.
     * Reglas según el CCyC:
     * - Ilimitado si posee CUD (Discapacidad).
     * - Hasta los 21 años (Art. 658).
     * - Hasta los 25 años si estudia y la capacitación le impide proveerse
     *   de los medios necesarios para sostenerse (Art. 663).
     */
    public boolean isEntitledToAlimony() {
        if (hasDisability) return true;
        
        int age = getAge();
        if (age < 21) return true;
        
        // Extensión por estudios (Art. 663)
        // Requiere estar estudiando Y NO poder autosustentarse
        return age >= 21 && age < 25 && isStudying && !canSelfSustain;
    }
}
