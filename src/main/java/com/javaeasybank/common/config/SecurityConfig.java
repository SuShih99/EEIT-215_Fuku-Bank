package com.javaeasybank.common.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

/**
 * Spring Security 設定
 * <p>
 * 重點：
 * 1. @EnableMethodSecurity → 啟用 @PreAuthorize 註解
 * 2. 管理端：Session-based 認證（/api/auth/**）
 * 3. 客戶端：JWT-based 認證（/api/customer/auth/**）
 * 4. JwtAuthenticationFilter 在 UsernamePasswordAuthenticationFilter 之前執行
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ← 這行讓 @PreAuthorize 生效
public class SecurityConfig {
    private static final Duration ADMIN_SESSION_TIMEOUT = Duration.ofHours(8);

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 關閉 CSRF 保護
                // 原因：CSRF 是針對瀏覽器 Cookie 的攻擊，
                // 我們用 Session + JSON API，前端用 axios 發請求，
                // 不是傳統的 HTML form submit，不需要這個保護
                .csrf(csrf -> csrf.disable())

                // 開啟 CORS，Spring Security 會自動去找底下名為 corsConfigurationSource 的 Bean
                .cors(cors -> {
                })

                // 路由權限設定
                .authorizeHttpRequests(auth -> auth
                        // === 管理端：登入/登出 不需驗證 ===
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/logout").permitAll()

                        // === 管理端：Seed 測試資料 ===
                        .requestMatchers("/api/auth/employees/seed").permitAll()
                        .requestMatchers("/api/customers/seed").permitAll()

                        // === 客戶端：註冊、登入、密碼重設 不需驗證 ===
                        .requestMatchers("/api/customer/auth/register").permitAll()
                        .requestMatchers("/api/customer/auth/login").permitAll()
                        .requestMatchers("/api/customer/auth/request-reset").permitAll()
                        .requestMatchers("/api/customer/auth/reset-password").permitAll()
                        .requestMatchers("/api/customer/auth/verify-email").permitAll()
                        .requestMatchers("/api/customer/auth/seed").permitAll()

                        // === 靜態資源：大頭照可公開存取 & 圖片可公開存取 ===
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/img/**").permitAll()

                        // === 公開 API & 付款API===
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/linepay/**").permitAll()
                        .requestMatchers("/api/loan-applications/rate-rules").permitAll()

                        // === 風控接口全部鎖在本機ip ===
                        .requestMatchers("/api/risk/**").access(new WebExpressionAuthorizationManager("hasIpAddress('127.0.0.1') or hasIpAddress('::1')"))
                        .requestMatchers("/api/loan-callbacks/**").access(new WebExpressionAuthorizationManager("hasIpAddress('127.0.0.1') or hasIpAddress('::1')"))
                        .requestMatchers("/api/transfer-callbacks/**").access(new WebExpressionAuthorizationManager("hasIpAddress('127.0.0.1') or hasIpAddress('::1')"))
                        .requestMatchers("/api/account-callbacks/**").access(new WebExpressionAuthorizationManager("hasIpAddress('127.0.0.1') or hasIpAddress('::1')"))
                        // === 其餘都要登入 ===
                        .anyRequest().authenticated()
                )

                // 在 UsernamePasswordAuthenticationFilter 之前加入 JWT 過濾器
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .formLogin(form -> form.disable())       // 不用 Spring 預設登入頁
                .httpBasic(basic -> basic.disable());     // 不用 HTTP Basic 認證

        return http.build();
    }

    /**
     * 密碼加密器
     * 目前測試階段密碼都是 $2a$10$dummyhash...
     * 正式上線要用 BCrypt 加密
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    ServletContextInitializer sessionTimeoutInitializer() {
        return servletContext -> servletContext.setSessionTimeout((int) ADMIN_SESSION_TIMEOUT.toMinutes());
    }

    /**
     * 提供 AuthenticationManager 給 Controller 做登入驗證
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * 真正的 CORS 設定在這
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
