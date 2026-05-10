package com.javaeasybank.customer.loan.dto.requests;

import com.javaeasybank.customer.loan.enums.LoanContactChannel;
import com.javaeasybank.customer.loan.enums.LoanContactStatus;
import lombok.Getter;
import lombok.Setter;

// ===新增聯繫紀錄===
@Getter
@Setter
public class LoanContactLogRequestDTO {
    private String empId;
    private LoanContactStatus contactStatus;
    private LoanContactChannel contactChannel;
    private String note;
}

