package com.lawrabot.divorce_mcp_server.infrastructure.mcp;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.lawrabot.divorce_mcp_server.application.port.in.TestIolLoginUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class IolMcpController {

    private final TestIolLoginUseCase testIolLoginUseCase;

    @Tool(name = "iol_login_test", description = "Realiza una prueba de inicio de sesión automatizado en el portal judicial IOL Mendoza. Usa las credenciales configuradas en el entorno.")
    public String iolLoginTest(
            @JsonPropertyDescription("Confirmación explícita del operador humano para ejecutar la prueba de login (ej. 'si')") String operatorConfirmation) {
        
        if (operatorConfirmation == null || !operatorConfirmation.trim().equalsIgnoreCase("si")) {
            return "Prueba de login cancelada. El operador debe confirmar enviando 'si'.";
        }

        log.info("Tool MCP: iol_login_test - Iniciando ejecución...");
        boolean success = testIolLoginUseCase.execute();
        
        if (success) {
            return "✅ ¡Login exitoso en IOL Mendoza! El navegador automatizado logró ingresar al portal y validar la URL `/iol-ui/p/inicio`.";
        } else {
            return "❌ Error en el inicio de sesión de IOL. Revisa los logs del servidor para ver posibles errores de Playwright o problemas de credenciales.";
        }
    }
}
