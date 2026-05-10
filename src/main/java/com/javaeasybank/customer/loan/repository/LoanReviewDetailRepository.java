package com.javaeasybank.customer.loan.repository;

import com.javaeasybank.customer.loan.entity.LoanReviewDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanReviewDetailRepository extends JpaRepository<LoanReviewDetail, String> {

    // 單筆，用 Optional 因為可能還沒填過
    Optional<LoanReviewDetail> findByApplicationId(String applicationId);
}