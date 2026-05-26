package com.javaeasybank.account.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class FavoriteAccountUpdateRequest {

    private String alias;

    @Pattern(regexp = "\\d{3}", message = "銀行代號須為 3 碼數字")
    private String bankCode;

    private String bankName;
}
