package com.javaeasybank.customer.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.util.JwtUtil;
import com.javaeasybank.customer.repository.CustomerRespository;
import com.javaeasybank.customer.service.CustomerAuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 客戶認證 Controller：
 * 處理客戶的註冊、登入、個人資料維護、大頭照上傳、密碼重設。
 *
 * 路徑前綴：/api/customer/auth
 */
@RestController
@RequestMapping("/api/customer/auth")
public class CustomerAuthController {

    private final CustomerAuthService customerAuthService;
    private final JwtUtil jwtUtil;

    public CustomerAuthController(CustomerAuthService customerAuthService, JwtUtil jwtUtil) {
        this.customerAuthService = customerAuthService;
        this.jwtUtil = jwtUtil;
    }
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<CustomerRespository.LoginResponse>> register(
            @RequestBody CustomerRespository.RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(customerAuthService.register(request)));
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<CustomerRespository.LoginResponse>> login(
            HttpServletRequest httpRequest,
            @RequestBody CustomerRespository.LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(customerAuthService.login(
                request,
                resolveClientIp(httpRequest),
                resolveUserAgent(httpRequest)
        )));
    }
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam("token") String token) {
        customerAuthService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("電子郵件驗證成功，請重新登入"));
    }
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CustomerRespository.CustomerResponse>> getProfile(
            HttpServletRequest request) {
        String customerId = extractCustomerId(request);
        return ResponseEntity.ok(ApiResponse.success(customerAuthService.getProfile(customerId)));
    }
    @PreAuthorize("hasRole('CUSTOMER')")
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<CustomerRespository.CustomerResponse>> updateProfile(
            HttpServletRequest httpRequest,
            @RequestBody CustomerRespository.ProfileUpdateRequest request) {
        String customerId = extractCustomerId(httpRequest);
        return ResponseEntity.ok(ApiResponse.success(customerAuthService.updateProfile(customerId, request)));
    }
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CustomerRespository.CustomerResponse>> uploadAvatar(
            HttpServletRequest httpRequest,
            @RequestParam("file") MultipartFile file) throws IOException {

        String customerId = extractCustomerId(httpRequest);

        // 1. 使用絕對路徑定位到專案目錄下的 uploads/avatars
        String projectPath = System.getProperty("user.dir");
        Path uploadDir = Paths.get(projectPath, "uploads", "avatars");
        
        // 2. 確保資料夾存在，如果不存在就建立它
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // 3. 產生唯一的檔名
        String fileName = customerId + "_" + UUID.randomUUID().toString().substring(0, 8)
                + getExtension(file.getOriginalFilename());

        // 4. 強制使用絕對路徑進行檔案轉移 (解決 FileNotFound 問題)
        Path filePath = uploadDir.resolve(fileName);
        file.transferTo(filePath.toAbsolutePath().toFile());

        // 5. 回傳給前端可以存取的虛擬路徑
        String avatarUrl = "/uploads/avatars/" + fileName;
        return ResponseEntity.ok(ApiResponse.success(customerAuthService.uploadAvatar(customerId, avatarUrl)));

    }
    @PostMapping("/request-reset")
    public ResponseEntity<ApiResponse<String>> requestPasswordReset(
            @RequestBody CustomerRespository.PasswordResetEmailRequest request) {
        customerAuthService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success("密碼重設連結已發送至您的電子信箱"));
    }
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(
            @RequestBody CustomerRespository.PasswordResetRequest request) {
        customerAuthService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("密碼已成功重設，請使用新密碼登入"));
    }
    @PatchMapping("/{customerId}/unlock")
    public ResponseEntity<Void> unlockCustomer(@PathVariable String customerId) {
        customerAuthService.unlockCustomer(customerId);
        return ResponseEntity.ok().build();
    }
    @PostMapping("/seed")
    public ResponseEntity<ApiResponse<String>> seedAuthData() {
        customerAuthService.seedAuthTestData();
        return ResponseEntity.ok(ApiResponse.success("客戶認證測試資料已帶入"));
    }
    // 私有方法

    /**
     * 從 Request Header 的 JWT Token 中取得 customerId。
     * JwtAuthenticationFilter 已驗證 Token 合法性，
     * 這裡直接解析 claims 取得 customerId。
     */
    private String extractCustomerId(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.getCustomerIdFromToken(token);
        }
        throw new com.javaeasybank.common.exception.BusinessException("無法取得客戶身分資訊");
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".jpg";
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private String resolveUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent == null || userAgent.isBlank() ? "未知裝置" : userAgent;
    }
}
