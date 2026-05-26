package com.javaeasybank.customer.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

public class CustomerDto {
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
    }
    @Getter
    @Setter
    public static class PasswordResetEmailRequest {
        private String email;
    }
    @Getter
    @Setter
    public static class PasswordResetRequest {
        private String token;
        private String newPassword;
    }
}