package com.lawrabot.divorce_mcp_server.domain.model.agreement;

import com.lawrabot.divorce_mcp_server.domain.valueobject.AddressVO;
import com.lawrabot.divorce_mcp_server.domain.enums.CommunicationRegimeTypeEnum;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Entidad que representa el Régimen de Comunicación (antiguas "Visitas") (Art.
 * 652 CCyC).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CommunicationRegime {

    private UUID id;

    // Naturaleza del régimen
    private CommunicationRegimeTypeEnum regimeType;
    private String customRegimeType; // Usado solo si regimeType == OTHER

    // Cronogramas acordados (Suelen ser párrafos de texto libre redactados
    // a base de lo que extrae el LLM del mensaje del cliente)
    private String regularSchedule; // Horarios en semana y fines de semana
    private String holidaySchedule; // Disposiciones para días festivos y vacaciones

    // Logística de intercambio del menor
    private AddressVO pickUpLocation;
    private String pickUpLocationDescription; // Ej: "Puerta de ingreso colegio San José"

    // Tercero involucrado (relevante solo para regímenes supervisados)
    private String supervisorName;

    /**
     * Fábrica para inicializar un régimen de comunicación vacío
     */
    public static CommunicationRegime createEmpty() {
        return CommunicationRegime.builder()
                .id(UUID.randomUUID())
                .build();
    }

    // ============================================
    // LÓGICA DE NEGOCIO (Guía para el Bot WhatsApp)
    // ============================================

    /**
     * Indica si el bot debe ahondar en detalles preguntando días y horas
     * específicas.
     * Si es Amplio y Flexible, no hace falta acorralar al usuario con tantas
     * preguntas.
     */
    public boolean requiresScheduleDetails() {
        return regimeType == CommunicationRegimeTypeEnum.SPECIFIC_SCHEDULE;
    }

    /**
     * Alerta al sistema (y al abogado) si el bot detecta un contexto familiar
     * severo
     * que demande la intervención de un supervisor.
     * Gatilla la necesidad de preguntar el nombre del "Supervisor".
     */
    public boolean isSupervisionRequiredWarning() {
        return regimeType == CommunicationRegimeTypeEnum.RESTRICTED_SUPERVISED;
    }
}
