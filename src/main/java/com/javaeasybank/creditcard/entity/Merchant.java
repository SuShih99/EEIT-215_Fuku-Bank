package com.javaeasybank.creditcard.entity;

import com.javaeasybank.creditcard.enums.MerchantCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {

	@Id
    private Integer merchantId;

    @Column(name = "merchant_name", length = 100, columnDefinition = "NVARCHAR(100)")
    private String merchantName;
    @Enumerated(EnumType.STRING)
    private MerchantCategory merchantCategory;
//    channel_id NVARCHAR(50),先拿掉
//    channel_secret NVARCHAR(100),

}
