package com.javaeasybank.account.repository;

import com.javaeasybank.account.entity.FavoriteAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteAccountRepository extends JpaRepository<FavoriteAccount, Long> {

    List<FavoriteAccount> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    boolean existsByCustomerIdAndBankCodeAndAccountNumber(String customerId, String bankCode, String accountNumber);

    boolean existsByCustomerIdAndBankCodeAndAccountNumberAndIdNot(
            String customerId,
            String bankCode,
            String accountNumber,
            Long id
    );

    Optional<FavoriteAccount> findByIdAndCustomerId(Long id, String customerId);
}
