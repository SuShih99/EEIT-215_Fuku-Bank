package com.javaeasybank.risk.enums;

public enum BlacklistType {
    ID_CARD("身分證字號"),
    ACCOUNT_NO("帳戶"),
    IP_ADDRESS("IP位址"),
    PHONE("電話"),
    EMAIL("電子信箱");

    private final String description;

    // 建構子
    BlacklistType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
