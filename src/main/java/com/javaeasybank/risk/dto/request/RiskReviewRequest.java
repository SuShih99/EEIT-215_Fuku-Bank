package com.javaeasybank.risk.dto.request;

import com.javaeasybank.account.enums.FundSource;
import com.javaeasybank.risk.enums.BusinessScene;
import com.javaeasybank.risk.enums.Occupation;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
//********待修正********
//審核用資料需要調整
public class RiskReviewRequest {

    // ── 路由資訊 ──────────────────────────────────────────

    /**
     * 業務場景：LOAN_APPLY / ACCOUNT_OPEN / CARD_APPLY
     * 風控 Service 依此走不同評分策略
     */
    @NotNull(message = "scene 不可為空")
    private BusinessScene scene;

    /**
     * 業務主鍵（貸款申請ID、帳戶申請ID...）
     * 對應 RiskEventLog.businessId
     */
    @NotBlank(message = "businessId 不可為空")
    @Size(max = 64)
    private String businessId;

    /**
     * 風控處理完後主動回調的地址
     * 由各模組的 Client 自行填入，例如：
     *   http://loan-service/api/loan-callbacks/{applicationId}/status
     */
    @NotBlank(message = "callbackUrl 不可為空")
    private String callbackUrl;

    // ── 客戶身份 ──────────────────────────────────────────

    /**
     * 客戶ID（對應 CustomerCreditInfo.customerId）
     * 風控用來查既有信用資料
     */
    @NotBlank(message = "customerId 不可為空")
    @Size(max = 20)
    private String customerId;

    /**
     * 用於黑名單比對（對應 Blacklist listType=EMAIL）
     */
    @Size(max = 20)
    private String idCard;


    @Email
    @Size(max = 100)
    private String email;

    /**
     * 用於黑名單比對（對應 Blacklist listType=PHONE）
     */
    @Size(max = 20)
    private String phone;

    // ── 財務資訊（補充或覆蓋 CustomerCreditInfo）────────────

    /**
     * 本次申請金額
     * 對應 RiskEventLog.transactionAmount
     */
    @NotNull(message = "amount 不可為空")
    @DecimalMin(value = "0.01", message = "金額必須大於 0")
    @Digits(integer = 14, fraction = 2)
    private BigDecimal amount;

    /**
     * 年收入（若已有 CustomerCreditInfo 可不傳，由風控自行查）
     * 傳了就用最新值覆蓋，不傳就沿用資料庫既有資料
     */
    @DecimalMin(value = "0.00")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal annualIncome;

    /**
     * 職業（同上，選填覆蓋）
     */
    private Occupation occupation;

    /**
     * 外部信用評分（模擬聯徵資料，300~800）
     */
    @Min(300) @Max(800)
    private Integer externalScore;

    /**
     * 他行負債
     */
    @DecimalMin(value = "0.00")
    @Digits(integer = 13, fraction = 2)
    private BigDecimal otherBankDebt;

    /**
     * 是否有不動產
     */
    private Boolean hasRealEstate;

    private Boolean isPep;

    private FundSource fundSource;

    private List<RiskAttachmentRequest.AttachmentDetail> documents;
}
