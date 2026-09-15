package com.hcmcyu.chat.service;

import com.hcmcyu.chat.entity.AttachmentKind;
import com.hcmcyu.chat.exception.ChatServiceException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalChatStorageService implements ChatStorageService {

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Map<String, String> EXTENSION_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif",
            "application/pdf", "pdf",
            "text/plain", "txt",
            "application/msword", "doc",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx",
            "application/vnd.ms-excel", "xls",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"
    );
    private static final Set<String> ALLOWED_ATTACHMENT_TYPES = EXTENSION_BY_CONTENT_TYPE.keySet();

    private final Path attachmentPath;
    private final Path groupAvatarPath;
    private final String attachmentPublicUrlPrefix;
    private final String groupAvatarPublicUrlPrefix;
    private final long maxAttachmentSizeBytes;
    private final long maxGroupAvatarSizeBytes;

    public LocalChatStorageService(
            @Value("${storage.chat.attachment-path:storage/chat-attachments}") String attachmentPath,
            @Value("${storage.chat.group-avatar-path:storage/chat-group-avatars}") String groupAvatarPath,
            @Value("${storage.chat.attachment-public-url-prefix:/uploads/chat/attachments}") String attachmentPublicUrlPrefix,
            @Value("${storage.chat.group-avatar-public-url-prefix:/uploads/chat/group-avatars}") String groupAvatarPublicUrlPrefix,
            @Value("${storage.chat.max-attachment-size-bytes:10485760}") long maxAttachmentSizeBytes,
            @Value("${storage.chat.max-group-avatar-size-bytes:2097152}") long maxGroupAvatarSizeBytes
    ) throws IOException {
        this.attachmentPath = Path.of(attachmentPath).toAbsolutePath().normalize();
        this.groupAvatarPath = Path.of(groupAvatarPath).toAbsolutePath().normalize();
        this.attachmentPublicUrlPrefix = stripTrailingSlash(attachmentPublicUrlPrefix);
        this.groupAvatarPublicUrlPrefix = stripTrailingSlash(groupAvatarPublicUrlPrefix);
        this.maxAttachmentSizeBytes = maxAttachmentSizeBytes;
        this.maxGroupAvatarSizeBytes = maxGroupAvatarSizeBytes;
        Files.createDirectories(this.attachmentPath);
        Files.createDirectories(this.groupAvatarPath);
    }

    @Override
    public StoredFile storeAttachment(MultipartFile file) {
        validate(file, ALLOWED_ATTACHMENT_TYPES, maxAttachmentSizeBytes, "Attachment");
        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        String originalName = sanitizeDisplayName(file.getOriginalFilename());
        String extension = extensionFor(file);
        String filename = UUID.randomUUID() + "." + extension;
        Path target = attachmentPath.resolve(filename).normalize();
        if (!target.startsWith(attachmentPath)) {
            throw invalidFile("Invalid attachment path");
        }
        try {
            file.transferTo(target);
        } catch (IOException exception) {
            throw invalidFile("Could not store attachment");
        }
        return new StoredFile(
                attachmentPublicUrlPrefix + "/" + filename,
                originalName,
                contentType,
                file.getSize(),
                IMAGE_TYPES.contains(contentType) ? AttachmentKind.IMAGE : AttachmentKind.FILE
        );
    }

    @Override
    public String storeGroupAvatar(MultipartFile file) {
        validate(file, IMAGE_TYPES, maxGroupAvatarSizeBytes, "Group avatar");
        String extension = extensionFor(file);
        if (!IMAGE_EXTENSIONS.contains(extension)) {
            throw invalidFile("Invalid group avatar extension");
        }
        String filename = UUID.randomUUID() + "." + extension;
        Path target = groupAvatarPath.resolve(filename).normalize();
        if (!target.startsWith(groupAvatarPath)) {
            throw invalidFile("Invalid group avatar path");
        }
        try {
            file.transferTo(target);
        } catch (IOException exception) {
            throw invalidFile("Could not store group avatar");
        }
        return groupAvatarPublicUrlPrefix + "/" + filename;
    }

    private void validate(MultipartFile file, Set<String> allowedTypes, long maxSizeBytes, String label) {
        if (file == null || file.isEmpty()) {
            throw invalidFile(label + " file is required");
        }
        if (file.getSize() > maxSizeBytes) {
            throw invalidFile(label + " file is too large");
        }
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw invalidFile(label + " file type is not allowed");
        }
        String extension = originalExtension(file);
        if (!EXTENSION_BY_CONTENT_TYPE.containsValue(extension) && !IMAGE_EXTENSIONS.contains(extension)) {
            throw invalidFile(label + " extension is not allowed");
        }
    }

    private String extensionFor(MultipartFile file) {
        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        return EXTENSION_BY_CONTENT_TYPE.getOrDefault(contentType, originalExtension(file));
    }

    private String originalExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw invalidFile("Invalid filename");
        }
        String cleanName = Path.of(originalFilename).getFileName().toString();
        int dotIndex = cleanName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == cleanName.length() - 1) {
            throw invalidFile("File extension is required");
        }
        return cleanName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitizeDisplayName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "file";
        }
        String cleanName = Path.of(originalFilename).getFileName().toString();
        return cleanName.length() > 255 ? cleanName.substring(cleanName.length() - 255) : cleanName;
    }

    private ChatServiceException invalidFile(String message) {
        return new ChatServiceException(HttpStatus.BAD_REQUEST, "INVALID_CHAT_FILE", message);
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
