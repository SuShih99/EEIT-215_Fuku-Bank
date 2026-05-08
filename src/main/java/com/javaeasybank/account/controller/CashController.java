package com.javaeasybank.account.controller;

import com.javaeasybank.account.dto.request.CashRequest;
import com.javaeasybank.account.dto.response.CashResponse;
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
 * 存提款操作的 REST 控制器。
 */
@Slf4j
@RestController
@RequestMapping("/api/customer/cash")
@RequiredArgsConstructor
public class CashController {

    private final TransferService transferService;

    /**
     * 處理存款請求。
     *
     * @param request 存款請求 DTO。
     * @return 包含存款響應的 ResponseEntity。
     */
    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<CashResponse>> deposit(@Valid @RequestBody CashRequest request) {
        log.info("Received deposit request for account: {}, amount: {}", request.getAccountNumber(), request.getAmount());
        CashResponse response = transferService.deposit(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 處理提款請求。
     *
     * @param request 提款請求 DTO。
     * @return 包含提款響應的 ResponseEntity。
     */
    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<CashResponse>> withdraw(@Valid @RequestBody CashRequest request) {
        log.info("Received withdraw request for account: {}, amount: {}", request.getAccountNumber(), request.getAmount());
        CashResponse response = transferService.withdraw(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
