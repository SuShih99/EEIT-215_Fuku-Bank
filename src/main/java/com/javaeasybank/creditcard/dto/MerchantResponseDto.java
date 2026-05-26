package com.javaeasybank.creditcard.dto;

import com.javaeasybank.creditcard.enums.MerchantCategory;

import lombok.Data;

@Data
public class MerchantResponseDto {

    private Integer merchantId;
    private String merchantName;
    private MerchantCategory merchantCategory;

}
