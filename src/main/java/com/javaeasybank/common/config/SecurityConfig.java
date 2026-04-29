package com.javaeasybank.common.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Spring Security 設定。
 *
 * 目前為階段一：所有路徑全部放行，由 SessionUtil 手動驗證。
 * 階段二（Week 3）由組長改為路徑權限控制，組員不需要動這個檔案。
 *
 * 禁止組員修改。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 關閉 CSRF 保護
                // 原因：CSRF 是針對瀏覽器 Cookie 的攻擊，
                // 我們用 Session + JSON API，前端用 axios 發請求，
                // 不是傳統的 HTML form submit，不需要這個保護
        .cors(cors -> {})//開啟CORS        
        .csrf(csrf -> csrf.disable())

                // 階段一：全部放行，由 SessionUtil 手動驗證
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        return http.build();
    }
 // 真正的 CORS 設定在這
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}