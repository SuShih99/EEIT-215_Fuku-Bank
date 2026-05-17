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
('LA202605100900001001', 'Q8M4T7K2', 'PERSONAL', 120000, 12, 0.041300, '070000000027', 'PENDING_CONTACT', '2026-05-10 09:00:00', NULL, NULL, NULL, NULL),
('LA202605111030001002', 'Q8M4T7K2', 'CAR',      380000, 36, 0.036000, '070000000027', 'IN_CONTACT',      '2026-05-11 10:30:00', 'REACHED',  '2026-05-11 14:20:00', NULL, '2026-05-11 14:20:00'),
('LA202605121430001003', 'Q8M4T7K2', 'PERSONAL', 250000, 24, 0.042000, '070000000027', 'PENDING_REVIEW',  '2026-05-12 14:30:00', 'CONFIRMED','2026-05-12 16:10:00', '2026-05-12 16:30:00', '2026-05-12 16:30:00'),
('LA202605131100001004', 'Q8M4T7K2', 'BUSINESS', 600000, 48, 0.038500, '070000000027', 'APPROVED',        '2026-05-13 11:00:00', 'CONFIRMED','2026-05-13 13:40:00', '2026-05-13 14:00:00', '2026-05-13 17:20:00'),
('LA202605141600001005', 'Q8M4T7K2', 'PERSONAL',  80000, 12, 0.041300, '070000000027', 'CANCELLED',       '2026-05-14 16:00:00', 'DECLINED', '2026-05-14 16:35:00', NULL, '2026-05-14 16:35:00');

INSERT INTO LOAN_CONTACT_LOG (
    log_id,
    application_id,
    emp_id,
    contact_status,
    contact_channel,
    contact_time,
    note
) VALUES
('CL202605111420001002', 'LA202605111030001002', 'EMP001', 'REACHED',   'PHONE', '2026-05-11 14:20:00', N'Customer reached, waiting for documents.'),
('CL202605121610001003', 'LA202605121430001003', 'EMP001', 'CONFIRMED', 'PHONE', '2026-05-12 16:10:00', N'Customer confirmed loan review details.'),
('CL202605131340001004', 'LA202605131100001004', 'EMP002', 'CONFIRMED', 'PHONE', '2026-05-13 13:40:00', N'Customer confirmed approved terms.'),
('CL202605141635001005', 'LA202605141600001005', 'EMP001', 'DECLINED',  'PHONE', '2026-05-14 16:35:00', N'Customer abandoned this loan application.');

INSERT INTO LOAN_REVIEW_DETAIL (
    review_id,
    application_id,
    confirmed_amount,
    confirmed_period,
    confirmed_rate,
    collateral_note,
    emp_id,
    review_time,
    review_status,
    submitted_time,
    review_note
) VALUES
('RD202605121630001003', 'LA202605121430001003', 250000.00, 24, 0.042000, NULL, 'EMP001', '2026-05-12 16:25:00', 'SUBMITTED', '2026-05-12 16:30:00', N'Submitted to risk review.'),
('RD202605131400001004', 'LA202605131100001004', 600000.00, 48, 0.038500, NULL, 'EMP002', '2026-05-13 13:55:00', 'SUBMITTED', '2026-05-13 14:00:00', N'Approved mock application.');

PRINT N'loan_mockdata.sql completed: seeded 5 loan applications for customer Q8M4T7K2.';
GO
