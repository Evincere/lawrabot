package com.lawrabot.divorce_mcp_server.domain.valueobject;

import lombok.Builder;
import lombok.Value;

/**
 * Objeto de Valor para Direcciones en Mendoza.
 * Estructura los datos para la competencia judicial y notificaciones.
 */
@Value
@Builder
public class AddressVO {
    String street;
    String number;
    String floorAppartment;
    String neighborhood;
    String locality; // Ej: Las Heras, Guaymallén, San Rafael
    String province; // Por defecto "Mendoza"
    String zipCode;

    /**
     * Formato legal exacto para escritos judiciales.
     */
    public String toLegalString() {
        StringBuilder sb = new StringBuilder();
        sb.append("calle ").append(street).append(" N° ").append(number);
        
        if (floorAppartment != null && !floorAppartment.isBlank()) {
            sb.append(", Piso/Depto: ").append(floorAppartment);
        }
        
        if (neighborhood != null && !neighborhood.isBlank()) {
            sb.append(", Barrio ").append(neighborhood);
        }
        
        sb.append(", ").append(locality)
          .append(", Provincia de ").append(province != null ? province : "Mendoza");
        
        return sb.toString();
    }
}
