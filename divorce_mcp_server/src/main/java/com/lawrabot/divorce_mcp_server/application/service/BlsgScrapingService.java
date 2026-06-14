package com.lawrabot.divorce_mcp_server.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lawrabot.divorce_mcp_server.application.port.in.ConsultarBlsgUseCase;
import com.lawrabot.divorce_mcp_server.infrastructure.config.BlsgProperties;
import com.lawrabot.divorce_mcp_server.infrastructure.service.PlaywrightBrowserManager;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class BlsgScrapingService implements ConsultarBlsgUseCase {

    private final BlsgProperties blsgProperties;
    private final PlaywrightBrowserManager browserManager;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    // Caché en memoria para el token JWT de Keycloak
    private volatile String cachedJwtToken = null;
    private volatile LocalDateTime tokenExpiry = null;

    private static final String BLSG_URL = "https://blsg.pjm.gob.ar/";
    private static final String CERTIFICATES_PATH = "storage/certificates/";
    private static final String DIAGNOSTICS_PATH = "storage/diagnostics/";

    public BlsgScrapingService(BlsgProperties blsgProperties,
            PlaywrightBrowserManager browserManager,
            ObjectMapper objectMapper) {
        this.blsgProperties = blsgProperties;
        this.browserManager = browserManager;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .build();
        try {
            Files.createDirectories(Paths.get(CERTIFICATES_PATH));
            Files.createDirectories(Paths.get(DIAGNOSTICS_PATH));
        } catch (IOException e) {
            log.error("No se pudo crear los directorios de almacenamiento: {}", e.getMessage());
        }
    }

    @Override
    public ScrapingResult execute(String phoneNumber, String dni) {
        log.info("Iniciando consulta BLSG para DNI: {}", dni);

        // ── Paso 0: Intentar consulta directa por API si hay un token cacheado ──
        String token = this.cachedJwtToken;
        if (token != null) {
            log.info("Token JWT encontrado en caché. Intentando consulta directa...");
            ScrapingResult apiResult = executeDirectApiQuery(dni, token);
            String benefitStatus = apiResult.benefitStatus();
            if (apiResult.success()) {
                log.info("Consulta directa por API exitosa. Retornando datos al instante.");
                return apiResult;
            } else if (benefitStatus != null && benefitStatus.equals("UNAUTHORIZED")) {
                log.warn("Token JWT expirado (401/403). Invalidando caché para renovación con Playwright.");
                this.cachedJwtToken = null;
            } else {
                log.warn("La consulta directa por API falló. Utilizando Playwright como red de seguridad.");
            }
        }

        // ── Paso 1: Fallback a Playwright para consulta y captura de nuevo token ──
        Path statePath = browserManager.getStorageStatePath();
        Browser.NewContextOptions options = new Browser.NewContextOptions()
                .setViewportSize(1280, 800)
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");

        if (Files.exists(statePath)) {
            options.setStorageStatePath(statePath);
            log.info("Cargando sesión persistente desde {}", statePath);
        }

        try (BrowserContext context = browserManager.createContext(options);
                Page page = context.newPage()) {

            // Interceptar peticiones para capturar de forma transparente el token JWT enviado a core.blsg.pjm.gob.ar
            page.onRequest(request -> {
                if (request.url().contains("core.blsg.pjm.gob.ar")) {
                    String authHeader = request.headers().get("authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String jwtToken = authHeader.substring(7);
                        this.cachedJwtToken = jwtToken;
                        this.tokenExpiry = LocalDateTime.now().plusHours(1);
                        log.info("Token JWT de BLSG interceptado y guardado en caché.");
                    }
                }
            });

            // Desactivar el bloqueo de recursos no esenciales. Algunos WAF / Anti-Bots 
            // como Cloudflare o Akamai detectan el bloqueo de fuentes o imágenes como señal segura de automatización.

            // ── Paso 1.1: Navegación inicial ──────────────────────────────────
            log.info("Navegando a {}", BLSG_URL);
            page.navigate(BLSG_URL);
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            takeDiagnosticScreenshot(page, "01_initial_navigation");

            // ── Paso 2: Gestión de Autenticación ────────────────────────────
            handleAuthentication(page);
            log.info("[DEBUG] URL post-auth: {}", page.url());
            takeDiagnosticScreenshot(page, "02_post_auth");

            // ── Paso 3: Verificación real del formulario (red de seguridad) ──
            ensureBlsgFormVisible(page);
            takeDiagnosticScreenshot(page, "03_form_visible");

            // ── Paso 4: Consulta de DNI ─────────────────────────────────────
            log.info("Consultando DNI: {}", dni);
            // Uso de selector específico input#dni para evitar colisión con button#dni
            page.locator("input#dni").waitFor(new Locator.WaitForOptions().setTimeout(25_000));
            // Deshabilitar autocompletado del navegador para evitar el dropdown que bloquea el botón
            page.locator("input#dni").evaluate("el => el.setAttribute('autocomplete', 'off')");
            page.locator("input#dni").fill(dni);

            // Cerrar el dropdown de autocompletado del navegador que bloquea el botón Consultar.
            // El dropdown aparece sobre el botón e intercepta el clic si no se cierra primero.
            page.locator("input#dni").press("Escape");
            page.waitForTimeout(300);
            takeDiagnosticScreenshot(page, "03b_autocomplete_dismissed");

            // Hacer clic en el botón Consultar (búsqueda principal)
            Locator consultarBtn = page.locator("button:has-text('Consultar')").first();
            consultarBtn.waitFor(new Locator.WaitForOptions().setTimeout(5_000));
            consultarBtn.click();
            takeDiagnosticScreenshot(page, "04_after_search_click");

            // Esperar que aparezca la Card de resultados o un cartel de error del portal
            log.info("Esperando card de resultados o cartel de error...");
            Locator resultOrError = page.locator("h2, h3, :text-matches('Error al obtener datos', 'i'), :text-matches('Error:', 'i')").first();
            resultOrError.waitFor(new Locator.WaitForOptions().setTimeout(blsgProperties.getTimeoutMs()));
            takeDiagnosticScreenshot(page, "05_results_card");
            
            String textFound = resultOrError.textContent().trim();
            if (textFound.toLowerCase().contains("error al obtener datos") || textFound.toLowerCase().contains("error:")) {
                try {
                    Files.deleteIfExists(statePath);
                    log.warn("Sesión borrada por precaución debido a un error de rate-limit o token expirado en el backend.");
                } catch (Exception ignored) {}
                throw new RuntimeException("El portal BLSG devolvió un error interno de su backend: " + textFound);
            }
            
            Locator resultName = page.locator("h2, h3").first();
            log.info("Resultado detectado: {}", resultName.textContent().trim());

            // ── Paso 4.1: Extracción de metadatos ───────────────────────────
            String fullName = resultName.textContent().trim();
            String cuil = sanitizeExtractedValue(extractValue(page, "CUIL:"), "^\\d{2}-\\d{8}-\\d{1}$|^\\d{11}$");
            String birthDate = sanitizeExtractedValue(extractValue(page, "Fecha Nacimiento:"), "^\\d{2}/\\d{2}/\\d{4}$|^\\d{4}-\\d{2}-\\d{2}$");
            String province = extractValue(page, "Provincia:");
            String sex = extractValue(page, "Sexo:");

            log.info("Datos extraídos -> CUIL: {}, Fecha Nac: {}, Provincia: {}", cuil, birthDate, province);

            // ── Paso 5: Expandir detalle BLSG (botón "Consultar" de la card) ─
            log.info("Solicitando estado detallado del beneficio...");
            page.locator("button:has-text('Consultar')").last().click();
            page.waitForLoadState(LoadState.NETWORKIDLE);
            takeDiagnosticScreenshot(page, "06_benefit_details");

            String benefitStatus = determineBenefitStatus(page);
            boolean isApproved = benefitStatus.contains("Se otorga") || benefitStatus.contains("ACCEDE");
            boolean isInconclusive = benefitStatus.contains("evaluación adicional");

            String certificatePath = null;
            if (isApproved || isInconclusive || benefitStatus.contains("No se otorga")
                    || benefitStatus.contains("NO ACCEDE")) {
                certificatePath = downloadCertificate(page, dni);
            }

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
        } else if (currentUrl.contains("auth24.pjm.gob.ar")) {
            log.info("Detectada redirección directa a Keycloak. Procesando auth local...");
            handleKeycloakLogin(page);
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

        // ── Paso 0: Verificación de URL ──────────────────────────────────
        if (!page.url().contains("login.microsoftonline.com")) {
            log.warn("Llamada a doMicrosoftLogin pero la URL no es de Microsoft: {}", page.url());
            return;
        }

        // ── Paso 1: Email ──────────────────────────────────────────────
        try {
            page.locator("input#i0116").waitFor(new Locator.WaitForOptions().setTimeout(15_000));
            page.locator("input#i0116").fill(blsgProperties.getUsername());
            page.locator("input#idSIButton9").click(); // Botón Next
            log.info("Email ingresado.");
        } catch (TimeoutError e) {
            log.warn("Timeout esperando campo de email Microsoft. Verificando si ya redirigió...");
            if (page.url().contains("blsg.pjm.gob.ar") || page.url().contains("auth24.pjm.gob.ar")) {
                return;
            }
            throw e;
        }

        // ── Paso 2: Contraseña ──────────────────────────────────────────
        page.locator("input#i0118").waitFor();
        page.locator("input#i0118").fill(blsgProperties.getPassword());
        page.locator("input#idSIButton9").click(); // Botón Sign In
        log.info("Contraseña ingresada.");

        // ── Paso 3: Diálogo 'Mantener sesión' (KMSI) ─────────────────────
        try {
            // El ID verificado es idSIButton9 para 'Sí'
            page.locator("input#idSIButton9").waitFor(new Locator.WaitForOptions().setTimeout(8_000));
            page.locator("input#idSIButton9").click();
            log.info("Diálogo 'Mantener sesión' aceptado.");
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
        } catch (Exception ignored) {
        }
    }

    /**
     * Maneja el login en Keycloak del Poder Judicial (auth24.pjm.gob.ar).
     * El screenshot de diagnóstico muestra un formulario con username y "Escribir
     * contraseña".
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
     * El marcador de estado estable es input#dni (identificado por rastreo en
     * vivo).
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
        // O interceptar los mensajes de error de la API si falla al cargar detalles
        Locator statusOrError = page.locator(
                "*:text-matches('Se otorga el Beneficio|No se otorga el Beneficio|No posee|evaluación adicional|ACCEDE|NO ACCEDE', 'i'), " +
                "*:text-matches('Error al consultar', 'i'), *:text-matches('Error al obtener datos', 'i')");

        try {
            log.info("Esperando texto de estado del beneficio o error detallado...");
            statusOrError.first().waitFor(new Locator.WaitForOptions().setTimeout(blsgProperties.getTimeoutMs()));

            String text = statusOrError.first().textContent().trim();
            log.info("Texto de estado BLSG detectado (raw): '{}'", text);
            
            if (text.toLowerCase().contains("error al consultar") || text.toLowerCase().contains("error al obtener")) {
                 log.warn("Error interno detectado al consultar los detalles del beneficio.");
                 try { Files.deleteIfExists(browserManager.getStorageStatePath()); } catch(Exception ignored){}
                 return "Error de backend (Inconcluso): " + text;
            }

            // IMPORTANTE: Primero verificar estados negativos/inconclusos para evitar
            // falsos positivos
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

    /**
     * Extrae un valor de texto desde la página buscando una etiqueta específica.
     */
    private String extractValue(Page page, String label) {
        try {
            // Intentar buscar por etiqueta de texto exacta o contenida
            Locator locator = page.locator("div, p, span, td, th, label")
                    .filter(new Locator.FilterOptions().setHasText(label))
                    .first();
            
            if (!locator.isVisible()) return "No disponible";

            String fullText = locator.innerText().trim();
            
            // Si el texto contiene la etiqueta, intentar extraer lo que sigue al ":" o el texto después de la etiqueta
            if (fullText.contains(":")) {
                String[] parts = fullText.split(":", 2);
                if (parts.length > 1) {
                    return parts[1].trim().split("\n")[0].trim();
                }
            }
            
            // Si la etiqueta está sola en un contenedor, buscar el siguiente hermano o el contenido restante
            return fullText.replace(label, "").trim().split("\n")[0].trim();
        } catch (Exception e) {
            return "No disponible";
        }
    }

    /**
     * Limpia un valor extraído y valida contra un patrón Regex si se provee.
     */
    private String sanitizeExtractedValue(String value, String regexPattern) {
        if (value == null || value.equalsIgnoreCase("No disponible") || value.isBlank()) {
            return "S/D";
        }
        
        // Limpieza básica: quitar caracteres de control y espacios extra
        String cleaned = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "").trim();
        
        if (regexPattern != null && !regexPattern.isBlank()) {
            if (!cleaned.matches(regexPattern)) {
                log.warn("El valor extraído '{}' no coincide con el patrón esperado: {}. Marcando como S/D.", cleaned, regexPattern);
                return "S/D";
            }
        }
        
        return cleaned;
    }

    /**
     * Consulta directa por API HTTP a core.blsg.pjm.gob.ar.
     */
    private ScrapingResult executeDirectApiQuery(String dni, String token) {
        log.info("Ejecutando consulta directa API BLSG para DNI: {}", dni);
        try {
            String requestBody = "{\"dni\":\"" + dni + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://core.blsg.pjm.gob.ar/consulta-persona"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Origin", "https://blsg.pjm.gob.ar")
                    .header("Referer", "https://blsg.pjm.gob.ar/")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            log.info("Respuesta de API directa recibida. Código de estado: {}", statusCode);

            if (statusCode == 200) {
                String responseBody = response.body();
                log.info("API directa de BLSG exitosa. Parseando JSON...");
                JsonNode root = objectMapper.readTree(responseBody);
                
                // Mapeo dinámico y tolerante
                String fullName = getJsonFieldValue(root, "nombre", "nombreCompleto", "fullName", "nombre_completo", "title");
                if (fullName == null || fullName.isBlank() || fullName.equalsIgnoreCase("null")) {
                    String firstName = getJsonFieldValue(root, "nombre", "first_name", "first");
                    String lastName = getJsonFieldValue(root, "apellido", "last_name", "last");
                    if (firstName != null && lastName != null) {
                        fullName = lastName + " " + firstName;
                    }
                }
                
                String cuil = getJsonFieldValue(root, "cuil", "cuit", "cuil_cuit");
                String birthDate = getJsonFieldValue(root, "fechaNacimiento", "birthDate", "fechaNac", "fecha_nacimiento");
                String province = getJsonFieldValue(root, "provincia", "province");
                String sex = getJsonFieldValue(root, "sexo", "sex", "genero", "gender");
                String benefitStatus = getJsonFieldValue(root, "estado", "beneficio", "benefitStatus", "resultado");
                
                if (benefitStatus == null || benefitStatus.isBlank()) {
                    benefitStatus = "Requiere evaluación adicional (Consulta directa)";
                }

                log.info("Datos parseados de la API -> Nombre: {}, CUIL: {}, Fecha Nac: {}, Provincia: {}", 
                        fullName, cuil, birthDate, province);

                return new ScrapingResult(
                        fullName != null ? fullName.toUpperCase().trim() : "SIN DATOS",
                        dni,
                        cuil != null ? cuil.trim() : "S/D",
                        birthDate != null ? birthDate.trim() : "S/D",
                        province != null ? province.trim() : "Mendoza",
                        sex != null ? sex.trim() : "S/D",
                        benefitStatus,
                        null, 
                        true
                );
            } else if (statusCode == 401 || statusCode == 403) {
                log.warn("El token JWT cacheado ha expirado o no es válido (HTTP {}). Se requiere renovación.", statusCode);
                return new ScrapingResult(null, dni, null, null, null, null, "UNAUTHORIZED", null, false);
            } else {
                String errorMsg = "Error en API directa: HTTP " + statusCode + " - " + response.body();
                log.error(errorMsg);
                return new ScrapingResult(null, dni, null, null, null, null, errorMsg, null, false);
            }
        } catch (Exception e) {
            log.error("Fallo la consulta directa a la API BLSG: {}", e.getMessage());
            return new ScrapingResult(null, dni, null, null, null, null, "Exception: " + e.getMessage(), null, false);
        }
    }

    /**
     * Obtiene de forma tolerante el valor de un campo en un nodo JSON.
     */
    private String getJsonFieldValue(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode valNode = node.get(fieldName);
            if (valNode != null && !valNode.isNull()) {
                return valNode.asText();
            }
            // Búsqueda insensible a mayúsculas/minúsculas
            java.util.Iterator<String> it = node.fieldNames();
            while (it.hasNext()) {
                String key = it.next();
                if (key.equalsIgnoreCase(fieldName)) {
                    JsonNode n = node.get(key);
                    if (n != null && !n.isNull()) {
                        return n.asText();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Captura una imagen de diagnóstico para depuración visual.
     */
    private void takeDiagnosticScreenshot(Page page, String stepName) {
        try {
            Path path = Paths.get(DIAGNOSTICS_PATH, stepName + "_" + System.currentTimeMillis() + ".png");
            page.screenshot(new Page.ScreenshotOptions().setPath(path).setFullPage(true));
            log.info("[DIAGNOSTIC] Screenshot guardada en: {}", path.toAbsolutePath());
        } catch (Exception e) {
            log.warn("[DIAGNOSTIC] Falló la captura de diagnóstico '{}': {}", stepName, e.getMessage());
        }
    }
}
