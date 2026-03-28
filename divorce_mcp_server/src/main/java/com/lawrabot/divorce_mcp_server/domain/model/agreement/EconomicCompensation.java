package com.lawrabot.divorce_mcp_server.domain.model.agreement;

import com.lawrabot.divorce_mcp_server.domain.enums.CompensationPaymentEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.SpouseRoleEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.UpdateMechanismEnum;
import com.lawrabot.divorce_mcp_server.domain.valueobject.AlimonyAmountVO;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Entidad exclusiva que modela la Compensación Económica (Arts. 441 y 442 CCyC).
 * Busca equilibrar el patrimonio cuando el divorcio produce un empeoramiento
 * manifiesto en uno de los cónyuges.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EconomicCompensation {

    private UUID id;

    // 1. ¿Aplican? O dicho de otro modo: ¿La están reclamando en este acuerdo?
    private boolean appliesEconomicCompensation;

    // 2. Beneficiario (a favor de quién es la compensación)
    private SpouseRoleEnum beneficiary;

    // 3. Justificación fáctica exigida por el Código (Art 442)
    // Ej: "La esposa dedicó 15 años a criar a los niños y no se insertó en el mercado laboral"
    private String imbalanceJustification;

    // 4. Modalidad de Pago
    private CompensationPaymentEnum paymentMethod;
    private String customPaymentMethod; // Solo si == OTHER

    // 5. Datos Monetarios (Usamos AlimonyAmountVO ya que encapsula Monto de forma inmutable)
    private AlimonyAmountVO compensationAmount;

    // 6. Datos de Cuotas (solo relevantes si paymentMethod == INSTALLMENTS)
    private Integer installmentsCount;
    private UpdateMechanismEnum updateMechanism;
    private String customUpdateMechanism;

    // 7. Especie / Usufructo (solo si paymentMethod == USUFRUCT o OTHER)
    private String inKindPaymentDescription;

    /**
     * Fábrica inicial. Si se crea y no se reclaman, queda inicializada en false (renuncia tacita).
     */
    public static EconomicCompensation createEmpty() {
        return EconomicCompensation.builder()
                .id(UUID.randomUUID())
                .appliesEconomicCompensation(false)
                .build();
    }

    // ============================================
    // LÓGICA DE NEGOCIO (Guía para el Bot y Alertas)
    // ============================================

    /**
     * Si aplican compensación, pero el bot "olvidó" recolectar el relato justificativo.
     * Un acuerdo de compensación sin la justificación del desequilibrio puede ser rechazado.
     */
    public boolean isMissingJustificationWarning() {
        return appliesEconomicCompensation && 
               (imbalanceJustification == null || imbalanceJustification.trim().isEmpty());
    }

    /**
     * Si no aplican compensación económica, indica que el bot (o el redactor del PDF) 
     * debe insertar la cláusula estándar de renuncia cruzada a reclamarla a futuro.
     */
    public boolean requiresExpressWaiver() {
        return !appliesEconomicCompensation;
    }
}
