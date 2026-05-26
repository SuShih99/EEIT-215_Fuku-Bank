package com.javaeasybank.creditcard.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class LinePayRequestDto {

    // 消費金額
    private BigDecimal amount;
    // 信用卡 ID
    private Integer cardId;
    // 商家 ID
    private Integer merchantId;
    // 消費描述
    private String description;
}
