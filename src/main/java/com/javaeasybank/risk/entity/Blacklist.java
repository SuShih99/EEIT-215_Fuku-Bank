package com.javaeasybank.risk.entity;

import com.javaeasybank.risk.core.enums.BlacklistType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "[BlackList]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Blacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "[list_type]", nullable = false, length = 20)
    private BlacklistType listType;

    @Column(name = "[list_value]", nullable = false, length = 100)
    private String listValue;

    @Column(name = "[source]", length = 50)
    private String source;

    @Column(name = "[reason]")
    private String reason;

    @Column(name = "[status]", nullable = false)
    private Boolean status = Boolean.TRUE;

    @Column(name = "[expires_at]")
    private LocalDateTime expireAt;

    @Column(name = "[created_at]")
    private LocalDateTime createdAt;
}
