package com.javaeasybank.customer.repository;

import com.javaeasybank.customer.entity.CustomerAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerAuthRepository extends JpaRepository<CustomerAuth, String> {
    Optional<CustomerAuth> findByUsername(String username);
    Optional<CustomerAuth> findByCustomerId(String customerId);
    Optional<CustomerAuth> findByResetToken(String resetToken);
    Optional<CustomerAuth> findByVerificationToken(String verificationToken);
    boolean existsByUsername(String username);
}
