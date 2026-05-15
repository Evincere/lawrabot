package com.lawrabot.divorce_mcp_server.infrastructure.rest;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lawrabot.divorce_mcp_server.application.port.in.ManageObservationsUseCase;
import com.lawrabot.divorce_mcp_server.application.port.in.ManageObservationsUseCase.CreateObservationCommand;
import com.lawrabot.divorce_mcp_server.domain.enums.ObservationStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Observation;
import com.lawrabot.divorce_mcp_server.infrastructure.rest.dto.CreateObservationRequest;
import com.lawrabot.divorce_mcp_server.infrastructure.rest.dto.ObservationResponseDTO;
import com.lawrabot.divorce_mcp_server.infrastructure.rest.dto.ResolveObservationRequest;
import com.lawrabot.divorce_mcp_server.infrastructure.rest.dto.TaskCompletionRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * REST Controller para la gestión de observaciones del operador humano.
 * Permite crear, consultar, resolver y descartar observaciones sobre expedientes,
 * así como marcar tareas de LawraBot como completadas.
 */
@RestController
@RequestMapping("/api/divorce/observations")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class ObservationRestController {

    private final ManageObservationsUseCase manageObservationsUseCase;

    @PostMapping
    public Mono<ResponseEntity<ObservationResponseDTO>> createObservation(
            @RequestBody CreateObservationRequest request) {
        log.info("REST: Creating observation for expediente {}", request.getExpedienteId());

        return Mono.fromCallable(() -> {
            UUID operatorUuid = null;
            if (request.getOperatorId() != null && !request.getOperatorId().isBlank()) {
                try {
                    operatorUuid = UUID.fromString(request.getOperatorId());
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid operatorId format: {}. Using null.", request.getOperatorId());
                }
            }

            CreateObservationCommand command = new CreateObservationCommand(
                    request.getExpedienteId(),
                    request.getFieldName(),
                    request.getSeverity(),
                    request.getMessage(),
                    request.getSuggestedValue(),
                    request.isCreateTask(),
                    request.getTaskType(),
                    request.isImmediate(),
                    operatorUuid
            );

            Observation observation = manageObservationsUseCase.createObservation(command);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ObservationResponseDTO.fromDomain(observation));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/expediente/{expedienteId}")
    public Mono<ResponseEntity<List<ObservationResponseDTO>>> getObservationsByExpediente(
            @PathVariable UUID expedienteId) {
        log.info("REST: Fetching observations for expediente {}", expedienteId);

        return Mono.fromCallable(() -> {
            List<ObservationResponseDTO> dtos = manageObservationsUseCase
                    .getObservationsByExpediente(expedienteId)
                    .stream()
                    .map(ObservationResponseDTO::fromDomain)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/expediente/{expedienteId}/status/{status}")
    public Mono<ResponseEntity<List<ObservationResponseDTO>>> getObservationsByStatus(
            @PathVariable UUID expedienteId,
            @PathVariable ObservationStatusEnum status) {
        log.info("REST: Fetching observations for expediente {} with status {}", expedienteId, status);

        return Mono.fromCallable(() -> {
            List<ObservationResponseDTO> dtos = manageObservationsUseCase
                    .getObservationsByExpedienteAndStatus(expedienteId, status)
                    .stream()
                    .map(ObservationResponseDTO::fromDomain)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(dtos);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @GetMapping("/expediente/{expedienteId}/count")
    public Mono<ResponseEntity<Long>> countObservations(
            @PathVariable UUID expedienteId,
            @RequestParam(required = false) List<ObservationStatusEnum> statuses) {
        return Mono.fromCallable(() -> {
            // Si no se envían estados, contamos pendientes y asignadas por defecto
            List<ObservationStatusEnum> statusList = (statuses == null || statuses.isEmpty())
                ? List.of(ObservationStatusEnum.PENDING, ObservationStatusEnum.ASSIGNED_TO_BOT)
                : statuses;

            long count = manageObservationsUseCase.countByExpedienteIdAndStatusIn(expedienteId, statusList);
            return ResponseEntity.ok(count);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/{observationId}/resolve")
    public Mono<ResponseEntity<ObservationResponseDTO>> resolveObservation(
            @PathVariable UUID observationId,
            @RequestBody ResolveObservationRequest request) {
        log.info("REST: Resolving observation {}", observationId);

        return Mono.fromCallable(() -> {
            Observation observation = manageObservationsUseCase.resolveObservation(
                    observationId, request.getNotes(), request.getResponseData());
            return ResponseEntity.ok(ObservationResponseDTO.fromDomain(observation));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @PutMapping("/{observationId}/dismiss")
    public Mono<ResponseEntity<ObservationResponseDTO>> dismissObservation(
            @PathVariable UUID observationId) {
        log.info("REST: Dismissing observation {}", observationId);

        return Mono.fromCallable(() -> {
            Observation observation = manageObservationsUseCase.dismissObservation(observationId);
            return ResponseEntity.ok(ObservationResponseDTO.fromDomain(observation));
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/{observationId}")
    public Mono<ResponseEntity<Void>> deleteObservation(
            @PathVariable UUID observationId) {
        log.info("REST: Deleting observation {}", observationId);

        return Mono.fromCallable(() -> {
            manageObservationsUseCase.deleteObservation(observationId);
            return ResponseEntity.ok().<Void>build();
        }).subscribeOn(Schedulers.boundedElastic()).thenReturn(ResponseEntity.ok().build());
    }

    @PostMapping("/tasks/{taskId}/complete")
    public Mono<ResponseEntity<Void>> completeTask(
            @PathVariable UUID taskId,
            @RequestBody TaskCompletionRequest request) {
        log.info("REST: Marking task {} as completed", taskId);

        return Mono.fromCallable(() -> {
            manageObservationsUseCase.markTaskAsCompleted(taskId, request.getResponseData());
            return ResponseEntity.ok().<Void>build();
        }).subscribeOn(Schedulers.boundedElastic()).thenReturn(ResponseEntity.ok().build());
    }
}