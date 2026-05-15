package com.lawrabot.divorce_mcp_server.application.service;

import com.lawrabot.divorce_mcp_server.domain.enums.AppointmentStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.model.SignatureAppointment;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.SignatureAppointmentJpaEntity;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.SignatureAppointmentJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Servicio de gestión inteligente de agenda para turnos de firma presencial.
 * Genera slots disponibles, verifica disponibilidad y gestiona reservas.
 */
@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final SignatureAppointmentJpaRepository appointmentRepository;

    // Configuración de agenda — valores por defecto para MVP
    @Value("${lawrabot.agenda.start-hour:08}")
    private int startHour;

    @Value("${lawrabot.agenda.start-minute:00}")
    private int startMinute;

    @Value("${lawrabot.agenda.end-hour:13}")
    private int endHour;

    @Value("${lawrabot.agenda.end-minute:00}")
    private int endMinute;

    @Value("${lawrabot.agenda.slot-duration-minutes:30}")
    private int slotDurationMinutes;

    @Value("${lawrabot.agenda.buffer-between-minutes:10}")
    private int bufferBetweenMinutes;

    @Value("${lawrabot.agenda.max-days-ahead:30}")
    private int maxDaysAhead;

    @Value("${lawrabot.agenda.default-location:Defensoría Oficial Civil - E. Civit 257, San Rafael, Mendoza}")
    private String defaultLocation;

    public AppointmentService(SignatureAppointmentJpaRepository appointmentRepository) {
        this.appointmentRepository = appointmentRepository;
    }

    /**
     * Obtiene los próximos N slots disponibles a partir de una fecha.
     * Excluye fines de semana y slots ya ocupados.
     *
     * @param count    Cantidad de slots a devolver (ej: 3)
     * @param fromDate Fecha de inicio de búsqueda (null = mañana)
     * @return Lista de slots disponibles con fecha y hora
     */
    public List<AvailableSlot> getAvailableSlots(int count, LocalDate fromDate) {
        if (fromDate == null) {
            fromDate = LocalDate.now().plusDays(1);
        }

        List<AvailableSlot> available = new ArrayList<>();
        LocalDate maxDate = LocalDate.now().plusDays(maxDaysAhead);
        LocalDate currentDate = fromDate;

        while (available.size() < count && !currentDate.isAfter(maxDate)) {
            // Saltar fines de semana
            DayOfWeek dow = currentDate.getDayOfWeek();
            if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
                currentDate = currentDate.plusDays(1);
                continue;
            }

            // Generar slots para este día
            LocalTime slotTime = LocalTime.of(startHour, startMinute);
            LocalTime endTime = LocalTime.of(endHour, endMinute);

            while (slotTime.plusMinutes(slotDurationMinutes).compareTo(endTime) <= 0 && available.size() < count) {
                LocalDateTime slotDateTime = LocalDateTime.of(currentDate, slotTime);

                // Verificar que el slot está en el futuro
                if (slotDateTime.isAfter(LocalDateTime.now())) {
                    // Verificar disponibilidad
                    LocalDateTime slotEnd = slotDateTime.plusMinutes(slotDurationMinutes);
                    long occupied = appointmentRepository.countActiveInRange(slotDateTime, slotEnd);
                    if (occupied == 0) {
                        available.add(new AvailableSlot(currentDate, slotTime, slotTime.plusMinutes(slotDurationMinutes)));
                    }
                }

                slotTime = slotTime.plusMinutes(slotDurationMinutes + bufferBetweenMinutes);
            }

            currentDate = currentDate.plusDays(1);
        }

        return available;
    }

    /**
     * Busca slots disponibles cercanos a una fecha y hora preferida por el usuario.
     * Para cuando el usuario propone una alternativa que no está disponible.
     */
    public List<AvailableSlot> findAlternativeSlots(LocalDate nearDate, LocalTime nearTime, int count) {
        // Buscar desde un día antes de la fecha preferida, priorizando cercanía
        LocalDate searchFrom = nearDate.minusDays(1);
        if (searchFrom.isBefore(LocalDate.now().plusDays(1))) {
            searchFrom = LocalDate.now().plusDays(1);
        }

        List<AvailableSlot> all = getAvailableSlots(count * 3, searchFrom);

        // Ordenar por cercanía a la preferencia del usuario
        all.sort((a, b) -> {
            long diffA = Math.abs(java.time.Duration.between(
                LocalDateTime.of(a.date(), a.startTime()),
                LocalDateTime.of(nearDate, nearTime)
            ).toMinutes());
            long diffB = Math.abs(java.time.Duration.between(
                LocalDateTime.of(b.date(), b.startTime()),
                LocalDateTime.of(nearDate, nearTime)
            ).toMinutes());
            return Long.compare(diffA, diffB);
        });

        return all.subList(0, Math.min(count, all.size()));
    }

    /**
     * Verifica si un slot específico está disponible.
     */
    public boolean isSlotAvailable(LocalDateTime dateTime) {
        // Verificar que es día laborable
        DayOfWeek dow = dateTime.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return false;
        }

        // Verificar que está dentro del horario
        LocalTime time = dateTime.toLocalTime();
        LocalTime start = LocalTime.of(startHour, startMinute);
        LocalTime end = LocalTime.of(endHour, endMinute);
        if (time.isBefore(start) || time.plusMinutes(slotDurationMinutes).isAfter(end)) {
            return false;
        }

        // Verificar que está en el futuro
        if (!dateTime.isAfter(LocalDateTime.now())) {
            return false;
        }

        // Verificar que no está ocupado
        LocalDateTime slotEnd = dateTime.plusMinutes(slotDurationMinutes);
        return appointmentRepository.countActiveInRange(dateTime, slotEnd) == 0;
    }

    /**
     * Reserva un turno de firma.
     */
    public Optional<SignatureAppointment> bookAppointment(UUID expedienteId, LocalDateTime dateTime, String contactPhone) {
        if (!isSlotAvailable(dateTime)) {
            log.warn("Slot no disponible para {}", dateTime);
            return Optional.empty();
        }

        // Cancelar citas previas activas del mismo expediente
        appointmentRepository.findActiveByExpedienteId(expedienteId).ifPresent(existing -> {
            existing.setStatus(AppointmentStatusEnum.CANCELLED);
            existing.setUpdatedAt(LocalDateTime.now());
            appointmentRepository.save(existing);
            log.info("Cita anterior cancelada para expediente {}", expedienteId);
        });

        SignatureAppointment appointment = SignatureAppointment.schedule(
            expedienteId, dateTime, defaultLocation, contactPhone
        );

        SignatureAppointmentJpaEntity entity = toEntity(appointment);
        if (entity == null) {
            log.error("Error al convertir cita a entidad para expediente {}", expedienteId);
            return Optional.empty();
        }
        appointmentRepository.save(entity);

        log.info("Turno agendado para expediente {} en {}", expedienteId, dateTime);
        return Optional.of(appointment);
    }

    /**
     * Confirma la asistencia del interesado.
     */
    public Optional<SignatureAppointment> confirmAppointment(UUID appointmentId) {
        if (appointmentId == null) return Optional.empty();
        return appointmentRepository.findById(appointmentId).map(entity -> {
            entity.setStatus(AppointmentStatusEnum.CONFIRMED);
            entity.setUpdatedAt(LocalDateTime.now());
            appointmentRepository.save(entity);
            return toDomain(entity);
        });
    }

    /**
     * Marca una cita como notificada por el bot.
     */
    public Optional<SignatureAppointment> markNotifiedByBot(UUID appointmentId) {
        if (appointmentId == null) return Optional.empty();
        return appointmentRepository.findById(appointmentId).map(entity -> {
            entity.setNotifiedByBot(true);
            entity.setUpdatedAt(LocalDateTime.now());
            appointmentRepository.save(entity);
            return toDomain(entity);
        });
    }

    /**
     * Obtiene la cita activa de un expediente.
     */
    public Optional<SignatureAppointment> getActiveAppointment(UUID expedienteId) {
        return appointmentRepository.findActiveByExpedienteId(expedienteId)
                .map(this::toDomain);
    }

    /**
     * Obtiene todas las citas de un expediente.
     */
    public List<SignatureAppointment> getAppointmentsByExpediente(UUID expedienteId) {
        return appointmentRepository.findByExpedienteIdOrderByAppointmentDateTimeDesc(expedienteId)
                .stream().map(this::toDomain).toList();
    }

    /**
     * Actualiza el estado de una cita.
     */
    public Optional<SignatureAppointment> updateStatus(UUID appointmentId, AppointmentStatusEnum newStatus) {
        if (appointmentId == null || newStatus == null) return Optional.empty();
        return appointmentRepository.findById(appointmentId).map(entity -> {
            entity.setStatus(newStatus);
            entity.setUpdatedAt(LocalDateTime.now());
            appointmentRepository.save(entity);
            return toDomain(entity);
        });
    }

    /**
     * Cancela una cita.
     */
    public boolean cancelAppointment(UUID appointmentId) {
        if (appointmentId == null) return false;
        return appointmentRepository.findById(appointmentId).map(entity -> {
            entity.setStatus(AppointmentStatusEnum.CANCELLED);
            entity.setUpdatedAt(LocalDateTime.now());
            appointmentRepository.save(entity);
            return true;
        }).orElse(false);
    }

    /**
     * Obtiene todas las citas en un rango de fechas (para el calendario del dashboard).
     */
    public List<SignatureAppointment> getAppointmentsInRange(LocalDateTime from, LocalDateTime to) {
        return appointmentRepository.findAllInRange(from, to)
                .stream().map(this::toDomain).toList();
    }

    // ============================================
    // MAPPERS INTERNOS (Dominio ↔ JPA)
    // ============================================

    private SignatureAppointmentJpaEntity toEntity(SignatureAppointment domain) {
        return SignatureAppointmentJpaEntity.builder()
                .id(domain.getId())
                .expedienteId(domain.getExpedienteId())
                .appointmentDateTime(domain.getAppointmentDateTime())
                .location(domain.getLocation())
                .contactPhone(domain.getContactPhone())
                .status(domain.getStatus())
                .notifiedByBot(domain.isNotifiedByBot())
                .notes(domain.getNotes())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private SignatureAppointment toDomain(SignatureAppointmentJpaEntity entity) {
        return SignatureAppointment.builder()
                .id(entity.getId())
                .expedienteId(entity.getExpedienteId())
                .appointmentDateTime(entity.getAppointmentDateTime())
                .location(entity.getLocation())
                .contactPhone(entity.getContactPhone())
                .status(entity.getStatus())
                .notifiedByBot(entity.isNotifiedByBot())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Slot disponible para proponer al interesado.
     */
    public record AvailableSlot(LocalDate date, LocalTime startTime, LocalTime endTime) {}
}
