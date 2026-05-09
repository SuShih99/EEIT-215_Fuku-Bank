package com.javaeasybank.customer.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

public class CustomerDto {

    // ===== 管理端：新增/修改客戶 =====
    @Getter
    @Setter
    public static
    class CustomerRequest{
        private String customerId;
        private String idNumber;
        private String name;
        private LocalDate birthday;
        private String gender;
        private String email;
        private String phone;
        private String address;

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