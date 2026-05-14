package com.javaeasybank.account.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_transfer")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class PendingTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── 轉帳資料 ──────────────────────────────────────

    /** 轉帳流水號（對外顯示用） */
    @Column(name = "reference_id", nullable = false, unique = true, length = 50)
    private String referenceId;

    /** 轉出帳號 */
    @Column(name = "from_account_number", nullable = false, length = 20)
    private String fromAccountNumber;

    /** 轉入帳號 */
    @Column(name = "to_account_number", nullable = false, length = 20)
    private String toAccountNumber;

    /** 轉入銀行代碼（跨行才有） */
    @Column(name = "to_bank_code", length = 10)
    private String toBankCode;

    /** 轉帳金額 */
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    /** 幣別 */
    @Column(length = 3)
    private String currency;

    /** 備註 */
    @Column(length = 200)
    private String note;

    // ── 風控關聯 ──────────────────────────────────────

    /** 對應的 RiskEventLog.logId */
    @Column(name = "risk_log_id")
    private Long riskLogId;

    /** 對應的 ReviewTask.taskId */
    @Column(name = "review_task_id")
    private Long reviewTaskId;

    /** 觸發暫停的原因 */
    @Column(name = "hold_reason", length = 200)
    private String holdReason;

    // ── 狀態 ──────────────────────────────────────────

    /**
     * PENDING   → 等待人工審核
     * APPROVED  → 審核通過，轉帳執行中
     * REJECTED  → 審核拒絕，交易取消
     * EXECUTED  → 轉帳已完成
     * EXPIRED   → 超時未審核，自動取消
     */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    // ── 時間戳 ────────────────────────────────────────

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 審核完成或執行完成的時間 */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /** 自動過期時間（預設建立後 24 小時）*/
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        if (this.expiresAt == null) {
            this.expiresAt = LocalDateTime.now().plusHours(24);
        }
    }
}