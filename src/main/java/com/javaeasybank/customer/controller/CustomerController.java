package com.javaeasybank.customer.controller;

import com.javaeasybank.auth.entity.AuthActionLog;
import com.javaeasybank.auth.entity.AuthEmp;
import com.javaeasybank.auth.repository.AuthEmpRepository;
import com.javaeasybank.auth.service.AuthActionLogService;
import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.service.FileStorageService;
import com.javaeasybank.customer.repository.CustomerRespository;
import com.javaeasybank.customer.service.CustomerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final FileStorageService fileStorageService;
    private final AuthEmpRepository authEmpRepository;
    private final AuthActionLogService actionLogService;
    private static final String ID_CARDS_DIR = "id-cards";

    public CustomerController(CustomerService customerService,
                              FileStorageService fileStorageService,
                              AuthEmpRepository authEmpRepository,
                              AuthActionLogService actionLogService) {
        this.customerService = customerService;
        this.fileStorageService = fileStorageService;
        this.authEmpRepository = authEmpRepository;
        this.actionLogService = actionLogService;
    }
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerRespository.CustomerResponse>>> getCustomers(
            @RequestParam(required = false) String keyword) {
        List<CustomerRespository.CustomerResponse> result;
        if (keyword != null && !keyword.isEmpty()) {
            result = customerService.searchByName(keyword);
        } else {
            result = customerService.getAllCustomers();
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerRespository.CustomerResponse>> createCustomer(
            @RequestBody CustomerRespository.CustomerRequest request) {
        try {
            CustomerRespository.CustomerResponse response = customerService.createCustomer(request);
            recordCurrentEmpLog(
                    "CREATE_CUSTOMER",
                    response.getCustomerId(),
                    "新增客戶：" + response.getName() + "（" + response.getCustomerId() + "）"
            );
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException ex) {
            recordCurrentEmpLog("FAILED_CREATE_CUSTOMER", request.getIdNumber(), "新增客戶失敗：" + ex.getMessage());
            throw ex;
        }
    }
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{customerId}")
    public ResponseEntity<ApiResponse<CustomerRespository.CustomerResponse>> updateCustomer(
            @PathVariable String customerId,
            @RequestBody CustomerRespository.CustomerRequest request) {
        try {
            CustomerRespository.CustomerResponse response = customerService.updateCustomer(customerId, request);
            recordCurrentEmpLog(
                    "UPDATE_CUSTOMER",
                    customerId,
                    "修改客戶資料：" + response.getName() + "（" + customerId + "）"
            );
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (RuntimeException ex) {
            recordCurrentEmpLog("FAILED_UPDATE_CUSTOMER", customerId, "修改客戶資料失敗：" + ex.getMessage());
            throw ex;
        }
    }
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadCustomerDocument(
            @RequestParam("file") MultipartFile file) {
        try {
            String url = fileStorageService.store(file, ID_CARDS_DIR);
            recordCurrentEmpLog(
                    "UPLOAD_CUSTOMER_DOCUMENT",
                    ID_CARDS_DIR,
                    "上傳客戶證件圖片：" + url
            );
            return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
        } catch (RuntimeException ex) {
            recordCurrentEmpLog("FAILED_UPLOAD_CUSTOMER_DOCUMENT", ID_CARDS_DIR, "上傳客戶證件圖片失敗：" + ex.getMessage());
            throw ex;
        }
    }
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{customerId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateCustomer(@PathVariable String customerId) {
        requireCustomerStatusOperator(
                "DEACTIVATE_CUSTOMER",
                customerId,
                "嘗試停用客戶帳號"
        );
        try {
            customerService.deactivateCustomer(customerId);
            recordCurrentEmpLog(
                    "DEACTIVATE_CUSTOMER",
                    customerId,
                    "停用客戶帳號：" + customerId
            );
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (RuntimeException ex) {
            recordCurrentEmpLog("FAILED_DEACTIVATE_CUSTOMER", customerId, "停用客戶帳號失敗：" + ex.getMessage());
            throw ex;
        }
    }
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{customerId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateCustomer(@PathVariable String customerId) {
        try {
            customerService.activateCustomer(customerId);
            recordCurrentEmpLog(
                    "ACTIVATE_CUSTOMER",
                    customerId,
                    "重新啟用客戶帳號：" + customerId
            );
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (RuntimeException ex) {
            recordCurrentEmpLog("FAILED_ACTIVATE_CUSTOMER", customerId, "重新啟用客戶帳號失敗：" + ex.getMessage());
            throw ex;
        }
    }
    @PreAuthorize("isAuthenticated()") 
    @PostMapping("/seed")
    public ResponseEntity<ApiResponse<String>> seedCustomers() {
        try {
            customerService.seedTestData();
            recordCurrentEmpLog(
                    "SEED_CUSTOMER_DATA",
                    "CUSTOMER",
                    "一鍵重置並帶入客戶測試資料"
            );
            return ResponseEntity.ok(ApiResponse.success("已成功重置並帶入資料"));
        } catch (RuntimeException ex) {
            recordCurrentEmpLog("FAILED_SEED_CUSTOMER_DATA", "CUSTOMER", "一鍵重置並帶入客戶測試資料失敗：" + ex.getMessage());
            throw ex;
        }
    }

    private void recordCurrentEmpLog(String action, String target, String details) {
        AuthEmp emp = resolveCurrentEmp(SecurityContextHolder.getContext().getAuthentication()).orElse(null);
        AuthActionLog logEntry = new AuthActionLog();
        logEntry.setEmpId(emp != null ? emp.getEmpId() : "UNKNOWN");
        logEntry.setEmpName(emp != null ? emp.getEmpName() : "未知員工");
        logEntry.setAction(action);
        logEntry.setTarget(target != null ? target : "-");
        logEntry.setDetails(details != null ? details : "-");
        logEntry.setIpAddress("127.0.0.1");
        actionLogService.saveLog(logEntry);
    }

    private void requireCustomerStatusOperator(String action, String target, String details) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean allowed = authentication != null
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> {
                    String role = authority.getAuthority();
                    return "ROLE_CFDM".equals(role)
                            || "ROLE_CISO".equals(role)
                            || "ROLE_ISSA".equals(role);
                });
        if (allowed) {
            return;
        }

        AuthEmp emp = resolveCurrentEmp(authentication).orElse(null);
        AuthActionLog logEntry = new AuthActionLog();
        logEntry.setEmpId(emp != null ? emp.getEmpId() : "UNKNOWN");
        logEntry.setEmpName(emp != null ? emp.getEmpName() : "未知員工");
        logEntry.setAction("UNAUTHORIZED_" + action);
        logEntry.setTarget(target);
        logEntry.setDetails(details + "，系統已阻擋此越權操作");
        logEntry.setIpAddress("127.0.0.1");
        actionLogService.saveLog(logEntry);

        throw new AccessDeniedException("權限不足，您的角色無法執行此操作");
    }

    private Optional<AuthEmp> resolveCurrentEmp(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return Optional.empty();
        }
        return authEmpRepository.findByEmail(authentication.getName());
    }
}
