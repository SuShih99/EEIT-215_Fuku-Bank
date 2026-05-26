package com.javaeasybank.creditcard.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.javaeasybank.account.dto.request.CreditCardPaymentRequest;
import com.javaeasybank.account.dto.response.CreditCardPaymentResponse;
import com.javaeasybank.account.service.AccountIntegrationService;
import com.javaeasybank.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class CardPaymentService {

    private final AccountIntegrationService accountIntegrationService;

    public CreditCardPaymentResponse processPayment(String customerId,
            CreditCardPaymentRequest paymentRequest) {

        //防止負數
        if (paymentRequest.getAmount() == null || paymentRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("繳費金額必須大於 0");
        }
        return accountIntegrationService.payCreditCard(customerId, paymentRequest);
    }
}
