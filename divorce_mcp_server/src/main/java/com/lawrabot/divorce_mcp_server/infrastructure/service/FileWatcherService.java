package com.lawrabot.divorce_mcp_server.infrastructure.service;

import com.lawrabot.divorce_mcp_server.domain.enums.ExpedienteStatusEnum;
import com.lawrabot.divorce_mcp_server.infrastructure.persistence.repository.jpa.SpringDataExpedienteRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
public class FileWatcherService {

    @Value("${lawrabot.storage.evidence-path:./storage/evidences}")
    private String storageBase;

    private final SpringDataExpedienteRepository expedienteRepository;
    private WatchService watchService;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public FileWatcherService(SpringDataExpedienteRepository expedienteRepository) {
        this.expedienteRepository = expedienteRepository;
    }

    @PostConstruct
    public void init() {
        try {
            watchService = FileSystems.getDefault().newWatchService();
            // Start the watching thread
            executorService.submit(this::watchDirectory);
            log.info("FileWatcherService initialized for asynchronous signature tracking.");
        } catch (IOException e) {
            log.error("Failed to initialize WatchService", e);
        }
    }

    /**
     * Registers a specific expediente's /signed/ folder to be watched dynamically.
     * This is called when the drafting is generated.
     */
    public void registerExpedienteForWatching(String expedienteId) {
        Path signedDir = Paths.get(storageBase, expedienteId, "signed");
        try {
            if (!Files.exists(signedDir)) {
                Files.createDirectories(signedDir);
            }
            signedDir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
            log.info("Registered Watcher for folder: {}", signedDir.toAbsolutePath());
        } catch (IOException e) {
            log.error("Error registering watch for directory " + signedDir, e);
        }
    }

    private void watchDirectory() {
        while (!Thread.currentThread().isInterrupted()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException x) {
                Thread.currentThread().interrupt();
                return;
            } catch (ClosedWatchServiceException e) {
                log.info("Watch service closed. Stopping watcher thread.");
                return;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path filename = ev.context();
                Path dir = (Path) key.watchable();
                Path child = dir.resolve(filename);

                log.info("Detected new/modified signed file: {}", child);

                // The parent folder of /signed/ is the expediente ID
                String expedienteIdStr = dir.getParent().getFileName().toString();
                try {
                    UUID id = UUID.fromString(expedienteIdStr);
                    if (id != null) {
                        expedienteRepository.findById(id).ifPresent(entity -> {
                            if (entity.getStatus() == ExpedienteStatusEnum.WAITING_SIGNATURE || entity.getStatus() == ExpedienteStatusEnum.DOCUMENTS_GENERATED) {
                                entity.setStatus(ExpedienteStatusEnum.READY_FOR_PORTAL);
                                expedienteRepository.save(entity);
                                log.info("Expediente {} status updated to READY_FOR_PORTAL because signed doc was detected.", id);
                            }
                        });
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("Folder name {} is not a valid UUID. Ignoring event.", expedienteIdStr);
                }
            }

            boolean valid = key.reset();
            if (!valid) {
                // The directory is inaccessible (deleted?)
                log.warn("WatchKey is no longer valid. Directory might have been deleted.");
            }
        }
    }

    @PreDestroy
    public void destroy() {
        try {
            if (watchService != null) {
                watchService.close();
            }
            executorService.shutdownNow();
        } catch (IOException e) {
            log.error("Error closing watch service", e);
        }
    }
}
