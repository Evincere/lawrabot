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
import java.util.ArrayList;
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
    private final CaseParticipantMapper participantMapper;

    public ExpedienteMapper(SpouseMapper spouseMapper,
                            ChildMapper childMapper,
                            RegulatoryAgreementMapper agreementMapper,
                            SocioEconomicProfileMapper profileMapper,
                            CaseParticipantMapper participantMapper) {
        this.spouseMapper = spouseMapper;
        this.childMapper = childMapper;
        this.agreementMapper = agreementMapper;
        this.profileMapper = profileMapper;
        this.participantMapper = participantMapper;
    }

    public ExpedienteJpaEntity toEntity(Expediente domain) {
        if (domain == null) return null;

        List<ChildJpaEntity> childEntities = (domain.getChildren() != null)
                ? domain.getChildren().stream()
                        .map(c -> childMapper.toEntity(c, domain.getId()))
                        .collect(Collectors.toList())
                : List.of();

        ExpedienteJpaEntity entity = ExpedienteJpaEntity.builder()
                .id(domain.getId())
                .contactPhoneNumber(toPhoneEmbeddable(domain.getContactPhoneNumber()))
                .status(domain.getStatus())
                .collectionStage(domain.getCollectionStage())
                .divorceType(domain.getDivorceType())
                .lastConjugalResidence(toAddressEmbeddable(domain.getLastConjugalResidence()))
                .marriageDate(domain.getMarriageDate())
                .deFactoSeparationDate(domain.getDeFactoSeparationDate())
                .marriageCertificateNumber(domain.getMarriageCertificateNumber())
                .marriageRegistryBook(domain.getMarriageRegistryBook())
                .marriageRegistryPage(domain.getMarriageRegistryPage())
                .marriageRegistryOffice(domain.getMarriageRegistryOffice())
                .marriagePlace(domain.getMarriagePlace())
                .petitioner(spouseMapper.toEntity(domain.getPetitioner()))
                .respondent(spouseMapper.toEntity(domain.getRespondent()))
                .children(childEntities)
                .socioEconomicProfile(profileMapper.toEntity(domain.getSocioEconomicProfile()))
                .regulatoryAgreement(agreementMapper.toEntity(domain.getRegulatoryAgreement()))
                .rawAgreementText(domain.getRawAgreementText())
                .participants(domain.getParticipants() != null ? 
                    domain.getParticipants().stream().map(participantMapper::toEntity).collect(Collectors.toList()) : 
                    new ArrayList<>())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
                
        // Set back-references to satisfy non-null constraints
        if (entity.getParticipants() != null) {
            entity.getParticipants().forEach(p -> p.setExpediente(entity));
        }
        
        return entity;
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
                .marriageCertificateNumber(entity.getMarriageCertificateNumber())
                .marriageRegistryBook(entity.getMarriageRegistryBook())
                .marriageRegistryPage(entity.getMarriageRegistryPage())
                .marriageRegistryOffice(entity.getMarriageRegistryOffice())
                .marriagePlace(entity.getMarriagePlace())
                .petitioner(spouseMapper.toDomain(entity.getPetitioner()))
                .respondent(spouseMapper.toDomain(entity.getRespondent()))
                .children(domainChildren)
                .socioEconomicProfile(profileMapper.toDomain(entity.getSocioEconomicProfile()))
                .regulatoryAgreement(agreementMapper.toDomain(entity.getRegulatoryAgreement()))
                .rawAgreementText(entity.getRawAgreementText())
                .participants(entity.getParticipants() != null ? 
                    entity.getParticipants().stream().map(participantMapper::toDomain).collect(Collectors.toList()) : 
                    new ArrayList<>())
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
