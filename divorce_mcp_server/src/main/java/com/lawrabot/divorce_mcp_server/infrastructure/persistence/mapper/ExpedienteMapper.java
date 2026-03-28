package com.lawrabot.divorce_mcp_server.infrastructure.persistence.mapper;

import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.domain.valueobject.AddressVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.PhoneNumberVO;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.embeddable.AddressEmbeddable;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.embeddable.PhoneNumberEmbeddable;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.ChildJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.ExpedienteJpaEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper raíz: convierte Expediente (Aggregate Root de Dominio) ↔ ExpedienteJpaEntity.
 */
@Component
public class ExpedienteMapper {

    private final SpouseMapper spouseMapper;
    private final ChildMapper childMapper;
    private final RegulatoryAgreementMapper agreementMapper;
    private final SocioEconomicProfileMapper profileMapper;

    public ExpedienteMapper(SpouseMapper spouseMapper,
                            ChildMapper childMapper,
                            RegulatoryAgreementMapper agreementMapper,
                            SocioEconomicProfileMapper profileMapper) {
        this.spouseMapper = spouseMapper;
        this.childMapper = childMapper;
        this.agreementMapper = agreementMapper;
        this.profileMapper = profileMapper;
    }

    public ExpedienteJpaEntity toEntity(Expediente domain) {
        if (domain == null) return null;

        List<ChildJpaEntity> childEntities = (domain.getChildren() != null)
                ? domain.getChildren().stream()
                        .map(c -> childMapper.toEntity(c, domain.getId()))
                        .collect(Collectors.toList())
                : List.of();

        return ExpedienteJpaEntity.builder()
                .id(domain.getId())
                .contactPhoneNumber(toPhoneEmbeddable(domain.getContactPhoneNumber()))
                .status(domain.getStatus())
                .collectionStage(domain.getCollectionStage())
                .divorceType(domain.getDivorceType())
                .lastConjugalResidence(toAddressEmbeddable(domain.getLastConjugalResidence()))
                .marriageDate(domain.getMarriageDate())
                .deFactoSeparationDate(domain.getDeFactoSeparationDate())
                .petitioner(spouseMapper.toEntity(domain.getPetitioner()))
                .respondent(spouseMapper.toEntity(domain.getRespondent()))
                .children(childEntities)
                .socioEconomicProfile(profileMapper.toEntity(domain.getSocioEconomicProfile()))
                .regulatoryAgreement(agreementMapper.toEntity(domain.getRegulatoryAgreement()))
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public Expediente toDomain(ExpedienteJpaEntity entity) {
        if (entity == null) return null;

        List<com.lawrabot.divorce_mcp_server.domain.model.Child> domainChildren = (entity.getChildren() != null)
                ? entity.getChildren().stream()
                        .map(childMapper::toDomain)
                        .collect(Collectors.toList())
                : List.of();

        return Expediente.builder()
                .id(entity.getId())
                .contactPhoneNumber(toPhoneVO(entity.getContactPhoneNumber()))
                .status(entity.getStatus())
                .collectionStage(entity.getCollectionStage())
                .divorceType(entity.getDivorceType())
                .lastConjugalResidence(toAddressVO(entity.getLastConjugalResidence()))
                .marriageDate(entity.getMarriageDate())
                .deFactoSeparationDate(entity.getDeFactoSeparationDate())
                .petitioner(spouseMapper.toDomain(entity.getPetitioner()))
                .respondent(spouseMapper.toDomain(entity.getRespondent()))
                .children(domainChildren)
                .socioEconomicProfile(profileMapper.toDomain(entity.getSocioEconomicProfile()))
                .regulatoryAgreement(agreementMapper.toDomain(entity.getRegulatoryAgreement()))
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt() : LocalDateTime.now())
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt() : LocalDateTime.now())
                .build();
    }

    private PhoneNumberEmbeddable toPhoneEmbeddable(@Nullable PhoneNumberVO vo) {
        return (vo != null) ? new PhoneNumberEmbeddable(vo.getValue()) : null;
    }

    private PhoneNumberVO toPhoneVO(@Nullable PhoneNumberEmbeddable emb) {
        return (emb != null && emb.getPhoneNumber() != null)
               ? PhoneNumberVO.of(emb.getPhoneNumber()) : null;
    }

    private AddressEmbeddable toAddressEmbeddable(@Nullable AddressVO vo) {
        if (vo == null) return null;
        return new AddressEmbeddable(vo.getStreet(), vo.getNumber(), vo.getFloorAppartment(),
                                     vo.getNeighborhood(), vo.getLocality(), vo.getProvince(), vo.getZipCode());
    }

    private AddressVO toAddressVO(@Nullable AddressEmbeddable emb) {
        if (emb == null) return null;
        return AddressVO.builder()
                .street(emb.getStreet())
                .number(emb.getNumber())
                .floorAppartment(emb.getFloorAppartment())
                .neighborhood(emb.getNeighborhood())
                .locality(emb.getLocality())
                .province(emb.getProvince())
                .zipCode(emb.getZipCode())
                .build();
    }
}
