package com.javaeasybank.account.controller;

import com.javaeasybank.account.dto.request.AccountResponse;
import com.javaeasybank.account.dto.response.TransLogResponse;
import com.javaeasybank.account.entity.Account;
import com.javaeasybank.account.entity.TransLog;
import com.javaeasybank.account.repository.AccountRepository;
import com.javaeasybank.account.repository.TransLogRepository;
import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.dto.response.PageResponse;
import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.common.util.JwtUtil;
import com.javaeasybank.customer.repository.CustomerProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 客戶端帳戶與交易查詢 Controller
 *
 * GET /api/customer/accounts       — 查詢我的所有帳戶
 * GET /api/customer/transactions   — 查詢我的交易紀錄（分頁 + 篩選）
 */
@Slf4j
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerAccountController {

    private final AccountRepository accountRepository;
    private final TransLogRepository transLogRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final JwtUtil jwtUtil;

    /**
     * 查詢當前客戶的所有帳戶
     */
    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getMyAccounts(HttpServletRequest request) {
        String customerId = extractCustomerId(request);
        List<Account> accounts = accountRepository.findAllByCustomerId(customerId);
        List<AccountResponse> list = accounts.stream()
                .map(a -> {
                    String customerName = customerProfileRepository.findById(a.getCustomerId())
                            .map(cp -> cp.getName())
                            .orElse(null);
                    return AccountResponse.fromEntity(a, customerName);
                })
                .toList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    /**
     * 查詢當前客戶的交易紀錄（支援帳號篩選 + 日期區間 + 分頁）
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<PageResponse<TransLogResponse>>> getMyTransactions(
            HttpServletRequest request,
            @RequestParam(required = false) String accountNumber,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String customerId = extractCustomerId(request);
        Page<TransLog> result;

        if (accountNumber != null && !accountNumber.isBlank()) {
            // 驗證帳號屬於該客戶
            Account account = accountRepository.findById(accountNumber).orElse(null);
            if (account == null || !account.getCustomerId().equals(customerId)) {
                throw new BusinessException("帳戶不存在或不屬於您");
            }
            result = transLogRepository.findByAccountInvolved(accountNumber,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        } else if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
            result = transLogRepository.findByCustomerIdAndDateRange(customerId, start, end,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        } else {
            result = transLogRepository.findByCustomerId(customerId,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        }

        List<TransLogResponse> content = result.getContent().stream()
                .map(TransLogResponse::fromEntity)
                .toList();

        PageResponse<TransLogResponse> pageResponse = PageResponse.of(content, page, size, result.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
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
