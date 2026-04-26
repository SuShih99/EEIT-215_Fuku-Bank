package com.javaeasybank.account.entity;

import com.javaeasybank.account.enums.AccountStatus;
import com.javaeasybank.account.enums.AccountType;
import com.javaeasybank.account.enums.Currency;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 帳戶實體類，對應資料庫中的 `account` 表。
 * 儲存了帳戶的詳細資訊，包括帳號、客戶 ID、帳戶類型、餘額、狀態等。
 */
@Entity
@Table(name = "account")
@Getter
@Setter
@NoArgsConstructor
public class Account {

    /**
     * 帳號，主鍵，長度為 12，不可為空。
     */
    @Id
    @Column(name = "account_number", length = 12, nullable = false)
    private String accountNumber;

    /**
     * 客戶 ID，不可為空。
     */
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    /**
     * 帳戶類型，使用枚舉儲存，不可為空。
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    /**
     * 帳戶貨幣，使用枚舉儲存，不可為空。
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    /**
     * 帳戶餘額，預設為 0，精度為 19，小數點後 4 位。
     */
    @Column(precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * 帳戶負債，預設為 0，精度為 19，小數點後 4 位。
     */
    @Column(precision = 19, scale = 4)
    private BigDecimal liability = BigDecimal.ZERO;

    /**
     * 帳戶利率，精度為 7，小數點後 5 位。
     */
    @Column(name = "interest_rate", precision = 7, scale = 5)
    private BigDecimal interestRate;

    /**
     * 帳戶狀態，使用枚舉儲存，預設為 PENDING，不可為空。
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.PENDING;

    /**
     * 父帳號，用於子帳戶，長度為 12。
     */
    @Column(name = "parent_account_number", length = 12)
    private String parentAccountNumber;

    /**
     * 帳戶創建時間，自動生成，不可為空，不可更新。
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 帳戶創建者，長度為 20。
     */
    @Column(name = "created_by", length = 20)
    private String createdBy;

    /**
     * 帳戶最後更新時間，自動更新，不可為空。
     */
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    /**
     * 帳戶最後更新者，長度為 20。
     */
    @Column(name = "changed_by", length = 20)
    private String changedBy;

    /**
     * 在實體持久化之前執行。
     * 如果 createdAt 為 null，則設置為當前時間。
     * 同時設置 changedAt 為當前時間。
     */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        changedAt = LocalDateTime.now();
    }

    /**
     * 在實體更新之前執行。
     * 設置 changedAt 為當前時間。
     */
    @PreUpdate
    public void preUpdate() {
        changedAt = LocalDateTime.now();
    }

    /**
     * 重寫 equals 方法，僅比較帳號 (accountNumber) 作為業務主鍵。
     * 這樣可以確保即使是不同記憶體地址的 Account 物件，只要帳號相同，就被視為相等。
     *
     * @param o 待比較的物件。
     * @return 如果帳號相同則返回 true，否則返回 false。
     */
    @Override
    public boolean equals(Object o) {
        // memory address check
        // 同個地址不可能不一樣
        if (this == o) return true;

        // 防呆，如果型別不對就不用比了
        // instanceof 是子類也包含
        // hibernate 的代理物件也算是一種子類別
        if (!(o instanceof Account that)) return false;

        // 因為 JPA lazyload的關係 這邊不能寫這樣
        // 剛載入的時候 很可能還沒進SQL下指令撈資料
        // 導致 "代理物件" 是空的 用.accountNumber 會出事
        // return Objects.equals(accountNumber, that.accountNumber);
        return this.getAccountNumber() != null &&
                this.getAccountNumber().equals(that.getAccountNumber());
    }

    /**
     * 重寫 hashCode 方法，與 equals 方法保持一致性。
     * 為了確保實體在不同生命週期（如剛 new 出來與存入 DB 後）的雜湊值穩定，
     * 在 JPA 實體中，最安全的做法是回傳固定值或基於 getClass() 計算。
     *
     * @return 該物件的雜湊碼。
     */
    @Override
    public int hashCode() {
        // 為了確保實體在不同生命週期（如剛 new 出來與存入 DB 後）的雜湊值穩定，
        // 在 JPA 實體中，最安全的做法是回傳固定值或基於 getClass() 計算。
        return getClass().hashCode();
    }
}
