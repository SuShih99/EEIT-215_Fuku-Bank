package com.javaeasybank.creditcard.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.javaeasybank.creditcard.entity.CardAccount;

public interface CardAccountRepository extends JpaRepository<CardAccount, Integer> {

    Optional<CardAccount> findByCustomer_CustomerId(String customerId);

}
