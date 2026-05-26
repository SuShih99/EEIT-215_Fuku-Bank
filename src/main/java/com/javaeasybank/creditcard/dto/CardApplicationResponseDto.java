package com.javaeasybank.creditcard.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.javaeasybank.creditcard.enums.CardApplicationItemResult;
import com.javaeasybank.creditcard.enums.CardApplicationStatus;

import lombok.Data;
@Data
public class CardApplicationResponseDto {

	private Integer applicationId;
    private String customerId;
    private String customerName;
    private Integer cardTypeId;
    private String cardTypeName;
    private LocalDateTime applyDate;
    private CardApplicationStatus status;
    private String remark;

    private CardApplicationItemResult itemResult;

    private List<CardApplicationDocumentResponseDto> documents;

    // 新欄位：多卡申請用
    private List<CardApplicationItemResponseDto> items;
}
