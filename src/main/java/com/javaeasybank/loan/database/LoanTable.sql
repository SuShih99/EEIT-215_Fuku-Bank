-- ===建表===
CREATE TABLE LOAN_APPLICATION (
    application_id      NVARCHAR(50)    NOT NULL PRIMARY KEY,
    customer_id         INT             NULL,
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
INSERT INTO LOAN_APPLICATION VALUES 
('LA202605011200000001', 1001, NULL, NULL, NULL, 'STUDENT', 500000, 60, 0.015, 'PENDING_CONTACT', GETDATE(), NULL, NULL),
('LA202605011200000002', NULL, '王小明', '0912345678', 'test@example.com', 'CAR', 300000, 48, 0.033, 'PENDING_CONTACT', GETDATE(), NULL, NULL);
