package com.superme.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final DailyLoginInterceptor dailyLoginInterceptor;

    @Override
    public void configurePathMatch(@NonNull PathMatchConfigurer configurer) {
        // Automatically adds /v1 prefix to all controller routes
        configurer.addPathPrefix("/v1", clazz -> true);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:uploads/journal/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(dailyLoginInterceptor)
                .addPathPatterns("/**")          // all authenticated APIs
                .excludePathPatterns("/auth/**"); // optional
    }
}