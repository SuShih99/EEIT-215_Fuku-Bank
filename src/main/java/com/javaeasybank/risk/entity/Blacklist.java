package com.javaeasybank.risk.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.javaeasybank.risk.enums.BlacklistType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "BLACK_LIST", indexes = {
        @Index(name = "idx_bl_lookup", columnList = "list_type, list_value")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Blacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    //黑名單類型（如：EMAIL, PHONE, IP）
    @Enumerated(EnumType.STRING)
    @Column(name = "list_type", nullable = false, length = 20)
    private BlacklistType listType;
    //具體的黑名單值
    @Column(name = "list_value", nullable = false, length = 100)
    private String listValue;
    //資料來源
    @Column(name = "source", length = 50, columnDefinition = "NVARCHAR(50)")
    private String source;

    @Column(name = "reason", columnDefinition = "NVARCHAR(255)")
    private String reason;
    //是否啟用
    @Column(name = "status", nullable = false)
    private Boolean status = Boolean.TRUE;
    //解封時間 設null表示永久
    @Column(name = "expires_at")
    @JsonFormat(pattern = "yyyy-MM-ddTHH:mm:ss", timezone = "GMT+8")
    private LocalDateTime expireAt;

    @CreatedDate
    @Column(name = "created_at")
    @JsonFormat(pattern = "yyyy-MM-ddTHH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-ddTHH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;
}
