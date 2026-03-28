package com.lawrabot.divorce_mcp_server.domain.model.agreement;

import com.lawrabot.divorce_mcp_server.domain.enums.PaymentFrequencyEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.PaymentMethodEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.ProvisionTypeEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.UpdateMechanismEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.CurrencyParameterEnum;
import com.lawrabot.divorce_mcp_server.domain.valueobject.AlimonyAmountVO;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Entidad que representa la propuesta o el acuerdo de Cuota Alimentaria (Art. 658 CCyC).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AlimonyProvision {

    private UUID id;

    // Naturaleza de la cuota
    private ProvisionTypeEnum provisionType;
    private String customProvisionType; // Rellenado solo si provisionType == OTHER

    // Objeto de valor inmutable (monto + moneda/porcentaje)
    // Se usa si provisionType es MONETARY o MIXED
    private AlimonyAmountVO amount;

    // Frecuencia
    private PaymentFrequencyEnum paymentFrequency;
    private String customPaymentFrequency; // Solo si == OTHER

    // Medio de pago
    private PaymentMethodEnum paymentMethod;
    private String customPaymentMethod; // Solo si == OTHER
    
    // CBU/Alias del beneficiario o CUIT/Razón Social del Empleador (para retención)
    private String paymentDetails; 

    // Fórmula para ganarle a la inflación argentina
    private UpdateMechanismEnum updateMechanism;
    private String customUpdateMechanism; // Solo si == OTHER

    /**
     * Fábrica para inicializar una cuota vacía
     */
    public static AlimonyProvision createEmpty() {
        return AlimonyProvision.builder()
                .id(UUID.randomUUID())
                .build();
    }

    // ============================================
    // LÓGICA DE NEGOCIO (Guía para el Bot)
    // ============================================

    /**
     * Indica si el bot está obligado a solicitar y validar un CBU/Alias.
     */
    public boolean requiresCbu() {
        return paymentMethod == PaymentMethodEnum.BANK_TRANSFER 
            || paymentMethod == PaymentMethodEnum.JUDICIAL_DEPOSIT;
    }

    /**
     * Indica si el bot necesita pedir los datos de la empresa del demandado para oficializar el embargo.
     */
    public boolean requiresEmployerData() {
        return paymentMethod == PaymentMethodEnum.EMPLOYER_WITHHOLDING;
    }

    /**
     * Advierte si la cuota pactada tiene riesgo de desvalorización acelerada.
     * En Argentina, una cuota en Pesos sin actualización es negligencia.
     */
    public boolean isIndexationMissingWarning() {
        if (provisionType == ProvisionTypeEnum.IN_KIND) return false;
        if (amount == null) return false; // Todavía no configurada

        boolean isPesos = amount.getCurrencyOrParameter() == CurrencyParameterEnum.ARS;
        boolean hasNoUpdate = updateMechanism == UpdateMechanismEnum.NONE;

        return isPesos && hasNoUpdate;
    }
}

