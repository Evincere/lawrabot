package com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository;

import com.lawrabot.divorce_mcp_server.infrastructure.persistence.entity.SignatureAppointmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio JPA para turnos de firma presencial.
 */
@Repository
public interface SignatureAppointmentJpaRepository extends JpaRepository<SignatureAppointmentJpaEntity, UUID> {

    /**
     * Busca la cita activa (SCHEDULED o CONFIRMED) de un expediente.
     */
    @Query("SELECT a FROM SignatureAppointmentJpaEntity a WHERE a.expedienteId = :expedienteId " +
           "AND a.status IN ('SCHEDULED', 'CONFIRMED') ORDER BY a.appointmentDateTime ASC")
    Optional<SignatureAppointmentJpaEntity> findActiveByExpedienteId(@Param("expedienteId") UUID expedienteId);

    /**
     * Busca todas las citas de un expediente.
     */
    List<SignatureAppointmentJpaEntity> findByExpedienteIdOrderByAppointmentDateTimeDesc(UUID expedienteId);

    /**
     * Cuenta cuántas citas activas (SCHEDULED o CONFIRMED) hay en un rango horario.
     * Útil para verificar disponibilidad de un slot.
     */
    @Query("SELECT COUNT(a) FROM SignatureAppointmentJpaEntity a " +
           "WHERE a.appointmentDateTime >= :from AND a.appointmentDateTime < :to " +
           "AND a.status IN ('SCHEDULED', 'CONFIRMED')")
    long countActiveInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Obtiene todas las citas en un rango de fechas (para visualizar en el calendario del dashboard).
     */
    @Query("SELECT a FROM SignatureAppointmentJpaEntity a " +
           "WHERE a.appointmentDateTime >= :from AND a.appointmentDateTime < :to " +
           "ORDER BY a.appointmentDateTime ASC")
    List<SignatureAppointmentJpaEntity> findAllInRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
