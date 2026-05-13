package com.javaeasybank.creditcard.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.javaeasybank.account.dto.request.AccountResponse;
import com.javaeasybank.account.dto.request.CreditCardPaymentRequest;
import com.javaeasybank.account.dto.response.CreditCardPaymentResponse;
import com.javaeasybank.account.service.AccountIntegrationService;
import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.util.SecurityUtil;
import com.javaeasybank.creditcard.service.CardPaymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/card-payments")
@RequiredArgsConstructor
public class CardPaymentController {

    private final CardPaymentService paymentService;
    private final SecurityUtil securityUtil;
    private final AccountIntegrationService accountIntegrationService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreditCardPaymentResponse>> payCreditCard(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody CreditCardPaymentRequest paymentRequest) {

        String customerId = securityUtil.getCustomerIdFromHeader(authHeader);
        CreditCardPaymentResponse response = paymentService.processPayment(customerId, paymentRequest);

        return ResponseEntity.ok(ApiResponse.success("Payment processed successfully", response));
    }

    // 取得繳款帳戶
    @GetMapping("/payment-accounts")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> getPaymentAccount(
            @RequestHeader("Authorization") String authHeader) {

        String customerId = securityUtil.getCustomerIdFromHeader(authHeader);
        List<AccountResponse> accounts = accountIntegrationService.getActiveTwdCheckingAccounts(customerId);
        return ResponseEntity.ok(
                ApiResponse.success("success", accounts));
    }

}
