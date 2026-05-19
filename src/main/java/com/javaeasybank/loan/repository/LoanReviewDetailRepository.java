package com.javaeasybank.loan.repository;

import com.javaeasybank.loan.entity.LoanReviewDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 行員二次填單（審核詳情）資料存取介面。
 *
 * <p>繼承 {@code JpaRepository}，提供 {@code LoanReviewDetail} 的 CRUD 操作。
 * 每筆申請最多只會有一筆審核詳情（一對一關係），
 * 行員可多次覆寫同一筆紀錄（草稿 → 送審）。</p>
 */
public interface LoanReviewDetailRepository extends JpaRepository<LoanReviewDetail, String> {

    /**
     * 依貸款申請 ID 查詢對應的審核詳情（單筆）。
     * 回傳 {@code Optional} 是因為申請可能尚未進入行員填單階段，
     * 此時資料庫中不存在對應紀錄。
     *
     * @param applicationId 貸款申請識別碼
     * @return 對應的審核詳情，若行員尚未填寫則為 {@code Optional.empty()}
     */
    Optional<LoanReviewDetail> findByApplicationId(String applicationId);
}
