package com.javaeasybank.loan.repository;

import com.javaeasybank.loan.entity.LoanDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 補件文件資料存取介面。
 *
 * <p>繼承 {@code JpaRepository}，提供 {@code LoanDocument} 的 CRUD 操作，
 * 並提供依申請查詢文件清單與上傳數量計算的方法。</p>
 */
@Repository
public interface LoanDocumentRepository extends JpaRepository<LoanDocument, String> {

    /**
     * 查詢指定申請的所有已上傳文件，依上傳時間升序排列（最早上傳的在前）。
     * 送審時由 {@code LoanDocumentService} 呼叫，取得文件清單一併傳遞給風控系統。
     *
     * @param applicationId 貸款申請識別碼
     * @return 該申請的文件清單，無文件時回傳空清單
     */
    List<LoanDocument> findByApplicationIdOrderByUploadTimeAsc(String applicationId);

    /**
     * 計算指定申請目前已上傳的文件總數量。
     * 用於 {@code LoanDocumentService} 執行文件上傳數量上限檢查（例如最多 10 份）。
     *
     * @param applicationId 貸款申請識別碼
     * @return 已上傳的文件數量
     */
    long countByApplicationId(String applicationId);
}
