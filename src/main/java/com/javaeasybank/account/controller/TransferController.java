package com.javaeasybank.account.controller;

import com.javaeasybank.account.dto.request.ExchangeRequest;
import com.javaeasybank.account.dto.request.ReversalRequest;
import com.javaeasybank.account.dto.request.TransferRequest;
import com.javaeasybank.account.dto.response.ExchangeResponse;
import com.javaeasybank.account.dto.response.ReversalResponse;
import com.javaeasybank.account.dto.response.TransferBankResponse;
import com.javaeasybank.account.dto.response.TransferResponse;
import com.javaeasybank.account.enums.TransferBank;
import com.javaeasybank.account.service.TransferService;
import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 轉帳與沖正操作的 REST 控制器。
 * - 轉帳：客戶端操作 (/api/customer/transfers)
 * - 沖正：管理端操作 (/api/admin/transfers/reversal)
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;
    private final JwtUtil jwtUtil;

    /**
     * 客戶端：取得國內轉帳銀行選單。
     */
    @GetMapping("/api/customer/transfer-banks")
    public ResponseEntity<ApiResponse<List<TransferBankResponse>>> transferBanks() {
        List<TransferBankResponse> banks = TransferBank.customerOptions().stream()
                .map(TransferBankResponse::fromEnum)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(banks));
    }

    /**
     * 客戶端：處理轉帳請求。
     */
    @PostMapping("/api/customer/transfers")
    public ResponseEntity<ApiResponse<TransferResponse>> transfer(@Valid @RequestBody TransferRequest request) {
        log.info("Received transfer request from account: {} to account: {}", request.getFromAccountNumber(), request.getToAccountNumber());
        TransferResponse response = transferService.transfer(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 客戶端：處理本人帳戶間換匯請求。
     */
    @PostMapping("/api/customer/exchanges")
    public ResponseEntity<ApiResponse<ExchangeResponse>> exchange(
            @Valid @RequestBody ExchangeRequest request,
            HttpServletRequest httpRequest) {
        String customerId = extractCustomerId(httpRequest);
        ExchangeResponse response = transferService.exchange(request, customerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 管理端：處理沖正請求。
     * 根據原始交易編號反向沖正所有相關帳戶餘額，並寫入新的沖正交易紀錄。
     */
    @PostMapping("/api/admin/transfers/reversal")
    public ResponseEntity<ApiResponse<ReversalResponse>> reversal(@Valid @RequestBody ReversalRequest request) {
        log.info("Received reversal request for original referenceId: {}", request.getOriginalReferenceId());
        ReversalResponse response = transferService.reversal(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String extractCustomerId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.getCustomerIdFromToken(token);
        }
        throw new BusinessException("無法取得客戶身分資訊");
    }
}
