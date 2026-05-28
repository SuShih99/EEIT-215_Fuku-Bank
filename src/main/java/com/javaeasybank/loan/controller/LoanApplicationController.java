package com.javaeasybank.loan.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.util.JwtUtil;
import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.loan.dto.requests.LoanMemberRequestDTO;
import com.javaeasybank.loan.dto.response.LoanApplicationResponseDTO;
import com.javaeasybank.loan.service.LoanApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 客戶端貸款申請 API。
 * 負責接收會員送出的貸款申請、查詢本人申請紀錄，以及提供前端利率規則。
 */
@RestController
@RequestMapping("/api/loan-applications")
@RequiredArgsConstructor
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;
    private final JwtUtil jwtUtil;

    // 提交貸款申請
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/member")
    public ResponseEntity<ApiResponse<String>> applyMember(
            @RequestBody LoanMemberRequestDTO dto, HttpServletRequest request) {
        String customerId = extractCustomerId(request);
        String applicationId = loanApplicationService.insertMember(customerId, dto);
        return ResponseEntity.status(201).body(ApiResponse.success(applicationId));
    }

    // 查詢客戶自己的所有貸款申請
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<LoanApplicationResponseDTO>>> getMyApplications(
            HttpServletRequest request) {
        String customerId = extractCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(
                loanApplicationService.getMyApplications(customerId)));
    }

    // 取得各貸款種類的利率規則
    @GetMapping("/rate-rules")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRateRules() {
        return ResponseEntity.ok(ApiResponse.success(loanApplicationService.getRateRules()));
    }

    // 從 Authorization Header 解析 JWT 並取得 customerId
    private String extractCustomerId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.getCustomerIdFromToken(token);
        }
        throw new BusinessException("無法取得客戶身分資訊");
    }
}
