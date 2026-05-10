package com.javaeasybank.account.entity;

import com.javaeasybank.account.enums.Currency;
import com.javaeasybank.account.enums.EntryType;
import com.javaeasybank.account.enums.TransactionType;
import com.javaeasybank.account.enums.TransferBank;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 交易日誌實體類，對應資料庫中的 `trans_log` 表。
 * 記錄了所有帳戶交易的詳細資訊。
 */
@Entity
@Table(
    name = "TRANS_LOG",
    indexes = {
        @Index(name = "idx_tx_ref", columnList = "reference_id"),
        @Index(name = "idx_tx_account_time", columnList = "account_number, created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class TransLog {

    /**
     * 交易 ID，主鍵，長度為 36 (UUID)，不可為空。
     */
    @Id
    @Column(name = "transaction_id", length = 36, nullable = false)
    private String transactionId;

    /**
     * 業務參考 ID，長度為 30，不可為空。
     */
    @Column(name = "reference_id", length = 30, nullable = false)
    private String referenceId;

    /**
     * 交易相關的帳號，長度為 12，不可為空。
     */
    @Column(name = "account_number", length = 12, nullable = false)
    private String accountNumber;

    /**
     * 對方帳號，行內 12 碼，跨行最多 20 碼。
     */
    @Column(name = "counterpart_account", length = 20)
    private String counterpartAccount;

    /**
     * 本筆交易所屬銀行代碼。
     */
    @Column(name = "bank_code", length = 10, nullable = false)
    private String bankCode = TransferBank.JVB.getCode();

    /**
     * 本筆交易所屬銀行名稱。
     */
    @Column(name = "bank_name", length = 50, nullable = false, columnDefinition = "NVARCHAR(50)")
    private String bankName = TransferBank.JVB.getDisplayName();

    /**
     * 對方銀行代碼。
     */
    @Column(name = "counterpart_bank_code", length = 10)
    private String counterpartBankCode;

    /**
     * 對方銀行名稱。
     */
    @Column(name = "counterpart_bank_name", length = 50, columnDefinition = "NVARCHAR(50)")
    private String counterpartBankName;

    /**
     * 是否為跨行交易。
     */
    @Column(name = "is_interbank", nullable = false)
    private boolean interbank = false;

    /**
     * 記帳類型（借方/貸方），使用枚舉儲存，不可為空。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private EntryType entryType;

    /**
     * 交易類型，使用枚舉儲存，不可為空。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 25)
    private TransactionType transactionType;

    /**
     * 交易金額，不可為空，精度為 19，小數點後 4 位。
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /**
     * 手續費金額。
     */
    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    /**
     * 本次業務總扣款金額。
     */
    @Column(name = "total_debit_amount", precision = 19, scale = 4)
    private BigDecimal totalDebitAmount;

    /**
     * 交易前的帳戶餘額，不可為空，精度為 19，小數點後 4 位。
     */
    @Column(name = "balance_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceBefore;

    /**
     * 交易後的帳戶餘額，不可為空，精度為 19，小數點後 4 位。
     */
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    /**
     * 交易貨幣，使用枚舉儲存，不可為空。
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    /**
     * 交易備註，長度為 200。
     */
    @Column(columnDefinition = "NVARCHAR(200)")
    private String note;

    /**
     * 交易創建時間，自動生成，不可為空，不可更新。
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 在持久化之前自動生成 transactionId (UUID)。
     */
    @PrePersist
    public void prePersist() {
        if (this.transactionId == null) {
            this.transactionId = UUID.randomUUID().toString();
        }
    }

    /**
     * 重寫 equals 方法，僅比較交易 ID (transactionId) 作為業務主鍵。
     *
     * @param o 待比較的物件。
     * @return 如果交易 ID 相同則返回 true，否則返回 false。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransLog that)) return false;
        return this.getTransactionId() != null &&
                this.getTransactionId().equals(that.getTransactionId());
    }

    /**
     * 重寫 hashCode 方法，與 equals 方法保持一致性。
     *
     * @return 該物件的雜湊碼。
     */
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
