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

        // 1. 透過 Spring Security 的 AuthenticationManager 做驗證
        //    會呼叫 CustomUserDetailsService.loadUserByUsername()
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // 2. 把驗證結果放進 SecurityContext → 存入 Session
        //    後續 API 呼叫時 Spring Security 會自動從 Session 讀取，就不會 403
        SecurityContextHolder.getContext().setAuthentication(authentication);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());

        // 3. 呼叫業務邏輯取得完整的員工資訊回傳給前端
        AuthDto.AuthEmpResponse response = authEmpService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ===== 登出 =====
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpSession session) {
        SecurityContextHolder.clearContext();
        session.invalidate();
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ===== 查詢員工（支援模糊搜尋）=====
    // GET /api/auth/employees          → 全部
    // GET /api/auth/employees?keyword=林 → 姓名含「林」的
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

    // ===== 一鍵帶入測試資料：僅 CISO =====
    @PostMapping("/employees/seed")
    public ResponseEntity<ApiResponse<String>> seedEmployees() {
        authEmpService.seedTestData();
        return ResponseEntity.ok(ApiResponse.success("已成功帶入測試資料"));
    }
}
