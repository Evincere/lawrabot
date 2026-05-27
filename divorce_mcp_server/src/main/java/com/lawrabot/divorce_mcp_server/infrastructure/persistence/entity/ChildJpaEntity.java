package com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidad JPA para la tabla 'children'.
 * FK al expediente padre via expediente_id.
 */
@Entity
@Table(name = "children")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    private UUID id;

    // Referencia al expediente padre (no un @ManyToOne para evitar acoplamiento circular en el mapper)
    @Column(name = "expediente_id", nullable = false, columnDefinition = "uuid")
    private UUID expedienteId;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "dni", length = 15)
    private String dni;

    @Column(name = "disabled", nullable = false)
    private boolean disabled;

    @Column(name = "is_student", nullable = false)
    private boolean student;
}
