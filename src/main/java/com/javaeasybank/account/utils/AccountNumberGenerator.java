package com.javaeasybank.account.utils;

import java.security.SecureRandom;

/**
 * 帳號生成器，用於生成符合 Luhn 演算法的 12 位帳號。
 */
public class AccountNumberGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 生成 12 碼帳號：前 11 碼亂數 + 1 碼 Luhn 檢核碼。
     *
     * @return 唯一的 12 位帳號字串。
     */
    public static String generate() {
        // 生成前 11 碼亂數
        StringBuilder sb = new StringBuilder(11);
        for (int i = 0; i < 11; i++) {
            sb.append(SECURE_RANDOM.nextInt(10));
        }
        String first11 = sb.toString();
        
        // 計算 Luhn 檢核碼
        int checkDigit = calculateLuhnCheckDigit(first11);
        
        return first11 + checkDigit;
    }

    /**
     * 計算 Luhn 演算法的檢核碼 (Check Digit)。
     * 從字串最後一位開始，奇數位乘以 2 (此處因為要加上最後的檢核碼，所以從原始長度的最後一位算起是偶數位)。
     *
     * @param number 待計算檢核碼的數字字串。
     * @return Luhn 檢核碼。
     */
    private static int calculateLuhnCheckDigit(String number) {
        int sum = 0;
        boolean alternate = true;

        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Character.getNumericValue(number.charAt(i));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        
        return (10 - (sum % 10)) % 10;
    }
}
