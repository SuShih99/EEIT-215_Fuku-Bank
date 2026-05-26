package com.javaeasybank.auth.repository;

import com.javaeasybank.auth.entity.AuthRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthRoleRepository extends JpaRepository<AuthRole, String> {
}
