package com.lawrabot.divorce_mcp_server.domain.valueobject;

import lombok.Value;

/**
 * Objeto de Valor para el Nombre Completo.
 * Normaliza a MAYÚSCULAS para cumplir con estándares judiciales.
 */
@Value
public class FullNameVO {
    String firstName;
    String lastName;

    public FullNameVO(String firstName, String lastName) {
        if (firstName == null || firstName.isBlank() || lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Nombre y Apellido son obligatorios.");
        }
        this.firstName = firstName.trim().toUpperCase();
        this.lastName = lastName.trim().toUpperCase();
    }

    /**
     * Devuelve el nombre completo en formato "APELLIDO, NOMBRE".
     */
    public String getLegalFormat() {
        return lastName + ", " + firstName;
    }

    /**
     * Devuelve el nombre completo en formato "NOMBRE APELLIDO".
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }
}
