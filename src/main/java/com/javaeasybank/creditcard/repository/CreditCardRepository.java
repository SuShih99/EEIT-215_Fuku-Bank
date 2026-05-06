package com.javaeasybank.creditcard.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.javaeasybank.creditcard.entity.CreditCard;

public interface CreditCardRepository extends JpaRepository<CreditCard, Integer> {

	boolean existsByCardType_CardTypeId(Integer cardTypeId);

    boolean existsByCardNumber(String cardNumber);

    List<CreditCard> findByCustomerCustomerId(Integer customerId);
}
