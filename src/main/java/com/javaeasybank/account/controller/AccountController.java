package com.javaeasybank.account.controller;

import com.javaeasybank.account.dto.request.AccountCreateRequest;
import com.javaeasybank.account.dto.request.AccountResponse;
import com.javaeasybank.account.enums.AccountStatus;
import com.javaeasybank.account.enums.AccountType;
import com.javaeasybank.account.enums.Currency;
import com.javaeasybank.account.service.AccountService;
import com.javaeasybank.auth.entity.AuthActionLog;
import com.javaeasybank.auth.entity.AuthEmp;
import com.javaeasybank.auth.repository.AuthEmpRepository;
import com.javaeasybank.auth.service.AuthActionLogService;
import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final AuthEmpRepository authEmpRepository;
    private final AuthActionLogService actionLogService;

    /**
     * 創建一個新帳戶。
     *
     * @param request 帳戶創建請求。
     * @return 包含創建帳戶響應的 ResponseEntity。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(@Valid @RequestBody AccountCreateRequest request) {
        requireAccountOperator("CREATE_ACCOUNT", request.getCustomerId(), "嘗試建立帳戶");
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
     * 後台帳戶複合搜尋，可同時依客戶姓名、客戶 ID、帳號、狀態、型別、幣別篩選。
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<AccountResponse>>> searchAccounts(
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) AccountStatus status,
            @RequestParam(required = false) AccountType type,
            @RequestParam(required = false) Currency currency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Received request to search accounts: customerName={}, customerId={}, accountNumber={}, status={}, type={}, currency={}",
                customerName, customerId, accountNumber, status, type, currency);
        Page<AccountResponse> result = accountService.searchAdminAccounts(
                customerId,
                customerName,
                accountNumber,
                status,
                type,
                currency,
                PageRequest.of(page, size));
        PageResponse<AccountResponse> pageResponse = PageResponse.of(result.getContent(), page, size, result.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }


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
     * 查詢全站帳戶統計數據
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(accountService.getStatistics()));
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
        requireAccountOperator("UPDATE_ACCOUNT_STATUS", accountNumber, "嘗試變更帳戶狀態為 " + newStatus);
        log.info("Received request to update account {} status to {}", accountNumber, newStatus);
        AccountResponse response = accountService.updateAccountStatus(accountNumber, newStatus);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private void requireAccountOperator(String action, String target, String details) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean allowed = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CFDM".equals(authority.getAuthority()));
        if (allowed) {
            return;
        }

        AuthEmp emp = resolveCurrentEmp(authentication).orElse(null);
        AuthActionLog logEntry = new AuthActionLog();
        logEntry.setEmpId(emp != null ? emp.getEmpId() : "UNKNOWN");
        logEntry.setEmpName(emp != null ? emp.getEmpName() : "未知員工");
        logEntry.setAction("UNAUTHORIZED_" + action);
        logEntry.setTarget(target);
        logEntry.setDetails(details + "，系統已阻擋此越權操作");
        logEntry.setIpAddress("127.0.0.1");
        actionLogService.saveLog(logEntry);

        throw new AccessDeniedException("權限不足，您的角色無法執行此操作");
    }

    private Optional<AuthEmp> resolveCurrentEmp(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return Optional.empty();
        }
        return authEmpRepository.findByEmail(authentication.getName());
    }
}
