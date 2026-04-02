package com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity;

import com.lawrabot.divorce_mcp_server.domain.enums.CaseRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Entidad que vincula a un Ciudadano con un Expediente específico bajo un Rol determinado.
 * Permite que un ciudadano participe en múltiples trámites (Divorcio, Sucesión, etc.)
 */
@Entity
@Table(name = "case_participants", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"citizen_id", "expediente_id", "role"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaseParticipantJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private CitizenJpaEntity citizen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediente_id", nullable = false)
    private ExpedienteJpaEntity expediente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CaseRole role;

    // Metadata adicional (exclusiva de esta participación)
    @Column(name = "intervention_summary")
    private String interventionSummary;
}
