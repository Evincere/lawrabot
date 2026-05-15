package com.lawrabot.divorce_mcp_server.application.port.out;

import org.springframework.core.io.Resource;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Puerto de salida para desacoplar el almacenamiento físico
 * de archivos de la lógica del controlador (Arquitectura Hexagonal).
 */
public interface IEvidenceStoragePort {

    /**
     * Guarda la evidencia de un expediente.
     * @param expedienteId el UUID del expediente.
     * @param filePart el archivo transferido vía multipart.
     * @param fileName el nombre del archivo.
     * @return Mono con la ruta absoluta de almacenamiento.
     */
    Mono<String> saveEvidence(UUID expedienteId, FilePart filePart, String fileName);

    /**
     * Carga el archivo como un recurso de Spring.
     * @param filePath la ruta absoluta del archivo en disco.
     * @return Mono con el Resource listo para HTTP.
     */
    Mono<Resource> loadEvidence(String filePath);

    /**
     * Elimina el archivo físico de evidencia.
     * @param filePath la ruta del archivo a eliminar.
     * @return Mono vacío confirmando eliminación.
     */
    Mono<Void> deleteEvidence(String filePath);
}
