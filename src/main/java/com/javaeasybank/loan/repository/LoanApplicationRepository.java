package com.javaeasybank.loan.repository;

/**
 * 程式說明：
 * - 貸款申請資料存取介面。
 * - 提供依申請狀態、客戶編號與更新時間排序查詢申請資料的方法。
 */

import com.javaeasybank.loan.entity.LoanApplication;
import com.javaeasybank.loan.enums.LoanApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 貸款申請資料存取介面。
 * 提供依狀態、客戶與最近更新時間排序的查詢，支援前台進度與後台案件列表。
 */
public interface LoanApplicationRepository extends JpaRepository<LoanApplication, String> {

    // 依申請狀態篩選，按建立時間降序排列（最新的在前）
    List<LoanApplication> findByApplicationStatusOrderByCreateTimeDesc(LoanApplicationStatus status);

    // 依客戶 ID 查詢該客戶的所有申請，按建立時間降序排列
    List<LoanApplication> findByCustomerIdOrderByCreateTimeDesc(String customerId);

    // 查詢所有 updateTime 不為 null 的申請，依 updateTime 降序排列
    List<LoanApplication> findByUpdateTimeIsNotNullOrderByUpdateTimeDesc();
}
