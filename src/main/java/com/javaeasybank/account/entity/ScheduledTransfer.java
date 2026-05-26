package com.javaeasybank.account.entity;

import com.javaeasybank.account.enums.TransferBank;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_transfer")
@Getter
@Setter
@NoArgsConstructor
public class ScheduledTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false, length = 20)
    private String customerId;

    @Column(name = "from_account_number", nullable = false, length = 14)
    private String fromAccountNumber;

    @Column(name = "to_bank_code", nullable = false, length = 3)
    private String toBankCode = TransferBank.JVB.getCode();

    @Column(name = "to_bank_name", nullable = false, columnDefinition = "NVARCHAR(80)")
    private String toBankName = TransferBank.JVB.getDisplayName();

    @Column(name = "to_account_number", nullable = false, length = 20)
    private String toAccountNumber;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(columnDefinition = "NVARCHAR(200)")
    private String note;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "fail_reason", columnDefinition = "NVARCHAR(500)")
    private String failReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
