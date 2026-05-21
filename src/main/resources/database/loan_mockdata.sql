-- ============================================================
-- Loan Module Demo Mock Data
-- Targets:
-- - Wang Daming: 4 new applications + 1 disbursed application + 1 abandoned application
-- - 45 more new applications
-- - 10 in-contact applications
-- - 10 under review applications
-- - 5 returned-for-supplement applications
-- - 15 approved applications
-- - 2 rejected applications
-- - 2 cancelled applications
-- - 11 disbursed applications with loan accounts
-- - Risk review queue and credit-visible demo rows stay aligned
-- ============================================================


DELETE FROM LOAN_REPAYMENT;
DELETE FROM LOAN_ACCOUNT;
DELETE FROM LOAN_DOCUMENT;
DELETE FROM LOAN_CONTACT_LOG;
DELETE FROM LOAN_REVIEW_DETAIL;
DELETE FROM LOAN_APPLICATION;
DELETE FROM REVIEW_TASK WHERE business_id LIKE 'LA202605%';
DELETE FROM RISK_EVENT_LOG WHERE business_id LIKE 'LA202605%';

DECLARE @customerPool TABLE (
    seq INT PRIMARY KEY,
    customer_id NVARCHAR(50) NOT NULL,
    checking_account_number VARCHAR(14) NOT NULL
);

INSERT INTO @customerPool (seq, customer_id, checking_account_number) VALUES
(1,  'A6R3M8J2', '070000000001'),
(2,  'B3N8T5P9', '070000000002'),
(3,  'B9P5N2W6', '070000000003'),
(4,  'C6T8R4J3', '070000000004'),
(5,  'C9W2M6R4', '070000000005'),
(6,  'D5Q9T2W7', '070000000006'),
(7,  'E2V7D9M5', '070000000007'),
(8,  'E5J7Q3D8', '070000000008'),
(9,  'F2P9V4K6', '070000000009'),
(10, 'F7V4C8N2', '070000000010'),
(11, 'G3K6P9M5', '070000000011'),
(12, 'G8A5C2N7', '070000000012'),
(13, 'H4D7R9M3', '070000000013'),
(14, 'H7C2P8D4', '070000000014'),
(15, 'J6K3W8Q5', '070000000015'),
(16, 'J8R2D5A7', '070000000016'),
(17, 'K2T8B4R7', '070000000017'),
(18, 'L4N9T6Q3', '070000000018'),
(19, 'L9M2T7A4', '070000000019'),
(20, 'M9A5D2Q8', '070000000020'),
(21, 'N5V8C3P6', '070000000022'),
(22, 'P4W7N6C3', '070000000023'),
(23, 'P7Q4J9D2', '070000000024'),
(24, 'Q2R6M8W5', '070000000025'),
(25, 'Q5H3K8A7', '070000000026'),
(26, 'R5N9W3A6', '070000000028'),
(27, 'R8J6N2C4', '070000000029'),
(28, 'S4C8N5V2', '070000000031'),
(29, 'S7A3K9P6', '070000000032'),
(30, 'T3M9P5W7', '070000000033'),
(31, 'T6D2P9M7', '070000000034'),
(32, 'T8H2K5V9', '070000000035'),
(33, 'U5D8M2R4', '070000000036'),
(34, 'U8H5Q3R6', '070000000037'),
(35, 'V3K7W4A9', '070000000038'),
(36, 'V6J3X9M5', '070000000039'),
(37, 'V7A4D8Q2', '070000000040'),
(38, 'W2K6T9N5', '070000000041'),
(39, 'W6M2C8D5', '070000000042'),
(40, 'W9F4T7N2', '070000000043'),
(41, 'X3J6Q8C5', '070000000044'),
(42, 'X8C3R7M4', '070000000045'),
(43, 'X9N5T3Q7', '070000000046'),
(44, 'Y8L2V5D9', '070000000048');

DECLARE @typeRules TABLE (
    seq INT PRIMARY KEY,
    apply_type NVARCHAR(50) NOT NULL,
    apply_period INT NOT NULL,
    rate DECIMAL(10, 6) NOT NULL
);

INSERT INTO @typeRules (seq, apply_type, apply_period, rate) VALUES
(1, 'PERSONAL', 12, 0.040000),
(2, 'CAR',      36, 0.025000),
(3, 'MOTOR',    24, 0.045000),
(4, 'STUDENT',  84, 0.015000),
(5, 'BUSINESS', 60, 0.020000),
(6, 'HOUSE',   120, 0.018000),
(7, 'LAND',    180, 0.028000);

DECLARE @apps TABLE (
    seq INT IDENTITY(1,1) PRIMARY KEY,
    application_id NVARCHAR(50) NOT NULL,
    customer_id NVARCHAR(50) NOT NULL,
    apply_type NVARCHAR(50) NOT NULL,
    apply_amount DECIMAL(18, 2) NOT NULL,
    apply_period INT NOT NULL,
    rate DECIMAL(10, 6) NOT NULL,
    disbursement_account NVARCHAR(14) NULL,
    application_status NVARCHAR(30) NOT NULL,
    create_time DATETIME2 NOT NULL,
    latest_contact_status NVARCHAR(30) NULL,
    latest_contact_time DATETIME2 NULL,
    documents_submitted_at DATETIME2 NULL,
    update_time DATETIME2 NULL,
    current_supplement_batch_no INT NOT NULL,
    admin_comment NVARCHAR(100) NULL,
    required_documents NVARCHAR(MAX) NULL,
    review_comment NVARCHAR(100) NULL
);

-- Wang Daming demo applications
INSERT INTO @apps (
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
    update_time,
    current_supplement_batch_no,
    admin_comment,
    required_documents,
    review_comment
) VALUES
('LA2026052001', 'Q8M4T7K2', 'PERSONAL', 120000.00, 12, 0.040000, '070000000001', 'PENDING_CONTACT', '2026-05-21 09:00:00', NULL, NULL, NULL, NULL, 0, NULL, NULL, 'NEW'),
('LA2026052002', 'Q8M4T7K2', 'CAR',      380000.00, 36, 0.025000, '070000000001', 'PENDING_CONTACT', '2026-05-21 10:00:00', NULL, NULL, NULL, NULL, 0, NULL, NULL, 'NEW'),
('LA2026052003', 'Q8M4T7K2', 'STUDENT',  220000.00, 84, 0.015000, '070000000001', 'PENDING_CONTACT', '2026-05-21 11:00:00', NULL, NULL, NULL, NULL, 0, NULL, NULL, 'NEW'),
('LA2026052004', 'Q8M4T7K2', 'BUSINESS', 600000.00, 60, 0.020000, '070000000001', 'PENDING_CONTACT', '2026-05-21 13:00:00', NULL, NULL, NULL, NULL, 0, NULL, NULL, 'NEW'),
('LA2026052005', 'Q8M4T7K2', 'HOUSE',   2500000.00, 120, 0.018000, '070000000001', 'DISBURSED', '2026-05-21 14:00:00', 'CONFIRMED', '2026-05-21 15:00:00', '2026-05-21 15:30:00', '2026-05-21 16:00:00', 0, 'HOUSE_COLLATERAL', 'DISBURSED', 'DISBURSED'),
('LA2026052006', 'Q8M4T7K2', 'PERSONAL',  80000.00, 24, 0.040000, '070000000001', 'CANCELLED',      '2026-05-16 16:00:00', 'DECLINED', '2026-05-16 16:30:00', NULL, '2026-05-16 16:30:00', 0, 'CUSTOMER_WITHDREW', NULL, 'CANCELLED');


;WITH nums AS (
    SELECT TOP (98) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS rn
    FROM sys.all_objects a
    CROSS JOIN sys.all_objects b
),
picked AS (
    SELECT
        n.rn,
        cp.customer_id,
        cp.checking_account_number,
        tr.apply_type,
        tr.apply_period,
        tr.rate,
        CASE tr.apply_type
            WHEN 'PERSONAL' THEN CAST(180000 + (n.rn * 2500) AS DECIMAL(18, 2))
            WHEN 'CAR'      THEN CAST(380000 + (n.rn * 4000) AS DECIMAL(18, 2))
            WHEN 'MOTOR'    THEN CAST(90000  + (n.rn * 1500) AS DECIMAL(18, 2))
            WHEN 'STUDENT'  THEN CAST(220000 + (n.rn * 2000) AS DECIMAL(18, 2))
            WHEN 'BUSINESS' THEN CAST(700000 + (n.rn * 10000) AS DECIMAL(18, 2))
            WHEN 'HOUSE'    THEN CAST(2500000 + (n.rn * 30000) AS DECIMAL(18, 2))
            ELSE CAST(3000000 + (n.rn * 40000) AS DECIMAL(18, 2))
        END AS apply_amount
    FROM nums n
    INNER JOIN @customerPool cp
        ON cp.seq = ((n.rn - 1) % 44) + 1
    INNER JOIN @typeRules tr
        ON tr.seq = ((n.rn - 1) % 7) + 1
),
planned AS (
    SELECT
        rn,
        customer_id,
        checking_account_number,
        apply_type,
        apply_amount,
        apply_period,
        rate,
        CASE
            WHEN rn <= 45 THEN 'PENDING_CONTACT'
            WHEN rn <= 55 THEN 'IN_CONTACT'
            WHEN rn <= 65 THEN 'PENDING_REVIEW'
            WHEN rn <= 70 THEN 'RETURNED'
            WHEN rn <= 85 THEN 'APPROVED'
            WHEN rn <= 87 THEN 'REJECTED'
            WHEN rn = 88 THEN 'CANCELLED'
            ELSE 'DISBURSED'
        END AS application_status,
        CASE
            WHEN rn <= 20 THEN DATEADD(MINUTE, rn * 11, CAST('2026-05-21 08:00:00' AS DATETIME2))
            WHEN rn BETWEEN 71 AND 85 THEN DATEADD(DAY, -(rn - 71), CAST('2026-05-18 10:00:00' AS DATETIME2))
            WHEN rn BETWEEN 89 AND 98 THEN DATEADD(DAY, -(rn - 89), CAST('2026-05-19 14:00:00' AS DATETIME2))
            ELSE DATEADD(DAY, -rn, CAST('2026-05-21 09:00:00' AS DATETIME2))
        END AS base_time
    FROM picked
)
INSERT INTO @apps (
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
    update_time,
    current_supplement_batch_no,
    admin_comment,
    required_documents,
    review_comment
)
SELECT
    CONCAT('LA20260521', RIGHT(CONCAT('000', CAST(rn AS VARCHAR(3))), 3)) AS application_id,
    customer_id,
    apply_type,
    apply_amount,
    apply_period,
    rate,
    checking_account_number AS disbursement_account,
    application_status,
    base_time AS create_time,
    CASE
        WHEN application_status = 'IN_CONTACT' THEN CASE WHEN rn % 2 = 0 THEN 'ATTEMPTED' ELSE 'REACHED' END
        WHEN application_status = 'CANCELLED' THEN 'DECLINED'
        WHEN application_status IN ('PENDING_REVIEW', 'RETURNED', 'APPROVED', 'REJECTED', 'DISBURSED') THEN 'CONFIRMED'
        ELSE NULL
    END AS latest_contact_status,
    CASE
        WHEN application_status = 'IN_CONTACT' THEN DATEADD(MINUTE, 30, base_time)
        WHEN application_status = 'CANCELLED' THEN DATEADD(MINUTE, 30, base_time)
        WHEN application_status IN ('PENDING_REVIEW', 'RETURNED', 'APPROVED', 'REJECTED', 'DISBURSED') THEN DATEADD(HOUR, 1, base_time)
        ELSE NULL
    END AS latest_contact_time,
    CASE
        WHEN application_status IN ('PENDING_REVIEW', 'RETURNED', 'APPROVED', 'REJECTED', 'DISBURSED') THEN DATEADD(HOUR, 2, base_time)
        ELSE NULL
    END AS documents_submitted_at,
    DATEADD(HOUR, 1, base_time) AS update_time,
    CASE WHEN application_status = 'RETURNED' THEN 1 ELSE 0 END AS current_supplement_batch_no,
    CASE
        WHEN application_status = 'IN_CONTACT' THEN 'IN_CONTACT'
        WHEN application_status = 'PENDING_REVIEW' THEN 'UNDER_REVIEW'
        WHEN application_status = 'RETURNED' THEN 'RETURN_FOR_SUPPLEMENT'
        WHEN application_status = 'APPROVED' THEN 'APPROVED'
        WHEN application_status = 'REJECTED' THEN 'REJECTED'
        WHEN application_status = 'CANCELLED' THEN 'CANCELLED'
        WHEN application_status = 'DISBURSED' THEN 'DISBURSED'
        ELSE NULL
    END AS admin_comment,
    CASE
        WHEN application_status IN ('PENDING_REVIEW', 'RETURNED', 'APPROVED', 'REJECTED', 'DISBURSED')
            THEN CASE WHEN application_status = 'RETURNED'
                THEN '["ID_CARD","INCOME_PROOF","SUPPLEMENT_NOTE"]'
                ELSE '["ID_CARD","INCOME_PROOF"]'
            END
        ELSE NULL
    END AS required_documents,
    CASE
        WHEN application_status = 'PENDING_CONTACT' THEN 'NEW'
        WHEN application_status = 'IN_CONTACT' THEN 'IN_CONTACT'
        WHEN application_status = 'PENDING_REVIEW' THEN 'UNDER_REVIEW'
        WHEN application_status = 'RETURNED' THEN 'RETURN_FOR_SUPPLEMENT'
        WHEN application_status = 'APPROVED' THEN 'APPROVED'
        WHEN application_status = 'REJECTED' THEN 'REJECTED'
        WHEN application_status = 'CANCELLED' THEN 'CANCELLED'
        WHEN application_status = 'DISBURSED' THEN 'DISBURSED'
    END AS review_comment
FROM planned;

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
    update_time,
    current_supplement_batch_no,
    required_documents,
    review_comment
)
SELECT
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
    update_time,
    current_supplement_batch_no,
    required_documents,
    review_comment
FROM @apps;

-- Contact logs for in-contact / returned / cancelled demo rows.
INSERT INTO LOAN_CONTACT_LOG (
    log_id,
    application_id,
    emp_id,
    contact_status,
    contact_channel,
    contact_time,
    note
)
SELECT
    CONCAT('CL', RIGHT(CONCAT('000000', CAST(ROW_NUMBER() OVER (ORDER BY a.seq) AS VARCHAR(6))), 6)) AS log_id,
    a.application_id,
    'EMP001',
    CASE
        WHEN a.application_status = 'IN_CONTACT' THEN CASE WHEN a.seq % 2 = 0 THEN 'ATTEMPTED' ELSE 'REACHED' END
        WHEN a.application_status = 'RETURNED' THEN 'CONFIRMED'
        WHEN a.application_status = 'CANCELLED' THEN 'DECLINED'
    END AS contact_status,
    CASE
        WHEN a.application_status = 'IN_CONTACT' THEN 'PHONE'
        WHEN a.application_status = 'RETURNED' THEN 'EMAIL'
        ELSE 'PHONE'
    END AS contact_channel,
    CASE
        WHEN a.application_status = 'IN_CONTACT' THEN DATEADD(MINUTE, 30, a.create_time)
        WHEN a.application_status = 'RETURNED' THEN DATEADD(HOUR, 1, a.create_time)
        ELSE DATEADD(MINUTE, 15, a.create_time)
    END AS contact_time,
    CASE
        WHEN a.application_status = 'IN_CONTACT' THEN 'FOLLOW_UP_IN_PROGRESS'
        WHEN a.application_status = 'RETURNED' THEN 'RETURN_FOR_SUPPLEMENT_NOTICE'
        ELSE 'CUSTOMER_WITHDREW'
    END AS note
FROM @apps a
WHERE a.application_status IN ('IN_CONTACT', 'RETURNED', 'CANCELLED');

DECLARE @riskMap TABLE (
    log_id BIGINT NOT NULL,
    application_id NVARCHAR(50) NOT NULL
);

INSERT INTO RISK_EVENT_LOG (
    event_type,
    business_id,
    target_identifier,
    risk_level,
    disposition,
    trigger_reason,
    meta_data,
    transaction_amount,
    callback_url,
    created_at
)
OUTPUT inserted.log_id, inserted.business_id INTO @riskMap (log_id, application_id)
SELECT
    'LOAN_APPLY' AS event_type,
    a.application_id AS business_id,
    a.customer_id AS target_identifier,
    CASE
        WHEN a.apply_type IN ('HOUSE', 'LAND') THEN 'HIGH'
        WHEN a.apply_type IN ('BUSINESS', 'STUDENT') THEN 'MEDIUM'
        ELSE 'LOW'
    END AS risk_level,
    CASE
        WHEN a.application_status = 'REJECTED' THEN 'REJECT'
        WHEN a.application_status = 'RETURNED' THEN 'MANUAL_REVIEW'
        WHEN a.application_status = 'PENDING_REVIEW' THEN 'MANUAL_REVIEW'
        ELSE 'PASS'
    END AS disposition,
    CASE
        WHEN a.application_status = 'PENDING_REVIEW' THEN 'LOAN_APPLICATION_SENT_TO_RISK'
        WHEN a.application_status = 'RETURNED' THEN 'RISK_RETURNED_FOR_SUPPLEMENT'
        WHEN a.application_status = 'APPROVED' THEN 'RISK_APPROVED'
        WHEN a.application_status = 'REJECTED' THEN 'RISK_REJECTED'
        ELSE 'APPROVED_THEN_DISBURSED'
    END AS trigger_reason,
    CONCAT('{"customerId":"', a.customer_id, '","applyType":"', a.apply_type, '"}') AS meta_data,
    CAST(a.apply_amount AS DECIMAL(18, 4)) AS transaction_amount,
    CONCAT('http://localhost:8080/api/loan-callbacks/', a.application_id, '/status') AS callback_url,
    a.create_time
FROM @apps a
WHERE a.application_status IN ('PENDING_REVIEW', 'RETURNED', 'APPROVED', 'REJECTED', 'DISBURSED');

INSERT INTO REVIEW_TASK (
    log_id,
    business_id,
    scene,
    status,
    substatus,
    review_result,
    assignee,
    admin_comment,
    required_documents_json,
    priority,
    create_at,
    processed_at,
    version,
    attachments
)
SELECT
    rm.log_id,
    a.application_id,
    'LOAN_APPLY',
    CASE
        WHEN a.application_status = 'PENDING_REVIEW' THEN 'PENDING'
        WHEN a.application_status = 'RETURNED' THEN 'PENDING'
        ELSE 'COMPLETED'
    END AS status,
    CASE
        WHEN a.application_status = 'PENDING_REVIEW' THEN 'NEW'
        WHEN a.application_status = 'RETURNED' THEN 'WAITING_DOCUMENT'
        ELSE NULL
    END AS substatus,
    CASE
        WHEN a.application_status IN ('APPROVED', 'DISBURSED') THEN 'APPROVED'
        WHEN a.application_status = 'REJECTED' THEN 'REJECTED'
        WHEN a.application_status = 'RETURNED' THEN 'RETURNED'
        ELSE NULL
    END AS review_result,
    CASE
        WHEN a.application_status IN ('PENDING_REVIEW', 'RETURNED') THEN NULL
        ELSE 'auditor01'
    END AS assignee,
    CASE
        WHEN a.application_status = 'PENDING_REVIEW' THEN 'UNDER_REVIEW'
        WHEN a.application_status = 'RETURNED' THEN 'RETURN_FOR_SUPPLEMENT'
        WHEN a.application_status = 'APPROVED' THEN 'APPROVED'
        WHEN a.application_status = 'REJECTED' THEN 'REJECTED'
        ELSE 'DISBURSED'
    END AS admin_comment,
    CASE
        WHEN a.application_status = 'RETURNED' THEN '["ID_CARD","INCOME_PROOF","SUPPLEMENT_NOTE"]'
        ELSE '["ID_CARD","INCOME_PROOF"]'
    END AS required_documents_json,
    CASE
        WHEN a.apply_type IN ('HOUSE', 'LAND') THEN 1
        WHEN a.apply_type IN ('BUSINESS', 'STUDENT') THEN 5
        ELSE 10
    END AS priority,
    DATEADD(MINUTE, 15, a.create_time) AS create_at,
    CASE
        WHEN a.application_status IN ('APPROVED', 'REJECTED', 'DISBURSED') THEN DATEADD(HOUR, 2, a.create_time)
        ELSE NULL
    END AS processed_at,
    0 AS version,
    '["income-proof.pdf","id-card.pdf"]' AS attachments
FROM @apps a
INNER JOIN @riskMap rm
    ON rm.application_id = a.application_id
WHERE a.application_status IN ('PENDING_REVIEW', 'RETURNED', 'APPROVED', 'REJECTED', 'DISBURSED');

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
)
SELECT
    CONCAT('LRD20260521', RIGHT(CONCAT('000', CAST(ROW_NUMBER() OVER (ORDER BY a.seq) AS VARCHAR(3))), 3)) AS review_id,
    a.application_id,
    a.apply_amount,
    a.apply_period,
    a.rate,
    CASE
        WHEN a.apply_type = 'CAR' THEN 'CAR_COLLATERAL'
        WHEN a.apply_type = 'HOUSE' THEN 'HOUSE_COLLATERAL'
        WHEN a.apply_type = 'LAND' THEN 'LAND_COLLATERAL'
        WHEN a.apply_type = 'BUSINESS' THEN 'BUSINESS_CASHFLOW'
        WHEN a.apply_type = 'STUDENT' THEN 'STUDENT_GUARANTEE'
        ELSE 'GENERAL_CREDIT'
    END AS collateral_note,
    'EMP001' AS emp_id,
    DATEADD(HOUR, 2, a.create_time) AS review_time,
    'SUBMITTED' AS review_status,
    DATEADD(HOUR, 1, a.create_time) AS submitted_time,
    CASE
        WHEN a.application_status = 'PENDING_REVIEW' THEN 'UNDER_REVIEW'
        WHEN a.application_status = 'RETURNED' THEN 'RETURN_FOR_SUPPLEMENT'
        WHEN a.application_status = 'APPROVED' THEN 'APPROVED'
        WHEN a.application_status = 'REJECTED' THEN 'REJECTED'
        ELSE 'DISBURSED'
    END AS review_note
FROM @apps a
WHERE a.application_status IN ('PENDING_REVIEW', 'RETURNED', 'APPROVED', 'REJECTED', 'DISBURSED');

INSERT INTO LOAN_ACCOUNT (
    account_id,
    account_number,
    application_id,
    customer_id,
    apply_type,
    principal_amount,
    confirmed_period,
    rate,
    monthly_payment,
    paid_periods,
    remaining_principal,
    start_date,
    next_payment_date,
    account_status,
    create_time,
    update_time
)
SELECT
    CONCAT('LAC20260521', RIGHT(CONCAT('00', CAST(ROW_NUMBER() OVER (ORDER BY a.seq) AS VARCHAR(2))), 2)) AS account_id,
    (SELECT TOP 1 account_number FROM account WHERE customer_id = a.customer_id AND account_type = 'LOAN') AS account_number,
    a.application_id,
    a.customer_id,
    a.apply_type,
    CAST(a.apply_amount AS BIGINT) AS principal_amount,
    a.apply_period AS confirmed_period,
    a.rate,
    CAST(ROUND((a.apply_amount / NULLIF(a.apply_period, 0)) * (1 + a.rate), 2) AS DECIMAL(18, 2)) AS monthly_payment,
    CASE ROW_NUMBER() OVER (ORDER BY a.seq) % 4
        WHEN 0 THEN a.apply_period
        ELSE 0
    END AS paid_periods,
    CASE ROW_NUMBER() OVER (ORDER BY a.seq) % 4
        WHEN 0 THEN CAST(0 AS DECIMAL(18, 2))
        ELSE CAST(a.apply_amount AS DECIMAL(18, 2))
    END AS remaining_principal,
    CAST(a.create_time AS DATE) AS start_date,
    CASE ROW_NUMBER() OVER (ORDER BY a.seq) % 4
        WHEN 0 THEN NULL
        ELSE DATEADD(MONTH, 1, CAST(a.create_time AS DATE))
    END AS next_payment_date,
    CASE ROW_NUMBER() OVER (ORDER BY a.seq) % 4
        WHEN 0 THEN 'PAID_OFF'
        ELSE 'ACTIVE'
    END AS account_status,
    DATEADD(HOUR, 1, a.create_time) AS create_time,
    DATEADD(HOUR, 2, a.create_time) AS update_time
FROM @apps a
WHERE a.application_status = 'DISBURSED';

;WITH period_numbers AS (
    SELECT TOP (480) ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS period_index
    FROM sys.all_objects a
    CROSS JOIN sys.all_objects b
),
repayment_rows AS (
    SELECT
        la.account_id,
        pn.period_index,
        DATEADD(MONTH, pn.period_index, la.start_date) AS scheduled_date,
        CAST(ROUND(la.principal_amount / NULLIF(CAST(la.confirmed_period AS DECIMAL(18, 2)), 0), 2) AS DECIMAL(18, 2)) AS principal_portion,
        CAST(ROUND(la.monthly_payment - (la.principal_amount / NULLIF(CAST(la.confirmed_period AS DECIMAL(18, 2)), 0)), 2) AS DECIMAL(18, 2)) AS interest_portion,
        la.principal_amount,
        la.confirmed_period,
        la.paid_periods,
        la.account_status,
        la.create_time
    FROM LOAN_ACCOUNT la
    INNER JOIN period_numbers pn
        ON pn.period_index <= la.confirmed_period
    WHERE la.application_id LIKE 'LA202605%'
)
INSERT INTO LOAN_REPAYMENT (
    repayment_id,
    account_id,
    period_index,
    scheduled_date,
    paid_date,
    total_amount,
    principal_portion,
    interest_portion,
    remaining_after,
    repayment_status,
    create_time,
    update_time
)
SELECT
    CONCAT('LRP', account_id, RIGHT(CONCAT('000', CAST(period_index AS VARCHAR(3))), 3)) AS repayment_id,
    account_id,
    period_index,
    scheduled_date,
    CASE
        WHEN account_status = 'PAID_OFF' OR period_index <= paid_periods THEN scheduled_date
        ELSE NULL
    END AS paid_date,
    principal_portion + interest_portion AS total_amount,
    principal_portion,
    interest_portion,
    CASE
        WHEN account_status = 'PAID_OFF' THEN 0
        ELSE
            CASE
                WHEN CAST(principal_amount AS DECIMAL(18, 2)) - (principal_portion * period_index) < 0 THEN 0
                ELSE CAST(principal_amount AS DECIMAL(18, 2)) - (principal_portion * period_index)
            END
    END AS remaining_after,
    CASE
        WHEN account_status = 'PAID_OFF' OR period_index <= paid_periods THEN 'PAID'
        WHEN account_status = 'OVERDUE' AND period_index = paid_periods + 1 THEN 'OVERDUE'
        ELSE 'SCHEDULED'
    END AS repayment_status,
    create_time,
    NULL AS update_time
FROM repayment_rows;

IF NOT EXISTS (SELECT 1 FROM TRANS_LOG WHERE reference_id = 'LOAN-DISB-2026052005')
BEGIN
    DECLARE @damingDisbursementBefore DECIMAL(19, 4);
    DECLARE @damingDisbursementAmount DECIMAL(19, 4) = 2500000.0000;

    SELECT @damingDisbursementBefore = balance
    FROM [ACCOUNT]
    WHERE account_number = '070000000001'
      AND customer_id = 'Q8M4T7K2'
      AND account_type = 'CHECKING'
      AND currency = 'TWD'
      AND status = 'ACTIVE';

    IF @damingDisbursementBefore IS NOT NULL
    BEGIN
        INSERT INTO TRANS_LOG (
            transaction_id,
            reference_id,
            account_number,
            counterpart_account,
            bank_code,
            bank_name,
            counterpart_bank_code,
            counterpart_bank_name,
            is_interbank,
            entry_type,
            transaction_type,
            amount,
            fee_amount,
            total_debit_amount,
            balance_before,
            balance_after,
            currency,
            note,
            created_at
        ) VALUES (
            '00000000-0000-0000-0000-202605200005',
            'LOAN-DISB-2026052005',
            '070000000001',
            '909000000001',
            '909',
            N'爪哇銀行',
            '909',
            N'爪哇銀行撥款帳戶',
            0,
            'CREDIT',
            'LOAN_DISBURSEMENT',
            @damingDisbursementAmount,
            0.0000,
            @damingDisbursementAmount,
            @damingDisbursementBefore,
            @damingDisbursementBefore + @damingDisbursementAmount,
            'TWD',
            N'貸款撥款 applicationId=LA2026052005',
            '2026-05-21 16:00:00.000'
        );

        UPDATE [ACCOUNT]
        SET
            balance = @damingDisbursementBefore + @damingDisbursementAmount,
            changed_at = '2026-05-21 16:00:00',
            changed_by = 'loan-mock'
        WHERE account_number = '070000000001';
    END
END;

PRINT 'loan_mockdata.sql completed: seeded 104 applications, 43 risk review tasks, 11 loan accounts, and repayment schedules.';
