package com.javaeasybank.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FavoriteAccountRequest {

    @NotBlank(message = "帳號不可為空")
    private String accountNumber;

    @NotBlank(message = "備註名稱不可為空")
    private String alias;

    private String bankName;
}
