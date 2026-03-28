package com.lawrabot.divorce_mcp_server.domain.model.agreement;

import com.lawrabot.divorce_mcp_server.domain.enums.HomeAttributionEnum;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Entidad que representa la liquidación de la comunidad de bienes del matrimonio y
 * el destino de la sede conyugal (Art. 443 CCyC Atribución de la vivienda).
 * Nota: La Compensación Económica (Art 441) tiene su propia entidad en el Agregado.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AssetDistribution {

    private UUID id;

    // ============================================
    // VIVIENDA (Sede del hogar conyugal)
    // ============================================

    // Indica a qué cónyuge se le atribuye la vivienda conyugal
    private HomeAttributionEnum homeAttributionTo; 
    private String customHomeAttributionTo; // Solo si == OTHER
    
    // Plazo de la atribución si aplica (ej: "HASTA_MAYORIA_EDAD_HIJOS", "2_AÑOS", "VITALICIO")
    private String homeAttributionTerm;

    // ============================================
    // BIENES Y DEUDAS (Comunidad ganancial o Régimen de Separación)
    // ============================================

    // Descripción general de la división (autos, muebles, cuentas, ahorros)
    private String assetsSummary;

    // Descripción de la asunción de deudas de la sociedad conyugal
    private String liabilitiesSummary;

    /**
     * Fábrica inicial
     */
    public static AssetDistribution createEmpty() {
        return AssetDistribution.builder()
                .id(UUID.randomUUID())
                .build();
    }

    // ============================================
    // LÓGICA DE NEGOCIO (Guía para el Bot)
    // ============================================

    /**
     * Si los cónyuges declaran vehículos o casas en assetsSummary, el abogado (o el bot)
     * está forzado a notificar que deberán pagar Tasa de Justicia de acuerdo al valor de los mismos.
     */
    public boolean requiresTaxWarning() {
        return (assetsSummary != null && !assetsSummary.trim().isEmpty()) 
            || (liabilitiesSummary != null && !liabilitiesSummary.trim().isEmpty());
    }
}

