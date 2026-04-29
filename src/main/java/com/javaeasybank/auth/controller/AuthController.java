package com.javaeasybank.auth.controller;

import com.javaeasybank.auth.dto.AuthDto;
import com.javaeasybank.auth.service.AuthEmpService;
import com.javaeasybank.common.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthEmpService authEmpService;

    public AuthController(AuthEmpService authEmpService) {
        this.authEmpService = authEmpService;
    }

    // ===== 登入（所有人都能打，SecurityConfig 設定為 permitAll）=====
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.AuthEmpResponse>> login(
            @RequestBody AuthDto.LoginRequest request) {
        AuthDto.AuthEmpResponse response = authEmpService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ===== 登出 =====
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ===== 查詢所有員工：CISO + ISSA 可查 =====
    @PreAuthorize("hasAnyRole('CISO', 'ISSA')")
    @GetMapping("/employees")
    public ResponseEntity<ApiResponse<List<AuthDto.AuthEmpResponse>>> getAllEmps() {
        return ResponseEntity.ok(ApiResponse.success(authEmpService.getAllEmps()));
    }

    // ===== 新增員工：僅 CISO =====
    @PreAuthorize("hasRole('CISO')")
    @PostMapping("/employees")
    public ResponseEntity<ApiResponse<AuthDto.AuthEmpResponse>> createEmp(
            @RequestBody AuthDto.AuthEmpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authEmpService.createEmp(request)));
    }

    // ===== 修改員工：僅 CISO =====
    @PreAuthorize("hasRole('CISO')")
    @PutMapping("/employees/{empId}")
    public ResponseEntity<ApiResponse<AuthDto.AuthEmpResponse>> updateEmp(
            @PathVariable String empId,
            @RequestBody AuthDto.AuthEmpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authEmpService.updateEmp(empId, request)));
    }

    // ===== 停用員工（軟刪除）：僅 CISO =====
    @PreAuthorize("hasRole('CISO')")
    @DeleteMapping("/employees/{empId}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspendEmp(@PathVariable String empId) {
        authEmpService.suspendEmp(empId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
