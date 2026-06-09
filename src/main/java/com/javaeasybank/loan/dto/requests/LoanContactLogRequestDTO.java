package com.javaeasybank.loan.dto.requests;

/**
 * 程式說明：
 * - 新增聯絡紀錄的請求資料物件。
 * - 封裝客服或審核人員與申請人聯絡時的管道、結果、備註與員工編號。
 */

import lombok.Getter;
import lombok.Setter;

// 新增聯繫紀錄的請求 DTO
/**
 * 行員新增聯繫紀錄時的請求 DTO。
 * 記錄聯繫管道、結果狀態、備註與下一次聯繫時間。
 */
@Getter
@Setter
public class LoanContactLogRequestDTO {

    // 行員工號，用於識別是哪位行員進行聯繫
    private String empId;

    // 聯繫結果狀態，對應 LoanContactStatus 列舉的 name()
    private String contactStatus;

    // 聯繫管道，對應 LoanContactChannel 列舉的 name()
    private String contactChannel;

    // 備註說明，例如客戶提出的問題或特殊情況（選填）
    private String note;
}
