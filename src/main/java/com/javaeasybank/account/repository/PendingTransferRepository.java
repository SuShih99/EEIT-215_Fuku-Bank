package com.javaeasybank.account.repository;

import com.javaeasybank.account.entity.PendingTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingTransferRepository extends JpaRepository<PendingTransfer, Long> {

    Optional<PendingTransfer> findByReferenceId(String referenceId);
}
