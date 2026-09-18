package com.hcmcyu.event.service;

import org.springframework.web.multipart.MultipartFile;

public interface EventStorageService {

    String storeEventAttachment(MultipartFile file);

    String attachmentKind(MultipartFile file);

    void delete(String storedUrl);
}
