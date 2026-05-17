package com.javaeasybank.customer.service;

import com.javaeasybank.customer.repository.CustomerRespository;
import java.util.List;

public interface CustomerService {

    // === 你自己的 CRUD ===
    List<CustomerRespository.CustomerResponse> getAllCustomers();
    List<CustomerRespository.CustomerResponse> searchByName(String keyword);
    CustomerRespository.CustomerResponse createCustomer(CustomerRespository.CustomerRequest request);
    CustomerRespository.CustomerResponse updateCustomer(String customerId, CustomerRespository.CustomerRequest request);
    void deactivateCustomer(String customerId);
    void activateCustomer(String customerId);

    // === 一鍵帶入測試資料 ===
    void seedTestData();

    // === 給其他模組對接用 ===
    CustomerRespository.CustomerResponse findByCustomerId(String customerId);
    CustomerRespository.CustomerResponse findByIdNumber(String idNumber);
    CustomerRespository.CustomerResponse findByCif(String cif);
    CustomerRespository.CustomerResponse syncAccountApplicationProfile(
            String customerId,
            CustomerRespository.AccountApplicationProfileSyncRequest request);
    String findEmailByCustomerId(String customerId);
}
