package com.javaeasybank.auth.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

public class AuthDto {

    @Getter
    @Setter
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Getter
    @Setter
    public static class AuthEmpRequest {
        private String empId;
        private String empName;
        private String deptId;
        private String roleId;
        private String email;
        private String password;
        private String status;
        private LocalDateTime contractEndDate;
        private LocalDateTime permissionExpire;
    }

    @Getter
    @Setter
    public static class AuthEmpResponse {
        private String empId;
        private String empName;
        private String deptId;
        private String roleId;
        private String roleCode;    // 新增：角色代碼 (如 CISO)
        private String permScope;   // 新增：權限範圍 (如 SYS)
        private String email;
        private String status;
        private LocalDateTime contractEndDate;
        private LocalDateTime permissionExpire;
        private LocalDateTime lastLoginDate;
    }
}
