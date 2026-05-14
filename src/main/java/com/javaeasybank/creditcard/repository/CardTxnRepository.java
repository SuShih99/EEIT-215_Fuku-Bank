package com.javaeasybank.creditcard.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.javaeasybank.creditcard.entity.CardTransaction;

public interface CardTxnRepository extends JpaRepository<CardTransaction, Integer> {

    boolean existsByRefTxn_TxnId(Integer txnId);

    Page<CardTransaction> findByCard_Customer_CustomerId(String customerId, Pageable pageable);

    List<CardTransaction> findByCardCardId(Integer cardId);

    Page<CardTransaction> findByCard_Customer_CustomerIdAndBillIsNull(String customerId, Pageable pageable);
}
