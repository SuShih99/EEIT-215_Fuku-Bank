package com.javaeasybank.creditcard.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.javaeasybank.creditcard.entity.CardTransaction;

public interface CardTxnRepository extends JpaRepository<CardTransaction, Integer> {

    boolean existsByRefTxn_TxnId(Integer txnId);
}
