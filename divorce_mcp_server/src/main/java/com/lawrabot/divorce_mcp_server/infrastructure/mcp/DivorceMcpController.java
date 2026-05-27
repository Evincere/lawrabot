package com.lawrabot.divorce_mcp_server.infrastructure.mcp;

import com.lawrabot.divorce_mcp_server.application.port.in.*;
import com.lawrabot.divorce_mcp_server.domain.enums.BlsgScrapingResultEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.DataCollectionStageEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.HousingSituationEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Child;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.domain.enums.ExpedienteStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Observation;
import com.lawrabot.divorce_mcp_server.domain.valueobject.AddressVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.DNIVO;
import com.lawrabot.divorce_mcp_server.domain.valueobject.FullNameVO;
import com.lawrabot.divorce_mcp_server.application.service.AdvancedRagService;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.DigitalEvidenceJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.ExpedienteJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa.SpringDataDigitalEvidenceRepository;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa.SpringDataExpedienteRepository;
import com.lawrabot.divorce_mcp_server.infrastructure.mcp.dto.*;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
@Slf4j
public class DivorceMcpController {

    private final Map<String, ConsultarBlsgUseCase.ScrapingResult> pendingBlsgResults = new ConcurrentHashMap<>();

    private final CreateDivorceDossierUseCase createDivorceDossierUseCase;
    private final GetExpedienteCollectionStageUseCase getExpedienteCollectionStageUseCase;
    private final SubmitMarriageDetailsUseCase submitMarriageDetailsUseCase;
    private final SubmitChildrenInfoUseCase submitChildrenInfoUseCase;
    private final SubmitSocioEconomicEvaluationUseCase submitSocioEconomicEvaluationUseCase;
    private final ProcessBlsgScrapingResultUseCase processBlsgScrapingResultUseCase;
    private final ConsultarBlsgUseCase consultarBlsgUseCase;
    private final DraftRegulatoryAgreementUseCase draftRegulatoryAgreementUseCase;
    private final ValidateAgreementLegalityUseCase validateAgreementLegalityUseCase;
    private final SetDivorceModalityUseCase setDivorceModalityUseCase;
    private final SubmitPersonalDataUseCase submitPersonalDataUseCase;
    private final GenerateReferralPdfUseCase generateReferralPdfUseCase;
    private final AdvancedRagService ragService;
    private final SpringDataExpedienteRepository expedienteRepository;
    private final com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository expedienteDomainRepo;
    private final ManageObservationsUseCase manageObservationsUseCase;
    private final SpringDataDigitalEvidenceRepository digitalEvidenceRepository;
    private final com.lawrabot.divorce_mcp_server.application.service.AppointmentService appointmentService;

    @Value("${lawrabot.storage.evidence-path:./storage/evidences}")
    private String storagePathBase;

    public DivorceMcpController(
            CreateDivorceDossierUseCase createDivorceDossierUseCase,
            GetExpedienteCollectionStageUseCase getExpedienteCollectionStageUseCase,
            SubmitMarriageDetailsUseCase submitMarriageDetailsUseCase,
            SubmitChildrenInfoUseCase submitChildrenInfoUseCase,
            SubmitSocioEconomicEvaluationUseCase submitSocioEconomicEvaluationUseCase,
            ProcessBlsgScrapingResultUseCase processBlsgScrapingResultUseCase,
            ConsultarBlsgUseCase consultarBlsgUseCase,
            DraftRegulatoryAgreementUseCase draftRegulatoryAgreementUseCase,
            ValidateAgreementLegalityUseCase validateAgreementLegalityUseCase,
            SetDivorceModalityUseCase setDivorceModalityUseCase,
            SubmitPersonalDataUseCase submitPersonalDataUseCase,
            GenerateReferralPdfUseCase generateReferralPdfUseCase,
            @Lazy @org.springframework.lang.Nullable AdvancedRagService ragService,
            SpringDataExpedienteRepository expedienteRepository,
            com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository expedienteDomainRepo,
            ManageObservationsUseCase manageObservationsUseCase,
            SpringDataDigitalEvidenceRepository digitalEvidenceRepository,
            com.lawrabot.divorce_mcp_server.application.service.AppointmentService appointmentService) {
        this.createDivorceDossierUseCase = createDivorceDossierUseCase;
        this.getExpedienteCollectionStageUseCase = getExpedienteCollectionStageUseCase;
        this.submitMarriageDetailsUseCase = submitMarriageDetailsUseCase;
        this.submitChildrenInfoUseCase = submitChildrenInfoUseCase;
        this.submitSocioEconomicEvaluationUseCase = submitSocioEconomicEvaluationUseCase;
        this.processBlsgScrapingResultUseCase = processBlsgScrapingResultUseCase;
        this.consultarBlsgUseCase = consultarBlsgUseCase;
        this.draftRegulatoryAgreementUseCase = draftRegulatoryAgreementUseCase;
        this.validateAgreementLegalityUseCase = validateAgreementLegalityUseCase;
        this.setDivorceModalityUseCase = setDivorceModalityUseCase;
        this.submitPersonalDataUseCase = submitPersonalDataUseCase;
        this.generateReferralPdfUseCase = generateReferralPdfUseCase;
        this.ragService = ragService;
        this.expedienteRepository = expedienteRepository;
        this.expedienteDomainRepo = expedienteDomainRepo;
        this.manageObservationsUseCase = manageObservationsUseCase;
        this.digitalEvidenceRepository = digitalEvidenceRepository;
        this.appointmentService = appointmentService;
    }
    
    /**
     * Normaliza el número de teléfono recibido desde el agente LLM.
     * Acepta formatos: 549XXXXXXXXXX (13 dígitos), 261XXXXXXX (10 dígitos),
     * o cualquier variante que PhoneNumberVO pueda procesar.
     * Retorna siempre exactamente 10 dígitos (ej: 2634515362).
     */
    private String normalizePhone(String rawPhone) {
        return com.lawrabot.divorce_mcp_server.domain.valueobject.PhoneNumberVO.of(rawPhone).getValue();
    }

    private UUID resolveExpedienteId(String phoneNumber) {
        String normalized = normalizePhone(phoneNumber);
        return expedienteDomainRepo.findActiveByClientPhone(normalized)
                .orElseThrow(() -> new IllegalArgumentException("status: NOT_FOUND. No se encontró expediente activo para: " + phoneNumber))
                .getId();
    }

    @Tool(name = "start_divorce_process", description = "Inicia una solicitud de proceso judicial de divorcio en el sistema.")
    public String startDivorceProcess(
            @JsonPropertyDescription("MANDATORIO: El número de teléfono REAL del remitente (extraído de [METADATA], ej: 5492634515362). PROHIBIDO inventar.") String phoneNumber,
            @JsonPropertyDescription("Primer nombre del cliente (extraido del resultado BLSG)") String firstName,
            @JsonPropertyDescription("Apellido del cliente (extraido del resultado BLSG)") String lastName,
            @JsonPropertyDescription("DNI del solicitante, ya proporcionado para la consulta BLSG") String dni) {
        // Normalizar para garantizar consistencia con la clave del cache de BLSG
        String normalized = normalizePhone(phoneNumber);
        Expediente exp = createDivorceDossierUseCase.execute(normalized, firstName, lastName, dni);
        
        // Buscar resultado cacheado por clave normalizada
        ConsultarBlsgUseCase.ScrapingResult cachedResult = pendingBlsgResults.remove(normalized);
        if (cachedResult != null) {
            BlsgScrapingResultEnum domainResult = cachedResult.benefitStatus().contains("Se otorga") 
                ? BlsgScrapingResultEnum.PROVISIONALLY_APPROVED 
                : ((cachedResult.benefitStatus().contains("evaluación adicional") || cachedResult.benefitStatus().contains("Inconcluso"))
                    ? BlsgScrapingResultEnum.INCONCLUSIVE 
                    : BlsgScrapingResultEnum.PROVISIONALLY_REJECTED);

            processBlsgScrapingResultUseCase.execute(
                exp.getId(), 
                domainResult, 
                cachedResult.benefitStatus(), 
                cachedResult.fullName(), 
                cachedResult.dni(), 
                cachedResult.cuil(), 
                cachedResult.birthDate(), 
                cachedResult.province(), 
                cachedResult.sex(), 
                cachedResult.certificatePath()
            );
            log.info("Resultado BLSG en caché vinculado al nuevo expediente {}", exp.getId());
        }

        return String.format("Solicitud registrada (Nro. %s). [NEXT_STEP] Pregunta al usuario si el divorcio será Unilateral o Conjunto. NO pidas ningún otro dato todavía.", exp.getId());
    }

    @Tool(name = "set_divorce_modality", description = "Establece si el divorcio es UNILATERAL o JOINT (conjunto).")
    public String setDivorceModality(
            @JsonPropertyDescription("MANDATORIO: El número de teléfono REAL del remitente (extraído de [METADATA], ej: 5492634515362). PROHIBIDO inventar.") String phoneNumber,
            @JsonPropertyDescription("Modalidad elegida: UNILATERAL o JOINT") String modality) {
        com.lawrabot.divorce_mcp_server.domain.enums.DivorceTypeEnum type;
        try {
            type = com.lawrabot.divorce_mcp_server.domain.enums.DivorceTypeEnum.valueOf(modality.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Error: Modalidad inválida. Use UNILATERAL o JOINT.";
        }

        try {
            setDivorceModalityUseCase.execute(phoneNumber, type);
            return "Modalidad establecida como: " + type.name() + ". [NEXT_STEP] Pide SOLO los datos personales del peticionante: nacionalidad, ocupación, fecha de nacimiento, domicilio real y email. NO pidas datos de la contraparte ni documentos todavía.";
        } catch (Exception e) {
            return "Error al establecer modalidad: " + e.getMessage();
        }
    }

    @Tool(name = "get_dossier_stage", description = "Obtiene la etapa actual del proceso judicial mediante el teléfono.")
    public String getDossierStage(
            @JsonPropertyDescription("MANDATORIO: El número de teléfono REAL del remitente (extraído de [METADATA], ej: 5492634515362). PROHIBIDO inventar.") String phoneNumber) {
        try {
            DataCollectionStageEnum stage = getExpedienteCollectionStageUseCase.queryByClientPhone(phoneNumber);
            return "La etapa actual del cliente es: " + stage.name();
        } catch (Exception e) {
            return "status: NOT_FOUND. No se encontró expediente activo para: " + phoneNumber;
        }
    }

    @Tool(name = "submit_petitioner_personal_data", description = "Recolección de datos personales completos del peticionante.")
    public String submitPetitionerPersonalData(
            @JsonPropertyDescription("MANDATORIO: El número de teléfono REAL del remitente (extraído de [METADATA], ej: 5492634515362). PROHIBIDO inventar.") String phoneNumber,
            @JsonPropertyDescription("Nacionalidad") String nationality,
            @JsonPropertyDescription("Ocupación") String occupation,
            @JsonPropertyDescription("Correo electrónico (Opcional)") String email,
            @JsonPropertyDescription("Fecha de nacimiento del peticionante en formato YYYY-MM-DD. El agente debe convertir cualquier formato coloquial del usuario (ej: '20 de mayo de 1978') a este formato.") String birthDate,
            @JsonPropertyDescription("Domicilio real") AddressDto address) {
        
        AddressVO addressVO = AddressVO.builder()
                .street(address.street())
                .number(address.number())
                .floorAppartment(address.floorAppartment())
                .neighborhood(address.neighborhood())
                .locality(address.locality())
                .province(address.province())
                .zipCode(address.zipCode())
                .build();

        submitPersonalDataUseCase.execute(phoneNumber, com.lawrabot.divorce_mcp_server.domain.enums.CaseRole.PETITIONER, 
                null, null, phoneNumber, nationality, occupation, email, birthDate, addressVO);
        
        return "Datos del peticionante registrados. [NEXT_STEP] Pide los datos de la ex-pareja (contraparte): nombre completo, DNI, nacionalidad, ocupación, fecha de nacimiento, domicilio actual, teléfono y email. NO pidas documentos ni datos socioeconómicos todavía.";
    }

    @Tool(name = "submit_respondent_personal_data", description = "Recolección de datos personales de la ex-pareja (contraparte).")
    public String submitRespondentPersonalData(
            @JsonPropertyDescription("MANDATORIO: El número de teléfono REAL del remitente (extraído de [METADATA], ej: 5492634515362). PROHIBIDO inventar.") String phoneNumber,
            @JsonPropertyDescription("Nombre completo (solo para unilateral o si no se tiene)") String fullName,
            @JsonPropertyDescription("DNI (solo para unilateral o si no se tiene)") String dni,
            @JsonPropertyDescription("Teléfono de la contraparte (opcional)") String participantPhone,
            @JsonPropertyDescription("Nacionalidad") String nationality,
            @JsonPropertyDescription("Ocupación") String occupation,
            @JsonPropertyDescription("Correo electrónico (opcional)") String email,
            @JsonPropertyDescription("Fecha de nacimiento de la contraparte en formato YYYY-MM-DD. El agente debe convertir cualquier formato coloquial del usuario a este formato.") String birthDate,
            @JsonPropertyDescription("Domicilio actual/real") AddressDto address) {
        
        AddressVO addressVO = AddressVO.builder()
                .street(address.street())
                .number(address.number())
                .floorAppartment(address.floorAppartment())
                .neighborhood(address.neighborhood())
                .locality(address.locality())
                .province(address.province())
                .zipCode(address.zipCode())
                .build();

        submitPersonalDataUseCase.execute(phoneNumber, com.lawrabot.divorce_mcp_server.domain.enums.CaseRole.RESPONDENT, 
                fullName, dni, participantPhone, nationality, occupation, email, birthDate, addressVO);
        
        return "Datos de la contraparte registrados. [NEXT_STEP] Pide al usuario SOLO la evaluación socioeconómica: empleo formal (sí/no), ingreso mensual aproximado, tipo de vivienda (alquilada/propia/familiar), cantidad de vehículos. NO menciones documentos de ingresos todavía. NO pidas datos del matrimonio todavía.";
    }

    @Tool(name = "submit_marriage_details", description = "Guarda los detalles del matrimonio en el expediente.")
    public String submitMarriageDetails(
            @JsonPropertyDescription("MANDATORIO: El número de teléfono REAL del remitente (extraído de [METADATA], ej: 5492634515362). PROHIBIDO inventar.") String phoneNumber,
            @JsonPropertyDescription("Fecha de matrimonio en formato ISO (YYYY-MM-DD). El agente debe convertir cualquier formato coloquial del usuario a este formato.") String marriageDate,
            @JsonPropertyDescription("Fecha de separación en formato ISO (YYYY-MM-DD). Si el usuario solo indica mes y año, usar el primer día del mes (ej: 2020-02-01).") String separationDate,
            @JsonPropertyDescription("Ultimo domicilio conyugal. IMPORTANTE: separar calle y numero en campos DISTINTOS 'street' y 'number'. Ejemplo correcto: {street:'Alem', number:'456', locality:'San Rafael', province:'Mendoza'}. PROHIBIDO usar claves en espanol (calle/numero/localidad/provincia).") AddressDto lastResidence) {
            
        UUID expedienteId = resolveExpedienteId(phoneNumber);

        AddressVO address = null;
        if (lastResidence != null) {
            address = AddressVO.builder()
                    .street(lastResidence.street())
                    .number(lastResidence.number())
                    .floorAppartment(lastResidence.floorAppartment())
                    .neighborhood(lastResidence.neighborhood())
                    .locality(lastResidence.locality())
                    .province(lastResidence.province())
                    .zipCode(lastResidence.zipCode())
                    .build();
        }

        LocalDate mDate = (marriageDate != null && !marriageDate.isBlank()) ? LocalDate.parse(marriageDate) : null;
        LocalDate sDate = (separationDate != null && !separationDate.isBlank()) ? LocalDate.parse(separationDate)
                : null;

        submitMarriageDetailsUseCase.execute(expedienteId, mDate, sDate, address);
        return "Datos del matrimonio registrados. [NEXT_STEP] PREGUNTA al usuario que envíe el Acta de Matrimonio (foto o PDF legible, emisión menor a 6 meses) por este chat. PROHIBIDO llamar a submit_digital_evidence ahora — solo podés llamarla cuando el usuario ENVÍE el archivo y veas [MEDIA] en su mensaje.";
    }

    @Tool(name = "submit_children_info", description = "Carga la información de los hijos al expediente.")
    public String submitChildrenInfo(
            @JsonPropertyDescription("MANDATORIO: El número de teléfono REAL del remitente (extraído de [METADATA], ej: 5492634515362). PROHIBIDO inventar.") String phoneNumber,
            @JsonPropertyDescription("Lista de hijos en formato texto simple. Cada elemento DEBE tener el formato exacto: 'nombre_completo | YYYY-MM-DD | DNI_o_null | disabled=true/false | isStudent=true/false/null'. Ej: ['Aleixo Toledo | 2009-09-20 | 51711299 | disabled=false | isStudent=null']. Si no hay hijos, enviá [].") List<String> childrenCsv) {
            
        List<ChildDto> children = new java.util.ArrayList<>();
        if (childrenCsv != null) {
            for (String csv : childrenCsv) {
                try {
                    String[] parts = csv.split("\\|");
                    if (parts.length >= 5) {
                        String name = parts[0].trim();
                        String bdate = parts[1].trim();
                        String dni = parts[2].trim().replace("null", "");
                        boolean disabled = parts[3].trim().toLowerCase().contains("true");
                        Boolean isStudent = null;
                        String studentStr = parts[4].trim().toLowerCase();
                        if (studentStr.contains("true")) isStudent = true;
                        if (studentStr.contains("false")) isStudent = false;
                        
                        children.add(new ChildDto(dni.isEmpty() ? null : dni, name, bdate, disabled, isStudent));
                    }
                } catch (Exception e) {
                    log.error("Error parsing childrenCsv element: {}", csv);
                }
            }
        }

        UUID expedienteId = resolveExpedienteId(phoneNumber);
        if (children.isEmpty()) {
            Expediente exp = expedienteDomainRepo.findById(expedienteId)
                    .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado."));
            exp.setChildren(List.of());
            exp.updateCollectionStage(DataCollectionStageEnum.PENDING_REGULATORY_AGREEMENT);
            expedienteDomainRepo.save(exp);
            return "Datos registrados. No se informaron hijos en común. [NEXT_STEP] El caso no posee hijos elegibles. Avanzado automáticamente al Convenio Regulador. EN UN BLOQUE SEPARADO con su propia cabecera '## 📝 CONVENIO REGULADOR', pregunta qué propone para el convenio regulador (distribución de bienes, uso de vivienda, etc.).";
        }

        LocalDate now = LocalDate.now();
        List<Child> eligibleChildren = new java.util.ArrayList<>();
        List<String> questionsToAsk = new java.util.ArrayList<>();
        List<String> exclusionsInfo = new java.util.ArrayList<>();

        for (ChildDto dto : children) {
            FullNameVO name = FullNameVO.fromFullString(dto.fullName());
            DNIVO dniVO = (dto.dni() != null && !dto.dni().isBlank()) ? DNIVO.of(dto.dni()) : null;
            LocalDate bDate = (dto.birthDate() != null && !dto.birthDate().isBlank()) ? LocalDate.parse(dto.birthDate())
                    : null;

            if (bDate == null) {
                // Si no tiene fecha de nacimiento, asumir menor para estar del lado de la seguridad
                eligibleChildren.add(Child.builder()
                        .name(name)
                        .dni(dniVO)
                        .birthDate(null)
                        .disabled(dto.disabled())
                        .student(dto.isStudent() != null && dto.isStudent())
                        .build());
                continue;
            }

            int age = java.time.Period.between(bDate, now).getYears();

            if (age < 18) {
                // Menor de 18: Elegible ordinario
                eligibleChildren.add(Child.builder()
                        .name(name)
                        .dni(dniVO)
                        .birthDate(bDate)
                        .disabled(dto.disabled())
                        .student(false)
                        .build());
            } else if (age < 21) {
                // Entre 18 y 20 años: Elegible ordinario
                eligibleChildren.add(Child.builder()
                        .name(name)
                        .dni(dniVO)
                        .birthDate(bDate)
                        .disabled(dto.disabled())
                        .student(false)
                        .build());
            } else if (age < 25) {
                // Entre 21 y 24 años: Depende de estudios o discapacidad
                if (dto.disabled()) {
                    eligibleChildren.add(Child.builder()
                            .name(name)
                            .dni(dniVO)
                            .birthDate(bDate)
                            .disabled(true)
                            .student(dto.isStudent() != null && dto.isStudent())
                            .build());
                } else if (dto.isStudent() == null) {
                    // Si no sabemos si estudia, preguntamos proactivamente
                    questionsToAsk.add("Noté que " + name.getFirstName() + " tiene " + age + " años. ¿Actualmente se encuentra estudiando o capacitándose en un oficio que le impida trabajar a tiempo completo?");
                    // Mantenemos al hijo temporalmente para no perder datos en la recolección
                    eligibleChildren.add(Child.builder()
                            .name(name)
                            .dni(dniVO)
                            .birthDate(bDate)
                            .disabled(false)
                            .student(false)
                            .build());
                } else if (dto.isStudent()) {
                    // Estudiante: Elegible
                    eligibleChildren.add(Child.builder()
                            .name(name)
                            .dni(dniVO)
                            .birthDate(bDate)
                            .disabled(false)
                            .student(true)
                            .build());
                } else {
                    // No estudia y no es discapacitado: Excluido (Fail Fast)
                    exclusionsInfo.add(name.getFirstName() + " (" + age + " años, no estudia)");
                }
            } else {
                // Mayor de 25 años: Solo si tiene discapacidad
                if (dto.disabled()) {
                    eligibleChildren.add(Child.builder()
                            .name(name)
                            .dni(dniVO)
                            .birthDate(bDate)
                            .disabled(true)
                            .student(false)
                            .build());
                } else {
                    exclusionsInfo.add(name.getFirstName() + " (" + age + " años, sin discapacidad declarada)");
                }
            }
        }

        // Guardar los hijos elegibles/en proceso
        submitChildrenInfoUseCase.execute(expedienteId, eligibleChildren);

        StringBuilder nextStep = new StringBuilder("Datos de los hijos procesados. ");
        if (!exclusionsInfo.isEmpty()) {
            nextStep.append("Los siguientes hijos mayores de edad fueron excluidos del convenio por no cumplir con los requisitos excepcionales de la ley (Art. 658, 663 CCyC): ")
                    .append(String.join(", ", exclusionsInfo)).append(". ");
        }

        if (!questionsToAsk.isEmpty()) {
            nextStep.append("[NEXT_STEP] ").append(String.join(" ", questionsToAsk));
            return nextStep.toString();
        }

        // Si no hay preguntas pendientes, pedir los documentos
        List<String> checklist = new java.util.ArrayList<>();
        checklist.add("Acta de nacimiento para cada hijo registrado (acredita el vínculo)");
        
        for (Child c : eligibleChildren) {
            if (c.isDisabled()) {
                checklist.add("Certificado Único de Discapacidad (CUD) para " + c.getFullName());
            }
            // Nota: El Certificado de Alumno Regular NO se solicita al ciudadano.
            // Será gestionado por el abogado defensor en una etapa posterior del proceso.
        }

        nextStep.append("[NEXT_STEP] Por favor, realizá la pregunta de barrido: '¿Alguno de los hijos tiene alguna discapacidad o requiere apoyo asistencial permanente?'. ");
        nextStep.append("Luego, solicitá proactivamente la siguiente documentación probatoria (foto o PDF legible):\n");
        for (String item : checklist) {
            nextStep.append("- ").append(item).append("\n");
        }
        nextStep.append("\nPROHIBIDO avanzar al convenio regulador hasta que todos los documentos pendientes hayan sido adjuntados digitalmente (submit_digital_evidence).");

        return nextStep.toString();
    }

    private void checkAndAdvanceChildrenStage(Expediente exp) {
        if (exp.getCollectionStage() != DataCollectionStageEnum.PENDING_CHILDREN_INFO) {
            return;
        }

        List<Child> childrenList = exp.getChildren();
        if (childrenList == null || childrenList.isEmpty()) {
            exp.updateCollectionStage(DataCollectionStageEnum.PENDING_REGULATORY_AGREEMENT);
            expedienteDomainRepo.save(exp);
            log.info("Expediente {} avanzado automáticamente a PENDING_REGULATORY_AGREEMENT por no tener hijos", exp.getId());
            return;
        }

        // Obtener evidencias subidas
        List<DigitalEvidenceJpaEntity> evidences = digitalEvidenceRepository.findByExpediente_IdOrderByCreatedAtDesc(exp.getId());
        
        long birthCertCount = evidences.stream().filter(e -> "BIRTH_CERT".equalsIgnoreCase(e.getDocumentType())).count();
        long disabilityCertCount = evidences.stream().filter(e -> "DISABILITY_CERT".equalsIgnoreCase(e.getDocumentType())).count();

        // Calcular cuántos de cada tipo requerimos
        int birthCertsRequired = childrenList.size(); // 1 para cada hijo elegible
        int disabilityCertsRequired = 0;
        // Nota: STUDENT_PROOF no se requiere al ciudadano; lo gestiona el abogado defensor.

        for (Child c : childrenList) {
            if (c.isDisabled()) {
                disabilityCertsRequired++;
            }
        }

        boolean hasAllBirthCerts = birthCertCount >= birthCertsRequired;
        boolean hasAllDisabilityCerts = disabilityCertCount >= disabilityCertsRequired;

        if (hasAllBirthCerts && hasAllDisabilityCerts) {
            exp.updateCollectionStage(DataCollectionStageEnum.PENDING_REGULATORY_AGREEMENT);
            expedienteDomainRepo.save(exp);
            log.info("Expediente {} avanzado automáticamente a PENDING_REGULATORY_AGREEMENT. Documentos de hijos cargados: {} actas nac., {} CUD.", 
                    exp.getId(), birthCertCount, disabilityCertCount);
        }
    }

    @Tool(name = "submit_socioeconomic_info", description = "Completa los datos del análisis socioeconómico para el BLSG.")
    public String submitSocioEconomicInfo(
            @JsonPropertyDescription("MANDATORIO: El número de teléfono REAL del remitente (extraído de [METADATA], ej: 5492634515362). PROHIBIDO inventar.") String phoneNumber,
            @JsonPropertyDescription("Ingreso mensual promedio en ARS") Double monthlyIncomeArs,
            @JsonPropertyDescription("Situación habitacional (Opción en español o inglés)") String housingSituation,
            @JsonPropertyDescription("Ocupación actual (profesión u oficio)") String occupation,
            @JsonPropertyDescription("Cantidad de vehículos registrados") Integer vehiclesRegistered,
            @JsonPropertyDescription("¿Posee empleo formal?") boolean hasFormalEmployment,
            @JsonPropertyDescription("Observaciones adicionales") String observations) {
            
        UUID expedienteId = resolveExpedienteId(phoneNumber);
        HousingSituationEnum housingEnum = null;
        try {
            if (housingSituation != null && !housingSituation.isBlank()) {
                String val = housingSituation.toUpperCase().trim();
                if (val.contains("ALQUIL") || val.contains("RENT")) {
                    housingEnum = HousingSituationEnum.RENTING;
                } else if (val.contains("PROPIA") || val.contains("OWNER") || val.contains("PROPIETARIO")) {
                    housingEnum = HousingSituationEnum.OWNER;
                } else if (val.contains("FAMILIAR") || val.contains("FAMILY") || val.contains("FAMILIA")) {
                    housingEnum = HousingSituationEnum.FAMILY_HOME;
                } else if (val.contains("COMPARTE") || val.contains("SHARED") || val.contains("COMPARTIDA")) {
                    housingEnum = HousingSituationEnum.SHARED_HOUSING;
                } else if (val.contains("PRESTADA") || val.contains("PRESTADO") || val.contains("CEDIDA") || val.contains("USUFRUCTO")) {
                    housingEnum = HousingSituationEnum.OTHER;
                } else {
                    // Fallback: intentar enum directo, si falla usar OTHER
                    try {
                        housingEnum = HousingSituationEnum.valueOf(val);
                    } catch (IllegalArgumentException fallback) {
                        housingEnum = HousingSituationEnum.OTHER;
                    }
                }
            }
        } catch (IllegalArgumentException e) {
            return "Error: Situación habitacional inválida. Valores permitidos: RENTING, OWNER, FAMILY_HOME, SHARED_HOUSING, OTHER.";
        }

        BigDecimal income = (monthlyIncomeArs != null) ? BigDecimal.valueOf(monthlyIncomeArs) : BigDecimal.ZERO;

        boolean approved = submitSocioEconomicEvaluationUseCase.execute(
                expedienteId,
                income,
                housingEnum,
                occupation,
                vehiclesRegistered,
                hasFormalEmployment,
                observations);

        if (approved) {
            return "Evaluación socioeconómica APROBADA. [NEXT_STEP] Verifica si el mensaje del usuario contiene un bloque [MEDIA]. Si NO contiene [MEDIA]: confirma los datos registrados y solicita el documento de ingresos (bono de sueldo si tiene empleo formal, certificado negativo de ANSES si no). PROHIBIDO avanzar a datos del matrimonio sin el documento. Si SÍ contiene [MEDIA]: llama a submit_digital_evidence con documentType='INCOME_PROOF'.";
        } else {
            return "Evaluación socioeconómica RECHAZADA. El ciudadano supera los límites establecidos por la Defensoría (Ingresos > 350.000 ARS o bienes significativos). Informar al ciudadano los motivos y finalizar la asistencia automática.";
        }
    }

    @Tool(name = "process_scraping_result", description = "Registra los resultados de búsqueda de bienes.")
    public String processScrapingResult(
            @JsonPropertyDescription("UUID del expediente") UUID expedienteId,
            @JsonPropertyDescription("Estado del resultado") String resultStatus,
            @JsonPropertyDescription("Justificación del resultado") String justification) {
        BlsgScrapingResultEnum scrapingResult;
        try {
            scrapingResult = BlsgScrapingResultEnum.valueOf(resultStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "Error: resultStatus inválido.";
        }

        processBlsgScrapingResultUseCase.execute(expedienteId, scrapingResult, justification, null, null, null, null, null, null, null);
        return "El resultado de la verificación judicial ha sido integrado al expediente.";
    }

    @Tool(name = "consultar_blsg", description = "Consulta el Beneficio de Litigar Sin Gastos (BLSG) en el Poder Judicial de Mendoza.")
    public String consultarBlsg(
            @JsonPropertyDescription("MANDATORIO: El número de teléfono REAL del remitente (extraído de [METADATA], ej: 5492634515362). PROHIBIDO inventar.") String phoneNumber,
            @JsonPropertyDescription("DNI del ciudadano") String dni) {
        log.info("Tool MCP: consultar_blsg - DNI: {}", dni);
        // Normalizar el número para uso consistente en cache y búsquedas
        String normalized = normalizePhone(phoneNumber);
        ConsultarBlsgUseCase.ScrapingResult result = consultarBlsgUseCase.execute(normalized, dni);
        
        if (!result.success()) {
            return "❌ Error en la consulta: " + result.benefitStatus();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("✅ *Resultado de la consulta BLSG*\n\n");
        sb.append("• *Nombre:* ").append(sanitize(result.fullName())).append("\n");
        sb.append("• *DNI:* ").append(sanitize(result.dni())).append("\n");
        sb.append("• *CUIL:* ").append(sanitize(result.cuil())).append("\n");
        sb.append("• *Resultado:* *").append(sanitize(result.benefitStatus())).append("*\n");
        
        if (result.certificatePath() != null) {
            sb.append("\n📄 Constancia descargada y disponible en: `").append(result.certificatePath()).append("`");
            if (result.benefitStatus() != null && !result.benefitStatus().contains("Se otorga el Beneficio")) {
                sb.append("\n_Agente: utiliza la herramienta 'send_local_file' con esta ruta absoluta para enviar la constancia original al ciudadano._");
            } else {
                sb.append("\n_Agente: NO envíes esta constancia al ciudadano bajo ninguna circunstancia, solo regístrala para el expediente interno._");
            }
        }
        
        // --- Orquestación de persistencia diferida (usa clave normalizada) ---
        try {
            Optional<Expediente> optExp = expedienteDomainRepo.findActiveByClientPhone(normalized);
            if (optExp.isPresent()) {
                UUID expedienteId = optExp.get().getId();
                BlsgScrapingResultEnum domainResult = result.benefitStatus().contains("Se otorga") 
                    ? BlsgScrapingResultEnum.PROVISIONALLY_APPROVED 
                    : ((result.benefitStatus().contains("evaluación adicional") || result.benefitStatus().contains("Inconcluso"))
                        ? BlsgScrapingResultEnum.INCONCLUSIVE 
                        : BlsgScrapingResultEnum.PROVISIONALLY_REJECTED);

                processBlsgScrapingResultUseCase.execute(
                    expedienteId, 
                    domainResult, 
                    result.benefitStatus(), 
                    result.fullName(), 
                    result.dni(), 
                    result.cuil(), 
                    result.birthDate(), 
                    result.province(), 
                    result.sex(), 
                    result.certificatePath()
                );
                log.info("Resultado de scraping vinculado al expediente {}", expedienteId);
            } else {
                // Guardar con clave normalizada para que start_divorce_process pueda encontrarlo
                pendingBlsgResults.put(normalized, result);
                log.info("Expediente no encontrado. Resultado de scraping cacheado para el número {}", normalized);
            }
        } catch (Exception e) {
            log.warn("No se pudo vincular automáticamente el resultado al expediente: {}", e.getMessage());
            sb.append("\n⚠️ *Aviso:* Ocurrió un error al procesar la vinculación automática.");
        }
        
        return sb.toString();
    }

    @Tool(name = "consultar_blsg_respondent", description = "Consulta BLSG para la ex-pareja (solo para proceso conjunto).")
    public String consultarBlsgRespondent(
            @JsonPropertyDescription("MANDATORIO: El número de teléfono REAL del remitente (extraído de [METADATA], ej: 5492634515362). PROHIBIDO inventar.") String phoneNumber,
            @JsonPropertyDescription("DNI del segundo cónyuge") String dni) {
        
        log.info("Tool MCP: consultar_blsg_respondent - DNI: {}", dni);
        ConsultarBlsgUseCase.ScrapingResult result = consultarBlsgUseCase.execute(phoneNumber, dni);
        
        if (!result.success()) {
            return "❌ Error en la consulta del segundo cónyuge: " + result.benefitStatus();
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("✅ *Resultado de la consulta BLSG (Cónyuge)*\n\n");
        sb.append("• *Nombre:* ").append(sanitize(result.fullName())).append("\n");
        sb.append("• *DNI:* ").append(sanitize(result.dni())).append("\n");
        sb.append("• *Resultado:* *").append(sanitize(result.benefitStatus())).append("*\n");
        
        if (!result.benefitStatus().contains("Se otorga") && !result.benefitStatus().contains("evaluación adicional")) {
            sb.append("\n⚠️ *Atención:* La ex-pareja no califica para el beneficio. El proceso conjunto no es posible por esta vía. Se recomienda continuar de manera UNILATERAL.");
        } else {
            // Vincular como RESPONDENT
            try {
                UUID expedienteId = resolveExpedienteId(phoneNumber);
                com.lawrabot.divorce_mcp_server.domain.enums.BlsgScrapingResultEnum domainResult = 
                    result.benefitStatus().contains("Se otorga") 
                    ? com.lawrabot.divorce_mcp_server.domain.enums.BlsgScrapingResultEnum.PROVISIONALLY_APPROVED 
                    : com.lawrabot.divorce_mcp_server.domain.enums.BlsgScrapingResultEnum.INCONCLUSIVE;

                processBlsgScrapingResultUseCase.execute(
                    expedienteId, 
                    domainResult, 
                    result.benefitStatus(), 
                    result.fullName(), 
                    result.dni(), 
                    result.cuil(), 
                    result.birthDate(), 
                    result.province(), 
                    result.sex(), 
                    result.certificatePath()
                );
                
                // Forzar stage a PERSONAL_DATA si califica
                Expediente exp = expedienteDomainRepo.findById(expedienteId).orElse(null);
                if (exp != null) {
                    exp.updateCollectionStage(com.lawrabot.divorce_mcp_server.domain.enums.DataCollectionStageEnum.PENDING_PERSONAL_DATA);
                    expedienteDomainRepo.save(exp);
                }
                
                sb.append("\nEl cónyuge califica. Pueden continuar con la recolección de datos personales de ambos.");
            } catch (Exception e) {
                sb.append("\n⚠️ Error al vincular el resultado del cónyuge.");
            }
        }
        
        return sb.toString();
    }

    @Tool(name = "draft_regulatory_agreement", description = "Guarda el borrador del convenio regulador.")
    public String draftRegulatoryAgreement(
            @JsonPropertyDescription("MANDATORIO: El número de teléfono REAL del remitente (extraído de [METADATA], ej: 5492634515362). PROHIBIDO inventar.") String phoneNumber,
            @JsonPropertyDescription("Resumen de la propuesta acordada") String proposalSummary) {
            
        UUID expedienteId = resolveExpedienteId(phoneNumber);
        draftRegulatoryAgreementUseCase.draftAlimony(expedienteId, proposalSummary);
        return "El borrador de la propuesta de Convenio Regulador ha sido guardado exitosamente. [NEXT_STEP] El expediente está ahora COMPLETO. Informa al ciudadano de forma muy cálida que el trámite ha concluido esta primera etapa y pasa a revisión de un operador de la Defensoría. LUEGO PREGÚNTALE DIRECTAMENTE: '¿Querés que te envíe ahora un resumen del trámite en PDF?'.";
    }

    @Tool(name = "validate_agreement_legality", description = "Ejecuta validaciones legales preventivas sobre el expediente.")
    public String validateAgreementLegality(
            @JsonPropertyDescription("MANDATORIO: El número de teléfono REAL del remitente (extraído de [METADATA], ej: 5492634515362). PROHIBIDO inventar.") String phoneNumber) {
        try {
            UUID expedienteId = resolveExpedienteId(phoneNumber);
            List<String> validaciones = validateAgreementLegalityUseCase.executeSanityCheck(expedienteId);

            if (validaciones.isEmpty()) {
                return "El análisis de consistencia legal del convenio y el expediente no han detectado alertas técnicas por el momento.";
            }

            StringBuilder sb = new StringBuilder("Atención - El análisis ha detectado las siguientes *inconsistencias legales*:\n");
            validaciones.forEach(v -> sb.append("- ").append(v).append("\n"));
            return sb.toString();
        } catch (Exception e) {
            return "Error ejecutando validación: " + e.getMessage();
        }
    }

    @Tool(name = "generate_referral_summary_pdf", description = "Genera un resumen en PDF para que el ciudadano lo presente en la defensoría presencial.")
    public String generateReferralSummaryPdf(
            @JsonPropertyDescription("MANDATORIO: El número de teléfono REAL del remitente (extraído de [METADATA], ej: 5492634515362). PROHIBIDO inventar.") String phoneNumber) {
        try {
            String pdfPath = generateReferralPdfUseCase.execute(phoneNumber);
            return "El resumen de derivación ha sido generado con éxito. Ubicación del documento: " + pdfPath;
        } catch (Exception e) {
            return "Error generando PDF: " + e.getMessage();
        }
    }

    @Tool(name = "get_pending_tasks", description = "OBLIGATORIO EN CADA INTERACCIÓN. Consulta si existen tareas u observaciones activas asignadas por el operador humano. Si esta herramienta devuelve tareas, el agente DEBE priorizar su resolución por encima de cualquier otro flujo. PROHIBIDO asumir que no hay tareas sin ejecutar esta herramienta primero.")
    public String getPendingTasks(
            @JsonPropertyDescription("MANDATORIO: El número de teléfono REAL del remitente (extraído de [METADATA], ej: 5492634515362). PROHIBIDO inventar.") String phoneNumber) {
        try {
            UUID expedienteId = resolveExpedienteId(phoneNumber);
            List<Observation> observations = manageObservationsUseCase.getObservationsByExpedienteAndStatus(
                    expedienteId, com.lawrabot.divorce_mcp_server.domain.enums.ObservationStatusEnum.ASSIGNED_TO_BOT);
            
            if (observations.isEmpty()) {
                return "No hay tareas o aclaraciones pendientes para este ciudadano.";
            }

            StringBuilder sb = new StringBuilder("📋 *Tareas Pendientes de Resolución*\n\n");
            for (Observation obs : observations) {
                var task = obs.getTask();
                // Una tarea es visible mientras no esté completada o fallada
                if (task != null && task.getStatus() != com.lawrabot.divorce_mcp_server.domain.enums.TaskStatusEnum.COMPLETED 
                        && task.getStatus() != com.lawrabot.divorce_mcp_server.domain.enums.TaskStatusEnum.FAILED) {
                    sb.append("🔹 *Tarea:* ").append(task.getType().name())
                            .append(" (Severidad: ").append(obs.getSeverity().name()).append(")\n");
                    sb.append("• *Campo:* ").append(obs.getFieldName()).append("\n");
                    sb.append("• *Contexto:* ").append(obs.getMessage()).append("\n");
                    var suggestedValue = obs.getSuggestedValue();
                    if (suggestedValue != null && !suggestedValue.isBlank()) {
                        sb.append("• *Valor Sugerido:* ").append(suggestedValue).append("\n");
                    }
                    sb.append("• *Mensaje Sugerido para el ciudadano:* ").append(task.getMessageTemplate()).append("\n");
                    sb.append("• *ID Tarea:* `").append(task.getId()).append("`\n\n");
                }
            }

            if (sb.length() < 50) { // Si solo quedó el header
                return "No hay tareas activas esperando respuesta del ciudadano.";
            }

            sb.append("_Agente: Utiliza el 'Mensaje Sugerido' para pedir la información al ciudadano. Una vez que responda, usa 'complete_observation_task' para cerrar la tarea._");
            return sb.toString();

        } catch (Exception e) {
            return "Error al consultar tareas: " + e.getMessage();
        }
    }

    @Tool(name = "complete_observation_task", description = "Registra la respuesta del ciudadano y completa una tarea de observación pendiente.")
    public String completeObservationTask(
            @JsonPropertyDescription("ID único de la tarea (taskId)") UUID taskId,
            @JsonPropertyDescription("La respuesta o dato proporcionado por el ciudadano") String responseData) {
        try {
            manageObservationsUseCase.markTaskAsCompleted(taskId, responseData);
            return "✅ Tarea completada con éxito. La observación ha sido actualizada para revisión del operador.";
        } catch (Exception e) {
            return "❌ Error al completar la tarea: " + e.getMessage();
        }
    }

    @Tool(name = "submit_digital_evidence", description = "Registra un archivo (foto/PDF) enviado por el ciudadano como evidencia digital.")
    public String submitDigitalEvidence(
            @JsonPropertyDescription("MANDATORIO: El número de teléfono REAL del remitente (extraído de [METADATA], ej: 5492634515362). PROHIBIDO inventar.") String phoneNumber,
            @JsonPropertyDescription("MANDATORIO EXACTO: Usa estrictamente uno de: 'DNI_FRONT', 'DNI_BACK', 'MARRIAGE_CERT', 'BIRTH_CERT', 'INCOME_PROOF' (bono de sueldo o certificado negativo ANSES), 'DISABILITY_CERT' (CUD), 'OTHER'. ¡NO uses descripciones en español!") String documentType,
            @JsonPropertyDescription("Ruta local absoluta del archivo descargado por el agente") String localFilePath,
            @JsonPropertyDescription("Nombre sugerido para el archivo") String fileName,
            @JsonPropertyDescription("Opcional: Nombre completo del hijo al que corresponde el documento (si aplica). Inferilo del mensaje del usuario, ej: 'esta es el acta de Micaela' → 'Micaela Toledo Pereyra'. Si no se puede determinar, dejá null.") @org.springframework.lang.Nullable String childFullName,
            @JsonPropertyDescription("Opcional: ID de la tarea de observación asociada") @org.springframework.lang.Nullable UUID taskId) {
        try {
            UUID expedienteId = resolveExpedienteId(phoneNumber);
            ExpedienteJpaEntity expediente = expedienteRepository.findById(java.util.Objects.requireNonNull(expedienteId))
                    .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado"));

            Path sourcePath = Paths.get(localFilePath);
            if (!Files.exists(sourcePath)) {
                return "Error: No se encontró el archivo en la ruta especificada: " + localFilePath;
            }

            // Crear directorio del expediente si no existe
            Path uploadDir = Paths.get(storagePathBase, expedienteId.toString());
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Normalizar el nombre del archivo
            String finalFileName = (fileName != null && !fileName.isBlank()) ? fileName : sourcePath.getFileName().toString();
            Path targetPath = uploadDir.resolve(finalFileName);

            // Mover el archivo al almacenamiento oficial
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Registrar en base de datos
            String mimeType = Files.probeContentType(targetPath);
            
            // Purgar duplicados: buscar todas las evidencias NO aprobadas de este tipo para este expediente
            List<DigitalEvidenceJpaEntity> others = digitalEvidenceRepository.findByExpediente_IdOrderByCreatedAtDesc(expedienteId).stream()
                .filter(ev -> ev.getDocumentType().equalsIgnoreCase(documentType) && !ev.isApproved())
                .filter(ev -> {
                    // Si es un documento asociado a un hijo, solo purgar si corresponde al mismo hijo
                    if ("BIRTH_CERT".equalsIgnoreCase(documentType) || "DISABILITY_CERT".equalsIgnoreCase(documentType) || "STUDENT_PROOF".equalsIgnoreCase(documentType)) {
                        if (childFullName == null) {
                            return ev.getChildFullName() == null;
                        }
                        return childFullName.equalsIgnoreCase(ev.getChildFullName());
                    }
                    // Para otros documentos generales (matrimonio, ingresos), se purgan siempre
                    return true;
                })
                .collect(Collectors.toList());
            
            DigitalEvidenceJpaEntity evidence;
            if (!others.isEmpty()) {
                // Nos quedamos con la más reciente para actualizarla
                evidence = others.get(0);
                // Borramos cualquier otro duplicado que se haya creado por error
                if (others.size() > 1) {
                    digitalEvidenceRepository.deleteAll(java.util.Objects.requireNonNull(others.subList(1, others.size())));
                    log.info("Purgados {} duplicados de evidencia '{}' para el expediente {}", 
                            others.size() - 1, documentType, expedienteId);
                }
            } else {
                evidence = DigitalEvidenceJpaEntity.builder()
                        .id(UUID.randomUUID())
                        .expediente(expediente)
                        .build();
            }
            
            evidence.setDocumentType(documentType);
            evidence.setFileName(finalFileName);
            evidence.setFilePath(targetPath.toAbsolutePath().toString());
            evidence.setMimeType(mimeType != null ? mimeType : "application/octet-stream");
            evidence.setApproved(false);
            evidence.setChildFullName(childFullName);
            evidence.setCreatedAt(LocalDateTime.now());

            digitalEvidenceRepository.save(evidence);
            
            // Si hay un taskId, completar la tarea automáticamente
            if (taskId != null) {
                try {
                    manageObservationsUseCase.markTaskAsCompleted(taskId, "Documento digital recibido: " + finalFileName);
                    log.info("Tarea {} completada automáticamente tras recibir evidencia", taskId);
                } catch (Exception e) {
                    log.warn("No se pudo completar la tarea {} automáticamente: {}", taskId, e.getMessage());
                }
            }
            
            log.info("Evidencia digital '{}' registrada/actualizada para el expediente {}", documentType, expedienteId);
            
            // Generar instrucción contextual según el tipo de documento recibido
            String nextStep;
            switch (documentType.toUpperCase()) {
                case "MARRIAGE_CERT":
                    Expediente marriageExp = expedienteDomainRepo.findById(expedienteId).orElse(null);
                    if (marriageExp != null && marriageExp.getCollectionStage() == DataCollectionStageEnum.PENDING_MARRIAGE_DETAILS) {
                        marriageExp.updateCollectionStage(DataCollectionStageEnum.PENDING_CHILDREN_INFO);
                        expedienteDomainRepo.save(marriageExp);
                        log.info("Stage avanzado a PENDING_CHILDREN_INFO tras recibir MARRIAGE_CERT para expediente {}", expedienteId);
                    }
                    nextStep = " [NEXT_STEP] Confirma recepción del acta de matrimonio. Luego pregunta: ¿tuvieron hijos en común? NO pidas otro documento todavía.";
                    break;
                case "BIRTH_CERT":
                    Expediente birthExp = expedienteDomainRepo.findById(expedienteId).orElse(null);
                    if (birthExp != null) {
                        checkAndAdvanceChildrenStage(birthExp);
                        birthExp = expedienteDomainRepo.findById(expedienteId).orElse(birthExp);
                        if (birthExp.getCollectionStage() == DataCollectionStageEnum.PENDING_REGULATORY_AGREEMENT) {
                            nextStep = " [NEXT_STEP] Confirma MUY BREVEMENTE la recepción del acta de nacimiento (máximo 1 oración corta, ej: 'Acta de nacimiento recibida ✅'). Todos los documentos cargados correctamente. Avanzado a Convenio Regulador. EN UN BLOQUE SEPARADO con su propia cabecera '## 📝 CONVENIO REGULADOR', pregunta qué propone para el convenio regulador (cuidado personal, cuota alimentaria y bienes).";
                        } else {
                            nextStep = " [NEXT_STEP] Confirma MUY BREVEMENTE la recepción del acta de nacimiento (máximo 1 oración corta, ej: 'Acta de nacimiento recibida ✅'). Solicita cualquier acta de nacimiento, Certificado CUD o alumno regular restante.";
                        }
                    } else {
                        nextStep = " [NEXT_STEP] Acta de nacimiento recibida. Continúa con el flujo.";
                    }
                    break;
                case "DISABILITY_CERT":
                    Expediente disExp = expedienteDomainRepo.findById(expedienteId).orElse(null);
                    if (disExp != null) {
                        checkAndAdvanceChildrenStage(disExp);
                        disExp = expedienteDomainRepo.findById(expedienteId).orElse(disExp);
                        if (disExp.getCollectionStage() == DataCollectionStageEnum.PENDING_REGULATORY_AGREEMENT) {
                            nextStep = " [NEXT_STEP] Certificado de discapacidad (CUD) recibido. Todos los documentos cargados correctamente. Avanzado a Convenio Regulador. EN UN BLOQUE SEPARADO con su propia cabecera '## 📝 CONVENIO REGULADOR', pregunta qué propone para el convenio regulador (cuidado personal, cuota alimentaria y bienes).";
                        } else {
                            nextStep = " [NEXT_STEP] Certificado de discapacidad (CUD) recibido. Solicita cualquier acta de nacimiento o certificado regular de estudios faltante.";
                        }
                    } else {
                        nextStep = " [NEXT_STEP] Certificado de discapacidad (CUD) recibido.";
                    }
                    break;
                case "STUDENT_PROOF":
                    Expediente studExp = expedienteDomainRepo.findById(expedienteId).orElse(null);
                    if (studExp != null) {
                        checkAndAdvanceChildrenStage(studExp);
                        studExp = expedienteDomainRepo.findById(expedienteId).orElse(studExp);
                        if (studExp.getCollectionStage() == DataCollectionStageEnum.PENDING_REGULATORY_AGREEMENT) {
                            nextStep = " [NEXT_STEP] Certificado de alumno regular recibido. Todos los documentos cargados correctamente. Avanzado a Convenio Regulador. EN UN BLOQUE SEPARADO con su propia cabecera '## 📝 CONVENIO REGULADOR', pregunta qué propone para el convenio regulador (cuidado personal, cuota alimentaria y bienes).";
                        } else {
                            nextStep = " [NEXT_STEP] Certificado de alumno regular recibido. Solicita cualquier acta de nacimiento o certificado CUD faltante.";
                        }
                    } else {
                        nextStep = " [NEXT_STEP] Certificado de alumno regular recibido.";
                    }
                    break;
                case "DNI_FRONT":
                    nextStep = " [NEXT_STEP] Confirma recepción del frente del DNI. Pide ahora el dorso del DNI.";
                    break;
                case "DNI_BACK":
                    nextStep = " [NEXT_STEP] Confirma recepción del dorso del DNI. Continúa con la siguiente fase pendiente del trámite.";
                    break;
                case "INCOME_PROOF":
                    // Avanzar el stage del expediente al recibir el documento de ingresos
                    Expediente incomeExp = expedienteDomainRepo.findById(expedienteId).orElse(null);
                    if (incomeExp != null && incomeExp.getCollectionStage() == DataCollectionStageEnum.PENDING_INCOME_PROOF) {
                        incomeExp.updateCollectionStage(DataCollectionStageEnum.PENDING_MARRIAGE_DETAILS);
                        expedienteDomainRepo.save(incomeExp);
                        log.info("Stage avanzado a PENDING_MARRIAGE_DETAILS tras recibir INCOME_PROOF para expediente {}", expedienteId);
                    }
                    nextStep = " [NEXT_STEP] Documento de ingresos recibido. Ahora pide SOLO los datos del matrimonio: fecha de matrimonio, fecha de separación y último domicilio conyugal. NO pidas actas ni datos de hijos todavía.";
                    break;
                default:
                    nextStep = " [NEXT_STEP] Confirma recepción del documento. Continúa con la siguiente fase pendiente del trámite.";
                    break;
            }
            
            return "Documento '" + documentType + "' adjuntado al expediente." + nextStep;
        } catch (IOException e) {
            log.error("Error al procesar archivo de evidencia: {}", e.getMessage());
            return "Error técnico al procesar el archivo: " + e.getMessage();
        } catch (Exception e) {
            log.error("Error al registrar evidencia: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    @Tool(name = "consultar_normativa", description = "Consulta la base de conocimientos legal (CCyC) para responder dudas de derecho de familia.")
    public Mono<String> consultarNormativa(
            @JsonPropertyDescription("MANDATORIO: El número de teléfono REAL del remitente (extraído de [METADATA], ej: 5492634515362). PROHIBIDO inventar.") String phoneNumber,
            @JsonPropertyDescription("La duda legal del usuario (coloquial o técnica)") String query) {
            
        log.info("Tool MCP: consultar_normativa - Query: {}", query);
        
        return Mono.fromCallable(() -> {
            Optional<ExpedienteJpaEntity> expedienteOpt = expedienteRepository.findFirstByPhone(phoneNumber);
            return expedienteOpt.map(this::summarizeCase).orElse("Contexto general.");
        }).subscribeOn(Schedulers.boundedElastic()).flatMap(caseMemory -> {
            if (ragService == null) {
                return Mono.just("El motor de búsqueda legal no está disponible en este entorno.");
            }

            return ragService.searchLegalKnowledge(query).map(legalArticles -> {
                if (legalArticles.isEmpty()) {
                    return "IMPORTANTE: No se han encontrado artículos pertinentes en la base de datos oficial para esta duda específica. Se recomienda consultar con un profesional del MPD.";
                }
                
                StringBuilder output = new StringBuilder();
                output.append("📖 *BASE LEGAL ASOCIADA (Código Civil y Comercial)*\n\n");
                
                for (Document doc : legalArticles) {
                    output.append("*Artículo ").append(doc.getMetadata().get("article_id")).append("*\n");
                    output.append("> ").append(doc.getText()).append("\n\n");
                }
                
                output.append("💡 *ORIENTACIÓN SEGÚN TU CASO*\n");
                output.append("Basado en tu situación: ").append(caseMemory).append("\n\n");
                
                output.append("---\n");
                output.append("⚠️ *ADVERTENCIA*: Esta información es generada por una IA con fines orientativos y no sustituye el asesoramiento legal de un abogado defensor.");
                
                return output.toString();
            });
        });
    }

    private String summarizeCase(ExpedienteJpaEntity e) {
        String base = (e.getDivorceType() != null ? e.getDivorceType().name() : "Desconocido") + " en proceso.";
        if (e.getChildren() != null && !e.getChildren().isEmpty()) {
            base += " Existen hijos menores registrados.";
        }
        return base;
    }

    private String sanitize(String value) {
        if (value == null) return "N/A";
        return value.replace("\n", " ").replace("\r", " ").trim();
    }

    // ==========================================
    // AGENDA Y CITACIONES (Fase 6)
    // ==========================================

    @Tool(name = "get_available_appointment_slots", description = "Consulta turnos disponibles para que el interesado asista presencialmente a firmar la demanda a la Defensoría.")
    public String getAvailableAppointmentSlots(
            @JsonPropertyDescription("Fecha preferida a partir de la cual buscar turnos (YYYY-MM-DD). Si está vacío, busca desde mañana.") @org.springframework.lang.Nullable String preferredDate,
            @JsonPropertyDescription("Cantidad de opciones a ofrecer (por defecto 3)") @org.springframework.lang.Nullable Integer count) {
        
        LocalDate fromDate = (preferredDate != null && !preferredDate.isBlank()) ? LocalDate.parse(preferredDate) : null;
        int slotsCount = (count != null && count > 0) ? count : 3;
        
        List<com.lawrabot.divorce_mcp_server.application.service.AppointmentService.AvailableSlot> slots = 
                appointmentService.getAvailableSlots(slotsCount, fromDate);
                
        if (slots.isEmpty()) {
            return "No hay turnos disponibles para los próximos 30 días.";
        }
        
        StringBuilder sb = new StringBuilder("Turnos disponibles para firma:\n");
        for (int i = 0; i < slots.size(); i++) {
            var slot = slots.get(i);
            sb.append(i + 1).append(") Fecha: ").append(slot.date().toString())
              .append(" - Hora: ").append(slot.startTime().toString())
              .append(" a ").append(slot.endTime().toString()).append("\n");
        }
        sb.append("\n_Agente: Ofrece estas opciones al interesado. Si ninguna le sirve, pídele que proponga una fecha y hora aproximada y usa 'check_appointment_availability'._");
        return sb.toString();
    }

    @Tool(name = "check_appointment_availability", description = "Verifica si una fecha y hora propuesta por el usuario está disponible para firma presencial.")
    public String checkAppointmentAvailability(
            @JsonPropertyDescription("Fecha propuesta (YYYY-MM-DD)") String date,
            @JsonPropertyDescription("Hora propuesta (HH:MM)") String time) {
        
        LocalDateTime dateTime = LocalDateTime.of(LocalDate.parse(date), java.time.LocalTime.parse(time));
        boolean available = appointmentService.isSlotAvailable(dateTime);
        
        if (available) {
            return "El turno solicitado el " + date + " a las " + time + " SÍ está disponible. _Agente: Puedes proceder a reservarlo._";
        } else {
            // Sugerir alternativas cercanas
            List<com.lawrabot.divorce_mcp_server.application.service.AppointmentService.AvailableSlot> alternatives = 
                    appointmentService.findAlternativeSlots(LocalDate.parse(date), java.time.LocalTime.parse(time), 3);
            
            StringBuilder sb = new StringBuilder("El turno solicitado NO está disponible. ");
            if (!alternatives.isEmpty()) {
                sb.append("Opciones cercanas disponibles:\n");
                for (int i = 0; i < alternatives.size(); i++) {
                    var slot = alternatives.get(i);
                    sb.append(i + 1).append(") ").append(slot.date().toString()).append(" a las ").append(slot.startTime().toString()).append("\n");
                }
            }
            return sb.toString();
        }
    }

    @Tool(name = "book_signature_appointment", description = "Reserva un turno de firma presencial para el interesado.")
    public String bookSignatureAppointment(
            @JsonPropertyDescription("MANDATORIO: El número de teléfono REAL del remitente (extraído de [METADATA], ej: 5492634515362). PROHIBIDO inventar.") String phoneNumber,
            @JsonPropertyDescription("Fecha y hora acordada para la reserva (YYYY-MM-DDTHH:MM:SS)") String dateTime) {
        
        UUID expedienteId = resolveExpedienteId(phoneNumber);
        LocalDateTime dt = LocalDateTime.parse(dateTime);
        
        Optional<com.lawrabot.divorce_mcp_server.domain.model.SignatureAppointment> appointment = 
                appointmentService.bookAppointment(expedienteId, dt, phoneNumber);
                
        if (appointment.isPresent()) {
            return "El turno ha sido pre-reservado con éxito para el " + dt.toString().replace("T", " a las ") + 
                   " en la ubicación: " + appointment.get().getLocation() + 
                   "\n\n_Agente: IMPORTANTE. Ahora debes pedir al usuario que confirme su compromiso de asistencia. Una vez que diga que SÍ, usa 'confirm_appointment_commitment' para validar el turno._";
        } else {
            return "No se pudo reservar el turno en esa fecha y hora. Verifica disponibilidad.";
        }
    }

    @Tool(name = "confirm_appointment_commitment", description = "Registra la confirmación expresa del interesado de que asistirá a la cita reservada.")
    public String confirmAppointmentCommitment(
            @JsonPropertyDescription("MANDATORIO: El número de teléfono REAL del remitente (extraído de [METADATA], ej: 5492634515362). PROHIBIDO inventar.") String phoneNumber) {
            
        UUID expedienteId = resolveExpedienteId(phoneNumber);
        Optional<com.lawrabot.divorce_mcp_server.domain.model.SignatureAppointment> active = 
                appointmentService.getActiveAppointment(expedienteId);
                
        if (active.isPresent()) {
            appointmentService.confirmAppointment(active.get().getId());
            
            // Actualizar estado del expediente si estaba en WAITING_SIGNATURE
            Optional<Expediente> exp = expedienteDomainRepo.findById(expedienteId);
            exp.ifPresent(e -> {
                if (e.getStatus() == ExpedienteStatusEnum.WAITING_SIGNATURE) {
                    e.updateStatus(ExpedienteStatusEnum.READY_FOR_PORTAL); // O un nuevo estado "APPOINTMENT_CONFIRMED"
                    expedienteDomainRepo.save(e);
                }
            });
            
            return """
                   ¡Firma agendada! 📅

                   Tu turno para firmar la demanda quedó confirmado para el:

                   **[FECHA Y HORA DEL TURNO]**
                   📍 [LUGAR/DIRECCIÓN DE LA DEFENSORÍA]

                   **IMPORTANTE:** Por favor, no olvides llevar tu DNI original.

                   La Defensoría te espera. ¡Hasta pronto! 😊
                   """;
        } else {
            return "Error: No se encontró ningún turno pre-reservado activo para este usuario.";
        }
    }

    // ==========================================
    // TOOL-GATED STATE MACHINE (Stage Context)
    // ==========================================

    @Tool(name = "get_stage_context", description = "[INTERNAL] Consulta el contexto de fase del expediente para filtrado de herramientas. No la llames directamente.")
    public String getStageContext(
            @JsonPropertyDescription("MANDATORIO: El número de teléfono REAL del remitente.") String phoneNumber) {
        try {
            String normalized = normalizePhone(phoneNumber);
            Optional<Expediente> optExp = expedienteDomainRepo.findActiveByClientPhone(normalized);

            if (optExp.isEmpty()) {
                return "{\"stage\":\"NO_EXPEDIENTE\",\"pendingDocuments\":[],\"allowedTools\":[\"consultar_blsg\",\"start_divorce_process\",\"get_datetime\",\"consultar_normativa\",\"get_stage_context\"]}";
            }

            Expediente exp = optExp.get();
            DataCollectionStageEnum stage = exp.getCollectionStage();
            UUID expedienteId = exp.getId();

            // Consultar documentos cargados
            List<DigitalEvidenceJpaEntity> evidences = digitalEvidenceRepository
                .findByExpediente_IdOrderByCreatedAtDesc(expedienteId);

            boolean hasIncomeProof = evidences.stream()
                .anyMatch(e -> "INCOME_PROOF".equalsIgnoreCase(e.getDocumentType()));
            boolean hasDniFront = evidences.stream()
                .anyMatch(e -> "DNI_FRONT".equalsIgnoreCase(e.getDocumentType()));
            boolean hasDniBack = evidences.stream()
                .anyMatch(e -> "DNI_BACK".equalsIgnoreCase(e.getDocumentType()));
            boolean hasMarriageCert = evidences.stream()
                .anyMatch(e -> "MARRIAGE_CERT".equalsIgnoreCase(e.getDocumentType()));

            // Documentos faltantes según la fase
            List<String> pendingDocs = new java.util.ArrayList<>();
            if (stage == DataCollectionStageEnum.PENDING_INCOME_PROOF && !hasIncomeProof) {
                pendingDocs.add("INCOME_PROOF");
            }

            // Herramientas permitidas según la fase
            List<String> allowedTools = getAllowedToolsForStage(stage, hasIncomeProof);

            StringBuilder sb = new StringBuilder("{");
            sb.append("\"stage\":\"").append(stage.name()).append("\",");
            sb.append("\"hasIncomeProof\":").append(hasIncomeProof).append(",");
            sb.append("\"hasDniFront\":").append(hasDniFront).append(",");
            sb.append("\"hasDniBack\":").append(hasDniBack).append(",");
            sb.append("\"hasMarriageCert\":").append(hasMarriageCert).append(",");
            sb.append("\"pendingDocuments\":[").append(
                pendingDocs.stream().map(d -> "\"" + d + "\"").collect(Collectors.joining(","))
            ).append("],");
            sb.append("\"allowedTools\":[").append(
                allowedTools.stream().map(t -> "\"" + t + "\"").collect(Collectors.joining(","))
            ).append("]}");

            return sb.toString();
        } catch (Exception e) {
            log.error("Error en get_stage_context: {}", e.getMessage());
            return "{\"stage\":\"ERROR\",\"pendingDocuments\":[],\"allowedTools\":[]}";
        }
    }

    /**
     * Determina qué herramientas del dominio están habilitadas para cada fase.
     * Las herramientas utilitarias están SIEMPRE habilitadas.
     */
    private List<String> getAllowedToolsForStage(DataCollectionStageEnum stage, boolean hasIncomeProof) {
        List<String> base = new java.util.ArrayList<>(List.of(
            "get_datetime", "consultar_normativa", "get_pending_tasks",
            "complete_observation_task", "get_dossier_stage", "get_stage_context",
            "submit_digital_evidence", "send_local_file", "generate_pdf"
        ));

        switch (stage) {
            case PENDING_BLSG_SCRAPING:
                base.addAll(List.of("consultar_blsg", "start_divorce_process", "process_scraping_result"));
                break;
            case PENDING_MODALITY_SELECTION:
                base.addAll(List.of("set_divorce_modality", "consultar_blsg_respondent"));
                break;
            case PENDING_RESPONDENT_BLSG:
                base.add("consultar_blsg_respondent");
                break;
            case PENDING_PERSONAL_DATA:
                base.addAll(List.of("submit_petitioner_personal_data", "submit_respondent_personal_data"));
                break;
            case PENDING_SOCIOECONOMIC_EVALUATION:
                base.add("submit_socioeconomic_info");
                break;
            case PENDING_INCOME_PROOF:
                // Solo submit_digital_evidence (ya en base). NO submit_marriage_details.
                // Si ya tiene el documento, habilitar submit_marriage_details
                if (hasIncomeProof) {
                    base.add("submit_marriage_details");
                }
                break;
            case PENDING_MARRIAGE_DETAILS:
                base.add("submit_marriage_details");
                break;
            case PENDING_CHILDREN_INFO:
                base.add("submit_children_info");
                break;
            case PENDING_REGULATORY_AGREEMENT:
                base.addAll(List.of("draft_regulatory_agreement", "validate_agreement_legality",
                    "generate_referral_summary_pdf", "get_available_appointment_slots",
                    "check_appointment_availability", "book_signature_appointment",
                    "confirm_appointment_commitment"));
                break;
            case COMPLETED:
                base.addAll(List.of("generate_referral_summary_pdf", "validate_agreement_legality",
                    "get_available_appointment_slots", "check_appointment_availability",
                    "book_signature_appointment", "confirm_appointment_commitment"));
                break;
            case REJECTED:
                // Solo utilitarias (ya en base)
                break;
        }

        return base;
    }
}