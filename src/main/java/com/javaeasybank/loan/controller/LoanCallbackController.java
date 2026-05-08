package com.javaeasybank.loan.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.loan.dto.requests.LoanStatusCallbackRequestDTO;
import com.javaeasybank.loan.service.LoanApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/*
 * 供外部模組回調用的入口，目前處理：
 *   - 風控模組：PENDING_REVIEW → APPROVED / REJECTED
 * 後續帳戶模組（撥款）也會用同一隻 controller，擴充 callerModule 分流即可。
 */
@RestController
@RequestMapping("/api/loan-callbacks")
@RequiredArgsConstructor
public class LoanCallbackController {

    private final LoanApplicationService loanApplicationService;

    // 風控審核結果回調
    // POST /api/loan-callbacks/{applicationId}/status
    // Body: { "newStatus": "APPROVED", "callerModule": "RISK", "note": "..." }
    @PostMapping("/{applicationId}/status")
    public ResponseEntity<ApiResponse<Void>> handleStatusCallback(
            @PathVariable String applicationId,
            @RequestBody LoanStatusCallbackRequestDTO dto) {

        loanApplicationService.handleStatusCallback(applicationId, dto);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}