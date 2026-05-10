-- ===建表===
CREATE TABLE LOAN_APPLICATION (
    application_id      NVARCHAR(50)    NOT NULL PRIMARY KEY,
    customer_id         NVARCHAR(50)    NULL,
    applicant_name      NVARCHAR(50)    NULL,
    applicant_phone     NVARCHAR(20)    NULL,
    applicant_email     NVARCHAR(100)   NULL,
    apply_type          NVARCHAR(20)    NOT NULL,
    apply_amount        BIGINT          NOT NULL,
    apply_period        INT             NOT NULL,
    rate                DECIMAL(15,2)   NOT NULL,
    application_status  NVARCHAR(20)    NOT NULL,
    create_time         DATETIME2       NOT NULL,
    latest_contact_status NVARCHAR(20)  NULL,
    latest_contact_time   DATETIME2     NULL
);

-- ===測試資料（全部 PENDING_CONTACT）===
INSERT INTO LOAN_APPLICATION (
    application_id, customer_id,
    applicant_name, applicant_phone, applicant_email,
    apply_type, apply_amount, apply_period, rate,
    application_status, create_time,
    latest_contact_status, latest_contact_time
) VALUES

-- 1. 會員 | 個人信貸 36期 | rate = 0.04 + 0.005 = 0.045
('LA202504201031000001', 'C10001',
 NULL, NULL, NULL,
 'PERSONAL', 500000, 36, 0.04500,
 'PENDING_CONTACT', '2025-04-20 10:31:00',
 NULL, NULL),
