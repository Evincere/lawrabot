package com.lawrabot.divorce_mcp_server.infrastructure.iol;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PlaywrightIolClient {

    private final String username;
    private final String password;
    private final boolean headless;
    private final int timeoutMs;

    public PlaywrightIolClient(
            @Value("${iol.portal.username}") String username,
            @Value("${iol.portal.password}") String password,
            @Value("${iol.portal.headless}") boolean headless,
            @Value("${iol.portal.timeout-ms:35000}") int timeoutMs) {
        this.username = username;
        this.password = password;
        this.headless = headless;
        this.timeoutMs = timeoutMs;
    }

    public boolean testLogin() {
        log.info("Iniciando Playwright para prueba de login en IOL Mendoza...");
        
        try (Playwright playwright = Playwright.create()) {
            BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                    .setHeadless(headless)
                    .setTimeout(timeoutMs);
                    
            try (Browser browser = playwright.chromium().launch(launchOptions);
                 BrowserContext context = browser.newContext();
                 Page page = context.newPage()) {
                 
                page.setDefaultTimeout(timeoutMs);

                // 1. Navegar a la página de login (OIDC)
                String loginUrl = "https://iol.jus.mendoza.gov.ar/auth/realms/iol-mendoza/protocol/openid-connect/auth?client_id=iol-ui&redirect_uri=https%3A%2F%2Fiol.jus.mendoza.gov.ar%2Fiol-ui%2Fp%2Finicio&state=bfc575f5-4345-4745-b1f7-342a75f70cee&nonce=f935365d-799a-4a50-95d5-9ccc2ffbe8fe&response_mode=fragment&response_type=code&scope=openid";
                log.info("Navegando a la URL de login OIDC...");
                page.navigate(loginUrl, new Page.NavigateOptions().setWaitUntil(WaitUntilState.NETWORKIDLE));

                // 2. Completar credenciales
                log.info("Completando credenciales...");
                // Los selectores dependen de Keycloak por lo general (username y password)
                page.fill("input[name='username']", username);
                page.fill("input[name='password']", password);
                
                // 3. Enviar formulario
                log.info("Enviando formulario de login...");
                page.click("input[type='submit'], button[type='submit']");
                
                // 4. Esperar a que redirija al portal y cargue la vista inicial
                log.info("Esperando redirección al portal /iol-ui/p/inicio...");
                page.waitForURL("**/iol-ui/p/inicio**");
                
                // 5. Validar que estamos en la página correcta y la sesión inició
                // Por ejemplo, buscando algo en el DOM que confirme que el portal cargó. 
                // Aquí solo comprobamos la URL.
                String currentUrl = page.url();
                log.info("Login exitoso. URL actual: {}", currentUrl);
                
                return currentUrl.contains("/iol-ui/p/inicio");
                
            } catch (Exception e) {
                log.error("Error durante el flujo de login en Playwright: ", e);
                return false;
            }
        }
    }
}
