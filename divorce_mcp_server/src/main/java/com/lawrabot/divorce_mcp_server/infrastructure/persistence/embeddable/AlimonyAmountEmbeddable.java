package com.lawrabot.divorce_mcp_server.infrastructure.persistence.embeddable;

import com.lawrabot.divorce_mcp_server.domain.enums.CurrencyParameterEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Representación persistible de AlimonyAmountVO.
 * Usado en la tabla de agreements y compensaciones económicas.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlimonyAmountEmbeddable {

    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency_or_parameter", length = 30)
    private CurrencyParameterEnum currencyOrParameter;

    @Column(name = "custom_parameter", length = 200)
    private String customParameter;
}
