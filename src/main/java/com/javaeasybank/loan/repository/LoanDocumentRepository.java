package com.javaeasybank.loan.repository;

import com.javaeasybank.loan.entity.LoanDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

// 補件文件資料存取介面
@Repository
public interface LoanDocumentRepository extends JpaRepository<LoanDocument, String> {

    // 查詢指定申請的所有已上傳文件，依上傳時間升序排列（最早上傳的在前）
    List<LoanDocument> findByApplicationIdOrderByUploadTimeAsc(String applicationId);

    @Query("""
            SELECT d
            FROM LoanDocument d
            WHERE d.applicationId = :applicationId
              AND (
                    (
                      :documentBatchType = 'INITIAL'
                      AND :documentBatchNo = 0
                      AND (d.documentBatchType IS NULL OR d.documentBatchType = 'INITIAL')
                      AND (d.documentBatchNo IS NULL OR d.documentBatchNo = 0)
                    )
                    OR
                    (
                      (:documentBatchType <> 'INITIAL' OR :documentBatchNo <> 0)
                      AND d.documentBatchType = :documentBatchType
                      AND d.documentBatchNo = :documentBatchNo
                    )
                  )
            ORDER BY d.uploadTime ASC
            """)
    List<LoanDocument> findByApplicationIdAndDocumentBatchTypeAndDocumentBatchNoOrderByUploadTimeAsc(
            @Param("applicationId") String applicationId,
            @Param("documentBatchType") String documentBatchType,
            @Param("documentBatchNo") Integer documentBatchNo);

    // 計算指定申請目前已上傳的文件總數量
    long countByApplicationId(String applicationId);

    @Query("""
            SELECT COUNT(d)
            FROM LoanDocument d
            WHERE d.applicationId = :applicationId
              AND (
                    (
                      :documentBatchType = 'INITIAL'
                      AND :documentBatchNo = 0
                      AND (d.documentBatchType IS NULL OR d.documentBatchType = 'INITIAL')
                      AND (d.documentBatchNo IS NULL OR d.documentBatchNo = 0)
                    )
                    OR
                    (
                      (:documentBatchType <> 'INITIAL' OR :documentBatchNo <> 0)
                      AND d.documentBatchType = :documentBatchType
                      AND d.documentBatchNo = :documentBatchNo
                    )
                  )
            """)
    long countByApplicationIdAndDocumentBatchTypeAndDocumentBatchNo(
            @Param("applicationId") String applicationId,
            @Param("documentBatchType") String documentBatchType,
            @Param("documentBatchNo") Integer documentBatchNo);
}
