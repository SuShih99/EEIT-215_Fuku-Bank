package com.javaeasybank.account.controller;

import com.javaeasybank.account.dto.request.RiskCallbackRequest;
import com.javaeasybank.account.service.TransferService;
import com.javaeasybank.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transfer-callbacks")
@RequiredArgsConstructor
@Slf4j
public class AccountCallbackController {


    private final TransferService transferService;

    @PostMapping("/{referenceId}/status")
    public ResponseEntity<ApiResponse<Void>> handleRiskCallback(
            @PathVariable String referenceId,
            @RequestBody RiskCallbackRequest dto) {

        log.info("[AccountCallback] 收到 referenceId={} newStatus={} callerModule={}",
                referenceId, dto.getNewStatus(), dto.getCallerModule());

        if ("APPROVED".equals(dto.getNewStatus())) {
            transferService.executePendingTransfer(referenceId);
        } else {
            transferService.rejectPendingTransfer(referenceId);
        }
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
