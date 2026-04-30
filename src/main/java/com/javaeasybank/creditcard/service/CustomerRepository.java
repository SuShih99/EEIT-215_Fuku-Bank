package com.javaeasybank.creditcard.service;

import org.springframework.data.jpa.repository.JpaRepository;

import com.javaeasybank.customer.entity.CustomerProfile;

public interface CustomerRepository extends JpaRepository<CustomerProfile, String> {

}
