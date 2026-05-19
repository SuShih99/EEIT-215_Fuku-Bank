DROP TABLE IF EXISTS loan_repayment;
DROP TABLE IF EXISTS loan_account;
DROP TABLE IF EXISTS loan_review_detail;
DROP TABLE IF EXISTS loan_contact_log;
DROP TABLE IF EXISTS loan_document;
DROP TABLE IF EXISTS loan_application;

CREATE TABLE loan_application
(
    application_id              NVARCHAR(50)   NOT NULL,
    customer_id                 NVARCHAR(50)   NOT NULL,
    apply_type                  NVARCHAR(50)   NULL,
    apply_amount                DECIMAL(18, 2) NULL,
    apply_period                INT            NULL,
    rate                        DECIMAL(10, 6) NULL,
    disbursement_account        NVARCHAR(14)   NULL,
    application_status          NVARCHAR(30)   NOT NULL,
    required_documents          NVARCHAR(MAX)  NULL,
    review_comment              NVARCHAR(50)   NULL,
    create_time                 DATETIME2      NOT NULL,
    latest_contact_status       NVARCHAR(30)   NULL,
    latest_contact_time         DATETIME2      NULL,
    update_time                 DATETIME2      NULL,
    documents_submitted_at      DATETIME2      NULL,
    current_supplement_batch_no INT            NOT NULL DEFAULT 0,
    CONSTRAINT PK_LOAN_APPLICATION PRIMARY KEY (application_id)
);

CREATE TABLE loan_contact_log
(
    log_id          NVARCHAR(50)   NOT NULL,
    application_id  NVARCHAR(50)   NOT NULL,
    emp_id          NVARCHAR(50)   NULL,
    contact_status  NVARCHAR(30)   NULL,
    contact_channel NVARCHAR(20)   NULL,
    contact_time    DATETIME2      NULL,
    note            NVARCHAR(1000) NULL,
    CONSTRAINT PK_LOAN_CONTACT_LOG PRIMARY KEY (log_id),
    CONSTRAINT FK_CONTACT_LOG_APPLICATION
        FOREIGN KEY (application_id)
            REFERENCES loan_application (application_id)
);

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
    review_status    NVARCHAR(20)   NULL,
    submitted_time   DATETIME2      NULL,
    review_note      NVARCHAR(2000) NULL,
    CONSTRAINT PK_LOAN_REVIEW_DETAIL PRIMARY KEY (review_id),
    CONSTRAINT FK_REVIEW_DETAIL_APPLICATION
        FOREIGN KEY (application_id)
            REFERENCES loan_application (application_id)
);

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
    account_status      NVARCHAR(20)   NOT NULL,
    create_time         DATETIME2      NOT NULL,
    update_time         DATETIME2      NULL,
    CONSTRAINT PK_LOAN_ACCOUNT PRIMARY KEY (account_id),
    CONSTRAINT FK_LOAN_ACCOUNT_APPLICATION
        FOREIGN KEY (application_id)
            REFERENCES loan_application (application_id)
);

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
    repayment_status  NVARCHAR(20)   NOT NULL,
    create_time       DATETIME2      NOT NULL,
    update_time       DATETIME2      NULL,
    CONSTRAINT PK_LOAN_REPAYMENT PRIMARY KEY (repayment_id),
    CONSTRAINT FK_REPAYMENT_ACCOUNT
        FOREIGN KEY (account_id)
            REFERENCES loan_account (account_id)
);

CREATE TABLE loan_document
(
    document_id         NVARCHAR(50)  NOT NULL,
    application_id      NVARCHAR(50)  NOT NULL,
    document_type       NVARCHAR(30)  NOT NULL,
    file_url            NVARCHAR(500) NOT NULL,
    original_name       NVARCHAR(255) NULL,
    uploaded_by         NVARCHAR(50)  NOT NULL,
    upload_time         DATETIME2     NOT NULL,
    document_batch_type NVARCHAR(20)  NOT NULL DEFAULT 'INITIAL',
    document_batch_no   INT           NOT NULL DEFAULT 0,
    submitted_at        DATETIME2     NULL,
    CONSTRAINT PK_LOAN_DOCUMENT PRIMARY KEY (document_id),
    CONSTRAINT FK_DOCUMENT_APPLICATION
        FOREIGN KEY (application_id)
            REFERENCES loan_application (application_id)
);

GO
