package com.javaeasybank.customer.service;

import com.javaeasybank.customer.dto.CustomerDto;

/**
 * 客戶認證服務介面：
 * 處理客戶的註冊、登入、個人資料修改、大頭照上傳、密碼重設。
 */
public interface CustomerAuthService {

    // === 註冊 & 登入 ===
    CustomerDto.LoginResponse register(CustomerDto.RegisterRequest request);
    CustomerDto.LoginResponse login(CustomerDto.LoginRequest request);

    // === 個人資料 ===
    CustomerDto.CustomerResponse getProfile(String customerId);
    CustomerDto.CustomerResponse updateProfile(String customerId, CustomerDto.ProfileUpdateRequest request);
    CustomerDto.CustomerResponse uploadAvatar(String customerId, String avatarUrl);

    // === 密碼重設 ===
    void requestPasswordReset(String email);
    void resetPassword(CustomerDto.PasswordResetRequest request);

    // === 一鍵帶入客戶認證測試資料 ===
    void seedAuthTestData();
}
