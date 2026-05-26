package com.javaeasybank.account.controller;

import com.javaeasybank.account.dto.request.AccountResponse;
import com.javaeasybank.account.dto.request.CreditCardPaymentRequest;
import com.javaeasybank.account.dto.request.LoanRepaymentRequest;
import com.javaeasybank.account.dto.response.CreditCardPaidAmountResponse;
import com.javaeasybank.account.dto.response.CreditCardPaymentResponse;
import com.javaeasybank.account.dto.response.LoanAccountTransactionResponse;
import com.javaeasybank.account.dto.response.TransLogResponse;
import com.javaeasybank.account.service.AccountIntegrationService;
import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.dto.response.PageResponse;
import com.javaeasybank.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class AccountIntegrationController {

    private final AccountIntegrationService accountIntegrationService;
    private final JwtUtil jwtUtil;

    @GetMapping("/api/customer/loan-repayments/debit-accounts")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getLoanRepaymentDebitAccounts(HttpServletRequest request) {
        String customerId = jwtUtil.resolveCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(accountIntegrationService.getActiveTwdCheckingAccounts(customerId)));
    }

    @PostMapping("/api/customer/loan-repayments")
    public ResponseEntity<ApiResponse<LoanAccountTransactionResponse>> repayLoan(
            HttpServletRequest request,
            @Valid @RequestBody LoanRepaymentRequest repaymentRequest) {
        String customerId = jwtUtil.resolveCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(accountIntegrationService.repayLoan(customerId, repaymentRequest)));
    }

    @GetMapping("/api/customer/loan-repayments")
    public ResponseEntity<ApiResponse<PageResponse<TransLogResponse>>> getLoanRepaymentRecords(
            HttpServletRequest request,
            @RequestParam(required = false) String loanAccountNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String customerId = jwtUtil.resolveCustomerId(request);
        Page<TransLogResponse> records = accountIntegrationService.getLoanRepaymentRecords(
                customerId,
                loanAccountNumber,
                startDate,
                endDate,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                records.getContent(),
                page,
                size,
                records.getTotalElements())));
    }

    @PostMapping("/api/customer/card-payments")
    public ResponseEntity<ApiResponse<CreditCardPaymentResponse>> payCreditCard(
            HttpServletRequest request,
            @Valid @RequestBody CreditCardPaymentRequest paymentRequest) {
        String customerId = jwtUtil.resolveCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(accountIntegrationService.payCreditCard(customerId, paymentRequest)));
    }

    @GetMapping("/api/customer/card-payments/paid-amount")
    public ResponseEntity<ApiResponse<CreditCardPaidAmountResponse>> getCreditCardPaidAmount(
            HttpServletRequest request,
            @RequestParam String creditCardAccountNumber,
            @RequestParam(required = false) String billingMonth,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String customerId = jwtUtil.resolveCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(accountIntegrationService.getCreditCardPaidAmount(
                customerId,
                creditCardAccountNumber,
                billingMonth,
                startDate,
                endDate)));
    }
}
