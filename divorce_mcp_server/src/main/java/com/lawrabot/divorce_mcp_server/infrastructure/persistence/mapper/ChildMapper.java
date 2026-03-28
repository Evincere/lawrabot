package com.lawrabot.divorce_mcp_server.infrastructure.persistence.mapper;

import com.lawrabot.divorce_mcp_server.domain.model.Child;
import com.lawrabot.divorce_mcp_server.domain.valueobject.DNIVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.FullNameVO;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.ChildJpaEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper entre Child (dominio) y ChildJpaEntity (infraestructura).
 */
@Component
public class ChildMapper {

    public ChildJpaEntity toEntity(Child domain, @Nullable UUID dispositivoId) {
        if (domain == null) return null;
        
        DNIVO dniVO = domain.getDni();
        String dniValue = (dniVO != null) ? dniVO.getValue() : null;
        String firstName = (domain.getName() != null) ? domain.getName().getFirstName() : null;
        String lastName = (domain.getName() != null) ? domain.getName().getLastName() : null;

        return ChildJpaEntity.builder()
                .id(domain.getId())
                .expedienteId(dispositivoId)
                .firstName(firstName)
                .lastName(lastName)
                .birthDate(domain.getBirthDate())
                .dni(dniValue)
                .disabled(domain.isDisabled())
                .build();
    }

    public Child toDomain(ChildJpaEntity entity) {
        if (entity == null) return null;
        
        FullNameVO name = (entity.getFirstName() != null)
                ? new FullNameVO(entity.getFirstName(), entity.getLastName())
                : null;
        
        DNIVO dni = (entity.getDni() != null) ? DNIVO.of(entity.getDni()) : null;

        return Child.builder()
                .id(entity.getId())
                .name(name)
                .birthDate(entity.getBirthDate())
                .dni(dni)
                .disabled(entity.isDisabled())
                .build();
    }
}
