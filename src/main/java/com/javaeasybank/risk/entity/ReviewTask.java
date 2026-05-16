package com.javaeasybank.risk.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.javaeasybank.risk.enums.BusinessScene;
import com.javaeasybank.risk.enums.ReviewResult;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
//人工審核任務表
public class ReviewTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "log_id", referencedColumnName = "log_id", nullable = false)
    private RiskEventLog riskEventLog;
    //業務編號 例: 貸款申請表ID
    @Column(nullable = false, length = 64)
    private String businessId;
    //業務場景
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private BusinessScene scene;
    /**
     * 任務進度：PENDING(待處理), PROCESSING(處理中), COMPLETED(已結案)
     */
    private String status;
    /**
     * 最終決策：APPROVED, REJECTED
     */
    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private ReviewResult reviewResult;
    //審核人
    private String assignee;
    //審核人備註意見
    @Column(columnDefinition = "NVARCHAR(500)")
    private String adminComment;
    //優先度
    private Integer priority;
    /**
     * 客戶送出的補件文件清單（JSON 陣列，null = 尚未收到補件）
     * ⚠ DB migration: ALTER TABLE REVIEW_TASK ADD attachments NVARCHAR(MAX) NULL
     */
    @Lob
    @Column(name = "attachments", columnDefinition = "NVARCHAR(MAX)")
    private String attachments;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createAt;
    //結案時間
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime processedAt;

    @Version // 樂觀鎖預防併發衝突
    private Long version;


}
