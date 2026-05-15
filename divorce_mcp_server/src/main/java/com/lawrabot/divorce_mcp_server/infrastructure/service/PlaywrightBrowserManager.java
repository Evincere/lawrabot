package com.lawrabot.divorce_mcp_server.infrastructure.service;

import com.lawrabot.divorce_mcp_server.infrastructure.config.BlsgProperties;
import com.microsoft.playwright.*;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Gestiona el ciclo de vida de Playwright y la instancia compartida de Browser.
 * Proporciona contextos aislados para cada tarea de scraping.
 */
@Service
@Slf4j
public class PlaywrightBrowserManager implements DisposableBean {

    private final BlsgProperties blsgProperties;
    private Playwright playwright;
    private Browser browser;

    public PlaywrightBrowserManager(BlsgProperties blsgProperties) {
        this.blsgProperties = blsgProperties;
    }

    @PostConstruct
    public void init() {
        log.info("Inicializando infraestructura de Playwright...");
        try {
            Files.createDirectories(Paths.get(blsgProperties.getStoragePath()));
            this.playwright = Playwright.create();
            this.browser = playwright.firefox().launch(new BrowserType.LaunchOptions()
                    .setHeadless(blsgProperties.isHeadless()));
            log.info("Navegador Playwright (Firefox) iniciado exitosamente.");
        } catch (IOException e) {
            log.error("Error al crear directorio de almacenamiento: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error crítico al iniciar Playwright: {}", e.getMessage());
        }
    }

    /**
     * Crea un nuevo contexto de navegador aislado.
     * @param options Opciones de contexto (ej. storageState, viewport).
     * @return BrowserContext nuevo y limpio.
     */
    public BrowserContext createContext(Browser.NewContextOptions options) {
        if (browser == null) {
            throw new IllegalStateException("El navegador Playwright no está inicializado.");
        }
        return browser.newContext(options);
    }

    /**
     * Devuelve el path donde se almacena el estado de la sesión.
     */
    public Path getStorageStatePath() {
        return Paths.get(blsgProperties.getStoragePath(), "state.json");
    }

    @Override
    @PreDestroy
    public void destroy() {
        log.info("Cerrando infraestructura de Playwright...");
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }
}
