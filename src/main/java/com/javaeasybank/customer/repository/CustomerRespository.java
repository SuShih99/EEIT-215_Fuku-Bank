package com.javaeasybank.customer.repository;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CustomerRespository {
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
        private Integer annualIncome;
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
    @Getter
    @Setter
    public static class LoginRequest {
        private String idNumber;
        private String username;
        private String password;
    }
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
    @Getter
    @Setter
    public static class ProfileUpdateRequest {
        private String phone;
        private String email;
        private String address;
        private String job;
        private String occupation;
        private String employer;
        private String fundSource;
        private Integer annualIncome;
    }
    @Getter
    @Setter
    public static class PasswordResetEmailRequest {
        private String idNumber;
        private LocalDate birthday;
        private String email;
    }
    @Getter
    @Setter
    public static class PasswordResetRequest {
        private String token;
        private String newPassword;
    }
}
