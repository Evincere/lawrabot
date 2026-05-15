package com.lawrabot.divorce_mcp_server.infrastructure.rest;

import com.lawrabot.divorce_mcp_server.application.service.AppointmentService;
import com.lawrabot.divorce_mcp_server.domain.enums.AppointmentStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.model.SignatureAppointment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/divorce/appointments")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class AppointmentRestController {

    private final AppointmentService appointmentService;

    @GetMapping("/slots")
    public ResponseEntity<List<AppointmentService.AvailableSlot>> getAvailableSlots(
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
        log.info("REST: Obteniendo slots disponibles. count={}, fromDate={}", count, fromDate);
        return ResponseEntity.ok(appointmentService.getAvailableSlots(count, fromDate));
    }

    @GetMapping("/case/{expedienteId}")
    public ResponseEntity<List<SignatureAppointment>> getAppointmentsByCase(@PathVariable UUID expedienteId) {
        log.info("REST: Obteniendo citas para expediente {}", expedienteId);
        return ResponseEntity.ok(appointmentService.getAppointmentsByExpediente(expedienteId));
    }

    @GetMapping("/case/{expedienteId}/active")
    public ResponseEntity<SignatureAppointment> getActiveAppointment(@PathVariable UUID expedienteId) {
        log.info("REST: Obteniendo cita activa para expediente {}", expedienteId);
        return appointmentService.getActiveAppointment(expedienteId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<SignatureAppointment> bookAppointment(@RequestBody BookAppointmentRequest request) {
        log.info("REST: Reservando cita para expediente {} en {}", request.expedienteId(), request.dateTime());
        return appointmentService.bookAppointment(request.expedienteId(), request.dateTime(), request.contactPhone())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().build());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<SignatureAppointment> updateAppointmentStatus(
            @PathVariable UUID id,
            @RequestBody UpdateStatusRequest request) {
        log.info("REST: Actualizando estado de cita {} a {}", id, request.status());
        return appointmentService.updateStatus(id, request.status())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelAppointment(@PathVariable UUID id) {
        log.info("REST: Cancelando cita {}", id);
        if (appointmentService.cancelAppointment(id)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping
    public ResponseEntity<List<SignatureAppointment>> getAppointmentsInRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        log.info("REST: Obteniendo citas desde {} hasta {}", from, to);
        return ResponseEntity.ok(appointmentService.getAppointmentsInRange(from, to));
    }

    // DTOs
    public record BookAppointmentRequest(
            UUID expedienteId,
            LocalDateTime dateTime,
            String contactPhone
    ) {}

    public record UpdateStatusRequest(
            AppointmentStatusEnum status
    ) {}
}
