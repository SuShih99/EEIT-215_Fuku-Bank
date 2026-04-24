package com.javaeasybank.creditcard.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.javaeasybank.creditcard.entity.Merchant;

public interface MerchantRepository extends JpaRepository<Merchant, Integer> {

}
