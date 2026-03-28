package com.lawrabot.divorce_mcp_server.domain.model.agreement;

import com.lawrabot.divorce_mcp_server.domain.enums.MainResidenceEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.PersonalCareTypeEnum;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Entidad que representa el Cuidado Personal (antigua "Tenencia") (Art. 648 CCyC).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PersonalCare {

    private UUID id;

    // Regla General: Compartido Indistinto
    private PersonalCareTypeEnum careType;
    private String customCareType; // Solo si == OTHER

    // El Centro de vida (Art. 651 CCyC)
    private MainResidenceEnum mainResidence;
    private String customMainResidence; // Solo si == OTHER

    /**
     * Fábrica para inicializar el cuidado personal vacío
     */
    public static PersonalCare createEmpty() {
        return PersonalCare.builder()
                .id(UUID.randomUUID())
                .build();
    }

    // ============================================
    // LÓGICA DE NEGOCIO (Guía para el Bot y Alertas)
    // ============================================

    /**
     * Alerta al usuario (abogado o abogado defensor) que la Ley prefiere y presume el 
     * Cuidado Compartido. Si el cliente de WhatsApp selecciona UNILATERAL, hay que pedirle un "motivo grave".
     */
    public boolean isUnilateralWarning() {
        return careType == PersonalCareTypeEnum.UNILATERAL_PETITIONER 
            || careType == PersonalCareTypeEnum.UNILATERAL_RESPONDENT;
    }

    /**
     * Advierte al bot que, según la configuración de residencia, debe existir 
     * SÍ O SÍ una Cuota Alimentaria (AlimonyProvision).
     * Si el niño vive principalmente con uno, el otro debe pasar alimentos (Art. 658 CCyC).
     */
    public boolean requiresAlimonyWarning() {
        if (careType == PersonalCareTypeEnum.SHARED_INDISTINCT && mainResidence != MainResidenceEnum.BOTH_EQUITABLE) {
            return true; // Compartido pero vive con uno.
        }
        if (isUnilateralWarning()) {
            return true; // Unilateral explícito.
        }
        return false;
    }
}
