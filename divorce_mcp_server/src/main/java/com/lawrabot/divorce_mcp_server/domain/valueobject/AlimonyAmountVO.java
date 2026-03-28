package com.lawrabot.divorce_mcp_server.domain.valueobject;

import com.lawrabot.divorce_mcp_server.domain.enums.CurrencyParameterEnum;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value Object inmutable que representa de forma segura una cantidad
 * alimentaria. Agrupa indivisiblemente el monto y la moneda/parámetro.
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AlimonyAmountVO {

    private BigDecimal value;
    private CurrencyParameterEnum currencyOrParameter;
    
    // Campo para especificar si el usuario eligió "OTHER" en moneda/parámetro
    private String customParameter;

    /**
     * Construye y valida una cantidad alimentaria.
     */
    public static AlimonyAmountVO of(BigDecimal value, CurrencyParameterEnum currency, String customParameter) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El monto alimentario no puede ser negativo o nulo.");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Se requiere una moneda o parámetro de actualización.");
        }
        if (currency == CurrencyParameterEnum.OTHER && (customParameter == null || customParameter.trim().isEmpty())) {
            throw new IllegalArgumentException("Debe especificar el parámetro personalizado al seleccionar OTHER.");
        }

        return new AlimonyAmountVO(value, currency, customParameter);
    }

    /**
     * Retorna una representación en texto legal de la cantidad.
     */
    public String toLegalString() {
        if (currencyOrParameter == CurrencyParameterEnum.OTHER) {
            return value.toString() + " " + customParameter;
        }
        // Ej: "30 SALARY_PERCENTAGE" u "80000 ARS"
        return value.toString() + " " + currencyOrParameter.name();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlimonyAmountVO that = (AlimonyAmountVO) o;
        return Objects.equals(value, that.value) &&
               currencyOrParameter == that.currencyOrParameter &&
               Objects.equals(customParameter, that.customParameter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, currencyOrParameter, customParameter);
    }
}
