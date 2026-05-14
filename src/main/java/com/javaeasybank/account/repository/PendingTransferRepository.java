package com.javaeasybank.account.repository;

import com.javaeasybank.account.entity.PendingTransfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingTransferRepository extends JpaRepository<PendingTransfer, Integer> {
}
