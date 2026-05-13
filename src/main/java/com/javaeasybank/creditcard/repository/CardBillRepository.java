package com.javaeasybank.creditcard.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.javaeasybank.creditcard.entity.CardBill;

public interface CardBillRepository extends JpaRepository<CardBill, Integer> {

    Page<CardBill> findByCardCustomerCustomerId(String customerId, Pageable pageable);

    boolean existsByBillingMonth(String billingMonth);
}
