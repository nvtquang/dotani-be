package com.hcmcyu.member.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final String avatarPath;
    private final String avatarPublicUrlPrefix;

    public WebMvcConfig(
            @Value("${storage.local.avatar-path:storage/avatars}") String avatarPath,
            @Value("${storage.public-url-prefix:/uploads/avatars}") String avatarPublicUrlPrefix
    ) {
        this.avatarPath = avatarPath;
        this.avatarPublicUrlPrefix = stripTrailingSlash(avatarPublicUrlPrefix);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(avatarPublicUrlPrefix + "/**")
                .addResourceLocations(resourceLocation(avatarPath));
    }

    private String resourceLocation(String storagePath) {
        String location = Path.of(storagePath).toAbsolutePath().normalize().toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }

    private String stripTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
