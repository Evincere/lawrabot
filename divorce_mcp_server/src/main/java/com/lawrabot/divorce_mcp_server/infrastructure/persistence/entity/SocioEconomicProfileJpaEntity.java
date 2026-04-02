package com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity;

import com.lawrabot.divorce_mcp_server.domain.enums.BlsgScrapingResultEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.HousingSituationEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidad JPA para la tabla 'socioeconomic_profiles'.
 * Guarda tanto el resultado del scraping como la evaluación de la Defensoría.
 */
@Entity
@Table(name = "socioeconomic_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SocioEconomicProfileJpaEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    // --- FASE 1: SCRAPING ---
    @Enumerated(EnumType.STRING)
    @Column(name = "scraping_result", length = 30)
    private BlsgScrapingResultEnum scrapingResult;

    @Column(name = "scraping_justification", columnDefinition = "TEXT")
    private String scrapingJustification;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "dni", length = 255)
    private String dni;

    @Column(name = "cuil", length = 255)
    private String cuil;

    @Column(name = "birth_date", length = 255)
    private String birthDate;

    @Column(name = "province", length = 255)
    private String province;

    @Column(name = "sex", length = 255)
    private String sex;

    @Column(name = "certificate_path", length = 1000)
    private String certificatePath;

    // --- FASE 2: DEFENSORÍA ---
    @Column(name = "monthly_income_ars", precision = 15, scale = 2)
    private BigDecimal monthlyIncomeArs;

    @Enumerated(EnumType.STRING)
    @Column(name = "housing_situation", length = 30)
    private HousingSituationEnum housingSituation;

    @Column(name = "vehicles_registered")
    private Integer vehiclesRegistered;

    @Column(name = "has_formal_employment")
    private Boolean hasFormalEmployment;

    @Column(name = "defensoria_observations", columnDefinition = "TEXT")
    private String defensoriaObservations;

    @Column(name = "blsg_approved_by_defensoria")
    private Boolean blsgApprovedByDefensoria;
}
