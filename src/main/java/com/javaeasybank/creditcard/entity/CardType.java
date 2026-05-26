package com.javaeasybank.creditcard.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "CARD_TYPE")
@NoArgsConstructor
@AllArgsConstructor
public class CardType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_type_id")
    private Integer cardTypeId;

    @Column(name = "card_type_name", length = 50, columnDefinition = "NVARCHAR(50)", nullable = false)
    private String cardTypeName;

    @Column(name = "brand", length = 20, columnDefinition = "NVARCHAR(20)", nullable = false)
    private String brand;

    @Column(name = "annual_fee", precision = 15, scale = 2)
    private BigDecimal annualFee;

    @Column(name = "cashback_rate", precision = 15, scale = 2)
    private BigDecimal cashbackRate;

    @Column(name = "card_image_url", length = 255)
    private String cardImageUrl;
}
