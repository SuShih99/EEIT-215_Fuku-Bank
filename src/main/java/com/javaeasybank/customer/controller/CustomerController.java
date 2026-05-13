package com.javaeasybank.customer.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.common.service.FileStorageService;
import com.javaeasybank.customer.repository.CustomerRespository;
import com.javaeasybank.customer.service.CustomerService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final FileStorageService fileStorageService;
    private static final String ID_CARDS_DIR = "id-cards";

    public CustomerController(CustomerService customerService, FileStorageService fileStorageService) {
        this.customerService = customerService;
        this.fileStorageService = fileStorageService;
    }

    // ===== 查詢客戶（支援模糊搜尋）=====
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

    // ===== 新增客戶 =====
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerRespository.CustomerResponse>> createCustomer(
            @RequestBody CustomerRespository.CustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(customerService.createCustomer(request)));
    }

    // ===== 修改客戶聯絡資訊 =====
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{customerId}")
    public ResponseEntity<ApiResponse<CustomerRespository.CustomerResponse>> updateCustomer(
            @PathVariable String customerId,
            @RequestBody CustomerRespository.CustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(customerService.updateCustomer(customerId, request)));
    }

    // ===== 上傳客戶證件圖片 =====
    @PreAuthorize("isAuthenticated()")
    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadCustomerDocument(
            @RequestParam("file") MultipartFile file) {
        String url = fileStorageService.store(file, ID_CARDS_DIR);
        return ResponseEntity.ok(ApiResponse.success(Map.of("url", url)));
    }

    // ===== 註銷客戶（軟刪除） =====
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{customerId}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateCustomer(@PathVariable String customerId) {
        customerService.deactivateCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ===== 啟用客戶 =====
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{customerId}/activate")
    public ResponseEntity<ApiResponse<Void>> activateCustomer(@PathVariable String customerId) {
        customerService.activateCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ===== 一鍵帶入資料 =====
    @PreAuthorize("isAuthenticated()") 
    @PostMapping("/seed")
    public ResponseEntity<ApiResponse<String>> seedCustomers() {
        customerService.seedTestData();
        return ResponseEntity.ok(ApiResponse.success("已成功重置並帶入資料"));
    }
}
