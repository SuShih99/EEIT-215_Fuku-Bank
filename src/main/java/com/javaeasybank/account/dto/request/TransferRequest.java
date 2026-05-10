package com.javaeasybank.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 轉帳請求 DTO，用於接收客戶端的轉帳資訊。
 */
@Data
public class TransferRequest {
    /**
     * 轉出帳號。
     */
    @NotBlank
    private String fromAccountNumber;
    
    /**
     * 轉入帳號。
     */
    @NotBlank
    @Size(min = 6, max = 20)
    @Pattern(regexp = "\\d{6,20}", message = "轉入帳號須為 6 到 20 碼數字")
    private String toAccountNumber;

    /**
     * 轉入銀行代碼。
     */
    @NotBlank
    @Pattern(regexp = "\\d{3}", message = "轉入銀行代碼須為 3 碼數字")
    private String toBankCode = "909";
    
    /**
     * 轉帳金額，必須為正數。
     */
    @NotNull
    @Positive
    private BigDecimal amount;

    /**
     * 轉帳備註。
     */
    private String note;
}
