package com.javaeasybank.loan.dto.response;

import com.javaeasybank.loan.enums.LoanContactChannel;
import com.javaeasybank.loan.enums.LoanContactStatus;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

// 聯繫紀錄查詢回應 DTO
@Getter
@Setter
public class LoanContactLogResponseDTO {

    // 聯繫紀錄唯一識別碼（UUID）
    private String logId;

    // 關聯的貸款申請識別碼
    private String applicationId;

    // 執行聯繫的行員工號
    private String empId;

    // 此次聯繫的結果狀態，參見 LoanContactStatus
    private LoanContactStatus contactStatus;

    // 此次聯繫使用的管道（電話、Email、簡訊），參見 LoanContactChannel
    private LoanContactChannel contactChannel;

    // 聯繫發生的時間戳記
    private LocalDateTime contactTime;

    // 行員備註（選填），記錄聯繫過程中的特殊說明或客戶反饋
    private String note;
}
