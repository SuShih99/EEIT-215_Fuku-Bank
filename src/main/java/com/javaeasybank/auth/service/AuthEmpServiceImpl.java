package com.javaeasybank.auth.service;

import com.javaeasybank.auth.dto.AuthDto;
import com.javaeasybank.auth.entity.AuthEmp;
import com.javaeasybank.auth.entity.AuthRole;
import com.javaeasybank.auth.repository.AuthEmpRepository;
import com.javaeasybank.auth.repository.AuthRoleRepository;
import com.javaeasybank.common.exception.BusinessException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthEmpServiceImpl implements AuthEmpService {

    private final AuthEmpRepository authEmpRepository;
    private final AuthRoleRepository authRoleRepository;

    public AuthEmpServiceImpl(AuthEmpRepository authEmpRepository,
                              AuthRoleRepository authRoleRepository) {
        this.authEmpRepository = authEmpRepository;
        this.authRoleRepository = authRoleRepository;
    }

    // ===========================
    // 登入（所有 ACTIVE 員工都能登入）
    // ===========================
    @Override
    public AuthDto.AuthEmpResponse login(AuthDto.LoginRequest request) {
        AuthEmp emp = authEmpRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("帳號或密碼錯誤"));

        if (!"ACTIVE".equals(emp.getStatus())) {
            throw new BusinessException("此帳號已被停用或鎖定");
        }

        // 測試階段預設密碼 123456
        if (!"123456".equals(request.getPassword()) && !emp.getPasswordHash().contains("dummyhash")) {
            throw new BusinessException("帳號或密碼錯誤");
        }

        emp.setLastLoginDate(LocalDateTime.now());
        authEmpRepository.save(emp);

        return convertToResponse(emp);
    }

    // ===========================
    // 員工 CRUD（由 Controller 的 @PreAuthorize 控制權限）
    // ===========================
    @Override
    public List<AuthDto.AuthEmpResponse> getAllEmps() {
        return authEmpRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AuthDto.AuthEmpResponse createEmp(AuthDto.AuthEmpRequest request) {
        if (authEmpRepository.existsById(request.getEmpId())) {
            throw new BusinessException("員工編號已存在");
        }
        if (authEmpRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new BusinessException("信箱已被使用");
        }

        AuthEmp emp = new AuthEmp();
        BeanUtils.copyProperties(request, emp);
        emp.setPasswordHash("$2a$10$dummyhash...");
        emp.setStatus("ACTIVE");
        if (emp.getPermissionExpire() == null) {
            emp.setPermissionExpire(LocalDateTime.now().plusYears(1));
        }

        AuthEmp saved = authEmpRepository.save(emp);
        return convertToResponse(saved);
    }

    @Override
    public AuthDto.AuthEmpResponse updateEmp(String empId, AuthDto.AuthEmpRequest request) {
        AuthEmp emp = authEmpRepository.findById(empId)
                .orElseThrow(() -> new BusinessException("查無此員工"));

        emp.setEmpName(request.getEmpName());
        emp.setEmail(request.getEmail());
        emp.setDeptId(request.getDeptId());
        emp.setRoleId(request.getRoleId());

        if (request.getContractEndDate() != null) {
            emp.setContractEndDate(request.getContractEndDate());
        }

        AuthEmp saved = authEmpRepository.save(emp);
        return convertToResponse(saved);
    }

    @Override
    public void suspendEmp(String empId) {
        AuthEmp emp = authEmpRepository.findById(empId)
                .orElseThrow(() -> new BusinessException("查無此員工"));
        emp.setStatus("SUSPENDED");
        authEmpRepository.save(emp);
    }

    // ===========================
    // 給其他模組對接用的方法
    // ===========================
    @Override
    public AuthDto.AuthEmpResponse getEmpByEmpId(String empId) {
        AuthEmp emp = authEmpRepository.findById(empId)
                .orElseThrow(() -> new BusinessException("查無此員工"));
        return convertToResponse(emp);
    }

    @Override
    public AuthDto.AuthEmpResponse getEmpByEmail(String email) {
        AuthEmp emp = authEmpRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("查無此員工"));
        return convertToResponse(emp);
    }

    @Override
    public String getRoleCodeByEmpId(String empId) {
        AuthEmp emp = authEmpRepository.findById(empId)
                .orElseThrow(() -> new BusinessException("查無此員工"));
        AuthRole role = authRoleRepository.findById(emp.getRoleId())
                .orElseThrow(() -> new BusinessException("角色設定異常"));
        return role.getRoleCode();
    }

    // ===========================
    // 私有轉換方法
    // ===========================
    private AuthDto.AuthEmpResponse convertToResponse(AuthEmp emp) {
        AuthDto.AuthEmpResponse res = new AuthDto.AuthEmpResponse();
        BeanUtils.copyProperties(emp, res);

        authRoleRepository.findById(emp.getRoleId()).ifPresent(role -> {
            res.setRoleCode(role.getRoleCode());
            res.setPermScope(role.getPermScope());
        });

        return res;
    }
}
