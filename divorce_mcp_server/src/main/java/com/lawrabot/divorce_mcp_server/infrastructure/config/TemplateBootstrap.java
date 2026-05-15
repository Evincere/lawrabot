package com.lawrabot.divorce_mcp_server.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@Slf4j
public class TemplateBootstrap implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(@org.springframework.lang.NonNull ApplicationReadyEvent event) {
        try {
            Path templateDir = Paths.get("./templates");
            File dir = templateDir.toFile();
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (created) log.info("Directorio templates/ creado exitosamente.");
            }

            File templateFile = templateDir.resolve("demanda_base.docx").toFile();
            if (!templateFile.exists()) {
                log.info("Creando plantilla base de demanda: {}", templateFile.getAbsolutePath());
                try (XWPFDocument document = new XWPFDocument()) {
                    
                    XWPFParagraph titleParagraph = document.createParagraph();
                    titleParagraph.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
                    XWPFRun titleRun = titleParagraph.createRun();
                    titleRun.setText("INICIA DEMANDA DE DIVORCIO");
                    titleRun.setBold(true);
                    titleRun.setFontSize(14);
                    
                    document.createParagraph().createRun().addBreak();
                    
                    XWPFParagraph body = document.createParagraph();
                    XWPFRun run = body.createRun();
                    run.setText("Señor Juez:");
                    run.addBreak();
                    run.setText("${peticionante_nombre}, DNI ${peticionante_dni}, con domicilio en ${peticionante_domicilio}, se presenta y dice:");
                    run.addBreak();
                    run.setText("1. OBJETO");
                    run.addBreak();
                    run.setText("Vengo a promover demanda de divorcio vincular por presentación unilateral contra ${demandado_nombre}, DNI ${demandado_dni}, con domicilio en ${demandado_domicilio}.");
                    run.addBreak();
                    run.setText("2. HECHOS");
                    run.addBreak();
                    run.setText("Contraje matrimonio con la parte demandada el ${fechaMatrimonio}. Nos separamos de hecho el ${fechaSeparacion}.");
                    run.addBreak();
                    run.setText("3. CONVENIO REGULADOR");
                    run.addBreak();
                    run.setText("¿Presenta propuesta de convenio regulador adjunta?: ${tieneConvenio}");
                    run.addBreak();
                    run.setText("Expediente de Referencia interno MPD: ${expedienteId}");

                    try (FileOutputStream out = new FileOutputStream(templateFile)) {
                        document.write(out);
                    }
                }
                log.info("Plantilla generada correctamente.");
            } else {
                log.info("La plantilla demanda_base.docx ya existe.");
            }

            File referralTemplate = templateDir.resolve("resumen_derivacion.docx").toFile();
            if (!referralTemplate.exists()) {
                log.info("Creando plantilla de derivación: {}", referralTemplate.getAbsolutePath());
                try (XWPFDocument document = new XWPFDocument()) {
                    XWPFParagraph title = document.createParagraph();
                    title.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
                    XWPFRun titleRun = title.createRun();
                    titleRun.setText("INFORME DE DERIVACIÓN - LAWRABOT");
                    titleRun.setBold(true);
                    titleRun.setFontSize(16);
                    
                    XWPFParagraph intro = document.createParagraph();
                    intro.createRun().setText("Este documento resume la información recolectada por el asistente virtual. Se recomienda atención presencial debido a alertas técnicas en la validación del convenio.");
                    
                    XWPFParagraph data = document.createParagraph();
                    XWPFRun dr = data.createRun();
                    dr.addBreak();
                    dr.setText("SOLICITANTE: ${peticionante_nombre} (DNI ${peticionante_dni})");
                    dr.addBreak();
                    dr.setText("CONTRA-PARTE: ${demandado_nombre} (DNI ${demandado_dni})");
                    dr.addBreak();
                    dr.setText("DOMICILIO CONTRA-PARTE: ${demandado_domicilio}");
                    dr.addBreak();
                    dr.setText("TIPO DIVORCIO: ${tipo_divorcio}");
                    dr.addBreak();
                    dr.setText("ESTADO DEL CONVENIO: ${estado_convenio}");
                    dr.addBreak();
                    dr.setText("ALERTAS DETECTADAS: ${alertas}");
                    dr.addBreak();
                    dr.addBreak();
                    dr.setText("ID EXPEDIENTE: ${expedienteId}");

                    try (FileOutputStream out = new FileOutputStream(referralTemplate)) {
                        document.write(out);
                    }
                }
                log.info("Plantilla de derivación generada correctamente.");
            } else {
                log.info("La plantilla resumen_derivacion.docx ya existe.");
            }
        } catch (Exception e) {
            log.error("Error inicializando plantillas", e);
        }
    }
}
