package com.javaeasybank.account.dto.request;

import lombok.Data;

@Data
public class FavoriteAccountUpdateRequest {

    private String alias;

    private String bankName;
}
