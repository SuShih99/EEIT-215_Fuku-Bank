package com.javaeasybank.creditcard.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.javaeasybank.creditcard.entity.CardBill;
import com.javaeasybank.creditcard.enums.BillStatus;

import jakarta.persistence.criteria.Predicate;

public class CardBillSpecification {

    public static Specification<CardBill> search(String customerName,
                                                 String billingMonth,
                                                 BillStatus billStatus) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (customerName != null && !customerName.isBlank()) {
                predicates.add(cb.like(
                    root.get("cardAccount").get("customer").get("name"),
                    "%" + customerName + "%"
                ));
            }

            if (billingMonth != null && !billingMonth.isBlank()) {
                predicates.add(cb.like(
                    root.get("billingMonth"),
                    "%" + billingMonth + "%"
                ));
            }

            if (billStatus != null) {
                predicates.add(cb.equal(
                    root.get("billStatus"),
                    billStatus
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
