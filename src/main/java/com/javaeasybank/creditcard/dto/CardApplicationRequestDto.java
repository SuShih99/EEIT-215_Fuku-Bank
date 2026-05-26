package com.javaeasybank.creditcard.dto;

import java.util.List;

import lombok.Data;

@Data
public class CardApplicationRequestDto {
	private String customerId;
    private String remark;
    private Integer cardTypeId;

    // 多卡申請
    private List<Integer> cardTypeIds;
}
