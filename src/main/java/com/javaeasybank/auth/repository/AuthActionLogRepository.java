package com.javaeasybank.auth.repository;

import com.javaeasybank.auth.entity.AuthActionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthActionLogRepository extends JpaRepository<AuthActionLog, Long> {
    List<AuthActionLog> findAllByOrderByActionTimeDesc();
}
