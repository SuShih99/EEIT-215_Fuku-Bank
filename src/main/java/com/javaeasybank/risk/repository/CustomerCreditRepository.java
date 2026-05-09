package com.javaeasybank.risk.repository;

import com.javaeasybank.risk.core.enums.RiskLevel;
import com.javaeasybank.risk.entity.CustomerCreditInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerCreditRepository extends JpaRepository<CustomerCreditInfo, String> {
    List<CustomerCreditInfo> findByRiskLevel(RiskLevel riskLevel);

    List<CustomerCreditInfo> findByFinalScoreBetween(int min, int max);
}
