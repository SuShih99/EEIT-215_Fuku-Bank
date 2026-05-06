CREATE TABLE review_task
(
    task_id       bigint      NOT NULL,
    log_id        bigint      NOT NULL,
    business_id   varchar(64) NOT NULL,
    scene         varchar(32),
    status        varchar(255),
    review_result varchar(20),
    assignee      varchar(255),
    admin_comment varchar(255),
    priority      int,
    create_at     datetime,
    processed_at  datetime,
    version       bigint,
    CONSTRAINT pk_review_task PRIMARY KEY (task_id)
)
GO

ALTER TABLE review_task
    ADD CONSTRAINT FK_REVIEW_TASK_ON_LOG FOREIGN KEY (log_id) REFERENCES risk_event_log (log_id)
GO