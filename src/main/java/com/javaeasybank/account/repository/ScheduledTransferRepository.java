package com.javaeasybank.account.repository;

import com.javaeasybank.account.entity.ScheduledTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduledTransferRepository extends JpaRepository<ScheduledTransfer, Long> {

    List<ScheduledTransfer> findByCustomerIdOrderByScheduledDateDesc(String customerId);

    Optional<ScheduledTransfer> findByIdAndCustomerId(Long id, String customerId);

    List<ScheduledTransfer> findByStatusAndScheduledDateLessThanEqual(String status, LocalDate date);
}
