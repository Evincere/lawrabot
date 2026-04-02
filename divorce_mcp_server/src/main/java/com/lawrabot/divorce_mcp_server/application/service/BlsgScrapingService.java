package com.lawrabot.divorce_mcp_server.application.service;

import com.lawrabot.divorce_mcp_server.application.port.in.ConsultarBlsgUseCase;
import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.enums.BlsgScrapingResultEnum;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.domain.enums.ExpedienteStatusEnum;
import com.lawrabot.divorce_mcp_server.domain.enums.DivorceTypeEnum;
import com.lawrabot.divorce_mcp_server.domain.valueobject.PhoneNumberVO;
import com.lawrabot.divorce_mcp_server.infrastructure.config.BlsgProperties;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
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
    private static final String BLSG_URL = "https://blsg.pjm.gob.ar/";
    private static final String STORAGE_PATH = "storage/certificates/";

    public BlsgScrapingService(IExpedienteRepository expedienteRepository, BlsgProperties blsgProperties) {
        this.expedienteRepository = expedienteRepository;
        this.blsgProperties = blsgProperties;
        try {
            Files.createDirectories(Paths.get(STORAGE_PATH));
        } catch (IOException e) {
            log.error("No se pudo crear el directorio de certificados: {}", e.getMessage());
        }
    }

    @Override
    public ScrapingResult execute(String phoneNumber, String dni) {
        log.info("Iniciando scraping BLSG para DNI: {} y teléfono: {}", dni, phoneNumber);

        Expediente expediente = expedienteRepository.findActiveByClientPhone(phoneNumber).orElse(null);
        java.util.Optional<Expediente> existingByDni = expedienteRepository.findActiveByDni(dni);

        if (existingByDni.isPresent()) {
            Expediente existing = existingByDni.get();
            if (existing.getStatus() != ExpedienteStatusEnum.BLSG_PRECONSULTA) {
                log.warn("Se abortó scraping: Ya existe trámite activo en progreso para DNI {}", dni);
                return new ScrapingResult(null, dni, null, null, null, null, "Error: Ya existe un trámite de divorcio activo para este DNI.", null, false);
            }
            log.info("Reutilizando Expediente Borrador existente para DNI {}", dni);
            expediente = existing;
        } else if (expediente != null) {
             if (expediente.getStatus() != ExpedienteStatusEnum.BLSG_PRECONSULTA) {
                log.warn("Se abortó scraping: Ya existe un trámite activo en progreso para celular {}", phoneNumber);
                return new ScrapingResult(null, dni, null, null, null, null, "Error: Ya existe un trámite de divorcio activo en progreso para este celular.", null, false);
             }
        } else {
            log.info("Creando Expediente Borrador para la consulta BLSG...");
            expediente = Expediente.createNew(PhoneNumberVO.of(phoneNumber), DivorceTypeEnum.UNILATERAL);
            expediente = expedienteRepository.save(expediente);
        }

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            // Establecer un User-Agent real y un Viewport fijo para evitar bloqueos y asegurar visibilidad
            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                    .setViewportSize(1280, 800));
            Page page = context.newPage();

            // 1. Navegación e Inicio de Sesión
            log.info("Navegando a {}", BLSG_URL);
            page.navigate(BLSG_URL);
            
            // Esperar a que la página se estabilice (con timeout de 10s para no colgar el Request MCP)
            try {
                page.waitForLoadState(com.microsoft.playwright.options.LoadState.NETWORKIDLE, 
                    new Page.WaitForLoadStateOptions().setTimeout(10000));
            } catch (Exception e) {
                log.warn("NetworkIdle timeout excedido, pero continuaremos con el flujo...");
            }
            
            if (page.title().contains("Sign in") || page.url().contains("microsoftonline.com")) {
                log.info("Inicio de sesión detectado. Procediendo con la autenticación...");
                
                // Paso 1: Email
                page.waitForSelector("input#i0116");
                page.fill("input#i0116", blsgProperties.getUsername());
                page.click("input#idSIButton9"); // Botón 'Siguiente'
                
                // Paso 2: Password
                page.waitForSelector("input#i0118");
                page.fill("input#i0118", blsgProperties.getPassword());
                page.click("input#idSIButton9"); // Botón 'Iniciar sesión'
                
                // Paso 3: ¿Mantener la sesión iniciada? -> Seleccionamos 'No'
                try {
                    page.waitForSelector("input#idBtn_Back", new Page.WaitForSelectorOptions().setTimeout(5000));
                    page.click("input#idBtn_Back");
                } catch (Exception e) {
                    log.warn("No se detectó la pantalla de 'Mantener sesión', continuando...");
                }
                
                // Esperar a aterrizar en la página de BLSG
                page.waitForSelector("input#dni");
                log.info("Sesión iniciada exitosamente.");
                // Pequeño respiro para que el formulario se estabilice tras el login
                page.waitForTimeout(2000);
            }

            // 2. Ingreso de DNI
            log.info("Buscando DNI: {}", dni);
            // Usamos un selector mucho más específico para evitar el conflicto con el radio button de ID 'dni'
            // ID duplicado detectado en el portal judicial: radio button y text input comparten el mismo ID
            page.locator("input[type='text']#dni").first().fill(dni);
            
            // Hacemos el click en el botón de consulta o presionamos ENTER (más robusto)
            Response response = page.waitForResponse(
                res -> res.url().contains("/consulta-persona") && "POST".equalsIgnoreCase(res.request().method()),
                () -> {
                    page.keyboard().press("Enter");
                    // Por si ENTER no funciona, también intentamos el clic tras un breve intervalo
                    page.waitForTimeout(500);
                    // Si el card no aparece pronto, el backend de Playwright intentará el clic
                }
            );

            if (response.status() != 200) {
                log.warn("La API judicial respondió con status: {}", response.status());
            }

            // 3. Esperar resultado (basado en texto)
            // Esperar a que el DNI aparezca en pantalla como confirmación de resultado. Timeout de 15s para no exceder los 60s de MCP.
            page.waitForSelector("text=DNI " + dni, new Page.WaitForSelectorOptions().setTimeout(15000));
            log.info("Resultado de la consulta detectado para DNI: {}", dni);

            // 4. Extraer metadatos usando relaciones de texto y hermanos (DOM real sin .card)
            // El nombre suele ser el encabezado inmediatamente anterior al texto del DNI
            String fullName = page.locator("text=DNI " + dni).locator("xpath=preceding-sibling::h3").innerText().trim();
            String cuil = extractValue(page, "CUIL:");
            String birthDate = extractValue(page, "Fecha Nacimiento:"); // Ajustado según DOM real
            String province = extractValue(page, "Provincia:");
            String sex = extractValue(page, "Sexo:");

            log.info("Datos extraídos: {}, CUIL: {}", fullName, cuil);

            // 5. Consulta de otorgamiento (Botón Consultar global o por rol)
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Consultar")).last().click();

            // 6. Esperar el resultado del beneficio
            page.waitForTimeout(2000); 
            
            String benefitStatus = "Desconocido";
            boolean isApproved = false;
            
            try {
                // Buscamos el texto de otorgamiento directamente en la página
                Locator resultBlock = page.getByText("Se otorga el Beneficio de Litigar sin Gastos", new Page.GetByTextOptions().setExact(false));
                if (resultBlock.isVisible()) {
                    benefitStatus = "Se otorga el Beneficio de Litigar sin Gastos";
                    isApproved = true;
                    log.info("Beneficio otorgado.");
                } else {
                    benefitStatus = "Beneficio no otorgado o requiere revisión.";
                    log.warn("Beneficio no otorgado.");
                }
            } catch (Exception e) {
                 log.error("Error al detectar el resultado del beneficio: {}", e.getMessage());
            }

            // 7. Descarga de Constancia (Siempre se intenta obtener según feedback del usuario)
            String certificatePath = null;
            try {
                Download download = page.waitForDownload(() -> {
                    page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Descargar Constancia")).click();
                });
                
                Path target = Paths.get(STORAGE_PATH, "BLSG_" + dni + "_" + UUID.randomUUID() + ".pdf");
                download.saveAs(target);
                certificatePath = target.toAbsolutePath().toString();
                log.info("Constancia descargada en: {}", certificatePath);
            } catch (Exception e) {
                log.error("Fallo la descarga de la constancia (puede que no esté disponible): {}", e.getMessage());
            }

            // 8. Actualizar dominio
            BlsgScrapingResultEnum resultEnum = isApproved ? BlsgScrapingResultEnum.PROVISIONALLY_APPROVED : BlsgScrapingResultEnum.PROVISIONALLY_REJECTED;
            expediente.processScrapingResult(resultEnum, benefitStatus, fullName, dni, cuil, birthDate, province, sex, certificatePath);
            expedienteRepository.save(expediente);

            browser.close();

            return new ScrapingResult(
                    fullName, dni, cuil, birthDate, province, sex, benefitStatus, certificatePath, true
            );

        } catch (Exception e) {
            log.error("Error crítico durante el scraping de BLSG: {}", e.getMessage());
            return new ScrapingResult(null, dni, null, null, null, null, "Error: " + e.getMessage(), null, false);
        }
    }

    private String extractValue(Page page, String label) {
        try {
            // Buscamos el texto que contiene la etiqueta (ej. "CUIL:") globalmente
            String fullText = page.locator("div, p, span").filter(new Locator.FilterOptions().setHasText(label)).first().innerText();
            if (fullText.contains(":")) {
                return fullText.split(":")[1].trim();
            }
            return fullText.replace(label, "").trim();
        } catch (Exception e) {
            return "No disponible";
        }
    }
}
