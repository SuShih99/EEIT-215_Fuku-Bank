package com.javaeasybank.loan.dto.response;

import com.javaeasybank.loan.enums.LoanApplicationStatus;
import com.javaeasybank.loan.enums.LoanContactStatus;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class LoanApplicationResponseDTO {
    private String applicationId;
    private String customerId;   // 內部識別，不對外顯示
    private String cif;          // 對外顯示用的顧客識別碼
    private String memberName;   // 會員姓名（供前端顯示）
    private String applyType;
    private BigDecimal applyAmount;
    private Integer applyPeriod;
    private BigDecimal rate;
    private String disbursementAccount;     // 客戶申請時選擇的撥款入帳帳號
    // 二次填單確認值（PENDING_REVIEW 以後的狀態才有值）
    private BigDecimal confirmedAmount;
    private Integer confirmedPeriod;
    private BigDecimal confirmedRate;
    private LoanApplicationStatus applicationStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LoanContactStatus latestContactStatus;
    private LocalDateTime latestContactTime;
    private LocalDateTime documentsSubmittedAt;  // 客戶送出補件時間，null = 尚未送出
    private List<String> requiredDocuments;
    private String reviewComment;
}
