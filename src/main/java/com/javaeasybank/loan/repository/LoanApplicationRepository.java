package com.javaeasybank.loan.repository;

import com.javaeasybank.loan.entity.LoanApplication;
import com.javaeasybank.loan.enums.LoanApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 貸款申請資料存取介面。
 *
 * <p>繼承 {@code JpaRepository}，提供 {@code LoanApplication} 的 CRUD 操作，
 * 並定義行員後台列表頁所需的篩選與排序查詢。</p>
 */
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, String> {

    /**
     * 依申請狀態篩選，按建立時間降序排列（最新的在前）。
     * 用於行員後台依狀態分類瀏覽申請清單。
     *
     * @param status 申請狀態，參見 {@code LoanApplicationStatus}
     * @return 符合狀態的申請清單，無資料時回傳空清單
     */
    List<LoanApplication> findByApplicationStatusOrderByCreateTimeDesc(LoanApplicationStatus status);

    /**
     * 依客戶 ID 查詢該客戶的所有申請，按建立時間降序排列。
     * 用於客戶端查看個人申請紀錄。
     *
     * @param customerId 客戶內部識別碼
     * @return 該客戶的申請清單，無資料時回傳空清單
     */
    List<LoanApplication> findByCustomerIdOrderByCreateTimeDesc(String customerId);

    /**
     * 查詢所有 {@code updateTime} 不為 null 的申請，依 {@code updateTime} 降序排列。
     * 用於行員後台「最近有異動」的申請置頂排序，方便追蹤最新進度。
     *
     * @return 有更新紀錄的申請清單，無資料時回傳空清單
     */
    List<LoanApplication> findByUpdateTimeIsNotNullOrderByUpdateTimeDesc();
}
