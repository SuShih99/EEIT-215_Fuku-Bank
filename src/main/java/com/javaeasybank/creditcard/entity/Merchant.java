package com.javaeasybank.creditcard.entity;

import com.javaeasybank.creditcard.enums.MerchantCategory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "MERCHANT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Merchant {

    @Id
    @Column(name = "merchant_id")
    private Integer merchantId;

    @Column(name = "merchant_name", length = 100, columnDefinition = "NVARCHAR(100)", nullable = false)
    private String merchantName;

    @Enumerated(EnumType.STRING)
    @Column(name = "merchant_category", length = 50, nullable = false)
    private MerchantCategory merchantCategory;
}
