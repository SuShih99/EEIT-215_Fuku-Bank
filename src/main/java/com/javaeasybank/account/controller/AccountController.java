package com.javaeasybank.account.controller;

import com.javaeasybank.account.dto.request.AccountCreateRequest;
import com.javaeasybank.account.dto.request.AccountResponse;
import com.javaeasybank.account.enums.AccountStatus;
import com.javaeasybank.account.enums.AccountType;
import com.javaeasybank.account.enums.Currency;
import com.javaeasybank.account.service.AccountService;
import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * 創建一個新帳戶。
     *
     * @param request 帳戶創建請求。
     * @return 包含創建帳戶響應的 ResponseEntity。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(@Valid @RequestBody AccountCreateRequest request) {
        log.info("Received request to create account for customer: {}", request.getCustomerId());
        AccountResponse response = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 根據帳號檢索帳戶。
     *
     * @param accountNumber 帳號。
     * @return 包含帳戶響應的 ResponseEntity。
     */
    @GetMapping("/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(@PathVariable String accountNumber) {
        log.info("Received request to get account: {}", accountNumber);
        AccountResponse response = accountService.getAccount(accountNumber);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 檢索指定客戶的帳戶分頁列表。
     *
     * @param customerId 客戶ID。
     * @param page       頁碼（默認為0）。
     * @param size       每頁帳戶數量（默認為10）。
     * @return 包含帳戶分頁列表響應的 ResponseEntity。
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AccountResponse>>> getAccountsByCustomerId(
            @RequestParam String customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Received request to get accounts for customer: {}", customerId);
        Page<AccountResponse> result = accountService.getAccountsByCustomerId(customerId, PageRequest.of(page, size));
        PageResponse<AccountResponse> pageResponse = PageResponse.of(result.getContent(), page, size, result.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    /**
     * 檢索按狀態過濾的帳戶分頁列表。
     *
     * @param status 要檢索的帳戶狀態。
     * @param page   頁碼（默認為0）。
     * @param size   每頁帳戶數量（默認為10）。
     * @return 包含帳戶分頁列表響應的 ResponseEntity。
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<PageResponse<AccountResponse>>> getAccountsByStatus(
            @PathVariable AccountStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Received request to get accounts by status: {}", status);
        Page<AccountResponse> result = accountService.getAccountsByStatus(status, PageRequest.of(page, size));
        PageResponse<AccountResponse> pageResponse = PageResponse.of(result.getContent(), page, size, result.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    /**
     * 檢索按類型和貨幣過濾的帳戶分頁列表。
     *
     * @param type     要檢索的帳戶類型。
     * @param currency 要檢索的帳戶貨幣。
     * @param page     頁碼（默認為0）。
     * @param size     每頁帳戶數量（默認為10）。
     * @return 包含帳戶分頁列表響應的 ResponseEntity。
     */
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<PageResponse<AccountResponse>>> getAccountsByTypeAndCurrency(
            @RequestParam AccountType type,
            @RequestParam Currency currency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Received request to get accounts by type: {} and currency: {}", type, currency);
        Page<AccountResponse> result = accountService.getAccountsByTypeAndCurrency(type, currency, PageRequest.of(page, size));
        PageResponse<AccountResponse> pageResponse = PageResponse.of(result.getContent(), page, size, result.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    /**
     * 檢索最新建立的帳戶分頁列表。
     *
     * @param page 頁碼（默認為0）。
     * @param size 每頁帳戶數量（默認為10）。
     * @return 包含最新帳戶分頁列表響應的 ResponseEntity。
     */
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<PageResponse<AccountResponse>>> getLatestAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Received request to get latest accounts");
        Page<AccountResponse> result = accountService.getLatest(PageRequest.of(page, size));
        PageResponse<AccountResponse> pageResponse = PageResponse.of(result.getContent(), page, size, result.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

    /**
     * 變更帳戶狀態。
     *
     * @param accountNumber 帳號。
     * @param newStatus 目標狀態。
     * @return 包含更新後帳戶響應的 ResponseEntity。
     */
    @PatchMapping("/{accountNumber}/status")
    public ResponseEntity<ApiResponse<AccountResponse>> updateAccountStatus(
            @PathVariable String accountNumber,
            @RequestParam AccountStatus newStatus) {
        log.info("Received request to update account {} status to {}", accountNumber, newStatus);
        AccountResponse response = accountService.updateAccountStatus(accountNumber, newStatus);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}