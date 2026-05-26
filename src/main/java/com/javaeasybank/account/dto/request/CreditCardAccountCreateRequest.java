package com.javaeasybank.account.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreditCardAccountCreateRequest {

    @NotBlank
    private String customerId;
}
