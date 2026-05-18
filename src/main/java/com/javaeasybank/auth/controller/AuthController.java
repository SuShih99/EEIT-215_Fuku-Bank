package com.javaeasybank.auth.controller;

import com.javaeasybank.auth.entity.AuthActionLog;
import com.javaeasybank.auth.entity.AuthEmp;
import com.javaeasybank.auth.repository.AuthEmpRepository;
import com.javaeasybank.auth.service.AuthActionLogService;
import com.javaeasybank.auth.repository.AuthRespository;
import com.javaeasybank.auth.service.AuthEmpService;
import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final int ADMIN_SESSION_TIMEOUT_SECONDS = 8 * 60 * 60;

    private final AuthEmpService authEmpService;
    private final AuthenticationManager authenticationManager;
    private final AuthEmpRepository authEmpRepository;
    private final AuthActionLogService actionLogService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthEmpService authEmpService,
                          AuthenticationManager authenticationManager,
                          AuthEmpRepository authEmpRepository,
                          AuthActionLogService actionLogService,
                          JwtUtil jwtUtil) {
        this.authEmpService = authEmpService;
        this.authenticationManager = authenticationManager;
        this.authEmpRepository = authEmpRepository;
        this.actionLogService = actionLogService;
        this.jwtUtil = jwtUtil;
    }

    // ===== 登入（所有人都能打）=====
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthRespository.AuthEmpResponse>> login(
            @RequestBody AuthRespository.LoginRequest request,
            HttpSession session,
            HttpServletRequest httpRequest) {

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (AuthenticationException e) {
            recordFailedLogin(request.getEmail(), getClientIp(httpRequest));
            throw e;
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
        session.setMaxInactiveInterval(ADMIN_SESSION_TIMEOUT_SECONDS);

        // 擷取來源 IP
        String ipAddress = getClientIp(httpRequest);
        AuthRespository.AuthEmpResponse response = authEmpService.login(request, ipAddress);
        response.setToken(jwtUtil.generateToken(response.getEmail(), response.getRoleCode(), response.getEmpId()));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ===== 確認登入狀態（給前端路由守衛用）=====
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthRespository.AuthEmpResponse>> me(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("登入已失效，請重新登入"));
        }

        String email = authentication.getName();
        if (authEmpRepository.findByEmail(email).isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("登入資訊已失效，請重新登入"));
        }

        AuthRespository.AuthEmpResponse employee = authEmpService.getEmpByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(employee));
    }

    // ===== 登出 =====
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpSession session,
                                                     HttpServletRequest httpRequest) {
        // 取得當前登入者 email，記錄登出日誌
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal()))
                ? auth.getName() : null;

        String ipAddress = getClientIp(httpRequest);
        authEmpService.logout(email, ipAddress);

        SecurityContextHolder.clearContext();
        session.invalidate();
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ===== 員工人數（所有已登入角色皆可存取）=====
    @GetMapping("/employees/count")
    public ResponseEntity<ApiResponse<Long>> getEmpCount() {
        return ResponseEntity.ok(ApiResponse.success(authEmpService.getEmpCount()));
    }

    // ===== 查詢員工（支援模糊搜尋）=====
    @PreAuthorize("hasAnyRole('CISO', 'ISSA')")
    @GetMapping("/employees")
    public ResponseEntity<ApiResponse<List<AuthRespository.AuthEmpResponse>>> getEmps(
            @RequestParam(required = false) String keyword) {
        List<AuthRespository.AuthEmpResponse> result;
        if (keyword != null && !keyword.isEmpty()) {
            result = authEmpService.searchEmpsByNameExcludingCurrent(keyword);
        } else {
            result = authEmpService.getAllEmpsExcludingCurrent();
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ===== 新增員工 =====
    @PreAuthorize("hasAnyRole('CISO', 'ISSA')")
    @PostMapping("/employees")
    public ResponseEntity<ApiResponse<AuthRespository.AuthEmpResponse>> createEmp(
            @RequestBody AuthRespository.AuthEmpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authEmpService.createEmp(request)));
    }

    // ===== 修改員工 =====
    @PreAuthorize("hasAnyRole('CISO', 'ISSA')")
    @PutMapping("/employees/{empId}")
    public ResponseEntity<ApiResponse<AuthRespository.AuthEmpResponse>> updateEmp(
            @PathVariable String empId,
            @RequestBody AuthRespository.AuthEmpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authEmpService.updateEmp(empId, request)));
    }

    // ===== 停用員工（軟刪除） =====
    @PreAuthorize("hasAnyRole('CISO', 'ISSA')")
    @DeleteMapping("/employees/{empId}/suspend")
    public ResponseEntity<ApiResponse<Void>> suspendEmp(@PathVariable String empId) {
        authEmpService.suspendEmp(empId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ===== 重新啟用員工 =====
    @PreAuthorize("hasAnyRole('CISO', 'ISSA')")
    @PutMapping("/employees/{empId}/resume")
    public ResponseEntity<ApiResponse<Void>> resumeEmp(@PathVariable String empId) {
        authEmpService.resumeEmp(empId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ===== 一鍵帶入資料 =====
    @PostMapping("/employees/seed")
    public ResponseEntity<ApiResponse<String>> seedEmployees() {
        authEmpService.seedTestData();
        return ResponseEntity.ok(ApiResponse.success("已成功帶入資料"));
    }

    // ===== 工具方法：取得真實 IP =====
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private void recordFailedLogin(String email, String ipAddress) {
        Optional<AuthEmp> optEmp = authEmpRepository.findByEmail(email);
        AuthActionLog log = new AuthActionLog();
        log.setEmpId(optEmp.map(AuthEmp::getEmpId).orElse("UNKNOWN"));
        log.setEmpName(optEmp.map(AuthEmp::getEmpName).orElse("未知員工"));
        log.setAction("FAILED_LOGIN");
        log.setTarget(optEmp.map(AuthEmp::getEmpId).orElse(email));
        log.setDetails("員工登入失敗或帳號狀態異常，系統已記錄此事件");
        log.setIpAddress(ipAddress);
        actionLogService.saveLog(log);
    }
}
