package com.javaeasybank.account.controller;

import com.javaeasybank.account.dto.request.ScheduledTransferRequest;
import com.javaeasybank.account.dto.response.ScheduledTransferResponse;
import com.javaeasybank.account.service.ScheduledTransferService;
import com.javaeasybank.account.service.TransferOtpService;
import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.util.JwtUtil;
import java.util.Map;
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
    private final TransferOtpService transferOtpService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduledTransferResponse>>> list(HttpServletRequest request) {
        String customerId = jwtUtil.resolveCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(scheduledTransferService.getByCustomerId(customerId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduledTransferResponse>> create(
            @Valid @RequestBody ScheduledTransferRequest body,
            HttpServletRequest request) {
        String customerId = jwtUtil.resolveCustomerId(request);
        transferOtpService.verifyOtp(customerId, body.getOtp());

        ScheduledTransferResponse response = scheduledTransferService.create(customerId, body);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/otp")
    public ResponseEntity<ApiResponse<Map<String, String>>> requestScheduledTransferOtp(HttpServletRequest request) {
        String customerId = jwtUtil.resolveCustomerId(request);
        String otp = transferOtpService.generateAndSendOtp(customerId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("demoOtp", otp)));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancel(
            @PathVariable Long id,
            HttpServletRequest request) {
        String customerId = jwtUtil.resolveCustomerId(request);
        scheduledTransferService.cancel(customerId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
