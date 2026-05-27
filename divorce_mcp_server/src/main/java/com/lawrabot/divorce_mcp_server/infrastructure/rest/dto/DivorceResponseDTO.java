package com.lawrabot.divorce_mcp_server.infrastructure.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la visualización de expedientes de divorcio en el Dashboard.
 * Sigue exactamente la estructura esperada por el UI de LawraBot.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DivorceResponseDTO {
    private String id;
    private String status;
    private PetitionerDTO petitioner;
    private LastConjugalResidenceDTO lastConjugalResidence;
    private String divorceType;
    private String marriageDate;
    private String deFactoSeparationDate;
    
    // Datos del acta de matrimonio
    private String marriageCertificateNumber;
    private String marriageRegistryBook;
    private String marriageRegistryPage;
    private String marriageRegistryOffice;
    private String marriagePlace;
    private String marriageCertificateId;
    private String marriageCertificateIssuanceDate;

    private RespondentDTO respondent;
    private Object regulatoryAgreement;
    private java.util.List<ChildDTO> children;
    private SocioEconomicProfileDTO socioEconomicProfile;
    private String rawAgreementText;
    private String createdAt;
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PetitionerDTO {
        private FullNameDTO fullName;
        private String dni;
        private String cuil;
        private String phoneNumber;
        private String nationality;
        private String email;
        private String profession;
        private String birthDate;
        private AddressDTO address;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FullNameDTO {
        private String fullName;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LastConjugalResidenceDTO {
        private String locality;
        private String street;
        private String number;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RespondentDTO {
        private FullNameDTO fullName;
        private String dni;
        private String cuil;
        private String phoneNumber;
        private String nationality;
        private String email;
        private String profession;
        private String birthDate;
        private AddressDTO residentialAddress;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AddressDTO {
        private String street;
        private String number;
        private String locality;
        private String floorAppartment;
        private String neighborhood;
        private String province;
        private String zipCode;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChildDTO {
        private String name;
        private String dni;
        private String birthDate;
        private Integer age;
        private Boolean hasDisability;
        private Boolean isStudent;
        private String birthCertificateId;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SocioEconomicProfileDTO {
        private Double avgMonthlyIncome;
        private String housingType;
        private String occupation;
        private String blsgScrapingResult;
        private String blsgObservations;
        
        // Fase 1: Scraping Externo (Nuevos)
        private String scrapingFullName;
        private String scrapingDni;
        private String scrapingCuil;
        private String scrapingBirthDate;
        private String scrapingProvince;
        private String scrapingSex;
        private String scrapingJustification;
        private String certificatePath;

        // Fase 2: Recolección Activa (Nuevos)
        private Integer vehiclesRegistered;
        private Boolean hasFormalEmployment;

        // Fase 3: Evaluación Defensoría (Nuevos)
        private Boolean blsgApprovedByDefensoria;
    }
}
