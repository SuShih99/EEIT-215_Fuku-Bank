-- ============================================================
-- Loan Module Mock Data (重新生成版)
-- 依賴：customer_profile 表必須先有資料
-- ============================================================

-- ===== 清除舊資料 =====
DELETE FROM LOAN_REPAYMENT;
DELETE FROM LOAN_ACCOUNT;
DELETE FROM LOAN_CONTACT_LOG;
DELETE FROM LOAN_REVIEW_DETAIL;
DELETE FROM LOAN_APPLICATION;

INSERT INTO LOAN_APPLICATION (
    application_id,
    customer_id,
    apply_type,
    apply_amount,
    apply_period,
    rate,
    disbursement_account,
    application_status,
    create_time,
    latest_contact_status,
    latest_contact_time,
    documents_submitted_at,
    update_time
) VALUES
('LA202605100900001001', 'Q8M4T7K2', 'PERSONAL', 120000, 12, 0.040000, '070000000027', 'PENDING_CONTACT', '2026-05-10 09:00:00', NULL, NULL, NULL, NULL),
('LA202605111030001002', 'Q8M4T7K2', 'CAR',      380000, 36, 0.030000, '070000000027', 'PENDING_CONTACT', '2026-05-11 10:30:00', NULL, NULL, NULL, NULL),
('LA202605121430001003', 'Q8M4T7K2', 'PERSONAL', 250000, 24, 0.042000, '070000000027', 'PENDING_CONTACT', '2026-05-12 14:30:00', NULL, NULL, NULL, NULL),
('LA202605131100001004', 'Q8M4T7K2', 'BUSINESS', 600000, 48, 0.028000, '070000000027', 'PENDING_CONTACT', '2026-05-13 11:00:00', NULL, NULL, NULL, NULL),
('LA202605141400001005', 'Q8M4T7K2', 'PERSONAL', 180000, 36, 0.045000, '070000000027', 'PENDING_CONTACT', '2026-05-14 14:00:00', NULL, NULL, NULL, NULL),
('LA202605141600001006', 'Q8M4T7K2', 'PERSONAL',  80000, 12, 0.040000, '070000000027', 'CANCELLED',       '2026-05-14 16:00:00', 'DECLINED', '2026-05-14 16:35:00', NULL, '2026-05-14 16:35:00');

INSERT INTO LOAN_CONTACT_LOG (
    log_id,
    application_id,
    emp_id,
    contact_status,
    contact_channel,
    contact_time,
    note
) VALUES
('CL202605141635001006', 'LA202605141600001006', 'EMP001', 'DECLINED', 'PHONE', '2026-05-14 16:35:00', N'Customer abandoned this loan application.');

PRINT N'loan_mockdata.sql completed: seeded 6 loan applications for customer Q8M4T7K2.';
GO
