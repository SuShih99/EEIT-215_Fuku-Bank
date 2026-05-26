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
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import com.javaeasybank.account.enums.TransactionType;

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
        String customerId = jwtUtil.resolveCustomerId(request);
        List<Account> accounts = accountRepository.findAllByCustomerId(customerId);
        List<AccountResponse> list = accounts.stream()
                .filter(a -> a.getAccountType().isCustomerVisible())
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
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String customerId = jwtUtil.resolveCustomerId(request);
        Page<TransLog> result;

        List<String> ownedAccounts = accountRepository.findAllByCustomerId(customerId).stream()
                .filter(a -> a.getAccountType().isCustomerVisible())
                .map(Account::getAccountNumber).toList();

        if (ownedAccounts.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(PageResponse.of(List.of(), page, size, 0)));
        }

        Specification<TransLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (accountNumber != null && !accountNumber.isBlank()) {
                if (!ownedAccounts.contains(accountNumber.trim())) {
                    throw new BusinessException("帳戶不存在或不屬於您");
                }
                predicates.add(cb.equal(root.get("accountNumber"), accountNumber.trim()));
            } else {
                predicates.add(root.get("accountNumber").in(ownedAccounts));
            }

            if (startDate != null && endDate != null) {
                LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
                LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
                predicates.add(cb.between(root.get("createdAt"), start, end));
            }

            if (transactionType != null) {
                predicates.add(cb.equal(root.get("transactionType"), transactionType));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        result = transLogRepository.findAll(spec, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<TransLogResponse> content = result.getContent().stream()
                .map(TransLogResponse::fromEntity)
                .toList();

        PageResponse<TransLogResponse> pageResponse = PageResponse.of(content, page, size, result.getTotalElements());
        return ResponseEntity.ok(ApiResponse.success(pageResponse));
    }

}
