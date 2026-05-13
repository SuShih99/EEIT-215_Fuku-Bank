package com.javaeasybank.creditcard.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.javaeasybank.account.dto.request.CreditCardPaymentRequest;
import com.javaeasybank.account.dto.response.CreditCardPaymentResponse;
import com.javaeasybank.account.service.AccountIntegrationService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CardPaymentService {

    private final AccountIntegrationService accountIntegrationService;

    public CreditCardPaymentResponse processPayment(String customerId,
        CreditCardPaymentRequest paymentRequest) {

            return accountIntegrationService.payCreditCard(customerId, paymentRequest);
    }
}
