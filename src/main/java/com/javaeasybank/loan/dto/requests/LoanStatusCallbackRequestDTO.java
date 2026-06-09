package com.javaeasybank.loan.dto.requests;

/**
 * 程式說明：
 * - 貸款狀態回呼的請求資料物件。
 * - 封裝外部流程通知本系統時需要的申請編號、狀態與補充訊息。
 */

import com.javaeasybank.loan.enums.LoanApplicationStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 外部模組狀態回調 DTO。
 * 風控可將 PENDING_REVIEW 推進為 APPROVED/REJECTED，帳務可將 APPROVED 推進為 DISBURSED。
 */
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
