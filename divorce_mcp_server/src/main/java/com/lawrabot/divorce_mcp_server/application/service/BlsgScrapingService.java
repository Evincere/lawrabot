package com.lawrabot.divorce_mcp_server.application.service;

import com.lawrabot.divorce_mcp_server.application.port.in.ConsultarBlsgUseCase;
import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.enums.BlsgScrapingResultEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.DivorceTypeEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.ExpedienteStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.domain.valueobject.PhoneNumberVO;
import com.lawrabot.divorce_mcp_server.infrastructure.config.BlsgProperties;
import com.lawrabot.divorce_mcp_server.infrastructure.service.PlaywrightBrowserManager;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@Slf4j
public class BlsgScrapingService implements ConsultarBlsgUseCase {

    private final IExpedienteRepository expedienteRepository;
    private final BlsgProperties blsgProperties;
    private final PlaywrightBrowserManager browserManager;
    private static final String BLSG_URL = "https://blsg.pjm.gob.ar/";
    private static final String CERTIFICATES_PATH = "storage/certificates/";

    public BlsgScrapingService(IExpedienteRepository expedienteRepository,
            BlsgProperties blsgProperties,
            PlaywrightBrowserManager browserManager) {
        this.expedienteRepository = expedienteRepository;
        this.blsgProperties = blsgProperties;
        this.browserManager = browserManager;
        try {
            Files.createDirectories(Paths.get(CERTIFICATES_PATH));
        } catch (IOException e) {
            log.error("No se pudo crear el directorio de certificados: {}", e.getMessage());
        }
    }

    @Override
    public ScrapingResult execute(String phoneNumber, String dni) {
        log.info("Iniciando scraping BLSG para DNI: {}", dni);

        Expediente expediente = resolveOrCreateExpediente(phoneNumber, dni);
        if (expediente == null) {
            return new ScrapingResult(null, dni, null, null, null, null, "Error de validación de expediente.", null,
                    false);
        }

        Path statePath = browserManager.getStorageStatePath();
        Browser.NewContextOptions options = new Browser.NewContextOptions()
                .setUserAgent(
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                .setViewportSize(1280, 800);

        if (Files.exists(statePath)) {
            options.setStorageStatePath(statePath);
            log.info("Cargando sesión persistente desde {}", statePath);
        }

        try (BrowserContext context = browserManager.createContext(options);
                Page page = context.newPage()) {

            // Bloqueo de recursos no esenciales para optimizar rendimiento
            page.route("**/*.{png,jpg,jpeg,svg,woff2,gif}", Route::abort);

            // ── Paso 1: Navegación inicial ──────────────────────────────────
            log.info("Navegando a {}", BLSG_URL);
            page.navigate(BLSG_URL);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            // ── Paso 2: Gestión de Autenticación ────────────────────────────
            handleAuthentication(page);
            log.info("[DEBUG] URL post-auth: {}", page.url());

            // ── Paso 3: Verificación real del formulario (red de seguridad) ──
            ensureBlsgFormVisible(page);

            // ── Paso 4: Consulta de DNI ─────────────────────────────────────
            log.info("Consultando DNI: {}", dni);
            page.locator("input#dni").waitFor();
            page.locator("input#dni").fill(dni);
            // El primer botón Consultar es el de búsqueda
            page.locator("button:has-text('Consultar')").first().click();

            // Esperar que aparezca la Card de resultados (vía nombre de la persona)
            log.info("Esperando card de resultados...");
            Locator resultName = page.locator("h2, h3").first();
            resultName.waitFor(new Locator.WaitForOptions().setTimeout(20_000));
            log.info("Resultado detectado: {}", resultName.textContent().trim());

            // ── Paso 4.1: Extracción de metadatos ───────────────────────────
            String fullName = resultName.textContent().trim();
            String cuil = extractValue(page, "CUIL:");
            String birthDate = extractValue(page, "Fecha Nacimiento:");
            String province = extractValue(page, "Provincia:");
            String sex = extractValue(page, "Sexo:");

            // ── Paso 5: Expandir detalle BLSG (botón "Consultar" de la card) ─
            log.info("Solicitando estado detallado del beneficio...");
            // Usar .last() para apuntar al botón de la card
            page.locator("button:has-text('Consultar')").last().click();
            page.waitForLoadState(LoadState.NETWORKIDLE);

            // ── Paso 6: Determinar resultado del beneficio ──────────────────
            String benefitStatus = determineBenefitStatus(page);
            boolean isApproved = benefitStatus.contains("Se otorga") || benefitStatus.contains("ACCEDE");
            boolean isInconclusive = benefitStatus.contains("evaluación adicional");

            // ── Paso 7: Descarga de Constancia ──────────────────────────────
            String certificatePath = null;
            if (isApproved || isInconclusive || benefitStatus.contains("No se otorga") || benefitStatus.contains("NO ACCEDE")) {
                certificatePath = downloadCertificate(page, dni);
            }

            // ── Paso 8: Actualización de dominio ────────────────────────────
            BlsgScrapingResultEnum resultEnum = isApproved ? BlsgScrapingResultEnum.PROVISIONALLY_APPROVED
                    : (isInconclusive ? BlsgScrapingResultEnum.INCONCLUSIVE : BlsgScrapingResultEnum.PROVISIONALLY_REJECTED);

            expediente.processScrapingResult(resultEnum, benefitStatus, fullName, dni, cuil, birthDate, province, sex,
                    certificatePath);
            expedienteRepository.save(expediente);
            log.info("Scraping BLSG exitoso para DNI: {}", dni);

            return new ScrapingResult(fullName, dni, cuil, birthDate, province, sex, benefitStatus, certificatePath,
                    true);

        } catch (Exception e) {
            log.error("Error crítico durante el scraping BLSG: {}", e.getMessage());
            return new ScrapingResult(null, dni, null, null, null, null, "Error: " + e.getMessage(), null, false);
        }
    }

    /**
     * Punto de entrada de autenticación. Decide por URL estable si
     * hay que hacer login o si la sesión de BLSG sigue activa.
     * La verificación lazy del JWT queda delegada a ensureBlsgFormVisible().
     */
    private void handleAuthentication(Page page) {
        log.info("Verificando estado de la sesión...");
        waitForStableAuthEntry(page);

        String currentUrl = page.url();

        if (currentUrl.contains("blsg.pjm.gob.ar")) {
            log.info("URL en BLSG. Verificación lazy pendiente en ensureBlsgFormVisible().");
            return;
        }

        if (currentUrl.contains("login.microsoftonline.com")) {
            log.info("Iniciando flujo de login Microsoft Entra ID...");
            doMicrosoftLogin(page);
        }

        waitForBlsgForm(page);
        log.info("Login completado. Sesión autenticada en BLSG.");
        page.context().storageState(
                new BrowserContext.StorageStateOptions().setPath(browserManager.getStorageStatePath()));
    }

    /**
     * Red de seguridad: espera a que el formulario de DNI sea visible
     * O detecta un redirect lazy a Microsoft por JWT expirado.
     * Si ocurre redirect, re-autentica y vuelve al dashboard.
     */
    private void ensureBlsgFormVisible(Page page) {
        log.info("Asegurando visibilidad del formulario BLSG...");
        try {
            page.waitForCondition(() -> {
                String url = page.url();
                boolean enLogin = url.contains("login.microsoftonline.com") || url.contains("auth24.pjm.gob.ar");
                boolean formularioVisible = page.locator("input#dni").isVisible();
                return enLogin || formularioVisible;
            }, new Page.WaitForConditionOptions().setTimeout(30_000));
        } catch (TimeoutError e) {
            log.warn("Timeout inicial en ensureBlsgFormVisible, verificando URL actual: {}", page.url());
        }

        String currentUrl = page.url();
        if (currentUrl.contains("login.microsoftonline.com") || currentUrl.contains("auth24.pjm.gob.ar")) {
            log.warn("Sesión expirada o redirección detectada ({}). Procesando auth...", currentUrl);
            
            if (currentUrl.contains("login.microsoftonline.com")) {
                doMicrosoftLogin(page);
            } else if (currentUrl.contains("auth24.pjm.gob.ar")) {
                handleKeycloakLogin(page);
            }
            
            waitForBlsgForm(page);
            page.context().storageState(
                    new BrowserContext.StorageStateOptions().setPath(browserManager.getStorageStatePath()));
        }
    }

    /**
     * Completa el flujo de login federado (Microsoft Entra ID).
     * Validado paso a paso con diagnóstico de sesión limpia.
     */
    private void doMicrosoftLogin(Page page) {
        log.info("Iniciando login de Microsoft...");

        // ── Paso 1: Email ──────────────────────────────────────────────
        page.locator("input#i0116").waitFor();
        page.locator("input#i0116").fill(blsgProperties.getUsername());
        page.locator("input#idSIButton9").click(); // Botón Next
        log.info("Email ingresado.");

        // ── Paso 2: Contraseña ──────────────────────────────────────────
        page.locator("input#i0118").waitFor();
        page.locator("input#i0118").fill(blsgProperties.getPassword());
        page.locator("input#idSIButton9").click(); // Botón Sign In
        log.info("Contraseña ingresada.");

        // ── Paso 3: Diálogo 'Mantener sesión' (KMSI) ─────────────────────
        try {
            // El ID verificado es idBtn_Back para 'No'
            page.locator("input#idBtn_Back").waitFor(new Locator.WaitForOptions().setTimeout(8_000));
            page.locator("input#idBtn_Back").click();
            log.info("Diálogo 'Mantener sesión' rechazado.");
        } catch (TimeoutError ignored) {
            log.debug("Diálogo 'Mantener sesión' no apareció o ya redirigió.");
        }

        // ── Paso 4: Esperar salida de Microsoft y manejar Keycloak ──────
        log.info("Esperando redirección final post-Microsoft...");
        try {
            page.waitForURL(url -> !url.contains("login.microsoftonline.com"),
                    new Page.WaitForURLOptions().setTimeout(15_000));
        } catch (TimeoutError e) {
            log.warn("Microsoft no redirigió automáticamente. Aplicando navegación de rescate.");
            if (page.url().contains("login.microsoftonline.com")) {
                page.navigate(BLSG_URL);
            }
        }
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);

        String postMsUrl = page.url();
        log.info("URL actual tras fase Microsoft: {}", postMsUrl);

        // ── Paso 5: Login Keycloak (auth24.pjm.gob.ar) ──────────────────
        try {
            java.net.URI uri = new java.net.URI(postMsUrl);
            String host = uri.getHost();
            if (host != null && host.contains("auth24.pjm.gob.ar")) {
                log.info("Detectado host de Keycloak ({}). Procesando login local...", host);
                handleKeycloakLogin(page);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Maneja el login en Keycloak del Poder Judicial (auth24.pjm.gob.ar).
     * El screenshot de diagnóstico muestra un formulario con username y "Escribir contraseña".
     */
    private void handleKeycloakLogin(Page page) {
        try {
            // Esperar el campo de contraseña de Keycloak
            Locator passwordField = page.locator("input[type='password']");
            passwordField.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(15_000));

            // Si hay campo de username, llenarlo también
            Locator usernameField = page.locator("input[type='text'], input[name='username']").first();
            if (usernameField.isVisible()) {
                usernameField.fill(blsgProperties.getUsername());
                log.info("Username ingresado en Keycloak.");
            }

            passwordField.fill(blsgProperties.getPassword());
            log.info("Contraseña ingresada en Keycloak.");

            // Buscar botón de login y presionar
            Locator loginBtn = page.locator("input[type='submit'], button[type='submit']").first();
            if (loginBtn.isVisible()) {
                loginBtn.click();
            } else {
                passwordField.press("Enter");
            }
            log.info("Login de Keycloak enviado. Esperando redirección a BLSG...");
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
        } catch (TimeoutError e) {
            log.warn("No se encontró formulario de Keycloak. Continuando...");
        }
    }

    /**
     * Espera a que la URL se estabilice en BLSG o Microsoft Login.
     * Evita interrogar el DOM durante redirects intermedios.
     */
    private void waitForStableAuthEntry(Page page) {
        page.waitForCondition(() -> {
            String url = page.url();
            return url.contains("blsg.pjm.gob.ar") || url.contains("login.microsoftonline.com");
        }, new Page.WaitForConditionOptions().setTimeout(45_000));
    }

    /**
     * Espera a que el formulario de consulta BLSG esté visible.
     * El marcador de estado estable es input#dni (identificado por rastreo en vivo).
     */
    private void waitForBlsgForm(Page page) {
        log.info("Esperando formulario de consulta BLSG (input#dni)...");
        try {
            page.locator("input#dni").waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(45_000));
            log.info("Formulario BLSG detectado. Listo para consulta.");
        } catch (TimeoutError e) {
            log.error("No se encontró el input#dni en BLSG después de 45s. URL actual: {}", page.url());
            log.error("Título de la página: {}", page.title());
            throw e;
        }
    }

    /**
     * Determina el estado del beneficio BLSG buscando textos clave en la página.
     * Validado contra el render asíncrono de React (Phase 6).
     */
    private String determineBenefitStatus(Page page) {
        // Buscar el texto de estado en toda la página tras expandir la card
        // Usamos RegEx para un OR lógico robusto en Playwright
        Locator statusText = page.locator("text=/(Se otorga el Beneficio|No se otorga el Beneficio|No posee|evaluación adicional|ACCEDE|NO ACCEDE)/i");

        try {
            log.info("Esperando texto de estado del beneficio...");
            statusText.first().waitFor(new Locator.WaitForOptions().setTimeout(10_000));
            
            String text = statusText.first().textContent().trim();
            log.info("Texto de estado BLSG detectado (raw): '{}'", text);

            // IMPORTANTE: Primero verificar estados negativos/inconclusos para evitar falsos positivos
            // 'No se otorga' contiene 'Se otorga', por lo que el orden es crítico.
            if (text.contains("No se otorga") || text.contains("NO ACCEDE") || text.contains("No posee")) {
                log.info("Resultado: RECHAZADO");
                return "No se otorga el Beneficio de Litigar sin Gastos.";
            } else if (text.contains("evaluación adicional")) {
                log.info("Resultado: INCONCLUSO");
                return "Requiere evaluación adicional del Beneficio de Litigar sin Gastos";
            } else if (text.contains("Se otorga") || text.contains("ACCEDE")) {
                log.info("Resultado: APROBADO");
                return "Se otorga el Beneficio de Litigar sin Gastos";
            }
            
            return text;
        } catch (Exception e) {
            log.warn("No se pudo determinar el estado del beneficio: {}", e.getMessage());
            return "Resultado inconcluso. Se requiere revisión manual.";
        }
    }

    /**
     * Descarga la constancia PDF del beneficio BLSG.
     */
    private String downloadCertificate(Page page, String dni) {
        try {
            Download download = page.waitForDownload(() -> {
                page.locator("button:has-text('Descargar'), button:has-text('Constancia')").first().click();
            });

            Path target = Paths.get(CERTIFICATES_PATH, "BLSG_" + dni + "_" + UUID.randomUUID() + ".pdf");
            download.saveAs(target);
            log.info("Constancia descargada exitosamente en: {}", target);
            return target.toAbsolutePath().toString();
        } catch (Exception e) {
            log.warn("No se pudo descargar la constancia para DNI {}: {}", dni, e.getMessage());
            return null;
        }
    }

    private Expediente resolveOrCreateExpediente(String phoneNumber, String dni) {
        // Lógica de dominio para reutilizar o crear expedientes
        java.util.Optional<Expediente> byDni = expedienteRepository.findActiveByDni(dni);
        if (byDni.isPresent()) {
            if (byDni.get().getStatus() != ExpedienteStatusEnum.BLSG_PRECONSULTA)
                return null;
            return byDni.get();
        }

        return expedienteRepository.findActiveByClientPhone(phoneNumber)
                .orElseGet(() -> expedienteRepository
                        .save(Expediente.createNew(PhoneNumberVO.of(phoneNumber), DivorceTypeEnum.UNILATERAL)));
    }

    /**
     * Extrae un valor de texto desde la página buscando una etiqueta específica.
     */
    private String extractValue(Page page, String label) {
        try {
            String fullText = page.locator("div, p, span")
                    .filter(new Locator.FilterOptions().setHasText(label))
                    .first().innerText();
            if (fullText.contains(":")) {
                return fullText.split(":")[1].trim().split("\n")[0].trim();
            }
            return fullText.replace(label, "").trim().split("\n")[0].trim();
        } catch (Exception e) {
            return "No disponible";
        }
    }
}
