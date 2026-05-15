package com.lawrabot.divorce_mcp_server.application.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import com.lawrabot.divorce_mcp_server.application.port.in.ManageObservationsUseCase;
import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.application.port.out.IObservationRepository;
import com.lawrabot.divorce_mcp_server.application.port.out.ITaskRepository;
import com.lawrabot.divorce_mcp_server.domain.enums.ObservationStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.domain.model.Observation;
import com.lawrabot.divorce_mcp_server.domain.model.Observation.ObservationTask;
import com.lawrabot.divorce_mcp_server.domain.valueobject.PhoneNumberVO;

import lombok.extern.slf4j.Slf4j;

/**
 * Servicio de aplicación que orquesta la gestión de observaciones y tareas.
 * Las observaciones son creadas por operadores humanos y pueden derivar
 * en tareas que LawraBot ejecuta vía WhatsApp.
 */
@Slf4j
public class ObservationService implements ManageObservationsUseCase {

    private final IObservationRepository observationRepository;
    private final ITaskRepository taskRepository;
    private final IExpedienteRepository expedienteRepository;
    private final WebClient.Builder webClientBuilder;
    private final String agentPushUrl;

    public ObservationService(
            IObservationRepository observationRepository,
            ITaskRepository taskRepository,
            IExpedienteRepository expedienteRepository,
            WebClient.Builder webClientBuilder,
            String agentPushUrl) {
        this.observationRepository = observationRepository;
        this.taskRepository = taskRepository;
        this.expedienteRepository = expedienteRepository;
        this.webClientBuilder = webClientBuilder;
        this.agentPushUrl = agentPushUrl;
    }

    @Override
    @Transactional
    public Observation createObservation(CreateObservationCommand command) {
        log.info("Creating observation for expediente {} field {} severity {}",
                command.expedienteId(), command.fieldName(), command.severity());

        // 1. Crear la observación en dominio
        Observation observation = Observation.create(
                command.expedienteId(),
                command.fieldName(),
                command.severity(),
                command.message(),
                command.suggestedValue(),
                command.operatorId()
        );

        // 2. Si createTask = true, generar tarea para LawraBot
        if (command.createTask() && command.taskType() != null) {
            ObservationTask task = observation.createTask(command.taskType(), command.isImmediate());
            log.info("Task {} of type {} (immediate={}) created for observation on field {}",
                    task.getId(), task.getType(), command.isImmediate(), command.fieldName());
        }

        // 3. Persistir (la observación con su tarea asociada)
        Observation saved = observationRepository.save(observation);
        log.info("Observation {} created successfully for expediente {}",
                saved.getId(), command.expedienteId());

        // 4. Si es inmediata, notificar proactivamente al agente DESPUÉS de persistir
        if (command.isImmediate() && saved.getTask() != null) {
            sendProactivePush(command.expedienteId(), saved.getTask());
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Observation> getObservationsByExpediente(UUID expedienteId) {
        return observationRepository.findByExpedienteId(expedienteId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Observation> getObservationsByExpedienteAndStatus(UUID expedienteId, ObservationStatusEnum status) {
        return observationRepository.findByExpedienteIdAndStatus(expedienteId, status);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByExpedienteIdAndStatusIn(UUID expedienteId, List<ObservationStatusEnum> statuses) {
        return observationRepository.countByExpedienteIdAndStatusIn(expedienteId, statuses);
    }

    @Override
    @Transactional
    public Observation resolveObservation(UUID observationId, String notes, String responseData) {
        log.info("Resolving observation {}", observationId);

        Observation observation = observationRepository.findById(observationId)
                .orElseThrow(() -> new ObservationNotFoundException(observationId));

        observation.resolve(notes, responseData);
        Observation saved = observationRepository.save(observation);

        log.info("Observation {} resolved successfully", observationId);
        return saved;
    }

    @Override
    @Transactional
    public Observation dismissObservation(UUID observationId) {
        log.info("Dismissing observation {}", observationId);

        Observation observation = observationRepository.findById(observationId)
                .orElseThrow(() -> new ObservationNotFoundException(observationId));

        observation.dismiss();
        Observation saved = observationRepository.save(observation);

        log.info("Observation {} dismissed", observationId);
        return saved;
    }

    @Override
    @Transactional
    public void markTaskAsCompleted(UUID taskId, String responseData) {
        log.info("Marking task {} as completed", taskId);

        ObservationTask task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        task.complete(responseData);

        // Buscar la observación asociada para actualizarla
        observationRepository.findById(task.getObservationId()).ifPresent(observation -> {
            // Si la observación sigue en ASSIGNED_TO_BOT, la pasamos a RESOLVED automáticamente
            if (observation.getStatus() == ObservationStatusEnum.ASSIGNED_TO_BOT) {
                observation.resolve("Resuelta automáticamente: ciudadano respondió vía LawraBot", responseData);
                observationRepository.save(observation);
                log.info("Observation {} auto-resolved after task completion", observation.getId());
            }
        });

        taskRepository.save(task);
        log.info("Task {} marked as completed", taskId);
    }

    /**
     * Envía una notificación proactiva al agente Node.js para que contacte al ciudadano
     * sin esperar a que éste escriba primero. El mensaje se entrega directamente vía WhatsApp.
     */
    @SuppressWarnings("null")
    private void sendProactivePush(UUID expedienteId, ObservationTask task) {
        Expediente exp = expedienteRepository.findById(expedienteId).orElse(null);
        if (exp == null) {
            log.warn("Cannot send proactive push: Expediente {} not found", expedienteId);
            return;
        }

        PhoneNumberVO phone = exp.getContactPhoneNumber();
        if (phone == null) {
            log.warn("Cannot send proactive push: No phone number in expediente {}", expedienteId);
            return;
        }

        // Usar el formato de WhatsApp (con código de país 549) para el JID
        String phoneNumber = phone.toWhatsAppFormat();
        String message = task.getMessageTemplate();

        log.info("Sending proactive push to {} for task {} (expediente {})",
                phoneNumber, task.getId(), expedienteId);

        // Llamada asíncrona al agente — no bloqueamos la transacción
        webClientBuilder.build()
                .post()
                .uri(agentPushUrl)
                .bodyValue(Map.of(
                        "phoneNumber", java.util.Objects.requireNonNull(phoneNumber),
                        "message", java.util.Objects.requireNonNull(message),
                        "taskId", java.util.Objects.requireNonNull(task.getId().toString())
                ))
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> {
                    log.info("Proactive push delivered successfully for task {}: {}", task.getId(), response);
                    // Marcamos la tarea como enviada
                    task.markAsSent();
                    taskRepository.save(task);
                })
                .subscribe(); // Fire-and-forget: no bloqueamos la respuesta al operador
    }

    @Override
    public void deleteObservation(UUID observationId) {
        log.info("Deleting observation {}", observationId);
        if (!observationRepository.existsById(observationId)) {
            throw new ObservationNotFoundException(observationId);
        }
        observationRepository.deleteById(observationId);
        log.info("Observation {} successfully deleted", observationId);
    }

    /**
     * Excepción cuando no se encuentra una observación.
     */
    public static class ObservationNotFoundException extends RuntimeException {
        public ObservationNotFoundException(UUID id) {
            super("Observation not found with id: " + id);
        }
    }

    /**
     * Excepción cuando no se encuentra una tarea.
     */
    public static class TaskNotFoundException extends RuntimeException {
        public TaskNotFoundException(UUID id) {
            super("Task not found with id: " + id);
        }
    }
}