package com.hcmcyu.member.service;

import com.hcmcyu.member.exception.MemberServiceException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalStorageService implements StorageService {

    private static final Map<String, String> EXTENSIONS_BY_CONTENT_TYPE = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    private final StorageBucket avatarBucket;
    private final StorageBucket bankQrBucket;

    public LocalStorageService(
            @Value("${storage.local.avatar-path:storage/avatars}") String avatarPath,
            @Value("${storage.local.bank-qr-path:storage/bank-qr}") String bankQrPath,
            @Value("${storage.public-url-prefix:/uploads/avatars}") String publicUrlPrefix,
            @Value("${storage.bank-qr-public-url-prefix:/uploads/bank-qr}") String bankQrPublicUrlPrefix,
            @Value("${storage.avatar.max-size-bytes:2097152}") long maxAvatarSizeBytes,
            @Value("${storage.bank-qr.max-size-bytes:2097152}") long maxBankQrSizeBytes
    ) {
        this.avatarBucket = new StorageBucket(
                Path.of(avatarPath).toAbsolutePath().normalize(),
                stripTrailingSlash(publicUrlPrefix),
                maxAvatarSizeBytes,
                "Avatar",
                "INVALID_AVATAR_FILE",
                "AVATAR_TOO_LARGE",
                "AVATAR_STORAGE_FAILED",
                "AVATAR_DELETE_FAILED"
        );
        this.bankQrBucket = new StorageBucket(
                Path.of(bankQrPath).toAbsolutePath().normalize(),
                stripTrailingSlash(bankQrPublicUrlPrefix),
                maxBankQrSizeBytes,
                "QR Banking image",
                "INVALID_BANK_QR_FILE",
                "BANK_QR_TOO_LARGE",
                "BANK_QR_STORAGE_FAILED",
                "BANK_QR_DELETE_FAILED"
        );
    }

    @Override
    public String storeAvatar(MultipartFile file) {
        return store(file, avatarBucket);
    }

    @Override
    public String storeBankQr(MultipartFile file) {
        return store(file, bankQrBucket);
    }

    @Override
    public Resource loadBankQr(String storedUrl) {
        if (storedUrl == null || !storedUrl.startsWith(bankQrBucket.publicUrlPrefix() + "/")) {
            throw new MemberServiceException(
                    HttpStatus.NOT_FOUND,
                    "BANK_QR_NOT_FOUND",
                    "Banking QR image not found"
            );
        }

        String filename = storedUrl.substring(bankQrBucket.publicUrlPrefix().length() + 1);
        Path target = bankQrBucket.storagePath().resolve(filename).normalize();
        ensureInsideStorage(target, bankQrBucket);
        try {
            Resource resource = new UrlResource(target.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new MemberServiceException(
                        HttpStatus.NOT_FOUND,
                        "BANK_QR_NOT_FOUND",
                        "Banking QR image not found"
                );
            }
            return resource;
        } catch (IOException exception) {
            throw new MemberServiceException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "BANK_QR_READ_FAILED",
                    "Failed to read QR Banking image"
            );
        }
    }

    private String store(MultipartFile file, StorageBucket bucket) {
        validateImage(file, bucket);
        try {
            Files.createDirectories(bucket.storagePath());
            String extension = extensionFor(file);
            String filename = UUID.randomUUID() + "." + extension;
            Path target = bucket.storagePath().resolve(filename).normalize();
            ensureInsideStorage(target, bucket);
            file.transferTo(target);
            return bucket.publicUrlPrefix() + "/" + filename;
        } catch (IOException exception) {
            throw new MemberServiceException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    bucket.storageErrorCode(),
                    "Failed to store " + bucket.label()
            );
        }
    }

    @Override
    public void delete(String storedUrl) {
        if (storedUrl == null) {
            return;
        }
        if (storedUrl.startsWith(avatarBucket.publicUrlPrefix() + "/")) {
            delete(storedUrl, avatarBucket);
        } else if (storedUrl.startsWith(bankQrBucket.publicUrlPrefix() + "/")) {
            delete(storedUrl, bankQrBucket);
        }
    }

    private void delete(String storedUrl, StorageBucket bucket) {
        String filename = storedUrl.substring(bucket.publicUrlPrefix().length() + 1);
        Path target = bucket.storagePath().resolve(filename).normalize();
        ensureInsideStorage(target, bucket);
        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new MemberServiceException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    bucket.deleteErrorCode(),
                    "Failed to delete " + bucket.label()
            );
        }
    }

    private void validateImage(MultipartFile file, StorageBucket bucket) {
        if (file == null || file.isEmpty()) {
            throw invalidFile(bucket, bucket.label() + " file is required");
        }
        if (file.getSize() > bucket.maxSizeBytes()) {
            throw new MemberServiceException(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    bucket.tooLargeErrorCode(),
                    bucket.label() + " file is too large"
            );
        }
        String contentType = file.getContentType();
        if (contentType == null || !EXTENSIONS_BY_CONTENT_TYPE.containsKey(contentType.toLowerCase(Locale.ROOT))) {
            throw invalidFile(bucket, bucket.label() + " must be jpg, jpeg, png, or webp");
        }
        if (!ALLOWED_EXTENSIONS.contains(originalExtension(file))) {
            throw invalidFile(bucket, bucket.label() + " must be jpg, jpeg, png, or webp");
        }
    }

    private String extensionFor(MultipartFile file) {
        String originalExtension = originalExtension(file);
        if ("jpeg".equals(originalExtension)) {
            return "jpg";
        }
        return EXTENSIONS_BY_CONTENT_TYPE.getOrDefault(
                file.getContentType().toLowerCase(Locale.ROOT),
                originalExtension
        );
    }

    private String originalExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private void ensureInsideStorage(Path target, StorageBucket bucket) {
        if (!target.startsWith(bucket.storagePath())) {
            throw invalidFile(bucket, "Invalid image filename");
        }
    }

    private MemberServiceException invalidFile(StorageBucket bucket, String message) {
        return new MemberServiceException(HttpStatus.BAD_REQUEST, bucket.invalidFileErrorCode(), message);
    }

    private String stripTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private record StorageBucket(
            Path storagePath,
            String publicUrlPrefix,
            long maxSizeBytes,
            String label,
            String invalidFileErrorCode,
            String tooLargeErrorCode,
            String storageErrorCode,
            String deleteErrorCode
    ) {
    }
}
