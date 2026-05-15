package com.lawrabot.divorce_mcp_server.infrastructure.storage;

import com.lawrabot.divorce_mcp_server.application.port.out.IEvidenceStoragePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.UUID;

@Component
@Slf4j
public class LocalFileSystemStorageAdapter implements IEvidenceStoragePort {

    @Value("${lawrabot.storage.evidence-path:./storage/evidences}")
    private String storagePathBase;

    @SuppressWarnings("null")
    @Override
    public Mono<String> saveEvidence(UUID expedienteId, FilePart filePart, String fileName) {
        return Mono.fromCallable(() -> {
            Path uploadDir = Paths.get(storagePathBase, Objects.requireNonNull(expedienteId).toString());
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            return uploadDir.resolve(Objects.requireNonNull(fileName)).toAbsolutePath();
        })
        .subscribeOn(Schedulers.boundedElastic())
        .flatMap(targetPath -> filePart.transferTo(targetPath).thenReturn(targetPath.toString()))
        .doOnError(e -> log.error("Failed to save evidence physical file for case {}: {}", expedienteId, fileName, e));
    }

    @SuppressWarnings("null")
    @Override
    public Mono<Resource> loadEvidence(String filePath) {
        return Mono.<Resource>fromCallable(() -> {
            Path path = Paths.get(Objects.requireNonNull(filePath));
            Resource resource = new UrlResource(path.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IOException("Resource not found or unreadable: " + filePath);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<Void> deleteEvidence(String filePath) {
        return Mono.<Void>fromCallable(() -> {
            Path path = Paths.get(Objects.requireNonNull(filePath));
            Files.deleteIfExists(path);
            log.info("Deleted physical evidence file at {}", filePath);
            return null;
        }).subscribeOn(Schedulers.boundedElastic())
        .doOnError(e -> log.error("Failed to delete physical file {}: {}", filePath, e.getMessage(), e));
    }
}
