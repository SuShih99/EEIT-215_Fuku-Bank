package com.javaeasybank.loan.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.loan.dto.requests.LoanStatusCallbackRequestDTO;
import com.javaeasybank.loan.service.LoanApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 外部模組回調 Controller
@RestController
@RequestMapping("/api/loan-callbacks")
@RequiredArgsConstructor
@Slf4j
public class LoanCallbackController {

    private final LoanApplicationService loanApplicationService;

    // 接收外部模組的申請狀態更新回調
    @PostMapping("/{applicationId}/status")
    public ResponseEntity<ApiResponse<Void>> handleStatusCallback(
            @PathVariable String applicationId,
            @RequestBody LoanStatusCallbackRequestDTO dto) {

        log.info("[Callback] 收到回調 applicationId={} newStatus={} callerModule={} note={}",
                applicationId, dto.getNewStatus(), dto.getCallerModule(), dto.getNote());

        loanApplicationService.handleStatusCallback(applicationId, dto);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
