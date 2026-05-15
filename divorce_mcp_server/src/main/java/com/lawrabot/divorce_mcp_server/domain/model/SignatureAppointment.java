package com.lawrabot.divorce_mcp_server.domain.model;

import com.lawrabot.divorce_mcp_server.domain.enums.AppointmentStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modelo de dominio que representa un turno de firma presencial
 * del interesado en la Defensoría Oficial.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignatureAppointment {

    private UUID id;
    private UUID expedienteId;
    private LocalDateTime appointmentDateTime;

    // Ubicación de la cita (ej: "Defensoría Oficial Civil - E. Civit 257, San Rafael")
    private String location;

    // Número de teléfono del interesado para notificación
    private String contactPhone;

    // Estado del turno
    private AppointmentStatusEnum status;

    // Si LawraBot ya notificó al interesado
    private boolean notifiedByBot;

    // Observaciones del operador o del sistema
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ============================================
    // LÓGICA DE NEGOCIO
    // ============================================

    public static SignatureAppointment schedule(UUID expedienteId, LocalDateTime dateTime,
                                                 String location, String contactPhone) {
        LocalDateTime now = LocalDateTime.now();
        return SignatureAppointment.builder()
                .id(UUID.randomUUID())
                .expedienteId(expedienteId)
                .appointmentDateTime(dateTime)
                .location(location)
                .contactPhone(contactPhone)
                .status(AppointmentStatusEnum.SCHEDULED)
                .notifiedByBot(false)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    public void confirm() {
        this.status = AppointmentStatusEnum.CONFIRMED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markNotifiedByBot() {
        this.notifiedByBot = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = AppointmentStatusEnum.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void cancel() {
        this.status = AppointmentStatusEnum.CANCELLED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markNoShow() {
        this.status = AppointmentStatusEnum.NO_SHOW;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return status == AppointmentStatusEnum.SCHEDULED
            || status == AppointmentStatusEnum.CONFIRMED;
    }
}
