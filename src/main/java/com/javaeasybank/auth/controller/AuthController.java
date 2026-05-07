package com.javaeasybank.auth.controller;

import com.javaeasybank.auth.dto.AuthDto;
import com.javaeasybank.auth.service.AuthEmpService;
import com.javaeasybank.common.dto.response.ApiResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthEmpService authEmpService;
    private final AuthenticationManager authenticationManager;

    public AuthController(AuthEmpService authEmpService,
                          AuthenticationManager authenticationManager) {
        this.authEmpService = authEmpService;
        this.authenticationManager = authenticationManager;
    }

    // ===== 登入（所有人都能打）=====
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.AuthEmpResponse>> login(
            @RequestBody AuthDto.LoginRequest request,
            HttpSession session) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        AuthDto.AuthEmpResponse response = authEmpService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ===== 確認登入狀態（給前端路由守衛用）=====
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Void>> me() {
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ===== 登出 =====
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpSession session) {
        SecurityContextHolder.clearContext();
        session.invalidate();
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ===== 查詢員工（支援模糊搜尋）=====
    @PreAuthorize("hasAnyRole('CISO', 'ISSA')")
    @GetMapping("/employees")
    public ResponseEntity<ApiResponse<List<AuthDto.AuthEmpResponse>>> getEmps(
            @RequestParam(required = false) String keyword) {
        List<AuthDto.AuthEmpResponse> result;
        if (keyword != null && !keyword.isEmpty()) {
            result = authEmpService.searchEmpsByName(keyword);
        } else {
            result = authEmpService.getAllEmps();
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ===== 新增員工 =====
    @PreAuthorize("hasAnyRole('CISO', 'ISSA')")
    @PostMapping("/employees")
    public ResponseEntity<ApiResponse<AuthDto.AuthEmpResponse>> createEmp(
            @RequestBody AuthDto.AuthEmpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authEmpService.createEmp(request)));
    }

    // ===== 修改員工 =====
    @PreAuthorize("hasAnyRole('CISO', 'ISSA')")
    @PutMapping("/employees/{empId}")
    public ResponseEntity<ApiResponse<AuthDto.AuthEmpResponse>> updateEmp(
            @PathVariable String empId,
            @RequestBody AuthDto.AuthEmpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authEmpService.updateEmp(empId, request)));
    }

    // ===== 停用員工（軟刪除） =====
    @PreAuthorize("hasAnyRole('CISO', 'ISSA')")
    @DeleteMapping("/employees/{empId}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspendEmp(@PathVariable String empId) {
        authEmpService.suspendEmp(empId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ===== 一鍵帶入資料 =====
    @PreAuthorize("hasAnyRole('CISO', 'ISSA')")
    @PostMapping("/employees/seed")
    public ResponseEntity<ApiResponse<String>> seedEmployees() {
        authEmpService.seedTestData();
        return ResponseEntity.ok(ApiResponse.success("已成功帶入資料"));
    }
}