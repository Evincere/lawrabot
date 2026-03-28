package com.lawrabot.divorce_mcp_server.infrastructure.persistence.mapper;

import com.lawrabot.divorce_mcp_server.domain.model.Spouse;
import com.lawrabot.divorce_mcp_server.domain.valueobject.AddressVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.CuilVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.FullNameVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.PhoneNumberVO;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.embeddable.AddressEmbeddable;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.embeddable.FullNameEmbeddable;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.SpouseJpaEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

/**
 * Mapper para Spouse ↔ SpouseJpaEntity.
 */
@Component
public class SpouseMapper {

    public SpouseJpaEntity toEntity(@Nullable Spouse domain) {
        if (domain == null) return null;

        String firstName = (domain.getName() != null) ? domain.getName().getFirstName() : null;
        String lastName = (domain.getName() != null) ? domain.getName().getLastName() : null;
        PhoneNumberVO phoneVO = domain.getPhoneNumber();
        String phone = (phoneVO != null) ? phoneVO.getValue() : null;
        CuilVO cuilVO = domain.getCuil();
        String cuil = (cuilVO != null) ? cuilVO.getValue() : null;

        return SpouseJpaEntity.builder()
                .id(domain.getId())
                .name(new FullNameEmbeddable(firstName, lastName))
                .phoneNumber(phone != null ? new com.lawrabot.divorce_mcp_server.infrastructure.persistence.embeddable.PhoneNumberEmbeddable(phone) : null)
                .cuil(cuil)
                .residentialAddress(toAddressEmbeddable(domain.getAddress()))
                .profession(domain.getProfession())
                .build();
    }

    public Spouse toDomain(@Nullable SpouseJpaEntity entity) {
        if (entity == null) return null;

        FullNameVO name = (entity.getName() != null)
                ? new FullNameVO(entity.getName().getFirstName(), entity.getName().getLastName())
                : null;

        String phone = (entity.getPhoneNumber() != null) ? entity.getPhoneNumber().getPhoneNumber() : null;

        return Spouse.builder()
                .id(entity.getId())
                .name(name)
                .phoneNumber(phone != null ? PhoneNumberVO.of(phone) : null)
                .cuil(entity.getCuil() != null ? new CuilVO(entity.getCuil()) : null)
                .address(toAddressVO(entity.getResidentialAddress()))
                .profession(entity.getProfession())
                .build();
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
