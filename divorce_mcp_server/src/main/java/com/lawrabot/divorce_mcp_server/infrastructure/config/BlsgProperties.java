package com.lawrabot.divorce_mcp_server.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración para el Portal BLSG (Beneficio de Litigar Sin Gastos).
 */
@Configuration
@ConfigurationProperties(prefix = "blsg.portal")
@Data
public class BlsgProperties {
    private String username;
    private String password;
    private boolean headless = true;
    private String storagePath = "storage/session/";
    private int timeoutMs = 35000;
}
