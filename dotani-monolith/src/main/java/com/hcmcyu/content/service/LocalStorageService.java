package com.hcmcyu.content.service;

import com.hcmcyu.content.exception.ContentServiceException;
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
public class LocalStorageService implements StorageService {

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

    public LocalStorageService(
            @Value("${storage.local.post-image-path:storage/post-images}") String storagePath,
            @Value("${storage.post-image-public-url-prefix:/uploads/post-images}") String publicUrlPrefix,
            @Value("${storage.post-image.max-size-bytes:5242880}") long maxSizeBytes
    ) {
        this.storagePath = Path.of(storagePath).toAbsolutePath().normalize();
        this.publicUrlPrefix = stripTrailingSlash(publicUrlPrefix);
        this.maxSizeBytes = maxSizeBytes;
    }

    @Override
    public String storePostImage(MultipartFile file) {
        validate(file);
        try {
            Files.createDirectories(storagePath);
            String extension = extensionFor(file);
            String filename = UUID.randomUUID() + "." + extension;
            Path destination = storagePath.resolve(filename).normalize();
            if (!destination.startsWith(storagePath)) {
                throw invalidFile("Invalid file path");
            }
            file.transferTo(destination);
            return publicUrlPrefix + "/" + filename;
        } catch (IOException exception) {
            throw new ContentServiceException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "POST_ATTACHMENT_STORAGE_FAILED",
                    "Could not store post attachment"
            );
        }
    }

    @Override
    public void delete(String storedUrl) {
        if (storedUrl == null || storedUrl.isBlank()) {
            return;
        }
        if (!storedUrl.startsWith(publicUrlPrefix + "/")) {
            return;
        }
        String filename = storedUrl.substring((publicUrlPrefix + "/").length());
        Path target = storagePath.resolve(filename).normalize();
        if (!target.startsWith(storagePath)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new ContentServiceException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "POST_ATTACHMENT_DELETE_FAILED",
                    "Could not delete post attachment"
            );
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalidFile("Post attachment is required");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new ContentServiceException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    "POST_ATTACHMENT_TOO_LARGE",
                    "Post attachment exceeds allowed size"
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
        if ("image/jpeg".equals(contentType)) {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.toLowerCase(Locale.ROOT).endsWith(".jpeg")) {
                return "jpeg";
            }
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
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private ContentServiceException invalidFile(String message) {
        return new ContentServiceException(HttpStatus.BAD_REQUEST, "INVALID_POST_ATTACHMENT_FILE", message);
    }
}
