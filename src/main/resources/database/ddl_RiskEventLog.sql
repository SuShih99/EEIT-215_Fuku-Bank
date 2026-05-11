CREATE TABLE risk_event_log
(
    log_id             bigint IDENTITY (1, 1) NOT NULL,
    event_type         varchar(50)            NOT NULL,
    business_id        varchar(100)           NOT NULL,
    target_identifier  varchar(100)           NOT NULL,
    risk_level         varchar(20)            NOT NULL,
    disposition       varchar(50)            NOT NULL,
    trigger_reason     varchar(500),
    meta_data          varchar(255),
    transaction_amount decimal(18, 4),
    callback_url       varchar(255),
    created_at         datetime               NOT NULL,
    CONSTRAINT pk_risk_event_log PRIMARY KEY (log_id)
)
    GO