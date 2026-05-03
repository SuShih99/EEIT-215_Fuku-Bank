package com.javaeasybank.creditcard.dto;

import java.time.LocalDateTime;

import com.javaeasybank.creditcard.enums.CardApplicationStatus;

import lombok.Data;
@Data
public class CardApplicationResponseDto {

	private Integer applicationId;
    private String customerId;
    private String customerName;
    private LocalDateTime applyDate;
    private CardApplicationStatus status;
    private String remark;
}
