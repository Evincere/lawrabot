package com.lawrabot.divorce_mcp_server.infrastructure.persistence.mapper;

import com.lawrabot.divorce_mcp_server.domain.model.Citizen;
import com.lawrabot.divorce_mcp_server.domain.valueobject.AddressVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.CuilVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.FullNameVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.PhoneNumberVO;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.CitizenJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CitizenMapper {

    public Citizen toDomain(CitizenJpaEntity entity) {
        if (entity == null) return null;

        var cuilStr = entity.getCuil();
        var nameStr = entity.getFullName();
        var phoneStr = entity.getPhoneNumber();
        var addressStr = entity.getAddress();

        return Citizen.builder()
                .id(entity.getId())
                .dni(entity.getDni())
                .cuil(cuilStr != null ? new CuilVO(cuilStr) : null)
                .fullName(nameStr != null ? FullNameVO.fromFullString(nameStr) : null)
                .phoneNumber(phoneStr != null ? PhoneNumberVO.of(phoneStr) : null)
                .email(entity.getEmail())
                .nationality(entity.getNationality())
                .occupation(entity.getOccupation())
                .address(addressStr != null ? AddressVO.builder().street(addressStr).build() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public CitizenJpaEntity toEntity(Citizen domain) {
        if (domain == null) return null;

        var fullName = domain.getFullName();
        var cuil = domain.getCuil();
        var phone = domain.getPhoneNumber();
        var address = domain.getAddress();

        return CitizenJpaEntity.builder()
                .id(domain.getId())
                .dni(domain.getDni())
                .cuil(cuil != null ? cuil.getValue() : null)
                .fullName(fullName != null ? fullName.getFullName() : null)
                .phoneNumber(phone != null ? phone.getValue() : null)
                .email(domain.getEmail())
                .nationality(domain.getNationality())
                .occupation(domain.getOccupation())
                .address(address != null ? address.toLegalString() : null)
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
