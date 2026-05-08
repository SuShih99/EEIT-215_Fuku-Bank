package com.javaeasybank.account.dto.response;

import com.javaeasybank.account.entity.AccountApplication;
import com.javaeasybank.account.enums.*;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 開戶申請 Response DTO
 */
@Getter
@Builder
public class AccountApplicationResponse {

    private Long id;
    private String applicationNo;
    private String customerId;

    // 帳戶
    private AccountType accountType;
    private Currency currency;

    // KYC
    private String name;
    private String idNumber;
    private LocalDate birthday;
    private String nationality;
    private String phone;
    private String registeredAddress;
    private String currentAddress;

    // 職業
    private String occupation;
    private String employer;
    private Integer estimatedMonthlyTx;

    // 目的 & 資金來源
    private AccountPurpose accountPurpose;
    private FundSource fundSource;

    // 法遵
    private String taxResidency;
    private Boolean isPep;

    // 證件圖片
    private String idFrontUrl;
    private String idBackUrl;
    private String secondIdUrl;

    // 風控
    private RiskFlag riskFlag;

    // 審核
    private ApplicationStatus status;
    private String rejectReason;
    private LocalDateTime reviewedAt;
    private String reviewedBy;
    private String createdAccountNumber;

    // 時間戳
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Entity → Response DTO
     */
    public static AccountApplicationResponse fromEntity(AccountApplication app) {
        return AccountApplicationResponse.builder()
                .id(app.getId())
                .applicationNo(app.getApplicationNo())
                .customerId(app.getCustomerId())
                .accountType(app.getAccountType())
                .currency(app.getCurrency())
                .name(app.getName())
                .idNumber(maskIdNumber(app.getIdNumber()))
                .birthday(app.getBirthday())
                .nationality(app.getNationality())
                .phone(maskPhone(app.getPhone()))
                .registeredAddress(app.getRegisteredAddress())
                .currentAddress(app.getCurrentAddress())
                .occupation(app.getOccupation())
                .employer(app.getEmployer())
                .estimatedMonthlyTx(app.getEstimatedMonthlyTx())
                .accountPurpose(app.getAccountPurpose())
                .fundSource(app.getFundSource())
                .taxResidency(app.getTaxResidency())
                .isPep(app.getIsPep())
                .idFrontUrl(app.getIdFrontUrl())
                .idBackUrl(app.getIdBackUrl())
                .secondIdUrl(app.getSecondIdUrl())
                .riskFlag(app.getRiskFlag())
                .status(app.getStatus())
                .rejectReason(app.getRejectReason())
                .reviewedAt(app.getReviewedAt())
                .reviewedBy(app.getReviewedBy())
                .createdAccountNumber(app.getCreatedAccountNumber())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }

    /**
     * 管理端專用：不遮蔽敏感欄位
     */
    public static AccountApplicationResponse fromEntityForAdmin(AccountApplication app) {
        return AccountApplicationResponse.builder()
                .id(app.getId())
                .applicationNo(app.getApplicationNo())
                .customerId(app.getCustomerId())
                .accountType(app.getAccountType())
                .currency(app.getCurrency())
                .name(app.getName())
                .idNumber(app.getIdNumber())
                .birthday(app.getBirthday())
                .nationality(app.getNationality())
                .phone(app.getPhone())
                .registeredAddress(app.getRegisteredAddress())
                .currentAddress(app.getCurrentAddress())
                .occupation(app.getOccupation())
                .employer(app.getEmployer())
                .estimatedMonthlyTx(app.getEstimatedMonthlyTx())
                .accountPurpose(app.getAccountPurpose())
                .fundSource(app.getFundSource())
                .taxResidency(app.getTaxResidency())
                .isPep(app.getIsPep())
                .idFrontUrl(app.getIdFrontUrl())
                .idBackUrl(app.getIdBackUrl())
                .secondIdUrl(app.getSecondIdUrl())
                .riskFlag(app.getRiskFlag())
                .status(app.getStatus())
                .rejectReason(app.getRejectReason())
                .reviewedAt(app.getReviewedAt())
                .reviewedBy(app.getReviewedBy())
                .createdAccountNumber(app.getCreatedAccountNumber())
                .createdAt(app.getCreatedAt())
                .updatedAt(app.getUpdatedAt())
                .build();
    }

    // ===== 遮蔽工具 =====

    /** 身分證遮蔽：A123456789 → A12****789 */
    private static String maskIdNumber(String idNumber) {
        if (idNumber == null || idNumber.length() < 6) return idNumber;
        return idNumber.substring(0, 3) + "****" + idNumber.substring(idNumber.length() - 3);
    }

    /** 手機遮蔽：0912345678 → 0912***678 */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 4) + "***" + phone.substring(phone.length() - 3);
    }
}
