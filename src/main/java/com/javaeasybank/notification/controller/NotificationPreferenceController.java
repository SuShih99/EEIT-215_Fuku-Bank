package com.javaeasybank.notification.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.common.util.JwtUtil;
import com.javaeasybank.notification.dto.NotificationPreferenceResponseDTO;
import com.javaeasybank.notification.dto.NotificationPreferenceUpdateRequest;
import com.javaeasybank.notification.service.NotificationPreferenceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer/notification-preferences")
@RequiredArgsConstructor
public class NotificationPreferenceController {

    private final NotificationPreferenceService notificationPreferenceService;
    private final JwtUtil jwtUtil;

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationPreferenceResponseDTO>>> getPreferences(
            HttpServletRequest request) {
        String customerId = jwtUtil.resolveCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(notificationPreferenceService.getPreferences(customerId)));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PatchMapping
    public ResponseEntity<ApiResponse<NotificationPreferenceResponseDTO>> updatePreference(
            @RequestBody NotificationPreferenceUpdateRequest request,
            HttpServletRequest httpRequest) {
        if (request.getType() == null || request.getEnabled() == null) {
            throw new BusinessException("通知偏好設定不完整");
        }
        String customerId = jwtUtil.resolveCustomerId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(
                notificationPreferenceService.updatePreference(customerId, request.getType(), request.getEnabled())));
    }
}
