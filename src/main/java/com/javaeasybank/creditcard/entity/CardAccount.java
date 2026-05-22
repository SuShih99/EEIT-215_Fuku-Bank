package com.javaeasybank.creditcard.entity;

import java.math.BigDecimal;
import java.util.List;

import com.javaeasybank.customer.entity.CustomerProfile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
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
    @Column(name = "id")
    private Integer id;

    @Column(name = "account_number", length = 20)
    private String accountNumber;

    @Column(name = "credit_limit", precision = 15, scale = 2)
    private BigDecimal creditLimit;

    @Column(name = "statement_day")
    private Integer statementDay;

    @Column(name = "due_days")
    private Integer dueDays;

    @OneToOne
    @JoinColumn(name = "customer_id", nullable = false,unique = true)
    private CustomerProfile customer;

    @OneToMany(mappedBy = "cardAccount")
    private List<CreditCard> cards;
}
