package com.javaeasybank.loan.repository;

import com.javaeasybank.loan.entity.LoanDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// 補件文件資料存取介面
@Repository
public interface LoanDocumentRepository extends JpaRepository<LoanDocument, String> {

    // 查詢指定申請的所有已上傳文件，依上傳時間升序排列（最早上傳的在前）
    List<LoanDocument> findByApplicationIdOrderByUploadTimeAsc(String applicationId);

    List<LoanDocument> findByApplicationIdAndDocumentBatchTypeAndDocumentBatchNoOrderByUploadTimeAsc(
            String applicationId,
            String documentBatchType,
            Integer documentBatchNo);

    // 計算指定申請目前已上傳的文件總數量
    long countByApplicationId(String applicationId);

    long countByApplicationIdAndDocumentBatchTypeAndDocumentBatchNo(
            String applicationId,
            String documentBatchType,
            Integer documentBatchNo);
}
