package com.hcmcyu.member.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String storeAvatar(MultipartFile file);

    String storeBankQr(MultipartFile file);

    Resource loadBankQr(String storedUrl);

    void delete(String storedUrl);
}
