package com.javaeasybank.loan.dto.requests;

import com.javaeasybank.loan.enums.LoanApplicationStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * 外部模組回調時更新申請狀態的請求 DTO。
 *
 * <p>供外部系統呼叫 {@code LoanCallbackController} 時傳入，
 * 用以通知貸款模組更新申請狀態。目前有兩個外部呼叫方：</p>
 * <ul>
 *   <li><b>風控模組（RISK）</b>：審核完成後將狀態從
 *       {@code PENDING_REVIEW} 更新為 {@code APPROVED} 或 {@code REJECTED}。</li>
 *   <li><b>帳戶模組（ACCOUNT）</b>：撥款完成後將狀態從
 *       {@code APPROVED} 更新為 {@code DISBURSED}，並帶回貸款帳號。</li>
 * </ul>
 */
@Getter
@Setter
public class LoanStatusCallbackRequestDTO {

    /**
     * 目標申請狀態，即外部模組希望貸款申請切換到的新狀態。
     * 允許值：{@code APPROVED}、{@code REJECTED}、{@code DISBURSED}。
     */
    private LoanApplicationStatus newStatus;

    /**
     * 呼叫方識別碼，用於驗證與日誌追蹤。
     * 允許值：{@code "RISK"}（風控模組）、{@code "ACCOUNT"}（帳戶模組）。
     */
    private String callerModule;

    /**
     * 備註說明（選填）。
     * 例如風控拒絕時填入拒絕原因，或撥款時記錄交易流水號。
     */
    private String note;

    /**
     * 貸款帳號（帳戶模組撥款後帶入）。
     * 僅 {@code callerModule = "ACCOUNT"} 時有值，
     * 用於建立 {@code LoanAccount} 記錄。
     */
    private String loanAccountNumber;
}
