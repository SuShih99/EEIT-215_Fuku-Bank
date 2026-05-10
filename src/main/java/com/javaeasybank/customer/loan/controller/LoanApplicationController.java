package com.javaeasybank.customer.loan.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.customer.loan.dto.requests.LoanMemberRequestDTO;
import com.javaeasybank.customer.loan.dto.requests.LoanNonMemberRequestDTO;
import com.javaeasybank.customer.loan.service.LoanApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// 客戶端：申請入口
@RestController
@RequestMapping("/api/loan-applications")
@RequiredArgsConstructor
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;

    // 會員申請
    @PostMapping("/member")
    public ResponseEntity<ApiResponse<String>> applyMember(
            @RequestBody LoanMemberRequestDTO dto) {
        String applicationId = loanApplicationService.insertMember(dto);
        return ResponseEntity.status(201).body(ApiResponse.success(applicationId));
    }

    // 非會員申請
    @PostMapping("/non-member")
    public ResponseEntity<ApiResponse<String>> applyNonMember(
            @RequestBody LoanNonMemberRequestDTO dto) {
        String applicationId = loanApplicationService.insertNonMember(dto);
        return ResponseEntity.status(201).body(ApiResponse.success(applicationId));
    }

    // 利率規則（申請表單載入時使用）
    @GetMapping("/rate-rules")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRateRules() {
        return ResponseEntity.ok(ApiResponse.success(loanApplicationService.getRateRules()));
    }
}
