package com.javaeasybank.loan.dto.requests;

import com.javaeasybank.loan.enums.LoanContactChannel;
import com.javaeasybank.loan.enums.LoanContactStatus;
import lombok.Getter;
import lombok.Setter;

// ===新增聯繫紀錄===
@Getter
@Setter
public class LoanContactLogRequestDTO {
    private String empId;
    private String contactStatus;
    private String contactChannel;
    private String note;
}

