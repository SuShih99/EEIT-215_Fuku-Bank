package com.javaeasybank.customer.service;

import com.javaeasybank.customer.repository.CustomerRespository;

/**
 * 客戶認證服務介面：
 * 處理客戶的註冊、登入、個人資料修改、大頭照上傳、密碼重設。
 */
public interface CustomerAuthService {
    CustomerRespository.LoginResponse register(CustomerRespository.RegisterRequest request);
    CustomerRespository.LoginResponse login(CustomerRespository.LoginRequest request, String ipAddress, String userAgent);
    void verifyEmail(String token);
    CustomerRespository.CustomerResponse getProfile(String customerId);
    CustomerRespository.CustomerResponse updateProfile(String customerId, CustomerRespository.ProfileUpdateRequest request);
    CustomerRespository.CustomerResponse uploadAvatar(String customerId, String avatarUrl);
    void requestPasswordReset(CustomerRespository.PasswordResetEmailRequest request);
    void resetPassword(CustomerRespository.PasswordResetRequest request);
    void seedAuthTestData();

    void unlockCustomer(String customerId);
}
