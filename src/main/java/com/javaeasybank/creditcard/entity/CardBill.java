package com.javaeasybank.creditcard.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.javaeasybank.creditcard.enums.BillStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "CARD_BILL")
public class CardBill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bill_id")
    private Integer billId;

    @Column(name = "billing_month", length = 7)
    private String billingMonth;

    @Column(name = "bill_date")
    private LocalDate billDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "minimum_payment", precision = 15, scale = 2)
    private BigDecimal minimumPayment;

    @Column(name = "paid_amount", precision = 15, scale = 2)
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "bill_status", length = 20)
    private BillStatus billStatus;

    @Column(name = "cashback_amount", precision = 15, scale = 2)
    private BigDecimal cashbackAmount = BigDecimal.ZERO;

    @Column(name = "reward_posted")
    private Boolean rewardPosted = false;

    @Column(name = "reward_reference_id", length = 100)
    private String rewardReferenceId;

    @ManyToOne
    @JoinColumn(name = "card_id")
    private CreditCard card;

    @ManyToOne
    @JoinColumn(name = "card_account_id")
    private CardAccount cardAccount;

    @OneToMany(mappedBy = "bill")
    private List<CardTransaction> transactions;
}
