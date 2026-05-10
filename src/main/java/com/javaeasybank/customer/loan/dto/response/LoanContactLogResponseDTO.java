package com.javaeasybank.customer.loan.dto.response;

import com.javaeasybank.customer.loan.enums.LoanContactChannel;
import com.javaeasybank.customer.loan.enums.LoanContactStatus;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
public class LoanContactLogResponseDTO {
    private String logId;
    private String applicationId;
    private String empId;
    private LoanContactStatus contactStatus;
    private LoanContactChannel contactChannel;
    private LocalDateTime contactTime;
    private String note;
}