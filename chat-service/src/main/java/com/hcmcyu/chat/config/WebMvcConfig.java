package com.hcmcyu.chat.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final String attachmentPath;
    private final String groupAvatarPath;
    private final String attachmentPublicUrlPrefix;
    private final String groupAvatarPublicUrlPrefix;

    public WebMvcConfig(
            @Value("${storage.chat.attachment-path:storage/chat-attachments}") String attachmentPath,
            @Value("${storage.chat.group-avatar-path:storage/chat-group-avatars}") String groupAvatarPath,
            @Value("${storage.chat.attachment-public-url-prefix:/uploads/chat/attachments}") String attachmentPublicUrlPrefix,
            @Value("${storage.chat.group-avatar-public-url-prefix:/uploads/chat/group-avatars}") String groupAvatarPublicUrlPrefix
    ) {
        this.attachmentPath = attachmentPath;
        this.groupAvatarPath = groupAvatarPath;
        this.attachmentPublicUrlPrefix = stripTrailingSlash(attachmentPublicUrlPrefix);
        this.groupAvatarPublicUrlPrefix = stripTrailingSlash(groupAvatarPublicUrlPrefix);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(attachmentPublicUrlPrefix + "/**")
                .addResourceLocations(resourceLocation(attachmentPath));
        registry.addResourceHandler(groupAvatarPublicUrlPrefix + "/**")
                .addResourceLocations(resourceLocation(groupAvatarPath));
    }

    private String resourceLocation(String storagePath) {
        String location = Path.of(storagePath).toAbsolutePath().normalize().toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }

    private String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
