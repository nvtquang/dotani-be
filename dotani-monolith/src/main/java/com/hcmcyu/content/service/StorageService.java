package com.hcmcyu.content.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String storePostImage(MultipartFile file);

    default String attachmentKind(MultipartFile file) {
        String contentType = file == null ? null : file.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("image/") ? "IMAGE" : "FILE";
    }

    void delete(String storedUrl);
}
