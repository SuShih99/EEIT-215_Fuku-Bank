package com.javaeasybank.customer.loan.entity;

import com.javaeasybank.customer.loan.enums.LoanContactChannel;
import com.javaeasybank.customer.loan.enums.LoanContactStatus;
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
    private String logId;                  // log_id (PK)

    private String applicationId;          // application_id (FK)

    private String empId;                  // 聯繫人員

    @Enumerated(EnumType.STRING)
    private LoanContactStatus contactStatus;

    @Enumerated(EnumType.STRING)
    private LoanContactChannel contactChannel;

    private LocalDateTime contactTime;
    private String note;
}
