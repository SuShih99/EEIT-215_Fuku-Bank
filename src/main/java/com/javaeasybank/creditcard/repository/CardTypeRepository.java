package com.javaeasybank.creditcard.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.javaeasybank.creditcard.entity.CardType;

public interface CardTypeRepository extends JpaRepository<CardType, Integer> {

}
