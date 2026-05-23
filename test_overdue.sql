SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRAN;

DECLARE @targetOverdueTotal INT = 6;
DECLARE @today DATE = CAST(GETDATE() AS DATE);
DECLARE @overdueDate DATE = DATEADD(DAY, -3, @today);          -- 逾期日
DECLARE @firstPaidDate DATE = DATEADD(MONTH, -1, @overdueDate); -- 第1期已繳
DECLARE @startDate DATE = DATEADD(MONTH, -2, @overdueDate);     -- 撥款日
DECLARE @now DATETIME2 = SYSDATETIME();

DECLARE @wangCustomerId NVARCHAR(50) = 'Q8M4T7K2';
DECLARE @wangAccountId NVARCHAR(50);

SELECT TOP 1
    @wangAccountId = la.account_id
FROM loan_account la
WHERE la.customer_id = @wangCustomerId
ORDER BY la.create_time DESC, la.account_id DESC;

IF @wangAccountId IS NULL
BEGIN
    THROW 52001, '找不到王大明(Q8M4T7K2)的 loan_account，請先確認 seed 已建立。', 1;
END;

IF OBJECT_ID('tempdb..#target_overdue_accounts') IS NOT NULL
    DROP TABLE #target_overdue_accounts;

CREATE TABLE #target_overdue_accounts
(
    account_id NVARCHAR(50) NOT NULL PRIMARY KEY
);

-- 先放入王大明
INSERT INTO #target_overdue_accounts(account_id)
VALUES (@wangAccountId);

DECLARE @currentOverdue INT;
DECLARE @additionalNeeded INT;

SELECT
    @currentOverdue = COUNT(*)
FROM loan_account
WHERE account_status = 'OVERDUE';

SET @additionalNeeded =
    @targetOverdueTotal
    - (
        @currentOverdue
        + CASE
            WHEN EXISTS (
                SELECT 1
                FROM loan_account
                WHERE account_id = @wangAccountId
                  AND account_status <> 'OVERDUE'
            ) THEN 1
            ELSE 0
          END
      );

IF @additionalNeeded < 0
    SET @additionalNeeded = 0;

-- 從 ACTIVE 補足到總共 6 筆 OVERDUE
INSERT INTO #target_overdue_accounts(account_id)
SELECT TOP (@additionalNeeded) la.account_id
FROM loan_account la
WHERE la.account_status = 'ACTIVE'
  AND la.account_id <> @wangAccountId
ORDER BY la.create_time DESC, la.account_id DESC;

IF (
    SELECT COUNT(*)
    FROM #target_overdue_accounts
) < (
    CASE
        WHEN @additionalNeeded > 0 THEN @additionalNeeded + 1
        ELSE 1
    END
)
BEGIN
    THROW 52002, 'ACTIVE 帳戶不足，無法補成 6 筆逾期帳戶。', 1;
END;

;WITH repayment_rank AS
(
    SELECT
        lr.repayment_id,
        lr.account_id,
        lr.period_index,
        ROW_NUMBER() OVER (
            PARTITION BY lr.account_id
            ORDER BY lr.period_index
        ) AS rn
    FROM loan_repayment lr
    INNER JOIN #target_overdue_accounts t
        ON t.account_id = lr.account_id
)
UPDATE lr
SET
    scheduled_date =
        CASE
            WHEN rr.rn = 1 THEN @firstPaidDate
            WHEN rr.rn = 2 THEN @overdueDate
            ELSE DATEADD(MONTH, rr.rn - 2, @overdueDate)
        END,
    paid_date =
        CASE
            WHEN rr.rn = 1 THEN @firstPaidDate
            ELSE NULL
        END,
    repayment_status =
        CASE
            WHEN rr.rn = 1 THEN 'PAID'
            WHEN rr.rn = 2 THEN 'OVERDUE'
            ELSE 'SCHEDULED'
        END,
    update_time = @now
FROM loan_repayment lr
INNER JOIN repayment_rank rr
    ON rr.repayment_id = lr.repayment_id;

;WITH first_period_remaining AS
(
    SELECT
        lr.account_id,
        lr.remaining_after,
        ROW_NUMBER() OVER (
            PARTITION BY lr.account_id
            ORDER BY lr.period_index
        ) AS rn
    FROM loan_repayment lr
    INNER JOIN #target_overdue_accounts t
        ON t.account_id = lr.account_id
)
UPDATE la
SET
    account_status = 'OVERDUE',
    start_date = @startDate,
    next_payment_date = @overdueDate,
    paid_periods = 1,
    remaining_principal = fpr.remaining_after,
    update_time = @now
FROM loan_account la
INNER JOIN first_period_remaining fpr
    ON fpr.account_id = la.account_id
   AND fpr.rn = 1
INNER JOIN #target_overdue_accounts t
    ON t.account_id = la.account_id;

COMMIT;

-- 驗證 1：狀態分布
SELECT
    account_status,
    COUNT(*) AS cnt
FROM loan_account
GROUP BY account_status
ORDER BY account_status;

-- 驗證 2：這次被調整成逾期的帳戶
SELECT
    la.account_id,
    la.account_number,
    la.application_id,
    la.customer_id,
    la.account_status,
    la.start_date,
    la.next_payment_date,
    la.paid_periods,
    la.remaining_principal
FROM loan_account la
INNER JOIN #target_overdue_accounts t
    ON t.account_id = la.account_id
ORDER BY
    CASE WHEN la.customer_id = 'Q8M4T7K2' THEN 0 ELSE 1 END,
    la.account_id;

-- 驗證 3：查看這些帳戶前 3 期是否合理
SELECT
    lr.account_id,
    lr.period_index,
    lr.scheduled_date,
    lr.paid_date,
    lr.total_amount,
    lr.remaining_after,
    lr.repayment_status
FROM loan_repayment lr
INNER JOIN #target_overdue_accounts t
    ON t.account_id = lr.account_id
WHERE lr.period_index <= 3
ORDER BY lr.account_id, lr.period_index;
