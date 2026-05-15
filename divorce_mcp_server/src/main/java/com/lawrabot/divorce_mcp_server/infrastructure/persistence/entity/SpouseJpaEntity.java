package com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity;

import com.lawrabot.divorce_mcp_server.infrastructure.persistence.embeddable.AddressEmbeddable;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.embeddable.FullNameEmbeddable;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.embeddable.PhoneNumberEmbeddable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad JPA para la tabla 'spouses'.
 * Es un detalle de infraestructura: el Dominio nunca depende de esta clase.
 */
@Entity
@Table(name = "spouses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpouseJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "firstName", column = @Column(name = "first_name")),
        @AttributeOverride(name = "lastName",  column = @Column(name = "last_name"))
    })
    private FullNameEmbeddable name;

    @Column(name = "dni", length = 15)
    private String dni;

    @Column(name = "cuil", length = 15)
    private String cuil;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "street",          column = @Column(name = "res_street")),
        @AttributeOverride(name = "number",          column = @Column(name = "res_number")),
        @AttributeOverride(name = "floorAppartment", column = @Column(name = "res_floor_apartment")),
        @AttributeOverride(name = "neighborhood",    column = @Column(name = "res_neighborhood")),
        @AttributeOverride(name = "locality",        column = @Column(name = "res_locality")),
        @AttributeOverride(name = "province",        column = @Column(name = "res_province")),
        @AttributeOverride(name = "zipCode",         column = @Column(name = "res_zip_code"))
    })
    private AddressEmbeddable residentialAddress;

    @Column(name = "profession", length = 100)
    private String profession;

    @Column(name = "nationality", length = 50)
    private String nationality;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "phoneNumber", column = @Column(name = "phone_number"))
    })
    private PhoneNumberEmbeddable phoneNumber;

    @Column(name = "email", length = 150)
    private String email;
}
