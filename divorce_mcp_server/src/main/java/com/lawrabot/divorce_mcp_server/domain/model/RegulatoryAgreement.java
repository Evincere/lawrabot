package com.lawrabot.divorce_mcp_server.domain.model;

import com.lawrabot.divorce_mcp_server.domain.enums.AgreementStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.model.agreement.AlimonyProvision;
import com.lawrabot.divorce_mcp_server.domain.model.agreement.AssetDistribution;
import com.lawrabot.divorce_mcp_server.domain.model.agreement.CommunicationRegime;
import com.lawrabot.divorce_mcp_server.domain.model.agreement.PersonalCare;
import com.lawrabot.divorce_mcp_server.domain.model.agreement.EconomicCompensation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Entidad que representa la Propuesta Reguladora o el Convenio Regulador
 * exigido por el Art. 438 y 439 del Código Civil y Comercial argentino.
 * 
 * Si el divorcio es UNILATERAL, se trata de una Propuesta.
 * Si el divorcio es JOINT (Conjunto), se trata de un Convenio Acordado.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RegulatoryAgreement {

    private UUID id;

    // Estado Procesal del Convenio / Propuesta (Dinamico post-presentación en Juzgado)
    private AgreementStatusEnum status;

    // ============================================
    // DISPOSICIONES SOBRE LOS HIJOS (Responsabilidad Parental)
    // ============================================
    
    // Indica si el convenio contempla cláusulas para hijos
    // (Útil para evitar campos nulos o para mostrar validaciones en el frontend)
    private boolean includesChildrenProvisions;

    private PersonalCare personalCare;
    private CommunicationRegime communicationRegime;
    private AlimonyProvision alimonyProvision;

    // ============================================
    // DISPOSICIONES PATRIMONIALES Y PERSONALES
    // ============================================

    private AssetDistribution assetDistribution;
    
    // Art. 441 - Compensación Económica
    private EconomicCompensation economicCompensation;

    /**
     * Fábrica para crear un convenio inicial vacío, listo para ser
     * rellenado por el Bot de WhatsApp mediante preguntas.
     */
    public static RegulatoryAgreement createEmpty() {
        return RegulatoryAgreement.builder()
                .id(UUID.randomUUID())
                .status(AgreementStatusEnum.PROPOSED)
                .includesChildrenProvisions(false)
                // Se instanciarán o asignarán a medida que avance el trámite
                .personalCare(null)
                .communicationRegime(null)
                .alimonyProvision(null)
                .assetDistribution(null)
                .economicCompensation(null)
                .build();
    }

}
