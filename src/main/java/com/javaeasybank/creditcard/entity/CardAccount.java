package com.javaeasybank.creditcard.entity;

import java.math.BigDecimal;
import java.util.List;



import com.javaeasybank.customer.entity.CustomerProfile;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "CARD_ACCOUNT")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    // 帳號
    private String accountNumber;
    // 總信用額度
    private BigDecimal creditLimit;
    // 已使用額度
    // 結帳日
    private Integer statementDay;
    // 繳款截止日
    private Integer dueDays;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private CustomerProfile customer;

    @OneToMany(mappedBy = "cardAccount")
    private List<CreditCard> cards;
}
