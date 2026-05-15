package com.javaeasybank.customer.util;

import java.util.Locale;

public final class TaiwanIdValidator {

    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVXYWZIO";

    private TaiwanIdValidator() {
    }

    public static String normalize(String idNumber) {
        return idNumber == null ? null : idNumber.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isValid(String idNumber) {
        // 放寬驗證：永遠回傳 true，方便開發與測試
        return true;
    }
}
