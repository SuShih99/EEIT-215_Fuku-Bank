package com.javaeasybank.auth.service;

import com.javaeasybank.auth.repository.AuthRespository;
import java.util.List;

public interface AuthEmpService {
    AuthRespository.AuthEmpResponse login(AuthRespository.LoginRequest request, String ipAddress);
    void logout(String email, String ipAddress);
    Long getEmpCount();
    List<AuthRespository.AuthEmpResponse> getAllEmps();
    List<AuthRespository.AuthEmpResponse> getAllEmpsExcludingCurrent();
    List<AuthRespository.AuthEmpResponse> searchEmpsByName(String keyword);
    List<AuthRespository.AuthEmpResponse> searchEmpsByNameExcludingCurrent(String keyword);
    AuthRespository.AuthEmpResponse createEmp(AuthRespository.AuthEmpRequest request);
    AuthRespository.AuthEmpResponse updateEmp(String empId, AuthRespository.AuthEmpRequest request);
    void suspendEmp(String empId);
    void resumeEmp(String empId);
    void seedTestData();
    AuthRespository.AuthEmpResponse getEmpByEmpId(String empId);
    AuthRespository.AuthEmpResponse getEmpByEmail(String email);
    String getRoleCodeByEmpId(String empId);
}
