package com.javaeasybank.common.config;

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
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.List;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
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
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/logout").permitAll()
                        .requestMatchers("/api/auth/employees/seed").permitAll()
                        .requestMatchers("/api/customers/seed").permitAll()
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

        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://easybank.project-sandbox-dev.com",
                "https://easybank.project-sandbox-dev.com"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
