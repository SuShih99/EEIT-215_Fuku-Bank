package com.javaeasybank.customer.service;

import com.javaeasybank.customer.dto.CustomerDto;
import java.util.List;

public interface CustomerService {

    // === 你自己的 CRUD ===
    List<CustomerDto.CustomerResponse> getAllCustomers();
    List<CustomerDto.CustomerResponse> searchByName(String keyword);
    CustomerDto.CustomerResponse createCustomer(CustomerDto.CustomerRequest request);
    CustomerDto.CustomerResponse updateCustomer(String customerId, CustomerDto.CustomerRequest request);
    void deactivateCustomer(String customerId);
    void activateCustomer(String customerId);

    // === 一鍵帶入測試資料 ===
    void seedTestData();

    // === 給其他模組對接用 ===
    CustomerDto.CustomerResponse findByCustomerId(String customerId);
    CustomerDto.CustomerResponse findByIdNumber(String idNumber);
    CustomerDto.CustomerResponse findByCif(String cif);
}
