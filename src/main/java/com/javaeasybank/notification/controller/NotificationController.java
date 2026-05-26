package com.javaeasybank.notification.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.util.JwtUtil;
import com.javaeasybank.notification.dto.NotificationResponseDTO;
import com.javaeasybank.notification.dto.NotificationUnreadCountResponseDTO;
import com.javaeasybank.notification.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponseDTO>>> getNotifications(HttpServletRequest request) {
        String customerId = jwtUtil.resolveCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUserNotifications(customerId)));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<NotificationUnreadCountResponseDTO>> getUnreadCount(HttpServletRequest request) {
        String customerId = jwtUtil.resolveCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getUnreadCount(customerId)));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponseDTO>> markAsRead(
            @PathVariable Long id,
            HttpServletRequest request) {
        String customerId = jwtUtil.resolveCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(notificationService.markAsRead(id, customerId)));
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<String>> markAllAsRead(HttpServletRequest request) {
        String customerId = jwtUtil.resolveCustomerId(request);
        int updated = notificationService.markAllAsRead(customerId);
        return ResponseEntity.ok(ApiResponse.success("已標記 " + updated + " 筆通知為已讀"));
    }
}
