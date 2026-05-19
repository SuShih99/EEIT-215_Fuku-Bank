DROP TABLE IF EXISTS loan_repayment;
DROP TABLE IF EXISTS loan_account;
DROP TABLE IF EXISTS loan_review_detail;
DROP TABLE IF EXISTS loan_contact_log;
DROP TABLE IF EXISTS loan_document;
DROP TABLE IF EXISTS loan_application;

-- ══════════════════════════════════════════
--  1. loan_application  貸款申請主表
-- ══════════════════════════════════════════
CREATE TABLE loan_application
(
    application_id        NVARCHAR(50)   NOT NULL,
    customer_id           NVARCHAR(50)   NOT NULL,
    apply_type            NVARCHAR(50)   NULL,
    apply_amount          DECIMAL(18, 2) NULL,
    apply_period          INT            NULL,
    rate                  DECIMAL(10, 6) NULL,
    disbursement_account  NVARCHAR(14)   NULL,     -- 客戶選擇的撥款入帳帳號（核准後撥款使用）
    application_status      NVARCHAR(30)   NOT NULL, -- LoanApplicationStatus enum
    create_time             DATETIME2      NOT NULL,
    latest_contact_status   NVARCHAR(30)   NULL,     -- LoanContactStatus enum
    latest_contact_time     DATETIME2      NULL,
    update_time             DATETIME2      NULL,
    documents_submitted_at  DATETIME2      NULL,     -- 客戶送出補件的時間（NULL = 尚未送出）
    current_supplement_batch_no INT NOT NULL DEFAULT 0,
    admin_comment         VARCHAR(50) NULL,
    requiredDocumentsJson NVARCHAR(MAX) NULL,
    CONSTRAINT PK_LOAN_APPLICATION PRIMARY KEY (application_id)
);


-- ══════════════════════════════════════════
--  2. loan_contact_log  聯繫紀錄子表（只寫不改）
-- ══════════════════════════════════════════
CREATE TABLE loan_contact_log
(
    log_id          NVARCHAR(50)   NOT NULL,
    application_id  NVARCHAR(50)   NOT NULL,
    emp_id          NVARCHAR(50)   NULL,
    contact_status  NVARCHAR(30)   NULL, -- LoanContactStatus enum
    contact_channel NVARCHAR(20)   NULL, -- LoanContactChannel enum
    contact_time    DATETIME2      NULL,
    note            NVARCHAR(1000) NULL,
    CONSTRAINT PK_LOAN_CONTACT_LOG PRIMARY KEY (log_id),
    CONSTRAINT FK_CONTACT_LOG_APPLICATION
        FOREIGN KEY (application_id)
            REFERENCES loan_application (application_id)
);

-- ══════════════════════════════════════════
--  3. loan_review_detail  二次填單子表
-- ══════════════════════════════════════════
CREATE TABLE loan_review_detail
(
    review_id        NVARCHAR(50)   NOT NULL,
    application_id   NVARCHAR(50)   NOT NULL,
    confirmed_amount DECIMAL(18, 2) NULL,
    confirmed_period INT            NULL,
    confirmed_rate   DECIMAL(10, 6) NULL,
    collateral_note  NVARCHAR(2000) NULL,
    emp_id           NVARCHAR(50)   NULL,
    review_time      DATETIME2      NULL,
    review_status    NVARCHAR(20)   NULL, -- LoanReviewStatus enum
    submitted_time   DATETIME2      NULL,
    review_note      NVARCHAR(2000) NULL,
    CONSTRAINT PK_LOAN_REVIEW_DETAIL PRIMARY KEY (review_id),
    CONSTRAINT FK_REVIEW_DETAIL_APPLICATION
        FOREIGN KEY (application_id)
            REFERENCES loan_application (application_id)
);

-- ══════════════════════════════════════════
--  4. loan_account  貸款帳戶主表
-- ══════════════════════════════════════════
CREATE TABLE loan_account
(
    account_id          NVARCHAR(50)   NOT NULL,
    account_number      NVARCHAR(14)   NULL,
    application_id      NVARCHAR(50)   NOT NULL,
    customer_id         NVARCHAR(50)   NOT NULL,
    apply_type          NVARCHAR(50)   NULL,
    principal_amount    BIGINT         NULL,
    confirmed_period    INT            NULL,
    rate                DECIMAL(10, 6) NULL,
    monthly_payment     DECIMAL(18, 2) NULL,
    paid_periods        INT            NOT NULL DEFAULT 0,
    remaining_principal DECIMAL(18, 2) NULL,
    start_date          DATE           NULL,
    next_payment_date   DATE           NULL,
    account_status      NVARCHAR(20)   NOT NULL, -- LoanAccountStatus enum
    create_time         DATETIME2      NOT NULL,
    update_time         DATETIME2      NULL,
    CONSTRAINT PK_LOAN_ACCOUNT PRIMARY KEY (account_id),
    CONSTRAINT FK_LOAN_ACCOUNT_APPLICATION
        FOREIGN KEY (application_id)
            REFERENCES loan_application (application_id)
);

-- ══════════════════════════════════════════
--  5. loan_repayment  每期還款紀錄
-- ══════════════════════════════════════════
CREATE TABLE loan_repayment
(
    repayment_id      NVARCHAR(50)   NOT NULL,
    account_id        NVARCHAR(50)   NOT NULL,
    period_index      INT            NOT NULL,
    scheduled_date    DATE           NOT NULL,
    paid_date         DATE           NULL,
    total_amount      DECIMAL(18, 2) NOT NULL,
    principal_portion DECIMAL(18, 2) NOT NULL,
    interest_portion  DECIMAL(18, 2) NOT NULL,
    remaining_after   DECIMAL(18, 2) NULL,
    repayment_status  NVARCHAR(20)   NOT NULL, -- LoanRepaymentStatus enum
    create_time       DATETIME2      NOT NULL,
    update_time       DATETIME2      NULL,
    CONSTRAINT PK_LOAN_REPAYMENT PRIMARY KEY (repayment_id),
    CONSTRAINT FK_REPAYMENT_ACCOUNT
        FOREIGN KEY (account_id)
            REFERENCES loan_account (account_id)
);

-- ══════════════════════════════════════════
--  6. loan_document  補件文件表
-- ══════════════════════════════════════════
CREATE TABLE loan_document
(
    document_id    NVARCHAR(50)  NOT NULL,
    application_id NVARCHAR(50)  NOT NULL,
    document_type  NVARCHAR(30)  NOT NULL,
    file_url       NVARCHAR(500) NOT NULL,
    original_name  NVARCHAR(255) NULL,
    uploaded_by    NVARCHAR(50)  NOT NULL,
    upload_time    DATETIME2     NOT NULL,
    document_batch_type NVARCHAR(20) NOT NULL DEFAULT 'INITIAL',
    document_batch_no   INT          NOT NULL DEFAULT 0,
    submitted_at        DATETIME2    NULL,

    CONSTRAINT PK_LOAN_DOCUMENT PRIMARY KEY (document_id),

    CONSTRAINT FK_DOCUMENT_APPLICATION
        FOREIGN KEY (application_id)
            REFERENCES loan_application (application_id)
);
