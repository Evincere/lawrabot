package com.lawrabot.divorce_mcp_server.domain.valueobject;

import java.util.Objects;

/**
 * Value Object representing a validated Argentinian DNI (Documento Nacional de Identidad).
 *
 * DOMAIN RULES:
 * - DNI consists of 7 or 8 digits
 * - Can be provided with or without dots (30.123.456 or 30123456)
 * - Normalized to no-dots format internally
 * - Immutable and validated at creation
 *
 * USAGE:
 * - DNIVO.of("30123456") - valid
 * - DNIVO.of("30.123.456") - valid, dots removed
 * - DNIVO.of("1234567") - valid (7 digits)
 * - DNIVO.of("123") - throws IllegalArgumentException
 */
public final class DNIVO {

    private final String value;

    private DNIVO(String value) {
        this.value = value;
    }

    /**
     * Factory method to create a validated DNIVO.
     *
     * @param rawValue DNI number, can include dots or not
     * @return Validated DNIVO
     * @throws IllegalArgumentException if DNI format is invalid
     */
    public static DNIVO of(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException("DNI cannot be null or empty");
        }

        // Remove dots and spaces
        String normalized = rawValue.replaceAll("[.\\s]", "");

        // Validate: must be 7 or 8 digits
        if (!normalized.matches("^\\d{7,8}$")) {
            throw new IllegalArgumentException(
                "Invalid DNI format. Expected 7-8 digits, got: " + rawValue
            );
        }

        return new DNIVO(normalized);
    }

    /**
     * Returns the normalized DNI (no dots, no spaces).
     * Example: "30123456"
     */
    public String getValue() {
        return value;
    }

    /**
     * Returns formatted for display.
     * Example: "30.123.456"
     */
    public String toDisplayFormat() {
        if (value.length() == 7) {
            return String.format("%s.%s.%s",
                value.substring(0, 1),
                value.substring(1, 4),
                value.substring(4, 7)
            );
        } else {
            return String.format("%s.%s.%s",
                value.substring(0, 2),
                value.substring(2, 5),
                value.substring(5, 8)
            );
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DNIVO dnivo = (DNIVO) o;
        return value.equals(dnivo.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "DNIVO{" + "value='" + toDisplayFormat() + '\'' + '}';
    }
}
