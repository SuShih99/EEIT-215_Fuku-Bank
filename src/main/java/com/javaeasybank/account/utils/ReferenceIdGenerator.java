package com.javaeasybank.account.utils;

/**
 * 參考 ID 生成器，用於生成唯一的業務交易參考 ID。
 */
public class ReferenceIdGenerator {
    private static final java.security.SecureRandom random = new java.security.SecureRandom();
    private static final java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /**
     * 產生業務交易參考 ID。
     * 格式：TXN-yyyyMMdd-HHmmss-8hex
     * 範例：TXN-20250417-103000-a1b2c3d4
     *
     * @return 唯一的業務交易參考 ID 字串。
     */
    public static String generate() {

        // 產生時間戳記
        String timestamp = java.time.LocalDateTime.now().format(formatter);


        byte[] bytes = new byte[4];
        random.nextBytes(bytes);

        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }

        return String.format("TXN-%s-%s", timestamp, hex.toString());
    }

}
