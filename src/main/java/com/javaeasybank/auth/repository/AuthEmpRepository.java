package com.javaeasybank.auth.repository;

import com.javaeasybank.auth.entity.AuthEmp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthEmpRepository extends JpaRepository<AuthEmp, String> {
    Optional<AuthEmp> findByEmail(String email);
    List<AuthEmp> findByEmpNameContaining(String keyword);
}