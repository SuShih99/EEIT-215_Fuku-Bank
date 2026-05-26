package com.javaeasybank.risk.repository;

import com.javaeasybank.risk.entity.RiskEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RiskEventLogRepository extends JpaRepository<RiskEventLog, Long>, JpaSpecificationExecutor<RiskEventLog> {

}
