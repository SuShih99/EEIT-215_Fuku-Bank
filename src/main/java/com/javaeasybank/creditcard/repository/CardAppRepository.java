package com.javaeasybank.creditcard.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.javaeasybank.creditcard.entity.CardApplication;
import com.javaeasybank.creditcard.enums.CardApplicationStatus;

public interface CardAppRepository extends JpaRepository<CardApplication, Integer> {

    @Query("""
            SELECT c FROM CardApplication c
            WHERE (
                :keyword IS NULL
                OR c.remark LIKE %:keyword%
                OR c.customerProfile.customerId LIKE %:keyword%
                OR c.customerProfile.name LIKE %:keyword%
            )
            AND (
                :status IS NULL
                OR c.status = :status
            )
            """)
    Page<CardApplication> search(
            Pageable pageable,
            @Param("keyword") String keyword,
            @Param("status") CardApplicationStatus status);
}
