package com.lawrabot.divorce_mcp_server.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Representa un documento o evidencia digital (fotos, PDFs) enviado por el ciudadano.
 */
@Getter
@Builder
public class DigitalEvidence {
    
    private final UUID id;
    private final UUID expedienteId;
    
    // El tipo de evidencia sirve para categorizar (EJ: DNI_FRONT, DNI_BACK, MARRIAGE_CERT, PAYSLIP, OTRO)
    private final String documentType;
    
    // La descripción o nombre original del archivo
    private final String fileName;
    
    // Ruta física o URI del archivo en el sistema
    private final String filePath;
    
    // Mime type (image/jpeg, application/pdf...)
    private final String mimeType;
    
    private boolean approved;
    private String rejectionReason;
    
    private final LocalDateTime createdAt;
    
    public void approve() {
        this.approved = true;
        this.rejectionReason = null;
    }
    
    public void reject(String reason) {
        this.approved = false;
        this.rejectionReason = reason;
    }
}
