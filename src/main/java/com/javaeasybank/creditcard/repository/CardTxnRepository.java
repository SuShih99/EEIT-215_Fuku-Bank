package com.javaeasybank.creditcard.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.javaeasybank.creditcard.entity.CardTransaction;
import com.javaeasybank.creditcard.enums.TxnType;

public interface CardTxnRepository
                extends JpaRepository<CardTransaction, Integer>, JpaSpecificationExecutor<CardTransaction> {

        boolean existsByRefTxn_TxnId(Integer txnId);

        boolean existsByCard_CardIdAndTxnType(Integer cardId, TxnType txnType);

        Page<CardTransaction> findByCard_Customer_CustomerId(String customerId, Pageable pageable);

        List<CardTransaction> findByCardCardId(Integer cardId);

        List<CardTransaction> findByCard_CardAccount_IdAndBillIsNull(Integer cardAccountId);

        Page<CardTransaction> findByCard_Customer_CustomerIdAndBillIsNull(String customerId, Pageable pageable);

        List<CardTransaction> findByBill_BillIdAndCard_CardIdAndCard_CardAccount_Customer_CustomerId(
                        Integer billId,
                        Integer cardId,
                        String customerId);

        List<CardTransaction> findByBill_BillIdAndCard_CardIdAndCard_CardAccount_Customer_CustomerIdOrderByTxnDateDesc(
                        Integer billId,
                        Integer cardId,
                        String customerId);

}
