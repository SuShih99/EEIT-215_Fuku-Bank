package com.javaeasybank.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class FavoriteAccountRequest {

    @NotBlank(message = "帳號不可為空")
    private String accountNumber;

    @NotBlank(message = "銀行代號不可為空")
    @Pattern(regexp = "\\d{3}", message = "銀行代號須為 3 碼數字")
    private String bankCode;

    @NotBlank(message = "備註名稱不可為空")
    private String alias;

    private String bankName;
}
