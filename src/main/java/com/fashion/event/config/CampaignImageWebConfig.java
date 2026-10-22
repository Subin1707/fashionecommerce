package com.fashion.event.config;

import com.fashion.event.service.CampaignImageStorageService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Configuration
public class CampaignImageWebConfig implements WebMvcConfigurer {
    private final CampaignImageStorageService imageStorage;

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> assetHeadersAndTraversalGuard() {
        OncePerRequestFilter filter = new OncePerRequestFilter() {
            @Override
            protected boolean shouldNotFilter(HttpServletRequest request) {
                return !request.getRequestURI().startsWith("/assets/");
            }

            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                response.setHeader("X-Content-Type-Options", "nosniff");
                response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=86400");
                String path = request.getRequestURI();
                String lowerPath = path.toLowerCase(java.util.Locale.ROOT);
                if (path.contains("\\") || lowerPath.contains("%2e") || lowerPath.contains("%2f") || lowerPath.contains("%5c")) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                chain.doFilter(request, response);
            }
        };
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }

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
