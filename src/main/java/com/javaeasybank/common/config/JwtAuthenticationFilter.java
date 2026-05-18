package com.javaeasybank.common.config;

import com.javaeasybank.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 認證過濾器：
 * 攔截所有請求，檢查 Authorization Header 是否有合法的 Bearer Token。
 * 若有 → 將認證資訊放入 SecurityContext，後續的 @PreAuthorize 即可判斷角色。
 * 若無 → 放行給下一個 Filter（Session 認證）處理。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // 只處理 Bearer Token，其餘放行
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String requestPath = request.getRequestURI();
            String token = authHeader.substring(7);

            if (jwtUtil.validateToken(token)) {
                String username = jwtUtil.getUsernameFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);

                if (shouldAuthenticate(requestPath, role)) {
                    // 建立 Spring Security 認證物件
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 放入 SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldAuthenticate(String requestPath, String role) {
        if ("CUSTOMER".equals(role)) {
            return isCustomerApi(requestPath);
        }
        return isAdminApi(requestPath);
    }

    private boolean isCustomerApi(String requestPath) {
        return requestPath.startsWith("/api/customer/")
                || requestPath.startsWith("/user/")
                || requestPath.startsWith("/api/loan-applications/")
                || requestPath.startsWith("/api/loan-accounts/")
                || requestPath.startsWith("/api/loan-documents/");
    }

    private boolean isAdminApi(String requestPath) {
        return requestPath.startsWith("/api/")
                && !requestPath.startsWith("/api/customer/")
                && !requestPath.startsWith("/api/public/")
                && !requestPath.startsWith("/api/linepay/");
    }
}
