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

        return Citizen.builder()
                .id(entity.getId())
                .dni(entity.getDni())
                .cuil(entity.getCuil() != null ? new CuilVO(entity.getCuil()) : null)
                .fullName(entity.getFullName() != null ? FullNameVO.fromFullString(entity.getFullName()) : null)
                .phoneNumber(entity.getPhoneNumber() != null ? PhoneNumberVO.of(entity.getPhoneNumber()) : null)
                .email(entity.getEmail())
                .address(entity.getAddress())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public CitizenJpaEntity toEntity(Citizen domain) {
        if (domain == null) return null;

        return CitizenJpaEntity.builder()
                .id(domain.getId())
                .dni(domain.getDni())
                .cuil(domain.getCuil() != null ? domain.getCuil().getValue() : null)
                .fullName(domain.getFullName() != null ? domain.getFullName().getFullName() : null)
                .phoneNumber(domain.getPhoneNumber() != null ? domain.getPhoneNumber().getValue() : null)
                .email(domain.getEmail())
                .address(domain.getAddress())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
