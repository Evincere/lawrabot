package com.lawrabot.divorce_mcp_server.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Propiedades de configuración para el portal BLSG.
 * Resuelve las advertencias de "unknown property" en application.properties.
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "blsg.portal")
public class BlsgProperties {
    /**
     * Nombre de usuario/email para el portal.
     */
    private String username;

    /**
     * Contraseña para el portal.
     */
    private String password;
}
