package com.javaeasybank.account.repository;

import com.javaeasybank.account.entity.AccountApplication;
import com.javaeasybank.account.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AccountApplicationRepository extends JpaRepository<AccountApplication, Long> {

    /** 查詢指定客戶的所有申請 */
    List<AccountApplication> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    /** 查詢指定狀態的申請（分頁，管理端用） */
    Page<AccountApplication> findByStatusOrderByCreatedAtDesc(ApplicationStatus status, Pageable pageable);

    /** 全部申請（分頁，管理端用） */
    Page<AccountApplication> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /** 同源防堵：指定時間範圍內，同 IP 的申請次數 */
    long countByApplyIpAndCreatedAtAfter(String applyIp, LocalDateTime after);

    /** 同源防堵：指定時間範圍內，同手機號碼的申請次數 */
    long countByPhoneAndCreatedAtAfter(String phone, LocalDateTime after);

    /** 檢查客戶是否有 PENDING 狀態的申請 */
    boolean existsByCustomerIdAndStatus(String customerId, ApplicationStatus status);
    // 統計用 (Dashboard)
    @org.springframework.data.jpa.repository.Query("SELECT a.status, COUNT(a) FROM AccountApplication a GROUP BY a.status")
    List<Object[]> countByStatus();

    @org.springframework.data.jpa.repository.Query("SELECT a.riskFlag, COUNT(a) FROM AccountApplication a GROUP BY a.riskFlag")
    List<Object[]> countByRiskFlag();

    @org.springframework.data.jpa.repository.Query("SELECT a.accountType, COUNT(a) FROM AccountApplication a GROUP BY a.accountType")
    List<Object[]> countByAccountType();
}
