package com.hcmcyu.event.service;

import com.hcmcyu.event.exception.EventServiceException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalEventStorageService implements EventStorageService {

    private static final Map<String, String> ALLOWED_EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "application/pdf", "pdf",
            "text/plain", "txt",
            "application/msword", "doc",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx",
            "application/vnd.ms-excel", "xls",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"
    );

    private final Path storagePath;
    private final String publicUrlPrefix;
    private final long maxSizeBytes;

    public LocalEventStorageService(
            @Value("${storage.local.event-attachment-path:storage/event-attachments}") String storagePath,
            @Value("${storage.event-attachment-public-url-prefix:/uploads/event-attachments}") String publicUrlPrefix,
            @Value("${storage.event-attachment.max-size-bytes:10485760}") long maxSizeBytes
    ) {
        this.storagePath = Path.of(storagePath).toAbsolutePath().normalize();
        this.publicUrlPrefix = stripTrailingSlash(publicUrlPrefix);
        this.maxSizeBytes = maxSizeBytes;
    }

    @Override
    public String storeEventAttachment(MultipartFile file) {
        validate(file);
        try {
            Files.createDirectories(storagePath);
            String filename = UUID.randomUUID() + "." + extensionFor(file);
            Path destination = storagePath.resolve(filename).normalize();
            if (!destination.startsWith(storagePath)) {
                throw invalidFile("Invalid event attachment path");
            }
            file.transferTo(destination);
            return publicUrlPrefix + "/" + filename;
        } catch (IOException exception) {
            throw new EventServiceException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "EVENT_ATTACHMENT_STORAGE_FAILED",
                    "Could not store event attachment"
            );
        }
    }

    @Override
    public String attachmentKind(MultipartFile file) {
        String contentType = file == null ? null : file.getContentType();
        return contentType != null && contentType.toLowerCase(Locale.ROOT).startsWith("image/") ? "IMAGE" : "FILE";
    }

    @Override
    public void delete(String storedUrl) {
        if (storedUrl == null || !storedUrl.startsWith(publicUrlPrefix + "/")) {
            return;
        }
        Path target = storagePath.resolve(storedUrl.substring((publicUrlPrefix + "/").length())).normalize();
        if (!target.startsWith(storagePath)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new EventServiceException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "EVENT_ATTACHMENT_DELETE_FAILED",
                    "Could not delete event attachment"
            );
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalidFile("Event attachment is required");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new EventServiceException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "EVENT_ATTACHMENT_TOO_LARGE",
                    "Event attachment exceeds allowed size"
            );
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_EXTENSIONS_BY_CONTENT_TYPE.containsKey(contentType.toLowerCase(Locale.ROOT))) {
            throw invalidFile("Only jpg, jpeg, png, webp, pdf, txt, doc, docx, xls, and xlsx files are allowed");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !hasAllowedExtension(originalFilename)) {
            throw invalidFile("Only jpg, jpeg, png, webp, pdf, txt, doc, docx, xls, and xlsx files are allowed");
        }
    }

    private String extensionFor(MultipartFile file) {
        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        if ("image/jpeg".equals(contentType) && file.getOriginalFilename() != null
                && file.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".jpeg")) {
            return "jpeg";
        }
        return ALLOWED_EXTENSIONS_BY_CONTENT_TYPE.get(contentType);
    }

    private boolean hasAllowedExtension(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".webp")
                || lower.endsWith(".pdf")
                || lower.endsWith(".txt")
                || lower.endsWith(".doc")
                || lower.endsWith(".docx")
                || lower.endsWith(".xls")
                || lower.endsWith(".xlsx");
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private EventServiceException invalidFile(String message) {
        return new EventServiceException(HttpStatus.BAD_REQUEST, "INVALID_EVENT_ATTACHMENT_FILE", message);
    }
}
