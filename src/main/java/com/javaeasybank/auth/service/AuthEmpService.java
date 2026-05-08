package com.javaeasybank.auth.service;

import com.javaeasybank.auth.dto.AuthDto;
import java.util.List;

public interface AuthEmpService {

    // === 登入 / 登出 ===
    AuthDto.AuthEmpResponse login(AuthDto.LoginRequest request, String ipAddress);
    void logout(String email, String ipAddress);

    // === 員工 CRUD ===
    List<AuthDto.AuthEmpResponse> getAllEmps();
    List<AuthDto.AuthEmpResponse> searchEmpsByName(String keyword);
    AuthDto.AuthEmpResponse createEmp(AuthDto.AuthEmpRequest request);
    AuthDto.AuthEmpResponse updateEmp(String empId, AuthDto.AuthEmpRequest request);
    void suspendEmp(String empId);
    void resumeEmp(String empId);

    // === 一鍵帶入測試資料 ===
    void seedTestData();

    // === 給其他模組對接用 ===
    AuthDto.AuthEmpResponse getEmpByEmpId(String empId);
    AuthDto.AuthEmpResponse getEmpByEmail(String email);
    String getRoleCodeByEmpId(String empId);
}
