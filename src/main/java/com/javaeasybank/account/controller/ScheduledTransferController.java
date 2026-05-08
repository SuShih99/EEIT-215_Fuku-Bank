package com.javaeasybank.account.controller;

import com.javaeasybank.account.dto.request.ScheduledTransferRequest;
import com.javaeasybank.account.dto.response.ScheduledTransferResponse;
import com.javaeasybank.account.service.ScheduledTransferService;
import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/customer/scheduled-transfers")
@RequiredArgsConstructor
public class ScheduledTransferController {

    private final ScheduledTransferService scheduledTransferService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduledTransferResponse>>> list(HttpServletRequest request) {
        String customerId = extractCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(scheduledTransferService.getByCustomerId(customerId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduledTransferResponse>> create(
            @Valid @RequestBody ScheduledTransferRequest body,
            HttpServletRequest request) {
        String customerId = extractCustomerId(request);
        ScheduledTransferResponse response = scheduledTransferService.create(customerId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable Long id,
            HttpServletRequest request) {
        String customerId = extractCustomerId(request);
        scheduledTransferService.cancel(customerId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
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
