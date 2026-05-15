package com.lawrabot.divorce_mcp_server.application.service;

import fr.opensagres.xdocreport.converter.ConverterTypeTo;
import fr.opensagres.xdocreport.converter.ConverterTypeVia;
import fr.opensagres.xdocreport.converter.Options;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@Service
@Slf4j
public class DocumentGeneratorService {

    @Value("${lawrabot.storage.evidence-path:./storage/evidences}")
    private String storageBase;

    /**
     * Reemplaza variables en un DOCX y convierte a PDF o guarda como DOCX final.
     * @param templatePath Ruta base del archivo .docx a rellenar
     * @param outputFileName Nombre que llevará el archivo de salida
     * @param contextVariables Diccionario de variables para reemplazar en el documento
     * @param asPdf Si true, convierte a PDF usando XDocReport
     * @return Ruta absoluta del archivo generado
     */
    public String generateDocument(String templatePath, String expedienteId, String outputFileName, Map<String, Object> contextVariables, boolean asPdf) throws Exception {
        log.info("Iniciando generación de documento para expediente {} usando template {}", expedienteId, templatePath);

        File templateFile = new File(templatePath);
        if (!templateFile.exists()) {
            throw new FileNotFoundException("Template no encontrado en: " + templatePath);
        }

        Path outputDir = Paths.get(storageBase, expedienteId, "drafts");
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
        }

        String finalExtension = asPdf ? ".pdf" : ".docx";
        Path outputPath = outputDir.resolve(outputFileName + finalExtension);

        try (InputStream in = new FileInputStream(templateFile);
             OutputStream out = new FileOutputStream(outputPath.toFile())) {

            // 1. Cargar el documento (soportando Freemarker)
            IXDocReport report = XDocReportRegistry.getRegistry().loadReport(in, TemplateEngineKind.Freemarker);

            // 2. Crear contexto e inyectar variables
            IContext context = report.createContext();
            for (Map.Entry<String, Object> entry : contextVariables.entrySet()) {
                context.put(entry.getKey(), entry.getValue());
            }

            // 3. Procesar y Convertir
            if (asPdf) {
                Options options = Options.getTo(ConverterTypeTo.PDF).via(ConverterTypeVia.XWPF);
                report.convert(context, options, out);
            } else {
                report.process(context, out);
            }
            
            log.info("Documento generado exitosamente en {}", outputPath.toAbsolutePath());
            return outputPath.toAbsolutePath().toString();

        } catch (Exception e) {
            log.error("Excepción al intentar generar y transformar el documento", e);
            throw e;
        }
    }
}
