package com.javaeasybank.loan.dto.response;

import com.javaeasybank.loan.enums.LoanApplicationStatus;
import com.javaeasybank.loan.enums.LoanContactStatus;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// 貸款申請查詢回應 DTO
@Getter
@Setter
public class LoanApplicationResponseDTO {

    // 貸款申請唯一識別碼（UUID）
    private String applicationId;

    // 客戶內部識別碼（系統內部使用，不對外顯示）
    private String customerId;

    // 客戶對外識別碼（CIF），前端顯示用
    private String cif;

    // 會員姓名，由會員模組查詢後填入，供前端列表直接顯示
    private String memberName;

    // 會員電話，供管理端聯繫紀錄顯示
    private String customerPhone;

    // 會員信箱，供管理端聯繫紀錄顯示
    private String customerEmail;

    // 貸款種類，例如 "PERSONAL"（信貸）、"HOUSE"（房貸）
    private String applyType;

    // 客戶原始申請金額（新台幣）
    private BigDecimal applyAmount;

    // 客戶原始申請期數（月）
    private Integer applyPeriod;

    // 申請時顯示的年利率（百分比小數）
    private BigDecimal rate;

    // 客戶申請時選擇的撥款入帳帳號（台幣活存）
    private String disbursementAccount;

    // 行員二次填單確認金額（新台幣）
    private BigDecimal confirmedAmount;

    // 行員二次填單確認期數（月）
    private Integer confirmedPeriod;

    // 行員二次填單確認年利率（百分比小數）
    private BigDecimal confirmedRate;

    // 申請目前的狀態，參見 LoanApplicationStatus
    private LoanApplicationStatus applicationStatus;

    // 申請建立時間
    private LocalDateTime createTime;

    // 申請最後更新時間（任何欄位異動均會刷新）
    private LocalDateTime updateTime;

    // 最近一筆聯繫紀錄的結果狀態，供行員列表快速判斷聯繫進度
    private LoanContactStatus latestContactStatus;

    // 最近一筆聯繫紀錄的時間
    private LocalDateTime latestContactTime;
    private List<String> requiredDocuments;
    private String reviewComment;

    // 客戶送出補件的時間戳記
    private LocalDateTime documentsSubmittedAt;
}
