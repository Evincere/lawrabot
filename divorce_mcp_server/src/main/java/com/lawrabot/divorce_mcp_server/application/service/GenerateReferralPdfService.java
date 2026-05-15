package com.lawrabot.divorce_mcp_server.application.service;

import com.lawrabot.divorce_mcp_server.application.port.in.GenerateReferralPdfUseCase;
import com.lawrabot.divorce_mcp_server.application.port.in.ValidateAgreementLegalityUseCase;
import com.lawrabot.divorce_mcp_server.application.port.out.IExpedienteRepository;
import com.lawrabot.divorce_mcp_server.domain.model.Expediente;
import com.lawrabot.divorce_mcp_server.domain.model.Spouse;
import fr.opensagres.xdocreport.converter.ConverterTypeTo;
import fr.opensagres.xdocreport.converter.Options;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenerateReferralPdfService implements GenerateReferralPdfUseCase {

    private final IExpedienteRepository expedienteRepository;
    private final ValidateAgreementLegalityUseCase validateAgreementUseCase;

    @Override
    public String execute(String phoneNumber) {
        Expediente expediente = expedienteRepository.findActiveByClientPhone(phoneNumber)
                .orElseThrow(() -> new IllegalArgumentException("Expediente no encontrado."));

        try {
            // 1. Cargar Plantilla
            InputStream in = new FileInputStream(new File("./templates/resumen_derivacion.docx"));
            IXDocReport report = XDocReportRegistry.getRegistry().loadReport(in, TemplateEngineKind.Freemarker);

            // 2. Preparar Contexto (Variables)
            IContext context = report.createContext();
            
            Spouse pet = expediente.getPetitioner();
            Spouse res = expediente.getRespondent();
            
            String peticionanteNombre = "Sin datos";
            String peticionanteDni = "Sin datos";
            if (pet != null) {
                if (pet.getName() != null) peticionanteNombre = pet.getName().getFullName();
                var petDni = pet.getDni();
                if (petDni != null) peticionanteDni = petDni.getValue();
            }
            context.put("peticionante_nombre", peticionanteNombre);
            context.put("peticionante_dni", peticionanteDni);
            
            String demandadoNombre = "Sin datos";
            String demandadoDni = "Sin datos";
            String demandadoDomicilio = "Sin datos";
            if (res != null) {
                if (res.getName() != null) demandadoNombre = res.getName().getFullName();
                var resDni = res.getDni();
                if (resDni != null) demandadoDni = resDni.getValue();
                var resAddr = res.getAddress();
                if (resAddr != null) demandadoDomicilio = resAddr.toString();
            }
            context.put("demandado_nombre", demandadoNombre);
            context.put("demandado_dni", demandadoDni);
            context.put("demandado_domicilio", demandadoDomicilio);
            
            context.put("tipo_divorcio", expediente.getDivorceType() != null ? expediente.getDivorceType().name() : "No especificado");
            context.put("estado_convenio", expediente.getRegulatoryAgreement() != null ? "BORRADOR CREADO" : "PENDIENTE");
            
            List<String> alerts = validateAgreementUseCase.executeSanityCheck(expediente.getId());
            context.put("alertas", alerts.isEmpty() ? "Sin alertas detectadas" : String.join(", ", alerts));
            context.put("expedienteId", expediente.getId().toString());

            // 3. Generar PDF
            Path outputDir = Paths.get("./storage/referrals");
            if (!Files.exists(outputDir)) Files.createDirectories(outputDir);
            
            String fileName = "referral_" + phoneNumber + ".pdf";
            File outputFile = outputDir.resolve(fileName).toFile();
            
            Options options = Options.getFrom(fr.opensagres.xdocreport.core.document.DocumentKind.DOCX).to(ConverterTypeTo.PDF);
            
            try (OutputStream out = new FileOutputStream(outputFile)) {
                report.convert(context, options, out);
            }
            
            log.info("PDF de derivación generado en: {}", outputFile.getAbsolutePath());
            return outputFile.getAbsolutePath();

        } catch (Exception e) {
            log.error("Error generando PDF de derivación", e);
            throw new RuntimeException("No se pudo generar el PDF de derivación: " + e.getMessage());
        }
    }
}
