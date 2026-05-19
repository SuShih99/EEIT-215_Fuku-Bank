package com.javaeasybank.loan.repository;

import com.javaeasybank.loan.entity.LoanContactLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 聯繫紀錄資料存取介面。
 *
 * <p>繼承 {@code JpaRepository}，提供 {@code LoanContactLog} 的 CRUD 操作。
 * 聯繫紀錄為唯寫（append-only）設計，行員只能新增，不修改歷史紀錄。</p>
 */
public interface LoanContactLogRepository extends JpaRepository<LoanContactLog, String> {

    /**
     * 查詢指定申請的所有聯繫紀錄，按聯繫時間降序排列（最新的在前）。
     * 用於行員詳情頁顯示完整的聯繫歷程。
     *
     * @param applicationId 貸款申請識別碼
     * @return 該申請的聯繫紀錄清單，無資料時回傳空清單
     */
    List<LoanContactLog> findByApplicationIdOrderByContactTimeDesc(String applicationId);
}
