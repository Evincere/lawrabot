package com.lawrabot.divorce_mcp_server.infrastructure.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO para actualizar datos de un expediente desde el Dashboard.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateCaseDataRequest {
    private SpouseUpdateDTO petitioner;
    private SpouseUpdateDTO respondent;
    private String marriageDate;
    private String deFactoSeparationDate;
    private LastConjugalResidenceUpdateDTO lastConjugalResidence;
    private List<ChildUpdateDTO> children;
    private String divorceType;
    
    // Datos del acta (opcional en el request)
    private String marriageCertificateId;
    private String marriageCertificateIssuanceDate;
    private String marriageCertificateNumber;
    private String marriageRegistryBook;
    private String marriageRegistryPage;
    private String marriageRegistryOffice;
    private String marriagePlace;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SpouseUpdateDTO {
        private String fullName;
        private String dni;
        private String cuil;
        private String phoneNumber;
        private String nationality;
        private String email;
        private String profession;
        private String birthDate;
        private AddressUpdateDTO address;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AddressUpdateDTO {
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
    public static class ChildUpdateDTO {
        private String name;
        private String dni;
        private String birthDate;
        private Boolean hasDisability;
        private String birthCertificateId;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LastConjugalResidenceUpdateDTO {
        private String locality;
        private String street;
        private String number;
    }
}
