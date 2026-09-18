package com.hcmcyu.shared.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private final String avatarPath;
    private final String bankQrPath;
    private final String postImagePath;
    private final String eventAttachmentPath;
    private final String chatAttachmentPath;
    private final String chatGroupAvatarPath;

    public StaticResourceConfig(
            @Value("${storage.local.avatar-path:storage/avatars}") String avatarPath,
            @Value("${storage.local.bank-qr-path:storage/bank-qr}") String bankQrPath,
            @Value("${storage.local.post-image-path:storage/post-images}") String postImagePath,
            @Value("${storage.local.event-attachment-path:storage/event-attachments}") String eventAttachmentPath,
            @Value("${storage.chat.attachment-path:storage/chat-attachments}") String chatAttachmentPath,
            @Value("${storage.chat.group-avatar-path:storage/chat-group-avatars}") String chatGroupAvatarPath
    ) {
        this.avatarPath = avatarPath;
        this.bankQrPath = bankQrPath;
        this.postImagePath = postImagePath;
        this.eventAttachmentPath = eventAttachmentPath;
        this.chatAttachmentPath = chatAttachmentPath;
        this.chatGroupAvatarPath = chatGroupAvatarPath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        add(registry, "/uploads/avatars/**", avatarPath);
        add(registry, "/uploads/bank-qr/**", bankQrPath);
        add(registry, "/uploads/post-images/**", postImagePath);
        add(registry, "/uploads/event-attachments/**", eventAttachmentPath);
        add(registry, "/uploads/chat/attachments/**", chatAttachmentPath);
        add(registry, "/uploads/chat/group-avatars/**", chatGroupAvatarPath);
    }

    private void add(ResourceHandlerRegistry registry, String pattern, String storagePath) {
        registry.addResourceHandler(pattern).addResourceLocations(resourceLocation(storagePath));
    }

    private String resourceLocation(String storagePath) {
        String location = Path.of(storagePath).toAbsolutePath().normalize().toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
