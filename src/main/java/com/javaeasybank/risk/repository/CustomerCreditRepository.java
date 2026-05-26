package com.javaeasybank.risk.repository;

import com.javaeasybank.risk.enums.RiskLevel;
import com.javaeasybank.risk.entity.CustomerCreditInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CustomerCreditRepository extends JpaRepository<CustomerCreditInfo, String>, JpaSpecificationExecutor<CustomerCreditInfo> {

    Page<CustomerCreditInfo> findByCustomerIdIn(List<String> ids, Pageable pageable);


}
