package com.javaeasybank.auth.service;

import com.javaeasybank.auth.dto.AuthDto;
import java.util.List;

public interface AuthEmpService {

    // === 登入登出 ===
    AuthDto.AuthEmpResponse login(AuthDto.LoginRequest request);

    // === 員工 CRUD ===
    List<AuthDto.AuthEmpResponse> getAllEmps();
    AuthDto.AuthEmpResponse createEmp(AuthDto.AuthEmpRequest request);
    AuthDto.AuthEmpResponse updateEmp(String empId, AuthDto.AuthEmpRequest request);
    void suspendEmp(String empId);

    // === 給其他模組對接用 ===
    AuthDto.AuthEmpResponse getEmpByEmpId(String empId);
    AuthDto.AuthEmpResponse getEmpByEmail(String email);
    String getRoleCodeByEmpId(String empId);
}
