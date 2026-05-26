package com.javaeasybank.customer.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.common.util.JwtUtil;
import com.javaeasybank.customer.dto.response.CustomerDeviceResponse;
import com.javaeasybank.customer.dto.response.CustomerLoginLogResponse;
import com.javaeasybank.customer.service.CustomerSecurityService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/security")
@RequiredArgsConstructor
public class CustomerSecurityController {

    private final CustomerSecurityService customerSecurityService;
    private final JwtUtil jwtUtil;

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/login-logs")
    public ResponseEntity<ApiResponse<List<CustomerLoginLogResponse>>> getLoginLogs(HttpServletRequest request) {
        String customerId = extractCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(customerSecurityService.getLoginLogs(customerId)));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/devices")
    public ResponseEntity<ApiResponse<List<CustomerDeviceResponse>>> getDevices(HttpServletRequest request) {
        String customerId = extractCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(customerSecurityService.getDevices(customerId)));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<ApiResponse<String>> revokeDevice(HttpServletRequest request,
                                                            @PathVariable Long deviceId) {
        String customerId = extractCustomerId(request);
        customerSecurityService.revokeDevice(customerId, deviceId);
        return ResponseEntity.ok(ApiResponse.success("裝置授權已移除"));
    }

    private String extractCustomerId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.getCustomerIdFromToken(token);
        }
        throw new BusinessException("無法取得客戶身分資訊");
    }
}
