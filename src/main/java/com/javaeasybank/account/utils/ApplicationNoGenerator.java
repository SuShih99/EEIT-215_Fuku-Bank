package com.javaeasybank.account.utils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 開戶申請編號生成器。
 * 格式：APP-yyyyMMdd-HHmmss-8hex
 * 範例：APP-20260508-143025-a1b2c3d4
 */
public class ApplicationNoGenerator {

    private static final SecureRandom random = new SecureRandom();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    public static String generate() {
        String timestamp = LocalDateTime.now().format(formatter);

        byte[] bytes = new byte[4];
        random.nextBytes(bytes);

        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }

        return String.format("APP-%s-%s", timestamp, hex.toString());
    }
}
