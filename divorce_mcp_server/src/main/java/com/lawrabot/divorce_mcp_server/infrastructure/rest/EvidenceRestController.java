package com.lawrabot.divorce_mcp_server.infrastructure.rest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.lawrabot.divorce_mcp_server.application.port.out.IEvidenceStoragePort;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.DigitalEvidenceJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.ExpedienteJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa.SpringDataDigitalEvidenceRepository;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa.SpringDataExpedienteRepository;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/divorce/evidence")
@CrossOrigin(origins = "*", methods = { RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS })
public class EvidenceRestController {

    private static final Logger log = LoggerFactory.getLogger(EvidenceRestController.class);

    private final SpringDataDigitalEvidenceRepository evidenceRepository;
    private final SpringDataExpedienteRepository expedienteRepository;
    private final IEvidenceStoragePort storagePort;

    public EvidenceRestController(
            SpringDataDigitalEvidenceRepository evidenceRepository,
            SpringDataExpedienteRepository expedienteRepository,
            IEvidenceStoragePort storagePort) {
        this.evidenceRepository = evidenceRepository;
        this.expedienteRepository = expedienteRepository;
        this.storagePort = storagePort;
    }

    @SuppressWarnings("null")
    @PostMapping(value = "/{expedienteId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<Object>> uploadEvidence(
            @PathVariable UUID expedienteId,
            @RequestPart("file") FilePart filePart,
            @RequestPart("documentType") String documentType) {

        log.info("REST: Uploading evidence '{}' for expediente: {}", filePart.filename(), expedienteId);

        return Mono.fromCallable(() -> {
            ExpedienteJpaEntity expediente = expedienteRepository.findById(Objects.requireNonNull(expedienteId)).orElse(null);
            if (expediente == null) {
                return null;
            }
            return expediente;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(expediente -> {
            if (expediente == null) {
                return Mono.just(ResponseEntity.notFound().build());
            }

            String originalFileName = Objects.requireNonNull(filePart.filename());
            MediaType contentType = filePart.headers().getContentType();

            // First Save Physical File to Storage Port
            return storagePort.saveEvidence(expediente.getId(), filePart, originalFileName)
                    .flatMap(absolutePath -> Mono.fromCallable(() -> {
                        DigitalEvidenceJpaEntity evidence = DigitalEvidenceJpaEntity.builder()
                                .id(UUID.randomUUID())
                                .expediente(expediente)
                                .documentType(documentType)
                                .fileName(originalFileName)
                                .filePath(absolutePath)
                                .mimeType(contentType != null ? contentType.toString() : "application/octet-stream")
                                .approved(false)
                                .createdAt(LocalDateTime.now())
                                .build();

                        evidenceRepository.save(evidence);
                        return ResponseEntity.ok().build();
                    }).subscribeOn(Schedulers.boundedElastic()));
        });
    }

    @GetMapping("/{expedienteId}")
    public Mono<ResponseEntity<List<DigitalEvidenceJpaEntity>>> getEvidences(@PathVariable UUID expedienteId) {
        return Mono.fromCallable(() -> {
            List<DigitalEvidenceJpaEntity> evidences = evidenceRepository
                    .findByExpediente_IdOrderByCreatedAtDesc(Objects.requireNonNull(expedienteId));
            return ResponseEntity.ok(evidences);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/download/{evidenceId}")
    public Mono<ResponseEntity<Resource>> downloadEvidence(@PathVariable UUID evidenceId) {
        return Mono.fromCallable(() -> evidenceRepository.findById(Objects.requireNonNull(evidenceId)).orElse(null))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(evidence -> {
                    if (evidence == null) {
                        return Mono.just(ResponseEntity.notFound().build());
                    }

                    return storagePort.loadEvidence(evidence.getFilePath())
                            .map(resource -> {
                                String mimeType = evidence.getMimeType() != null ? evidence.getMimeType() : "application/octet-stream";
                                return ResponseEntity.ok()
                                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + evidence.getFileName() + "\"")
                                        .contentType(MediaType.parseMediaType(Objects.requireNonNull(mimeType)))
                                        .body(resource);
                            })
                            .onErrorResume(e -> {
                                log.error("Failed to read resource file for evidence {}", evidence.getId(), e);
                                return Mono.just(ResponseEntity.notFound().build());
                            });
                });
    }

    @PutMapping("/{evidenceId}/status")
    public Mono<ResponseEntity<DigitalEvidenceJpaEntity>> updateStatus(
            @PathVariable UUID evidenceId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String reason) {
        return Mono.fromCallable(() -> evidenceRepository.findById(Objects.requireNonNull(evidenceId)).map(ev -> {
            ev.setApproved(approved);
            ev.setRejectionReason(approved ? null : reason);
            return ResponseEntity.ok(evidenceRepository.save(ev));
        }).orElse(ResponseEntity.notFound().build())).subscribeOn(Schedulers.boundedElastic());
    }

    @DeleteMapping("/{evidenceId}")
    public Mono<ResponseEntity<Void>> deleteEvidence(@PathVariable UUID evidenceId) {
        log.info("REST: Deleting evidence: {}", evidenceId);

        return Mono.fromCallable(() -> evidenceRepository.findById(Objects.requireNonNull(evidenceId)).orElse(null))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(evidence -> {
                    if (evidence == null) {
                        return Mono.just(ResponseEntity.notFound().<Void>build());
                    }

                    return storagePort.deleteEvidence(evidence.getFilePath())
                            .then(Mono.fromCallable(() -> {
                                evidenceRepository.delete(evidence);
                                return ResponseEntity.noContent().<Void>build();
                            }).subscribeOn(Schedulers.boundedElastic()));
                });
    }

    @PutMapping("/{evidenceId}/type")
    public Mono<ResponseEntity<DigitalEvidenceJpaEntity>> updateDocumentType(
            @PathVariable UUID evidenceId,
            @RequestParam String documentType) {
        log.info("REST: Changing document type for evidence: {} to {}", evidenceId, documentType);
        return Mono.fromCallable(() -> {
            DigitalEvidenceJpaEntity evidence = evidenceRepository.findById(Objects.requireNonNull(evidenceId)).orElse(null);
            if (evidence == null) return ResponseEntity.notFound().<DigitalEvidenceJpaEntity>build();

            evidence.setDocumentType(documentType);
            return ResponseEntity.ok(evidenceRepository.save(evidence));
        }).subscribeOn(Schedulers.boundedElastic());
    }
}