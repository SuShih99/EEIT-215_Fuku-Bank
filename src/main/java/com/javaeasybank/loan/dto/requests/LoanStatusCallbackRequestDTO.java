package com.javaeasybank.loan.dto.requests;

import com.javaeasybank.loan.enums.LoanApplicationStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// ===外部模組回調：更新申請狀態===
// 風控模組：PENDING_REVIEW → APPROVED / REJECTED
// 帳戶模組：APPROVED → DISBURSED
@Getter
@Setter
public class LoanStatusCallbackRequestDTO {
    private LoanApplicationStatus newStatus; // 目標狀態
    private String callerModule; // 呼叫方識別："RISK" | "ACCOUNT"
    private String note; // 備註（選填，例如風控拒絕原因）
    private String loanAccountNumber;
    private List<String> requiredDocuments; // 補件文件清單，例如 ["ID_CARD", "PROOF_OF_INCOME"]
    private String adminComment;
}
