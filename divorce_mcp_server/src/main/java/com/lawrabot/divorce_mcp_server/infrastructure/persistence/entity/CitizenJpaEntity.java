package com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad JPA que representa a un Ciudadano en el Master Client Index (MCI).
 */
@Entity
@Table(name = "citizens", indexes = {
    @Index(name = "idx_citizen_dni", columnList = "dni", unique = true),
    @Index(name = "idx_citizen_cuil", columnList = "cuil")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitizenJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(unique = true, nullable = false)
    private String dni;

    private String cuil;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_number")
    private String phoneNumber;

    private String email;

    private String nationality;

    private String occupation;

    private String address;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
}
