package com.javaeasybank.account.dto.response;

import com.javaeasybank.account.entity.ScheduledTransfer;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Data
@Builder
public class ScheduledTransferResponse {

    private Long id;
    private String fromAccountNumber;
    private String toBankCode;
    private String toBankName;
    private String toAccountNumber;
    private BigDecimal amount;
    private String scheduledDate;
    private String note;
    private String status;
    private String executedAt;
    private String failReason;
    private String createdAt;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static ScheduledTransferResponse fromEntity(ScheduledTransfer entity) {
        return ScheduledTransferResponse.builder()
                .id(entity.getId())
                .fromAccountNumber(entity.getFromAccountNumber())
                .toBankCode(entity.getToBankCode())
                .toBankName(entity.getToBankName())
                .toAccountNumber(entity.getToAccountNumber())
                .amount(entity.getAmount())
                .scheduledDate(entity.getScheduledDate() != null ? entity.getScheduledDate().toString() : null)
                .note(entity.getNote())
                .status(entity.getStatus())
                .executedAt(entity.getExecutedAt() != null ? entity.getExecutedAt().format(FMT) : null)
                .failReason(entity.getFailReason())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().format(FMT) : null)
                .build();
    }
}
