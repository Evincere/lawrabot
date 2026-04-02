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

    public static FullNameVO fromFullString(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Nombre completo no puede ser nulo o vacío.");
        }
        String[] parts = fullName.split(" ", 2);
        if (parts.length == 2) {
            return new FullNameVO(parts[0], parts[1]);
        } else {
            return new FullNameVO(parts[0], "PENDIENTE"); // Apellido por defecto si solo hay un nombre
        }
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
