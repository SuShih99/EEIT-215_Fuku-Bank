package com.javaeasybank.risk.controller;

import com.javaeasybank.auth.service.AuthActionLogService;
import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.risk.dto.response.CreditInfoResponse;
import com.javaeasybank.risk.service.CreditScoreService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/risk/creditscore")
@PreAuthorize("hasAnyRole('CFDM', 'CFSO')")
@RequiredArgsConstructor
public class CreditScoreController {

    private final CreditScoreService creditScoreService;
    private final AuthActionLogService authActionLogService;

    // 列表（脫敏，只顯示風險等級）
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CreditInfoResponse>>> listAll(
            @RequestParam(required = false) String keyword,
            Pageable pageable,
            HttpServletRequest request) {

        authActionLogService.recordAction(
                "LIST_CREDIT_INFO",
                null,
                "瀏覽客戶信用列表",
                getClientIp(request)

        );
        Page<CreditInfoResponse> result = creditScoreService.listAllCredit(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<ApiResponse<CreditInfoResponse>> getCreditInfo(
            @PathVariable String customerId,
            Authentication auth, HttpServletRequest request) {

        // 記錄查看行為
        authActionLogService.recordAction(
                "SEARCH_CREDIT_DETAIL",
                customerId,
                "查看客戶信用評分",
                getClientIp(request)

        );

        return ResponseEntity.ok(ApiResponse.success(
                creditScoreService.getCreditInfoByCustomerId(customerId)
        ));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
