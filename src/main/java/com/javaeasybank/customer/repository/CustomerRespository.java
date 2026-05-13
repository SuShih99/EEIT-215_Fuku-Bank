package com.javaeasybank.customer.repository;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CustomerRespository {

    // ===== 管理端：新增/修改客戶 =====
    @Getter
    @Setter
    public static class CustomerRequest {
        private String customerId;
        private String idNumber;
        private String name;
        private LocalDate birthday;
        private String gender;
        private String email;
        private String phone;
        private String address;
        private String nationality;
        private String registeredAddress;
        private String currentAddress;
        private String occupation;
        private String employer;
        private Integer estimatedMonthlyTx;
        private String accountPurpose;
        private String fundSource;
        private String taxResidency;
        private Boolean isPep;
        private String idFrontUrl;
        private String idBackUrl;
        private String secondIdUrl;
        private Long latestAccountApplicationId;
        private String latestAccountApplicationNo;
        private String latestAppliedAccountType;
        private String latestAppliedCurrency;
        private String latestAccountApplicationStatus;
        private String latestAccountApplicationRiskFlag;
        private LocalDateTime latestAccountApplicationReviewedAt;
        private String latestAccountApplicationReviewedBy;
        private String latestAccountApplicationRejectReason;
        private String createdAccountNumber;
        private LocalDateTime accountApplicationSyncedAt;
        private String avatarUrl;
        private String status;
        private String job;
        private Integer annualIncome;
        private String riskLevel;
    }

    // ===== 管理端：回傳客戶資訊 =====
    @Getter
    @Setter
    public static class CustomerResponse {
        private String customerId;
        private String cif;
        private String idNumber;
        private String name;
        private LocalDate birthday;
        private String gender;
        private String email;
        private String phone;
        private String address;
        private String status;
        private String avatarUrl;
        private String username;
        private String nationality;
        private String registeredAddress;
        private String currentAddress;
        private String occupation;
        private String employer;
        private Integer estimatedMonthlyTx;
        private String accountPurpose;
        private String fundSource;
        private String taxResidency;
        private Boolean isPep;
        private String idFrontUrl;
        private String idBackUrl;
        private String secondIdUrl;
        private Long latestAccountApplicationId;
        private String latestAccountApplicationNo;
        private String latestAppliedAccountType;
        private String latestAppliedCurrency;
        private String latestAccountApplicationStatus;
        private String latestAccountApplicationRiskFlag;
        private LocalDateTime latestAccountApplicationReviewedAt;
        private String latestAccountApplicationReviewedBy;
        private String latestAccountApplicationRejectReason;
        private String createdAccountNumber;
        private LocalDateTime accountApplicationSyncedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String job;
        private Integer annualIncome;
        private String riskLevel;
    }

    // ===== 帳戶模組：開戶申請資料同步 =====
    @Getter
    @Setter
    public static class AccountApplicationProfileSyncRequest {
        private String name;
        private String idNumber;
        private LocalDate birthday;
        private String gender;
        private String email;
        private String address;
        private String nationality;
        private String phone;
        private String registeredAddress;
        private String currentAddress;
        private String occupation;
        private String employer;
        private Integer estimatedMonthlyTx;
        private String accountPurpose;
        private String fundSource;
        private String taxResidency;
        private Boolean isPep;
        private String idFrontUrl;
        private String idBackUrl;
        private String secondIdUrl;
        private Long latestAccountApplicationId;
        private String latestAccountApplicationNo;
        private String latestAppliedAccountType;
        private String latestAppliedCurrency;
        private String latestAccountApplicationStatus;
        private String latestAccountApplicationRiskFlag;
        private LocalDateTime latestAccountApplicationReviewedAt;
        private String latestAccountApplicationReviewedBy;
        private String latestAccountApplicationRejectReason;
        private String createdAccountNumber;
    }

    // ===== 客戶端：註冊 =====
    @Getter
    @Setter
    public static class RegisterRequest {
        private String name;
        private LocalDate birthday;
        private String gender;
        private String idNumber;
        private String username;
        private String password;
        private String address;
        private String phone;
        private String email;
    }

    // ===== 客戶端：登入 =====
    @Getter
    @Setter
    public static class LoginRequest {
        private String idNumber;
        private String username;
        private String password;
    }

    // ===== 客戶端：登入回應 =====
    @Getter
    @Setter
    public static class LoginResponse {
        private String token;
        private String customerId;
        private String cif;
        private String name;
        private String username;
        private String avatarUrl;
        private String role;
    }

    // ===== 客戶端：修改個人資料 =====
    @Getter
    @Setter
    public static class ProfileUpdateRequest {
        private String phone;
        private String email;
        private String address;
    }

    // ===== 客戶端：密碼重設請求 =====
    @Getter
    @Setter
    public static class PasswordResetEmailRequest {
        private String idNumber;
        private LocalDate birthday;
        private String email;
    }

    // ===== 客戶端：密碼重設執行 =====
    @Getter
    @Setter
    public static class PasswordResetRequest {
        private String token;
        private String newPassword;
    }
}
