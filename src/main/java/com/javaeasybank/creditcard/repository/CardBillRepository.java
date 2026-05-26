package com.javaeasybank.creditcard.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.javaeasybank.creditcard.entity.CardBill;
import com.javaeasybank.creditcard.enums.BillStatus;

public interface CardBillRepository extends JpaRepository<CardBill, Integer>, JpaSpecificationExecutor<CardBill> {

        Page<CardBill> findByCardAccountCustomerCustomerId(String customerId, Pageable pageable);

        boolean existsByBillingMonth(String billingMonth);

        Optional<CardBill> findTopByCardAccountCustomerCustomerIdAndBillStatusInOrderByDueDateAsc(
                        String customerId,
                        List<BillStatus> statuses);

        Optional<CardBill> findByBillIdAndCardAccountCustomerCustomerId(
                        Integer billId,
                        String customerId);

        boolean existsByCardAccountIdAndBillingMonth(
                        Integer cardAccountId,
                        String billingMonth);
        //最近一期未繳狀態
        List<CardBill> findByCardAccountIdAndBillStatusInOrderByBillingMonthAsc(
                        Integer cardAccountId,
                        List<BillStatus> statuses);

}
