package com.hcmcyu.content.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String storePostImage(MultipartFile file);

    void delete(String storedUrl);
}
