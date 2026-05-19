package com.javaeasybank.loan.dto.requests;

import lombok.Getter;
import lombok.Setter;

/**
 * 新增聯繫紀錄的請求 DTO。
 *
 * <p>行員每次與客戶聯繫後，透過此 DTO 記錄聯繫結果，
 * 對應 {@code POST /api/staff/loan-applications/{id}/contact-logs}。</p>
 */
@Getter
@Setter
public class LoanContactLogRequestDTO {

    /** 行員工號，用於識別是哪位行員進行聯繫。 */
    private String empId;

    /**
     * 聯繫結果狀態，對應 {@code LoanContactStatus} 列舉的 name()。
     * 例如：{@code "REACHED"}、{@code "ATTEMPTED"}。
     */
    private String contactStatus;

    /**
     * 聯繫管道，對應 {@code LoanContactChannel} 列舉的 name()。
     * 例如：{@code "PHONE"}、{@code "EMAIL"}。
     */
    private String contactChannel;

    /** 備註說明，例如客戶提出的問題或特殊情況（選填）。 */
    private String note;
}
