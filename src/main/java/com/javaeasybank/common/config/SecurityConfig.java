package com.javaeasybank.common.config;

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

/**
 * Spring Security 設定（階段二：取代 SessionUtil）
 * 
 * 重點：
 * 1. @EnableMethodSecurity → 啟用 @PreAuthorize 註解
 * 2. /api/auth/login 和 /api/auth/logout → 不用登入就能打
 * 3. 其餘所有 /api/** → 必須登入才能打
 * 4. 組員在自己的 Controller 上加 @PreAuthorize("hasRole('XXX')") 控制角色
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ← 這行讓 @PreAuthorize 生效
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configure(http))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()   // 登入不需驗證
                .requestMatchers("/api/auth/logout").permitAll()  // 登出不需驗證
                .anyRequest().authenticated()                     // 其餘都要登入
            )
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

    /**
     * 提供 AuthenticationManager 給 Controller 做登入驗證
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
