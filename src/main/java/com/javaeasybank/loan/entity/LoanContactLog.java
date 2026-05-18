package com.javaeasybank.loan.entity;

import com.javaeasybank.loan.enums.LoanContactChannel;
import com.javaeasybank.loan.enums.LoanContactStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "LOAN_CONTACT_LOG")
@Getter
@Setter
@NoArgsConstructor
public class LoanContactLog {

    @Id
    // 紀錄ID
    private String logId;

    // 申請ID
    private String applicationId;

    //  聯繫人員ID
    private String empId;

    // 聯繫狀態
    @Enumerated(EnumType.STRING)
    private LoanContactStatus contactStatus;

    // 聯繫管道
    @Enumerated(EnumType.STRING)
    private LoanContactChannel contactChannel;

    // 聯繫時間
    private LocalDateTime contactTime;

    // 備註
    private String note;
}
