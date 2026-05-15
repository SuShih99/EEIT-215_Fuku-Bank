package com.javaeasybank.creditcard.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.javaeasybank.creditcard.entity.CardBill;
import com.javaeasybank.creditcard.enums.BillStatus;

public interface CardBillRepository extends JpaRepository<CardBill, Integer> {

    Page<CardBill> findByCardAccountCustomerCustomerId(String customerId, Pageable pageable);

    boolean existsByBillingMonth(String billingMonth);

    Optional<CardBill> findTopByCardAccountCustomerCustomerIdAndBillStatusInOrderByDueDateAsc(
            String customerId,
            List<BillStatus> statuses);
}
