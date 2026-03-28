package com.lawrabot.divorce_mcp_server.domain.model;

import com.lawrabot.divorce_mcp_server.domain.enums.AgreementStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.model.agreement.AlimonyProvision;
import com.lawrabot.divorce_mcp_server.domain.model.agreement.AssetDistribution;
import com.lawrabot.divorce_mcp_server.domain.model.agreement.CommunicationRegime;
import com.lawrabot.divorce_mcp_server.domain.model.agreement.EconomicCompensation;
import com.lawrabot.divorce_mcp_server.domain.model.agreement.PersonalCare;
import lombok.Builder;
import lombok.Getter;
import org.springframework.lang.Nullable;

import java.util.UUID;

/**
 * Convenio Regulatorio (Art. 439 CCCN).
 * Engloba todas las disposiciones sobre hijos, bienes y prestaciones.
 */
@Getter
@Builder
public class RegulatoryAgreement {

    private final UUID id;

    private AgreementStatusEnum status;

    private boolean includesChildrenProvisions;

    // HIJOS Y CUIDADO
    // ============================================

    @Nullable
    private PersonalCare personalCare;
    @Nullable
    private CommunicationRegime communicationRegime;

    // ALIMENTOS (Art. 439 inc. a)
    @Nullable
    private AlimonyProvision alimonyProvision;

    // DISPOSICIONES PATRIMONIALES Y PERSONALES
    // ============================================

    @Nullable
    private AssetDistribution assetDistribution;
    
    // Art. 441 - Compensación Económica
    @Nullable
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
                .build();
    }

}
