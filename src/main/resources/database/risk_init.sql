CREATE TABLE RISK_EVENT_LOG
(
    log_id             bigint IDENTITY (1, 1) NOT NULL,
    event_type         varchar(50)  NOT NULL,
    business_id        varchar(64)  NOT NULL,
    target_identifier  varchar(100) NOT NULL,
    risk_level         varchar(20)  NOT NULL,
    disposition        varchar(50)  NOT NULL,
    trigger_reason     varchar(500),
    meta_data          varchar(255),
    transaction_amount decimal(18, 4),
    callback_url       varchar(255),
    created_at         datetime     NOT NULL,
    CONSTRAINT pk_risk_event_log PRIMARY KEY (log_id)
)
    GO

CREATE TABLE REVIEW_TASK
(
    task_id       BIGINT IDENTITY(1,1),
    log_id        BIGINT      NOT NULL,
    business_id   VARCHAR(64) NOT NULL,
    scene         VARCHAR(32) NULL,
    status        VARCHAR(50) NULL,           -- 255 有點大，通常狀態代碼 50 綽綽有餘
    review_result VARCHAR(20) NULL,
    assignee      VARCHAR(255) NULL,
    admin_comment VARCHAR(255) NULL,
    priority      INT      DEFAULT 0,
    attachments   NVARCHAR(MAX) NULL,
    create_at     DATETIME DEFAULT GETDATE(), -- 自動記錄建立時間
    processed_at  DATETIME NULL,
    version       BIGINT       DEFAULT 0,
    attachments   NVARCHAR(MAX) NULL,             -- 補件文件清單（JSON 陣列，null = 尚未收到補件）
    CONSTRAINT pk_review_task PRIMARY KEY (task_id)
)
    GO

ALTER TABLE REVIEW_TASK
    ADD CONSTRAINT FK_REVIEW_TASK_ON_LOG FOREIGN KEY (log_id) REFERENCES RISK_EVENT_LOG (log_id);
GO

CREATE TABLE BLACK_LIST
(
    id         bigint IDENTITY (1, 1) NOT NULL,
    list_type  varchar(20)  NOT NULL,
    list_value varchar(100) NOT NULL,
    source     varchar(50),
    reason     varchar(255),
    status     bit          NOT NULL,
    expires_at datetime,
    created_at datetime,
    updated_at datetime,
    CONSTRAINT pk_black_list PRIMARY KEY (id)
)
    GO

CREATE
NONCLUSTERED INDEX idx_bl_lookup ON BLACK_LIST (list_type, list_value)
GO

CREATE TABLE customer_credit_info
(
    customer_id     varchar(20)                             NOT NULL,
    annual_income   decimal(15, 2),
    occupation      nvarchar(50),
    job             nvarchar(100),
    external_score  int,
    other_bank_debt decimal(15, 2),
    has_real_estate bit,
    fund_source     varchar(30),
    is_pep          bit
        CONSTRAINT DF_customer_credit_info_is_pep DEFAULT 0 NOT NULL,
    final_score     int,
    risk_level      varchar(10),
    last_updated_at datetime                                NOT NULL,
    CONSTRAINT pk_customer_credit_info PRIMARY KEY (customer_id)
)
    GO