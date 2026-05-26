package com.javaeasybank.account.dto.response;

import com.javaeasybank.account.entity.FavoriteAccount;
import lombok.Builder;
import lombok.Data;

import java.time.format.DateTimeFormatter;

@Data
@Builder
public class FavoriteAccountResponse {

    private Long id;
    private String accountNumber;
    private String bankCode;
    private String alias;
    private String bankName;
    private String createdAt;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static FavoriteAccountResponse fromEntity(FavoriteAccount entity) {
        return FavoriteAccountResponse.builder()
                .id(entity.getId())
                .accountNumber(entity.getAccountNumber())
                .bankCode(entity.getBankCode())
                .alias(entity.getAlias())
                .bankName(entity.getBankName())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().format(FMT) : null)
                .build();
    }
}
