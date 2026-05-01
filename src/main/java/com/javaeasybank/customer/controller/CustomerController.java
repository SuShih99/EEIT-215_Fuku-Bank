package com.javaeasybank.customer.controller;

import com.javaeasybank.common.dto.response.ApiResponse;
import com.javaeasybank.customer.dto.CustomerDto;
import com.javaeasybank.customer.service.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    // ===== 查詢客戶（支援模糊搜尋）=====
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerDto.CustomerResponse>>> getCustomers(
            @RequestParam(required = false) String keyword) {
        List<CustomerDto.CustomerResponse> result;
        if (keyword != null && !keyword.isEmpty()) {
            result = customerService.searchByName(keyword);
        } else {
            result = customerService.getAllCustomers();
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ===== 新增客戶 =====
    @PreAuthorize("hasAnyRole('CSVO', 'CSDM', 'CFSO')")
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerDto.CustomerResponse>> createCustomer(
            @RequestBody CustomerDto.CustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(customerService.createCustomer(request)));
    }

    // ===== 修改客戶聯絡資訊 =====
    @PreAuthorize("hasAnyRole('CSVO', 'CSDM')")
    @PutMapping("/{customerId}")
    public ResponseEntity<ApiResponse<CustomerDto.CustomerResponse>> updateCustomer(
            @PathVariable String customerId,
            @RequestBody CustomerDto.CustomerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(customerService.updateCustomer(customerId, request)));
    }

    // ===== 註銷客戶（軟刪除）：建議改用 PutMapping 體現狀態更新 =====
    @PreAuthorize("hasRole('CSDM')")
    @PutMapping("/{customerId}/deactivate") // 💡 改為 PutMapping，符合狀態變更邏輯
    public ResponseEntity<ApiResponse<Void>> deactivateCustomer(@PathVariable String customerId) {
        customerService.deactivateCustomer(customerId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ===== 一鍵帶入測試資料 =====
    // 💡 seed 會執行 DELETE 清空動作，建議移除 CSVO (專員) 權限，僅限經理與資安長
    @PreAuthorize("hasAnyRole('CSDM', 'CISO')") 
    @PostMapping("/seed")
    public ResponseEntity<ApiResponse<String>> seedCustomers() {
        customerService.seedTestData();
        return ResponseEntity.ok(ApiResponse.success("已成功重置並帶入測試資料"));
    }
}