package com.javaeasybank.account.utils;

import com.javaeasybank.account.entity.Account;
import com.javaeasybank.account.enums.Currency;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 帳戶建立時共用的預設值。
 */
public final class AccountDefaults {

    private static final BigDecimal CHECKING_INITIAL_BALANCE = new BigDecimal("1000");
    private static final BigDecimal CHECKING_INTEREST_RATE = new BigDecimal("0.0015");

    private AccountDefaults() {
    }

    public static void applyCheckingDefaults(Account account) {
        account.setBalance(checkingInitialBalance(account.getCurrency()));
        account.setInterestRate(CHECKING_INTEREST_RATE);
    }

    public static void applySubAccountDefaults(Account account) {
        account.setInterestRate(CHECKING_INTEREST_RATE);
    }

    private static BigDecimal checkingInitialBalance(Currency currency) {
        return CHECKING_INITIAL_BALANCE.setScale(currency.getDecimalPlaces(), RoundingMode.HALF_EVEN);
    }
}
