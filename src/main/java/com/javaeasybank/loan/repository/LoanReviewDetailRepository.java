package com.javaeasybank.loan.repository;

/**
 * 程式說明：
 * - 貸款審核明細資料存取介面。
 * - 提供依申請編號取得審核結果與核准條件的查詢方法。
 */

import com.javaeasybank.loan.entity.LoanReviewDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 行員二次填單（審核詳情）資料存取介面
/**
 * 行員二次填單資料存取介面。
 * 每件貸款申請對應一筆審核詳情，用於送風控、核准條件與契約書產生。
 */
public interface LoanReviewDetailRepository extends JpaRepository<LoanReviewDetail, String> {

    // 依貸款申請 ID 查詢對應的審核詳情（單筆）
    Optional<LoanReviewDetail> findByApplicationId(String applicationId);
}
