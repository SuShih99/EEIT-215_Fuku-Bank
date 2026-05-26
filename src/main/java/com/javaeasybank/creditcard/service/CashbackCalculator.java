package com.javaeasybank.creditcard.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.javaeasybank.creditcard.entity.CreditCard;

@Service
public class CashbackCalculator {

    public BigDecimal calculateCashback(BigDecimal amount, CreditCard card) {
        if (amount == null || card == null || card.getCardType() == null) {
            return BigDecimal.ZERO;
        }

        return calculateCashback(amount, card.getCardType().getCashbackRate());
    }

    public BigDecimal calculateCashback(BigDecimal amount, BigDecimal rate) {
        if (amount == null || rate == null) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(rate).divide(BigDecimal.valueOf(100),0, RoundingMode.HALF_UP);
    }
}
