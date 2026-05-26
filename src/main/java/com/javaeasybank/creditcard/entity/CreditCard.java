package com.javaeasybank.creditcard.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.javaeasybank.creditcard.enums.CardStatus;
import com.javaeasybank.customer.entity.CustomerProfile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "CREDIT_CARD")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreditCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_id")
    private Integer cardId;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerProfile customer;

    @ManyToOne
    @JoinColumn(name = "card_type_id", nullable = false)
    private CardType cardType;

    @Column(name = "card_number", length = 16, nullable = false, unique = true)
    private String cardNumber;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(name = "current_debt", precision = 15, scale = 2)
    private BigDecimal currentDebt;

    @Column(name = "create_date")
    private LocalDateTime createDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private CardStatus status;

    @ManyToOne
    @JoinColumn(name = "application_item_id")
    private CardApplicationItem applicationItem;

    @Column(name = "credit_card_account_number", length = 20)
    private String creditCardAccountNumber;

    @ManyToOne
    @JoinColumn(name = "card_account_id")
    private CardAccount cardAccount;

    @PrePersist
    public void prePersist() {
        if (createDate == null) {
            createDate = LocalDateTime.now();
        }
    }
}
