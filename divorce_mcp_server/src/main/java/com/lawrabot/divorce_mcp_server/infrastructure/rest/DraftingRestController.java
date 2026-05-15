package com.lawrabot.divorce_mcp_server.infrastructure.rest;
 
import com.lawrabot.divorce_mcp_server.application.service.DocumentGeneratorService;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.ExpedienteJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.SpouseJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.service.FileWatcherService;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa.SpringDataExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.enums.ExpedienteStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
 
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
 
@RestController
@RequestMapping("/api/divorce/drafting")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class DraftingRestController {
 
    private final DocumentGeneratorService documentGeneratorService;
    private final SpringDataExpedienteRepository expedienteRepository;
    private final FileWatcherService fileWatcherService;
 
    @PostMapping("/generate/{expedienteId}")
    public Mono<ResponseEntity<Resource>> generateDemandaUnilateral(
            @PathVariable UUID expedienteId,
            @RequestParam(defaultValue = "true") boolean asPdf) {
        
        log.info("REST: Solicitando generación de demanda. Expediente: {}", expedienteId);
        
        return Mono.<ResponseEntity<Resource>>fromCallable(() -> {
            ExpedienteJpaEntity expediente = expedienteRepository.findByIdWithChildren(Objects.requireNonNull(expedienteId)).orElse(null);
            if (expediente == null) {
                return ResponseEntity.notFound().build();
            }
 
            String templateName = expediente.getDivorceType() == com.lawrabot.divorce_mcp_server.domain.enums.DivorceTypeEnum.JOINT
                ? "modelo_demanda_divorcio_bilateral.docx"
                : "modelo_demanda_divorcio_unilateral.docx";
            
            String templatePath = "./templates/" + templateName;
            
            // Build injection context
            Map<String, Object> context = buildExpedienteContext(expediente);
            
            String generatedPathStr = documentGeneratorService.generateDocument(
                    templatePath, 
                    expediente.getId().toString(), 
                    "Demanda_Divorcio", 
                    context, 
                    asPdf
            );
 
            Path filePath = Paths.get(generatedPathStr);
            Resource resource = new UrlResource(Objects.requireNonNull(filePath.toUri()));
 
            // NUEVO: Registrar en el FileWatcher y actualizar estado
            fileWatcherService.registerExpedienteForWatching(expediente.getId().toString());
            if (expediente.getStatus() != ExpedienteStatusEnum.WAITING_SIGNATURE && expediente.getStatus() != ExpedienteStatusEnum.READY_FOR_PORTAL) {
                expediente.setStatus(ExpedienteStatusEnum.WAITING_SIGNATURE);
                expedienteRepository.save(expediente);
            }
 
            if (resource.exists() && resource.isReadable()) {
                String contentType = asPdf 
                    ? "application/pdf" 
                    : "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filePath.getFileName().toString() + "\"")
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            }
            return ResponseEntity.internalServerError().build();
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(e -> {
            log.error("Generation failed", e);
            return Mono.just(ResponseEntity.internalServerError().build());
        });
    }
     private Map<String, Object> buildExpedienteContext(ExpedienteJpaEntity exp) {
        Map<String, Object> ctx = new HashMap<>();
        
        ctx.put("expedienteId", exp.getId().toString());
        ctx.put("tipoPresentacion", exp.getDivorceType() == com.lawrabot.divorce_mcp_server.domain.enums.DivorceTypeEnum.JOINT ? "BILATERAL" : "UNILATERAL");
        
        // Defensoría (hardcoded MVP)
        ctx.put("defensora_nombre", "MARIA JORGELINA BAYÓN");
        ctx.put("despacho_direccion", "E. Civit N° 257");

        ctx.put("fecha_matrimonio_texto", exp.getMarriageDate() != null ? exp.getMarriageDate().toString() : "S/D");
        ctx.put("fecha_separacion", exp.getDeFactoSeparationDate() != null ? exp.getDeFactoSeparationDate().toString() : "S/D");
        
        // Datos del acta
        ctx.put("acta_numero", exp.getMarriageCertificateNumber() != null ? exp.getMarriageCertificateNumber() : "S/D");
        ctx.put("libro_registro", exp.getMarriageRegistryBook() != null ? exp.getMarriageRegistryBook() : "S/D");
        ctx.put("anio_matrimonio", exp.getMarriageDate() != null ? String.valueOf(exp.getMarriageDate().getYear()) : "S/D");
        ctx.put("foja", exp.getMarriageRegistryPage() != null ? exp.getMarriageRegistryPage() : "S/D");
        ctx.put("oficina_denominada", exp.getMarriageRegistryOffice() != null ? exp.getMarriageRegistryOffice() : "S/D");
        ctx.put("lugar_matrimonio", exp.getMarriagePlace() != null ? exp.getMarriagePlace() : "S/D");

        // Domicilio conyugal
        String conjStreet = exp.getLastConjugalResidence() != null && exp.getLastConjugalResidence().getStreet() != null ? exp.getLastConjugalResidence().getStreet() : "";
        String conjNumber = exp.getLastConjugalResidence() != null && exp.getLastConjugalResidence().getNumber() != null ? exp.getLastConjugalResidence().getNumber() : "";
        String conjLocality = exp.getLastConjugalResidence() != null && exp.getLastConjugalResidence().getLocality() != null ? exp.getLastConjugalResidence().getLocality() : "";
        ctx.put("domicilio_conyugal", (conjStreet + " " + conjNumber + ", " + conjLocality).trim());

        // Peticionante
        if (exp.getPetitioner() != null) {
            SpouseJpaEntity petitioner = exp.getPetitioner();
            String firstName = petitioner.getName() != null && petitioner.getName().getFirstName() != null ? petitioner.getName().getFirstName() : "";
            String lastName  = petitioner.getName() != null && petitioner.getName().getLastName()  != null ? petitioner.getName().getLastName()  : "";
            ctx.put("peticionante_nombre", (firstName + " " + lastName).trim());
            ctx.put("peticionante_dni", petitioner.getDni() != null ? petitioner.getDni() : "S/D");
            ctx.put("peticionante_nacionalidad", petitioner.getNationality() != null ? petitioner.getNationality() : "argentina/o");
            
            // Edad aproximada
            int edad = 0;
            if (petitioner.getBirthDate() != null) {
                edad = java.time.Period.between(petitioner.getBirthDate(), java.time.LocalDate.now()).getYears();
            }
            ctx.put("peticionante_edad", edad > 0 ? String.valueOf(edad) : "S/D");
            ctx.put("peticionante_profesion", petitioner.getProfession() != null ? petitioner.getProfession() : "S/D");
            
            String street = petitioner.getResidentialAddress() != null && petitioner.getResidentialAddress().getStreet() != null ? petitioner.getResidentialAddress().getStreet() : "";
            String number = petitioner.getResidentialAddress() != null && petitioner.getResidentialAddress().getNumber() != null ? petitioner.getResidentialAddress().getNumber() : "";
            String locality = petitioner.getResidentialAddress() != null && petitioner.getResidentialAddress().getLocality() != null ? petitioner.getResidentialAddress().getLocality() : "San Rafael, Mendoza";
            ctx.put("peticionante_domicilio", (street + " " + number + ", " + locality).trim());
            ctx.put("peticionante_celular", petitioner.getPhoneNumber() != null ? petitioner.getPhoneNumber() : "S/D");
            ctx.put("peticionante_email", petitioner.getEmail() != null ? petitioner.getEmail() : "S/D");
        } else {
            ctx.put("peticionante_nombre", "[NOMBRE TITULAR]");
            ctx.put("peticionante_dni", "[DNI TITULAR]");
            ctx.put("peticionante_nacionalidad", "[NACIONALIDAD]");
            ctx.put("peticionante_edad", "[EDAD]");
            ctx.put("peticionante_profesion", "[PROFESION]");
            ctx.put("peticionante_domicilio", "[DOMICILIO]");
            ctx.put("peticionante_celular", "[CELULAR]");
            ctx.put("peticionante_email", "[EMAIL]");
        }
  
        // Demandado
        if (exp.getRespondent() != null) {
            SpouseJpaEntity respondent = exp.getRespondent();
            String firstName = respondent.getName() != null && respondent.getName().getFirstName() != null ? respondent.getName().getFirstName() : "";
            String lastName  = respondent.getName() != null && respondent.getName().getLastName()  != null ? respondent.getName().getLastName()  : "";
            ctx.put("demandado_nombre", (firstName + " " + lastName).trim());
            ctx.put("demandado_dni", respondent.getDni() != null ? respondent.getDni() : "S/D");
            ctx.put("demandado_nacionalidad", respondent.getNationality() != null ? respondent.getNationality() : "argentina/o");
            
            int edad = 0;
            if (respondent.getBirthDate() != null) {
                edad = java.time.Period.between(respondent.getBirthDate(), java.time.LocalDate.now()).getYears();
            }
            ctx.put("demandado_edad", edad > 0 ? String.valueOf(edad) : "S/D");
            ctx.put("demandado_profesion", respondent.getProfession() != null ? respondent.getProfession() : "S/D");
            
            String street = respondent.getResidentialAddress() != null && respondent.getResidentialAddress().getStreet() != null ? respondent.getResidentialAddress().getStreet() : "";
            String number = respondent.getResidentialAddress() != null && respondent.getResidentialAddress().getNumber() != null ? respondent.getResidentialAddress().getNumber() : "";
            String locality = respondent.getResidentialAddress() != null && respondent.getResidentialAddress().getLocality() != null ? respondent.getResidentialAddress().getLocality() : "San Rafael, Mendoza";
            ctx.put("demandado_domicilio", (street + " " + number + ", " + locality).trim());
            ctx.put("demandado_celular", respondent.getPhoneNumber() != null ? respondent.getPhoneNumber() : "S/D");
            ctx.put("demandado_email", respondent.getEmail() != null ? respondent.getEmail() : "S/D");
        } else {
            ctx.put("demandado_nombre", "[NOMBRE DEMANDADO]");
            ctx.put("demandado_dni", "[DNI DEMANDADO]");
            ctx.put("demandado_nacionalidad", "[NACIONALIDAD]");
            ctx.put("demandado_edad", "[EDAD]");
            ctx.put("demandado_profesion", "[PROFESION]");
            ctx.put("demandado_domicilio", "[DOMICILIO]");
            ctx.put("demandado_celular", "[CELULAR]");
            ctx.put("demandado_email", "[EMAIL]");
        }

        // Hijos
        ctx.put("tiene_hijos_menores", exp.getChildren() != null && !exp.getChildren().isEmpty() ? "SI" : "NO");
        if (exp.getChildren() != null && !exp.getChildren().isEmpty()) {
            StringBuilder hijosStr = new StringBuilder();
            for (int i = 0; i < exp.getChildren().size(); i++) {
                var child = exp.getChildren().get(i);
                String childName = child.getFirstName() != null ? (child.getFirstName() + " " + child.getLastName()).trim() : "S/D";
                int edad = 0;
                if (child.getBirthDate() != null) {
                    edad = java.time.Period.between(child.getBirthDate(), java.time.LocalDate.now()).getYears();
                }
                hijosStr.append(childName).append(" de ").append(edad).append(" años de edad");
                if (i < exp.getChildren().size() - 1) {
                    hijosStr.append(" y ");
                }
            }
            ctx.put("lista_hijos", hijosStr.toString());
            ctx.put("cantidad_hijos", String.valueOf(exp.getChildren().size()));
        }

        // Convenio
        ctx.put("texto_convenio_crudo", exp.getRawAgreementText() != null ? exp.getRawAgreementText() : "Sin propuesta de convenio declarada");
        
        // El texto crudo actual en DB suele contener todo el convenio (muebles, inmuebles, etc.)
        // Para simplificar la inyección en el template, mapeamos el texto crudo a la sección pertinente
        ctx.put("texto_convenio_inmuebles", "Que durante la unión matrimonial los cónyuges no han adquirido ningún inmueble en común, razón por la cual no corresponde efectuar propuesta alguna respecto de este rubro.");
        ctx.put("texto_convenio_muebles", "Los bienes muebles y útiles del hogar fueron repartidos al momento de la separación, razón por la cual no hay propuesta alguna respecto a este rubro.");
        ctx.put("texto_responsabilidad_parental", exp.getRawAgreementText() != null ? exp.getRawAgreementText() : "Se propone un régimen de cuidado compartido.");
        
        // Evidencias (Documental)
        // Actualmente no hay JPA entity vinculada directamente a `expediente` para digital evidence aquí,
        // así que inyectamos los básicos:
        ctx.put("lista_prueba_documental", "1.- Copia DNIs\n2.- Acta de matrimonio actualizada.\n3.- Actas de nacimiento de los menores.");
        
        return ctx;
    }
}
