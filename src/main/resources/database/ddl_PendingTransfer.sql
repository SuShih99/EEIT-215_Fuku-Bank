CREATE TABLE pending_transfer
(
    id                  bigint IDENTITY (1, 1) NOT NULL,
    reference_id        varchar(50)            NOT NULL,
    from_account_number varchar(20)            NOT NULL,
    to_account_number   varchar(20)            NOT NULL,
    to_bank_code        varchar(10),
    amount              decimal(18, 4)         NOT NULL,
    currency            varchar(3),
    note                varchar(200),
    risk_log_id         bigint,
    review_task_id      bigint,
    hold_reason         varchar(200),
    status              varchar(20)            NOT NULL,
    created_at          datetime               NOT NULL,
    processed_at        datetime,
    expires_at          datetime               NOT NULL,
    CONSTRAINT pk_pending_transfer PRIMARY KEY (id)
)
GO

ALTER TABLE pending_transfer
    ADD CONSTRAINT uc_pending_transfer_reference UNIQUE (reference_id)
GO