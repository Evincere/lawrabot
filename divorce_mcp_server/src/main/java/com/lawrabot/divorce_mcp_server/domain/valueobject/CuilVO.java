package com.lawrabot.divorce_mcp_server.domain.valueobject;

import java.util.Objects;

/**
 * Objeto de Valor que representa el CUIL/CUIT en Argentina.
 * Incluye validación del dígito verificador según el algoritmo de ANSES/AFIP.
 */
public final class CuilVO {
    private final String value;

    public CuilVO(String value) {
        if (value == null || !isValid(value)) {
            throw new IllegalArgumentException("Formato de CUIL/CUIT inválido: " + value);
        }
        this.value = normalize(value);
    }

    public String getValue() {
        return value;
    }

    /**
     * Normaliza el CUIL eliminando guiones y espacios.
     */
    private String normalize(String value) {
        return value.replaceAll("[^0-9]", "");
    }

    /**
     * Algoritmo de validación de CUIL/CUIT.
     */
    private boolean isValid(String cuil) {
        String cleanCuil = normalize(cuil);
        if (cleanCuil.length() != 11) return false;

        int[] multipliers = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(cleanCuil.charAt(i)) * multipliers[i];
        }

        int remainder = sum % 11;
        int result = 11 - remainder;

        if (result == 11) result = 0;
        if (result == 10) result = 9; // Caso especial

        return result == Character.getNumericValue(cleanCuil.charAt(10));
    }

    /**
     * Devuelve el CUIL formateado con guiones (XX-XXXXXXXX-X).
     */
    public String getFormatted() {
        return value.substring(0, 2) + "-" + value.substring(2, 10) + "-" + value.substring(10);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CuilVO cuilVO = (CuilVO) o;
        return Objects.equals(value, cuilVO.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return getFormatted();
    }
}
