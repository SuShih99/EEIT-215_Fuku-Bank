package com.javaeasybank.creditcard.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.javaeasybank.creditcard.enums.TransactionChannel;
import com.javaeasybank.creditcard.enums.TxnType;

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

@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "CARD_TRANSACTION")
@Getter
@Setter
public class CardTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "txn_id")
    private Integer txnId;

    @Column(name = "txn_amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal txnAmount;

    @Column(name = "cashback_rate", precision = 15, scale = 2)
    private BigDecimal cashbackRate;

    @Column(name = "cashback_amount", precision = 15, scale = 2)
    private BigDecimal cashbackAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", length = 20)
    private TxnType txnType;

    @Column(name = "txn_date")
    private LocalDateTime txnDate;

    @Column(name = "description", length = 200, columnDefinition = "NVARCHAR(200)")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", length = 50)
    private TransactionChannel channel;

    @Column(name = "external_txn_id", length = 100)
    private String externalTxnId;

    @ManyToOne
    @JoinColumn(name = "ref_txn_id")
    private CardTransaction refTxn;

    @ManyToOne
    @JoinColumn(name = "card_id", nullable = false)
    private CreditCard card;

    @ManyToOne
    @JoinColumn(name = "merchant_id")
    private Merchant merchant;

    @ManyToOne
    @JoinColumn(name = "bill_id")
    private CardBill bill;

    @PrePersist
    public void prePersist() {
        if (this.txnDate == null) {
            this.txnDate = LocalDateTime.now();
        }
    }
}
