package com.javaeasybank.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具類：產生 / 驗證 / 解析 JWT Token。
 *
 * 用途：客戶端登入後發放 JWT，前端存入 localStorage，
 *       每次請求透過 Axios Interceptor 自動帶入 Authorization Header。
 */
@Component
public class JwtUtil {

    @Value("${app.jwt.secret:DefaultSecretKeyForJavaEasyBankProjectThatIsLongEnough2026}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 產生 JWT Token
     * @param username 使用者帳號
     * @param role 角色（如 CUSTOMER）
     * @param customerId 客戶 ID
     */
    public String generateToken(String username, String role, String customerId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .claim("customerId", customerId)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 從 Token 取得使用者帳號
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 從 Token 取得角色
     */
    public String getRoleFromToken(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * 從 Token 取得客戶 ID
     */
    public String getCustomerIdFromToken(String token) {
        return parseClaims(token).get("customerId", String.class);
    }

    /**
     * 驗證 Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
