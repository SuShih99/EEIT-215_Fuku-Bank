package com.javaeasybank.account.controller;

import com.javaeasybank.account.service.TransferService;
import com.javaeasybank.common.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account-callbacks")
@RequiredArgsConstructor
public class AccountCallbackController {


    private final TransferService transferService;

    @PostMapping("/{referenceId}/status")
    public ResponseEntity<ApiResponse<Void>> handleRiskCallback(
            @PathVariable String referenceId
            /*@RequestBody RiskCallbackRequest dto*/) {

//        if ("APPROVED".equals(dto.getNewStatus())) {
//            transferService.executePendingTransfer(referenceId);
//        } else {
//            transferService.rejectPendingTransfer(referenceId);
//        }
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
