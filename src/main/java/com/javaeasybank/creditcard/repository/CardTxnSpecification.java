package com.javaeasybank.creditcard.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


import org.springframework.data.jpa.domain.Specification;

import com.javaeasybank.creditcard.entity.CardTransaction;
import com.javaeasybank.creditcard.enums.TxnType;

import jakarta.persistence.criteria.Predicate;


public class CardTxnSpecification {

    public static Specification<CardTransaction> search(
            String keyword,
            TxnType txnType,
            LocalDate startDate,
            LocalDate endDate
    ) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // 關鍵字
            if (keyword != null && !keyword.trim().isEmpty()) {

                String likeKeyword = "%" + keyword.trim() + "%";

                predicates.add(cb.or(
                        cb.like(
                                root.get("card")
                                        .get("customer")
                                        .get("name"),
                                likeKeyword),

                        cb.like(
                                root.get("merchant")
                                        .get("merchantName"),
                                likeKeyword),

                        cb.like(
                                root.get("description"),
                                likeKeyword)
                ));
            }

            // 類型
            if (txnType != null) {
                predicates.add(
                        cb.equal(root.get("txnType"), txnType));
            }

            // 開始日期
            if (startDate != null) {

                LocalDateTime start = startDate.atStartOfDay();

                predicates.add(
                        cb.greaterThanOrEqualTo(
                                root.get("txnDate"),
                                start));
            }

            // 結束日期
            if (endDate != null) {

                LocalDateTime end = endDate
                        .plusDays(1)
                        .atStartOfDay();

                predicates.add(
                        cb.lessThan(
                                root.get("txnDate"),
                                end));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

