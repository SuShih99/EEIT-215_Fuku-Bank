package com.javaeasybank.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;



@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // 允許來自 Vue 開發伺服器的請求
                .allowedOrigins("http://localhost:5173")
                // 允許的 HTTP 方法
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                // 允許所有 headers
                .allowedHeaders("*")
                // 允許攜帶 Cookie（Session 需要這個）
                .allowCredentials(true);
    }
}