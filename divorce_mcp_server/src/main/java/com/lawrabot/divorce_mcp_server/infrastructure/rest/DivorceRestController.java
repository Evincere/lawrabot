package com.lawrabot.divorce_mcp_server.infrastructure.rest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.enums.DivorceTypeEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Child;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.domain.model.Spouse;
import com.lawrabot.divorce_mcp_server.domain.valueobject.AddressVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.CuilVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.DNIVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.FullNameVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.PhoneNumberVO;
import com.lawrabot.divorce_mcp_server.domain.enums.ExpedienteStatusEnum;
import com.lawrabot.divorce_mcp_server.infrastructure.rest.dto.DivorceResponseDTO;
import com.lawrabot.divorce_mcp_server.infrastructure.rest.dto.DivorceResponseDTO.AddressDTO;
import com.lawrabot.divorce_mcp_server.infrastructure.rest.dto.DivorceResponseDTO.ChildDTO;
import com.lawrabot.divorce_mcp_server.infrastructure.rest.dto.DivorceResponseDTO.FullNameDTO;
import com.lawrabot.divorce_mcp_server.infrastructure.rest.dto.DivorceResponseDTO.LastConjugalResidenceDTO;
import com.lawrabot.divorce_mcp_server.infrastructure.rest.dto.DivorceResponseDTO.PetitionerDTO;
import com.lawrabot.divorce_mcp_server.infrastructure.rest.dto.DivorceResponseDTO.RespondentDTO;
import com.lawrabot.divorce_mcp_server.infrastructure.rest.dto.DivorceResponseDTO.SocioEconomicProfileDTO;
import com.lawrabot.divorce_mcp_server.infrastructure.rest.dto.UpdateCaseDataRequest;

@RestController
@RequestMapping("/api/divorce")
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class DivorceRestController {

    private static final Logger log = LoggerFactory.getLogger(DivorceRestController.class);

    private final IExpedienteRepository expedienteRepository;

    public DivorceRestController(IExpedienteRepository expedienteRepository) {
        this.expedienteRepository = expedienteRepository;
    }

    @GetMapping("/cases")
    public ResponseEntity<List<DivorceResponseDTO>> getAllCases() {
        log.info("REST: Listando todos los expedientes de divorcio");
        List<Expediente> cases = expedienteRepository.findAll();
        List<DivorceResponseDTO> dtos = cases.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/cases/{id}")
    public ResponseEntity<DivorceResponseDTO> getCaseById(@PathVariable UUID id) {
        log.info("REST: Consultando detalle del expediente: {}", id);
        return expedienteRepository.findById(id)
                .map(this::mapToDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/cases/{id}")
    public ResponseEntity<Void> archiveCase(@PathVariable UUID id) {
        log.info("REST: Archivado expediente: {}", id);
        expedienteRepository.findById(id).ifPresent(exp -> {
            exp.archive();
            expedienteRepository.save(exp);
        });
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/cases/{id}")
    public ResponseEntity<DivorceResponseDTO> updateCaseData(@PathVariable UUID id, @RequestBody UpdateCaseDataRequest request) {
        log.info("REST: Actualizando datos para el expediente: {}", id);
        return expedienteRepository.findById(id).map(exp -> {
            // Update Petitioner
            if (request.getPetitioner() != null) {
                exp.setPetitioner(mapToSpouse(request.getPetitioner()));
            }

            // Update Respondent
            if (request.getRespondent() != null) {
                exp.setRespondent(mapToSpouse(request.getRespondent()));
            }

            // Update Marriage Details
            LocalDate mDate = parseDate(request.getMarriageDate());
            LocalDate sDate = parseDate(request.getDeFactoSeparationDate());
            exp.provideMarriageDetails(mDate, sDate, mapToAddressVO(request.getLastConjugalResidence()));

            // Update Divorce Type
            if (request.getDivorceType() != null) {
                exp.updateDivorceType(DivorceTypeEnum.valueOf(request.getDivorceType()));
            }

            // Update Children
            if (request.getChildren() != null) {
                List<Child> childList = request.getChildren().stream()
                        .map(this::mapToChild)
                        .collect(Collectors.toList());
                exp.setChildren(childList);
            }

            // Update Marriage Certificate Details
            exp.provideMarriageCertificateDetails(
                request.getMarriageCertificateNumber(),
                request.getMarriageRegistryBook(),
                request.getMarriageRegistryPage(),
                request.getMarriageRegistryOffice(),
                request.getMarriagePlace(),
                request.getMarriageCertificateId() != null ? UUID.fromString(request.getMarriageCertificateId()) : null,
                parseDate(request.getMarriageCertificateIssuanceDate())
            );

            expedienteRepository.save(exp);
            return ResponseEntity.ok(mapToDTO(exp));
        }).orElse(ResponseEntity.notFound().build());
    }

    private Spouse mapToSpouse(UpdateCaseDataRequest.SpouseUpdateDTO dto) {
        if (dto == null) return null;
        
        String dni = dto.getDni();
        String cuil = dto.getCuil();
        String phone = dto.getPhoneNumber();
        
        return Spouse.builder()
                .id(UUID.randomUUID())
                .name(dto.getFullName() != null && !dto.getFullName().isBlank() && !"S/D".equals(dto.getFullName()) ? FullNameVO.fromFullString(dto.getFullName()) : null)
                .dni(dni != null && !dni.isBlank() && !"S/D".equals(dni) ? DNIVO.of(dni) : null)
                .cuil(cuil != null && !cuil.isBlank() && !"S/D".equals(cuil) ? new CuilVO(cuil) : null)
                .phoneNumber(phone != null && !phone.isBlank() && !"S/D".equals(phone) ? PhoneNumberVO.of(phone) : null)
                .email(dto.getEmail() != null && !"S/D".equals(dto.getEmail()) ? dto.getEmail() : null)
                .nationality(dto.getNationality() != null && !"S/D".equals(dto.getNationality()) ? dto.getNationality() : null)
                .profession(dto.getProfession() != null && !"S/D".equals(dto.getProfession()) ? dto.getProfession() : null)
                .birthDate(parseDate(dto.getBirthDate()))
                .address(mapToAddressVO(dto.getAddress()))
                .build();
    }

    private Child mapToChild(UpdateCaseDataRequest.ChildUpdateDTO dto) {
        if (dto == null) return null;
        
        String dni = dto.getDni();
        
        return Child.builder()
                .id(UUID.randomUUID())
                .name(dto.getName() != null && !dto.getName().isBlank() && !"S/D".equals(dto.getName()) ? FullNameVO.fromFullString(dto.getName()) : null)
                .dni(dni != null && !dni.isBlank() && !"S/D".equals(dni) ? DNIVO.of(dni) : null)
                .birthDate(parseDate(dto.getBirthDate()))
                .disabled(dto.getHasDisability() != null && dto.getHasDisability())
                .student(dto.getIsStudent() != null && dto.getIsStudent())
                .birthCertificateId(dto.getBirthCertificateId())
                .build();
    }

    private AddressVO mapToAddressVO(UpdateCaseDataRequest.AddressUpdateDTO dto) {
        if (dto == null) return null;
        return AddressVO.builder()
                .street(dto.getStreet())
                .number(dto.getNumber())
                .locality(dto.getLocality())
                .floorAppartment(dto.getFloorAppartment())
                .neighborhood(dto.getNeighborhood())
                .province(dto.getProvince())
                .zipCode(dto.getZipCode())
                .build();
    }

    private AddressVO mapToAddressVO(UpdateCaseDataRequest.LastConjugalResidenceUpdateDTO dto) {
        if (dto == null) return null;
        return AddressVO.builder()
                .locality(dto.getLocality())
                .street(dto.getStreet())
                .number(dto.getNumber())
                .build();
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || "S/D".equals(dateStr)) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (DateTimeParseException e) {
            log.warn("Formato de fecha inválido: {}", dateStr);
            return null;
        }
    }

    /**
     * Intenta parsear un String como fecha ISO (YYYY-MM-DD) y retorna su representación.
     * Si el valor no es parseable (ej: datos raw del scraping como CUIL o formatos no ISO),
     * retorna "S/D" en lugar de exponer datos incorrectos.
     */
    private String parseDateToString(String dateStr) {
        if (dateStr == null || dateStr.isEmpty() || "S/D".equals(dateStr)) return "S/D";
        try {
            return LocalDate.parse(dateStr).toString();
        } catch (DateTimeParseException e) {
            log.warn("Fecha del profile no es ISO parseable, descartando como birthDate: {}", dateStr);
            return "S/D";
        }
    }

    @PostMapping("/cases/{id}/raw-agreement")
    public ResponseEntity<Void> updateRawAgreementText(@PathVariable UUID id, @RequestBody java.util.Map<String, String> payload) {
        String text = payload.get("rawAgreementText");
        log.info("REST: Actualizando rawAgreementText para expediente: {}", id);
        return expedienteRepository.findById(id).map(exp -> {
            exp.setRawAgreementText(text);
            expedienteRepository.save(exp);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/cases/{id}/blsg-decision")
    public ResponseEntity<DivorceResponseDTO> updateBlsgDecision(
            @PathVariable UUID id,
            @RequestParam boolean approved,
            @RequestParam(required = false) String observations) {
        log.info("REST: Actualizando decisión BLSG para expediente: {} -> {}", id, approved);
        return expedienteRepository.findById(id).map(exp -> {
            var profile = exp.getSocioEconomicProfile();
            if (profile != null) {
                profile.setBlsgApprovedByDefensoria(approved);
                profile.setDefensoriaObservations(observations);
                expedienteRepository.save(exp);
            }
            return ResponseEntity.ok(mapToDTO(exp));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/cases/{id}/approve")
    public ResponseEntity<DivorceResponseDTO> approveCaseAndProvideMarriageDetails(
            @PathVariable UUID id,
            @RequestBody MarriageCertificateRequest request) {
        log.info("REST: Aprobando expediente y registrando datos del acta para: {}", id);
        return expedienteRepository.findById(id).map(exp -> {
            exp.provideMarriageCertificateDetails(
                request.certificateNumber(),
                request.registryBook(),
                request.registryPage(),
                request.registryOffice(),
                request.place(),
                null, // certificateId no disponible en este request simplificado
                null  // issuanceDate no disponible en este request simplificado
            );
            
            // Si el estado es anterior a READY_FOR_PORTAL o DOCUMENTS_GENERATED, avanzarlo
            if (exp.getStatus() == ExpedienteStatusEnum.BLSG_PRECONSULTA || 
                exp.getStatus() == ExpedienteStatusEnum.IN_DATA_COLLECTION_PROGRESS) {
                exp.updateStatus(ExpedienteStatusEnum.DATA_COMPLETE);
            }
            
            expedienteRepository.save(exp);
            return ResponseEntity.ok(mapToDTO(exp));
        }).orElse(ResponseEntity.notFound().build());
    }
    
    public record MarriageCertificateRequest(
        String certificateNumber,
        String registryBook,
        String registryPage,
        String registryOffice,
        String place
    ) {}

    @GetMapping("/cases/{id}/blsg-certificate")
    public ResponseEntity<Resource> getBlsgCertificate(@PathVariable UUID id) {
        log.info("REST: Descargando constancia judicial BLSG para expediente: {}", id);
        return expedienteRepository.findById(id).map(exp -> {
            var profile = exp.getSocioEconomicProfile();
            if (profile != null && profile.getCertificatePath() != null) {
                try {
                    Path filePath = Paths.get(profile.getCertificatePath());
                    Resource resource = new UrlResource(java.util.Objects.requireNonNull(filePath.toUri()));
                    if (resource.exists() && resource.isReadable()) {
                        return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"constancia_blsg.pdf\"")
                                .contentType(java.util.Objects.requireNonNull(MediaType.APPLICATION_PDF))
                                .body(resource);
                    }
                } catch (java.io.IOException e) {
                    log.error("Error al leer certificado BLSG", e);
                }
            }
            return ResponseEntity.notFound().<Resource>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    private DivorceResponseDTO mapToDTO(Expediente expediente) {
        if (expediente == null) return null;

        // Cache common properties from expediente to avoid multiple getter calls (Rule[coding-standards.md])
        final Spouse petitioner = expediente.getPetitioner();
        final List<com.lawrabot.divorce_mcp_server.domain.model.CaseParticipant> participants = expediente.getParticipants();
        final com.lawrabot.divorce_mcp_server.domain.valueobject.AddressVO residence = expediente.getLastConjugalResidence();
        final Spouse respondent = expediente.getRespondent();
        final List<Child> children = expediente.getChildren();
        final com.lawrabot.divorce_mcp_server.domain.model.SocioEconomicProfile profile = expediente.getSocioEconomicProfile();
        final LocalDate marriageDate = expediente.getMarriageDate();
        final LocalDate separationDate = expediente.getDeFactoSeparationDate();
        final UUID id = expediente.getId();
        final com.lawrabot.divorce_mcp_server.domain.enums.ExpedienteStatusEnum status = expediente.getStatus();
        final com.lawrabot.divorce_mcp_server.domain.enums.DivorceTypeEnum divorceType = expediente.getDivorceType();
        final com.lawrabot.divorce_mcp_server.domain.model.RegulatoryAgreement agreement = expediente.getRegulatoryAgreement();
        final String rawText = expediente.getRawAgreementText();
        final java.time.LocalDateTime createdAt = expediente.getCreatedAt();

        DivorceResponseDTO.PetitionerDTO petDTO = null;

        if (petitioner != null) {
            final FullNameVO nameVo = petitioner.getName();
            String fullNameStr = nameVo != null ? nameVo.getFullName() : "Expediente Nuevo";
            
            final DNIVO dniVo = petitioner.getDni();
            String dniStr = dniVo != null ? dniVo.getValue() : "S/D";

            final CuilVO cuilVo = petitioner.getCuil();
            String cuilStr = (cuilVo != null) ? cuilVo.getValue() : 
                             (profile != null && profile.getCuil() != null) ? profile.getCuil() : "S/D";

            final PhoneNumberVO phoneVo = petitioner.getPhoneNumber();
            String phoneStr = phoneVo != null ? phoneVo.getValue() : "S/D";

            final LocalDate pBirthDate = petitioner.getBirthDate();
            // Fallback: si el Spouse no tiene birthDate, intentar usar la fecha del perfil socioeconómico (BLSG).
            // Solo usar si es parseable como LocalDate para evitar datos crudos del scraping (ej: CUIL).
            String birthStr;
            if (pBirthDate != null) {
                birthStr = pBirthDate.toString();
            } else {
                birthStr = parseDateToString(profile != null ? profile.getBirthDate() : null);
            }

            final String nationality = petitioner.getNationality();
            final String email = petitioner.getEmail();
            final String profession = petitioner.getProfession();

            petDTO = PetitionerDTO.builder()
                .fullName(FullNameDTO.builder().fullName(fullNameStr).build())
                .dni(dniStr)
                .cuil(cuilStr)
                .phoneNumber(phoneStr)
                .nationality(nationality != null ? nationality : "S/D")
                .email(email != null ? email : "S/D")
                .profession(profession != null ? profession : "S/D")
                .birthDate(birthStr)
                .address(mapAddress(petitioner.getAddress()))
                .build();
        } else if (participants != null && !participants.isEmpty()) {
            // Fallback: Buscar en los participantes de MCI
            var participant = participants.stream()
                    .filter(p -> p.getRole() == com.lawrabot.divorce_mcp_server.domain.enums.CaseRole.PETITIONER)
                    .findFirst()
                    .orElse(null);
            
            if (participant != null) {
                var citizen = participant.getCitizen();
                if (citizen != null) {
                    var citName = citizen.getFullName();
                    String fullNameStr = citName != null ? citName.getFullName() : "S/D";
                    
                    var citCuil = citizen.getCuil();
                    String cuilStr = (citCuil != null) ? citCuil.getValue() : 
                                     (profile != null && profile.getCuil() != null) ? profile.getCuil() : "S/D";

                    var citPhone = citizen.getPhoneNumber();
                    var citEmail = citizen.getEmail();

                    String birthStr = parseDateToString(profile != null ? profile.getBirthDate() : null);

                    petDTO = PetitionerDTO.builder()
                        .fullName(FullNameDTO.builder().fullName(fullNameStr).build())
                        .dni(citizen.getDni())
                        .cuil(cuilStr)
                        .email(citEmail != null ? citEmail : "S/D")
                        .phoneNumber(citPhone != null ? citPhone.getValue() : "S/D")
                        .birthDate(birthStr)
                        .build();
                }
            }
        }

        LastConjugalResidenceDTO residenceDTO = null;
        if (residence != null) {
            residenceDTO = LastConjugalResidenceDTO.builder()
                .locality(residence.getLocality() != null ? residence.getLocality() : "N/A")
                .street(residence.getStreet())
                .number(residence.getNumber())
                .build();
        }

        DivorceResponseDTO.RespondentDTO respDTO = null;
        if (respondent != null) {
            final FullNameVO nameVo = respondent.getName();
            String fullRespStr = nameVo != null ? nameVo.getFullName() : "S/D";
            
            final DNIVO dniVo = respondent.getDni();
            String dnistr = dniVo != null ? dniVo.getValue() : "S/D";

            final CuilVO cuilVo = respondent.getCuil();
            String cuilStr = (cuilVo != null) ? cuilVo.getValue() : "S/D";

            final PhoneNumberVO phoneVo = respondent.getPhoneNumber();
            String phoneStr = phoneVo != null ? phoneVo.getValue() : "S/D";

            final LocalDate bDate = respondent.getBirthDate();
            String birthStr = bDate != null ? bDate.toString() : "S/D";

            final String nationality = respondent.getNationality();
            final String email = respondent.getEmail();
            final String profession = respondent.getProfession();

            respDTO = RespondentDTO.builder()
                .fullName(FullNameDTO.builder().fullName(fullRespStr).build())
                .dni(dnistr)
                .cuil(cuilStr)
                .phoneNumber(phoneStr)
                .nationality(nationality != null ? nationality : "S/D")
                .email(email != null ? email : "S/D")
                .profession(profession != null ? profession : "S/D")
                .birthDate(birthStr)
                .residentialAddress(mapAddress(respondent.getAddress()))
                .build();
        } else if (participants != null && !participants.isEmpty()) {
            // Fallback: Buscar en los participantes de MCI para el demandado
            var participant = participants.stream()
                    .filter(p -> p.getRole() == com.lawrabot.divorce_mcp_server.domain.enums.CaseRole.RESPONDENT)
                    .findFirst()
                    .orElse(null);
            
            if (participant != null) {
                var citizen = participant.getCitizen();
                if (citizen != null) {
                    var citName = citizen.getFullName();
                    String fullNameStr = citName != null ? citName.getFullName() : "S/D";
                    
                    var citCuil = citizen.getCuil();
                    var citPhone = citizen.getPhoneNumber();
                    var citEmail = citizen.getEmail();

                    respDTO = DivorceResponseDTO.RespondentDTO.builder()
                        .fullName(DivorceResponseDTO.FullNameDTO.builder().fullName(fullNameStr).build())
                        .dni(citizen.getDni())
                        .cuil(citCuil != null ? citCuil.getValue() : "S/D")
                        .email(citEmail != null ? citEmail : "S/D")
                        .phoneNumber(citPhone != null ? citPhone.getValue() : "S/D")
                        .nationality(citizen.getNationality() != null ? citizen.getNationality() : "S/D")
                        .profession(citizen.getOccupation() != null ? citizen.getOccupation() : "S/D")
                        .build();
                }
            }
        }

        List<DivorceResponseDTO.ChildDTO> childrenDTOs = null;
        if (children != null && !children.isEmpty()) {
            childrenDTOs = children.stream().map(c -> {
                final DNIVO cDni = c.getDni();
                final LocalDate cBirth = c.getBirthDate();
                return ChildDTO.builder()
                    .name(c.getFullName())
                    .dni(cDni != null ? cDni.getValue() : "S/D")
                    .birthDate(cBirth != null ? cBirth.toString() : "S/D")
                    .age(c.getAge())
                    .hasDisability(c.isDisabled())
                    .isStudent(c.isStudent())
                    .birthCertificateId(c.getBirthCertificateId())
                    .build();
            }).collect(Collectors.toList());
        }

        DivorceResponseDTO.SocioEconomicProfileDTO profileDTO = null;
        if (profile != null) {
            final java.math.BigDecimal income = profile.getMonthlyIncomeArs();
            final com.lawrabot.divorce_mcp_server.domain.enums.HousingSituationEnum housingPadding = profile.getHousingSituation();
            final com.lawrabot.divorce_mcp_server.domain.enums.BlsgScrapingResultEnum scrapingResult = profile.getScrapingResult();
            
            profileDTO = SocioEconomicProfileDTO.builder()
                .avgMonthlyIncome(income != null ? income.doubleValue() : 0.0)
                .housingType(housingPadding != null ? housingPadding.name() : null)
                .occupation(profile.getOccupation())
                .blsgScrapingResult(scrapingResult != null ? scrapingResult.name() : null)
                .blsgObservations(profile.getDefensoriaObservations())
                .scrapingFullName(profile.getFullName())
                .scrapingDni(profile.getDni())
                .scrapingCuil(profile.getCuil())
                .scrapingBirthDate(profile.getBirthDate())
                .scrapingProvince(profile.getProvince())
                .scrapingSex(profile.getSex())
                .scrapingJustification(profile.getScrapingJustification())
                .certificatePath(profile.getCertificatePath())
                .vehiclesRegistered(profile.getVehiclesRegistered())
                .hasFormalEmployment(profile.isHasFormalEmployment())
                .blsgApprovedByDefensoria(profile.getBlsgApprovedByDefensoria())
                .build();
        }

        return DivorceResponseDTO.builder()
            .id(id != null ? id.toString() : "S/D")
            .status(status != null ? status.name() : "PENDIENTE")
            .divorceType(divorceType != null ? divorceType.name() : "UNILATERAL")
            .marriageDate(marriageDate != null ? marriageDate.toString() : null)
            .deFactoSeparationDate(separationDate != null ? separationDate.toString() : null)
            .marriageCertificateNumber(expediente.getMarriageCertificateNumber())
            .marriageRegistryBook(expediente.getMarriageRegistryBook())
            .marriageRegistryPage(expediente.getMarriageRegistryPage())
            .marriageRegistryOffice(expediente.getMarriageRegistryOffice())
            .marriagePlace(expediente.getMarriagePlace())
            .marriageCertificateId(expediente.getMarriageCertificateId() != null ? expediente.getMarriageCertificateId().toString() : null)
            .marriageCertificateIssuanceDate(expediente.getMarriageCertificateIssuanceDate() != null ? expediente.getMarriageCertificateIssuanceDate().toString() : null)
            .petitioner(petDTO)
            .respondent(respDTO)
            .lastConjugalResidence(residenceDTO)
            .children(childrenDTOs)
            .socioEconomicProfile(profileDTO)
            .regulatoryAgreement(agreement)
            .rawAgreementText(rawText)
            .createdAt(createdAt != null ? createdAt.toString() : null)
            .build();
    }


    private DivorceResponseDTO.AddressDTO mapAddress(com.lawrabot.divorce_mcp_server.domain.valueobject.AddressVO vo) {
        if (vo == null) return null;
        return AddressDTO.builder()
            .street(vo.getStreet() != null ? vo.getStreet() : "")
            .number(vo.getNumber() != null ? vo.getNumber() : "")
            .locality(vo.getLocality() != null ? vo.getLocality() : "")
            .floorAppartment(vo.getFloorAppartment())
            .neighborhood(vo.getNeighborhood())
            .province(vo.getProvince())
            .zipCode(vo.getZipCode())
            .build();
    }
}
