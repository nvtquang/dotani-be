package com.dotani.chat.service;

import com.dotani.chat.entity.AttachmentKind;
import org.springframework.web.multipart.MultipartFile;

public interface ChatStorageService {

    StoredFile storeAttachment(MultipartFile file);

    String storeGroupAvatar(MultipartFile file);

    record StoredFile(
            String url,
            String originalName,
            String contentType,
            long size,
            AttachmentKind kind
    ) {
    }
}
