package com.javaeasybank.risk.repository;

import com.javaeasybank.risk.entity.RiskEventLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface RiskEventLogRepository extends JpaRepository<RiskEventLog,Long> {

    @Query("SELECT r FROM RiskEventLog r WHERE " +
            "(:eventType IS NULL OR r.eventType LIKE %:eventType%) AND " +
            "(:actionTaken IS NULL OR r.actionTaken = :actionTaken)")
    Page<RiskEventLog> search(String eventType, String actionTaken, Pageable pageable);

    List<RiskEventLog> findByCreatedAt(LocalDateTime createdAt);
}
