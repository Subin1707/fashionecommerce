package com.fashion.event.config;

import com.fashion.event.service.CampaignImageStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CampaignImageWebConfig implements WebMvcConfigurer {
    private final CampaignImageStorageService imageStorage;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/events/**")
                .addResourceLocations(imageStorage.getEventDirectory().toUri().toString());
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(imageStorage.getAvatarDirectory().toUri().toString());
        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations(imageStorage.getProductDirectory().toUri().toString());
    }

    public CampaignImageWebConfig(CampaignImageStorageService imageStorage) {
        this.imageStorage = imageStorage;
    }
}
