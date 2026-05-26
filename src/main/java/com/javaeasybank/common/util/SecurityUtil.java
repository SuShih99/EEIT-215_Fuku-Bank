package com.javaeasybank.common.util;

import org.springframework.stereotype.Component;

import com.javaeasybank.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SecurityUtil {

    private final JwtUtil jwtUtil;

    /**
     * 從 Authorization Header 取得目前登入的客戶 ID
     * 
     * @param authHeader 格式通常為 "Bearer <token>"
     * @return 客戶 ID，若解析失敗則回傳 null
     */
    public String getCustomerIdFromHeader(String authHeader) {
        if (authHeader == null
                || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(
                    "Missing Authorization Header");
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException("Invalid Token");
        }
        String customerId = jwtUtil.getCustomerIdFromToken(token);
        if (customerId == null) {
            throw new BusinessException(
                    "CustomerId not found");
        }
        return customerId;
    }

    /**
     * 從 Authorization Header 取得目前登入的角色
     * 
     * @param authHeader 格式通常為 "Bearer <token>"
     * @return 角色名稱，若解析失敗則回傳 null
     */
    public String getRoleFromHeader(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("Missing Authorization Header");
        }
        String token = authHeader.substring(7);
        if (jwtUtil.validateToken(token)) {
            return jwtUtil.getRoleFromToken(token);
        }
        return null;
    }

}
