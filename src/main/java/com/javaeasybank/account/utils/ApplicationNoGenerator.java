package com.javaeasybank.account.utils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 開戶申請編號生成器。
 * 格式：APP-yyyyMMdd-4digits
 * 範例：APP-20260508-1234
 */
public class ApplicationNoGenerator {

    private static final SecureRandom random = new SecureRandom();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static String generate() {
        String datePart = LocalDateTime.now().format(formatter);
        int randomNum = random.nextInt(10000);
        return String.format("APP-%s-%04d", datePart, randomNum);
    }
}
