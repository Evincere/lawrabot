package com.lawrabot.divorce_mcp_server.infrastructure.persistence.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representación persistible de AddressVO.
 * Se incrustra como columnas dentro de la tabla que la contiene.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressEmbeddable {

    @Column(name = "street")
    private String street;

    @Column(name = "number")
    private String number;

    @Column(name = "floor_apartment")
    private String floorAppartment;

    @Column(name = "neighborhood")
    private String neighborhood;

    @Column(name = "locality")
    private String locality;

    @Column(name = "province")
    private String province;

    @Column(name = "zip_code", length = 10)
    private String zipCode;
}
