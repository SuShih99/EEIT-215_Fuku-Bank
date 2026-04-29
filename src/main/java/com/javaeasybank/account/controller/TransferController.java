package com.javaeasybank.account.controller;

import com.javaeasybank.account.dto.request.ReversalRequest;
import com.javaeasybank.account.dto.request.TransferRequest;
import com.javaeasybank.account.dto.response.ReversalResponse;
import com.javaeasybank.account.dto.response.TransferResponse;
import com.javaeasybank.account.service.TransferService;
import com.javaeasybank.common.dto.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 轉帳操作的 REST 控制器。
 * 處理來自客戶端的轉帳相關請求。
 */
@Slf4j
@RestController
@RequestMapping("/api/transfers")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    /**
     * 處理轉帳請求。
     *
     * @param request 轉帳請求 DTO。
     * @return 包含轉帳響應的 ResponseEntity。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TransferResponse>> transfer(@Valid @RequestBody TransferRequest request) {
        log.info("Received transfer request from account: {} to account: {}", request.getFromAccountNumber(), request.getToAccountNumber());
        TransferResponse response = transferService.transfer(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 處理沖正請求。
     * 根據原始交易編號反向沖正所有相關帳戶餘額，並寫入新的沖正交易紀錄。
     *
     * @param request 沖正請求 DTO（含原始交易編號）。
     * @return 包含沖正響應的 ResponseEntity。
     */
    @PostMapping("/reversal")
    public ResponseEntity<ApiResponse<ReversalResponse>> reversal(@Valid @RequestBody ReversalRequest request) {
        log.info("Received reversal request for original referenceId: {}", request.getOriginalReferenceId());
        ReversalResponse response = transferService.reversal(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
