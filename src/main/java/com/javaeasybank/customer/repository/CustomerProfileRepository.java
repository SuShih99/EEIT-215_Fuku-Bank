package com.javaeasybank.customer.repository;

import com.javaeasybank.customer.entity.CustomerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, String> {
    Optional<CustomerProfile> findByIdNumber(String idNumber);
    Optional<CustomerProfile> findByCif(String cif);
    Optional<CustomerProfile> findByEmail(String email);
    Optional<CustomerProfile> findByPhone(String phone);
    List<CustomerProfile> findByNameContaining(String keyword);
}
