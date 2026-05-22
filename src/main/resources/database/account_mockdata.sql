/*
===============================================================================
Java Easy Bank Account Mock Data
- Requires customer_insert.sql first
- Creates realistic customer accounts from 100 customer profiles
- Generates exactly 500 transaction log rows
- JPY balances and transaction amounts are generated without fractional values
===============================================================================
*/


-- Clear account mock data in FK-safe order. Business accounts are recreated below.
IF OBJECT_ID('SCHEDULED_TRANSFER', 'U') IS NOT NULL DELETE FROM SCHEDULED_TRANSFER;
IF OBJECT_ID('FAVORITE_ACCOUNT', 'U') IS NOT NULL DELETE FROM FAVORITE_ACCOUNT;
IF OBJECT_ID('TRANS_LOG', 'U') IS NOT NULL DELETE FROM TRANS_LOG;
IF OBJECT_ID('ACCOUNT_DAILY_SNAPSHOTS', 'U') IS NOT NULL DELETE FROM ACCOUNT_DAILY_SNAPSHOTS;
IF OBJECT_ID('ACCOUNT_STATUS_HISTORY', 'U') IS NOT NULL DELETE FROM ACCOUNT_STATUS_HISTORY;
IF OBJECT_ID('ACCOUNT', 'U') IS NOT NULL DELETE FROM [ACCOUNT];
IF OBJECT_ID('ACCOUNT_APPLICATION', 'U') IS NOT NULL DELETE FROM ACCOUNT_APPLICATION;
IF OBJECT_ID('ACCOUNT_APPLICATION', 'U') IS NOT NULL
BEGIN
    DBCC CHECKIDENT ('ACCOUNT_APPLICATION', RESEED, 10000) WITH NO_INFOMSGS;
END;

IF OBJECT_ID('tempdb..#customers') IS NOT NULL DROP TABLE #customers;
IF OBJECT_ID('tempdb..#mock_accounts') IS NOT NULL DROP TABLE #mock_accounts;
IF OBJECT_ID('tempdb..#tx_accounts') IS NOT NULL DROP TABLE #tx_accounts;

SELECT
    mock_customers.rn,
    mock_customers.customer_id,
    mock_customers.cif,
    mock_customers.id_number,
    mock_customers.name,
    mock_customers.birthday,
    mock_customers.gender,
    mock_customers.email,
    mock_customers.phone,
    mock_customers.address,
    mock_customers.nationality,
    mock_customers.registered_address,
    mock_customers.current_address,
    mock_customers.occupation,
    mock_customers.employer,
    mock_customers.estimated_monthly_tx,
    mock_customers.account_purpose,
    mock_customers.fund_source,
    mock_customers.tax_residency,
    mock_customers.is_pep,
    mock_customers.id_front_url,
    mock_customers.id_back_url,
    mock_customers.second_id_url,
    mock_customers.risk_level,
    mock_customers.status
INTO #customers
FROM (
    SELECT
        TRY_CAST(SUBSTRING(ca.username, 5, 4) AS INT) AS rn,
        cp.customer_id, cp.cif, cp.id_number, cp.name, cp.birthday, cp.gender, cp.email, cp.phone, cp.address,
        cp.nationality, cp.registered_address, cp.current_address, cp.occupation, cp.employer,
        cp.estimated_monthly_tx, cp.account_purpose, cp.fund_source, cp.tax_residency, cp.is_pep,
        cp.id_front_url, cp.id_back_url, cp.second_id_url, cp.risk_level, cp.status
    FROM CUSTOMER_PROFILE cp
    INNER JOIN CUSTOMER_AUTH ca ON ca.customer_id = cp.customer_id
    WHERE ca.username LIKE 'cust[0-9][0-9][0-9][0-9]'
) mock_customers
WHERE mock_customers.rn BETWEEN 1 AND 100
ORDER BY mock_customers.rn;

IF (SELECT COUNT(*) FROM #customers) <> 100
    THROW 51101, 'account_mockdata.sql requires exactly 100 customers. Run customer_insert.sql first.', 1;

CREATE TABLE #mock_accounts (
    account_number VARCHAR(14) NOT NULL PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    currency CHAR(3) NOT NULL,
    balance DECIMAL(19,4) NULL,
    liability DECIMAL(19,4) NULL,
    interest_rate DECIMAL(7,5) NULL,
    status VARCHAR(20) NOT NULL,
    parent_account_number VARCHAR(14) NULL,
    created_at DATETIME2 NOT NULL,
    changed_at DATETIME2 NOT NULL,
    created_by VARCHAR(20) NULL,
    changed_by VARCHAR(20) NULL
);

-- Bank business accounts used by loan/card/account integrations.
INSERT INTO #mock_accounts VALUES
('909000000001', 'BANK_INTERNAL', 'BUSINESS', 'TWD', 999999999999.0000, 0.0000, NULL, 'ACTIVE', NULL, '2026-01-01 09:00:00', '2026-05-13 09:00:00', N'總行', N'總行'),
('909000000002', 'BANK_INTERNAL', 'BUSINESS', 'TWD', 0.0000, 0.0000, NULL, 'ACTIVE', NULL, '2026-01-01 09:00:00', '2026-05-13 09:00:00', N'總行', N'總行');

-- One TWD checking account per customer.
INSERT INTO #mock_accounts (
    account_number, customer_id, account_type, currency, balance, liability,
    interest_rate, status, parent_account_number, created_at, changed_at, created_by, changed_by
)
SELECT
    '070' + RIGHT(REPLICATE('0', 9) + CAST(100000000 + ((rn * 7919 + 314159) % 900000000) AS VARCHAR(9)), 9),
    customer_id,
    'CHECKING',
    'TWD',
    CAST(12000 + ((rn * 7919) % 185000) + ((rn * 37) % 100) / 100.0 AS DECIMAL(19,4)),
    0.0000,
    0.00500,
    CASE WHEN status = 'ACTIVE' THEN 'ACTIVE' ELSE 'FROZEN' END,
    NULL,
    DATEADD(DAY, -220 + rn, CAST('2026-05-13 09:00:00' AS DATETIME2)),
    CAST('2026-05-13 09:00:00' AS DATETIME2),
    N'總行',
    N'總行'
FROM #customers
WHERE rn NOT IN (45, 95);

-- Foreign currency savings accounts for customers who applied for FX services.
INSERT INTO #mock_accounts (
    account_number, customer_id, account_type, currency, balance, liability,
    interest_rate, status, parent_account_number, created_at, changed_at, created_by, changed_by
)
SELECT
    '071' + RIGHT(REPLICATE('0', 9) + CAST(100000000 + ((rn * 13877 + 271828) % 900000000) AS VARCHAR(9)), 9),
    customer_id,
    'SAVINGS',
    CASE rn % 6 WHEN 0 THEN 'JPY' WHEN 1 THEN 'USD' WHEN 2 THEN 'EUR' WHEN 3 THEN 'GBP' WHEN 4 THEN 'CNY' ELSE 'AUD' END,
    CASE rn % 6
        WHEN 0 THEN CAST(50000 + ((rn * 13877) % 980000) AS DECIMAL(19,4))
        ELSE CAST(250 + ((rn * 197) % 12000) + ((rn * 31) % 100) / 100.0 AS DECIMAL(19,4))
    END,
    0.0000,
    CASE rn % 6 WHEN 0 THEN 0.00100 ELSE 0.00250 END,
    'ACTIVE',
    NULL,
    DATEADD(DAY, -190 + rn, CAST('2026-05-13 09:00:00' AS DATETIME2)),
    CAST('2026-05-13 09:00:00' AS DATETIME2),
    N'總行',
    N'總行'
FROM #customers
WHERE (rn <= 24 OR (rn >= 51 AND rn <= 74)) AND status = 'ACTIVE';

-- Time deposits: fewer, larger, and lower activity.
INSERT INTO #mock_accounts (
    account_number, customer_id, account_type, currency, balance, liability,
    interest_rate, status, parent_account_number, created_at, changed_at, created_by, changed_by
)
SELECT
    '072' + RIGHT(REPLICATE('0', 9) + CAST(100000000 + ((rn * 25789 + 161803) % 900000000) AS VARCHAR(9)), 9),
    customer_id,
    'TIME_DEPOSIT',
    CASE rn % 4 WHEN 0 THEN 'JPY' WHEN 1 THEN 'TWD' WHEN 2 THEN 'USD' ELSE 'EUR' END,
    CASE rn % 4
        WHEN 0 THEN CAST(100000 + ((rn * 25789) % 1800000) AS DECIMAL(19,4))
        WHEN 1 THEN CAST(80000 + ((rn * 43711) % 2200000) AS DECIMAL(19,4))
        ELSE CAST(1500 + ((rn * 983) % 45000) + ((rn * 29) % 100) / 100.0 AS DECIMAL(19,4))
    END,
    0.0000,
    CASE rn % 4 WHEN 0 THEN 0.00350 WHEN 1 THEN 0.01200 ELSE 0.00800 END,
    'ACTIVE',
    NULL,
    DATEADD(DAY, -175 + rn, CAST('2026-05-13 09:00:00' AS DATETIME2)),
    CAST('2026-05-13 09:00:00' AS DATETIME2),
    N'總行',
    N'總行'
FROM #customers
WHERE (rn <= 12 OR (rn >= 51 AND rn <= 62)) AND status = 'ACTIVE';

-- Sub accounts only exist for customers who already have TWD checking accounts.
INSERT INTO #mock_accounts (
    account_number, customer_id, account_type, currency, balance, liability,
    interest_rate, status, parent_account_number, created_at, changed_at, created_by, changed_by
)
SELECT
    '073' + RIGHT(REPLICATE('0', 9) + CAST(100000000 + ((rn * 3203 + 141421) % 900000000) AS VARCHAR(9)), 9),
    customer_id,
    'SUB_ACCOUNT',
    'TWD',
    CAST(800 + ((rn * 3203) % 98000) + ((rn * 13) % 100) / 100.0 AS DECIMAL(19,4)),
    0.0000,
    0.00500,
    'ACTIVE',
    '070' + RIGHT(REPLICATE('0', 9) + CAST(100000000 + ((rn * 7919 + 314159) % 900000000) AS VARCHAR(9)), 9),
    DATEADD(DAY, -145 + rn, CAST('2026-05-13 09:00:00' AS DATETIME2)),
    CAST('2026-05-13 09:00:00' AS DATETIME2),
    N'總行',
    N'總行'
FROM #customers
WHERE (rn <= 10 OR (rn >= 51 AND rn <= 60)) AND status = 'ACTIVE';

-- Credit-card payment accounts: hidden from general account views.
INSERT INTO #mock_accounts (
    account_number, customer_id, account_type, currency, balance, liability,
    interest_rate, status, parent_account_number, created_at, changed_at, created_by, changed_by
)
SELECT
    '801' + RIGHT(REPLICATE('0', 11) + CAST(rn AS VARCHAR(11)), 11),
    customer_id,
    'CREDIT_CARD',
    'TWD',
    CAST(((rn * 4217) % 92000) + ((rn * 19) % 100) / 100.0 AS DECIMAL(19,4)),
    0.0000,
    NULL,
    'ACTIVE',
    NULL,
    DATEADD(DAY, -135 + rn, CAST('2026-05-13 09:00:00' AS DATETIME2)),
    CAST('2026-05-13 09:00:00' AS DATETIME2),
    N'總行',
    N'總行'
FROM #customers
WHERE (rn <= 8 OR (rn >= 51 AND rn <= 58)) AND status = 'ACTIVE';

-- Loan accounts: balance stays 0; liability carries the loan principal.
INSERT INTO #mock_accounts (
    account_number, customer_id, account_type, currency, balance, liability,
    interest_rate, status, parent_account_number, created_at, changed_at, created_by, changed_by
)
SELECT
    '901'
        + RIGHT('00' + CAST(ASCII(UPPER(LEFT(id_number, 1))) - ASCII('A') + 1 AS VARCHAR(2)), 2)
        + SUBSTRING(id_number, 2, LEN(id_number)),
    customer_id,
    'LOAN',
    'TWD',
    0.0000,
    CAST(80000 + ((rn * 67411) % 3600000) AS DECIMAL(19,4)),
    0.02100,
    'ACTIVE',
    NULL,
    DATEADD(DAY, -120 + rn, CAST('2026-05-13 09:00:00' AS DATETIME2)),
    CAST('2026-05-13 09:00:00' AS DATETIME2),
    N'總行',
    N'總行'
FROM #customers
WHERE customer_id IN (
    'Q8M4T7K2',
    'A6R3M8J2',
    'B3N8T5P9',
    'B9P5N2W6',
    'C6T8R4J3',
    'C9W2M6R4',
    'D5Q9T2W7',
    'E2V7D9M5',
    'E5J7Q3D8',
    'F2P9V4K6',
    'F7V4C8N2'
) AND status = 'ACTIVE';

IF NOT EXISTS (SELECT 1 FROM [ACCOUNT])
INSERT INTO [ACCOUNT] (
    account_number, customer_id, account_type, currency, balance, liability,
    interest_rate, status, parent_account_number, created_at, changed_at, created_by, changed_by
)
SELECT
    account_number, customer_id, account_type, currency, balance, liability,
    interest_rate, status, parent_account_number, created_at, changed_at, created_by, changed_by
FROM #mock_accounts
WHERE parent_account_number IS NULL
ORDER BY account_number;

IF NOT EXISTS (SELECT 1 FROM [ACCOUNT] WHERE parent_account_number IS NOT NULL)
INSERT INTO [ACCOUNT] (
    account_number, customer_id, account_type, currency, balance, liability,
    interest_rate, status, parent_account_number, created_at, changed_at, created_by, changed_by
)
SELECT
    account_number, customer_id, account_type, currency, balance, liability,
    interest_rate, status, parent_account_number, created_at, changed_at, created_by, changed_by
FROM #mock_accounts
WHERE parent_account_number IS NOT NULL
ORDER BY account_number;

-- Account applications: exactly one latest application per mock customer.
IF NOT EXISTS (SELECT 1 FROM ACCOUNT_APPLICATION)
INSERT INTO ACCOUNT_APPLICATION (
    application_no, customer_id, account_type, currency,
    customer_name, id_number, birthday, gender, email, address, nationality, phone,
    registered_address, current_address, occupation, employer, estimated_monthly_tx,
    account_purpose, fund_source, tax_residency, is_pep,
    id_front_url, id_back_url, second_id_url, risk_flag, apply_ip,
    status, reject_reason, reviewed_at, reviewed_by, created_account_number,
    created_at, updated_at
)
SELECT
    'APP-20260501-' + RIGHT(REPLICATE('0', 4) + CAST(c.rn AS VARCHAR(4)), 4),
    c.customer_id,
    x.account_type,
    x.currency,
    c.name,
    c.id_number,
    c.birthday,
    c.gender,
    c.email,
    c.address,
    ISNULL(c.nationality, 'TW'),
    c.phone,
    ISNULL(c.registered_address, c.address),
    ISNULL(c.current_address, c.address),
    c.occupation,
    c.employer,
    ISNULL(c.estimated_monthly_tx, 20),
    ISNULL(c.account_purpose, 'SAVINGS'),
    ISNULL(c.fund_source, 'SALARY'),
    ISNULL(c.tax_residency, 'TW'),
    ISNULL(c.is_pep, 0),
    ISNULL(c.id_front_url, CONCAT('/uploads/mock/customer/', c.customer_id, '-front.jpg')),
    ISNULL(c.id_back_url, CONCAT('/uploads/mock/customer/', c.customer_id, '-back.jpg')),
    ISNULL(c.second_id_url, CONCAT('/uploads/mock/customer/', c.customer_id, '-second.jpg')),
    CASE
        WHEN ISNULL(c.is_pep, 0) = 1 THEN 'PEP'
        WHEN c.risk_level = 'HIGH' THEN 'HIGH_RISK'
        WHEN c.risk_level = 'MEDIUM' THEN 'WATCH'
        ELSE 'NORMAL'
    END,
    CONCAT('203.0.113.', (c.rn % 200) + 20),
    x.application_status,
    CASE
        WHEN x.application_status = 'REJECTED' THEN N'收入證明與申請用途不符'
        WHEN x.application_status = 'SUPPLEMENT_REQUIRED' THEN N'請補上第二證件影像'
        ELSE NULL
    END,
    CASE WHEN x.application_status IN ('APPROVED', 'REJECTED', 'SUPPLEMENT_REQUIRED')
        THEN DATEADD(DAY, 2, x.created_at)
        ELSE NULL
    END,
    CASE WHEN x.application_status IN ('APPROVED', 'REJECTED', 'SUPPLEMENT_REQUIRED') THEN 'E26001' ELSE NULL END,
    CASE WHEN x.application_status = 'APPROVED'
        THEN approved_account.account_number
        ELSE NULL
    END,
    x.created_at,
    CASE WHEN x.application_status IN ('APPROVED', 'REJECTED', 'SUPPLEMENT_REQUIRED')
        THEN DATEADD(DAY, 2, x.created_at)
        ELSE x.created_at
    END
FROM #customers c
CROSS APPLY (
    SELECT
        CASE
            WHEN c.rn IN (45, 95) THEN 'PENDING'
            WHEN c.rn IN (46, 96) THEN 'SUPPLEMENT_REQUIRED'
            WHEN c.rn IN (47, 97) THEN 'REJECTED'
            WHEN c.rn IN (48, 98) THEN 'CANCELLED'
            ELSE 'APPROVED'
        END AS application_status,
        CASE
            WHEN c.rn IN (46, 96) THEN 'SAVINGS'
            WHEN c.rn IN (47, 97) THEN 'TIME_DEPOSIT'
            WHEN c.rn IN (48, 98) THEN 'SUB_ACCOUNT'
            ELSE 'CHECKING'
        END AS account_type,
        CASE
            WHEN c.rn IN (46, 96) THEN 'JPY'
            WHEN c.rn IN (47, 97) THEN 'USD'
            ELSE 'TWD'
        END AS currency,
        DATEADD(DAY, -160 + c.rn, CAST('2026-05-01 09:00:00' AS DATETIME2)) AS created_at
) x
OUTER APPLY (
    SELECT TOP 1 ma.account_number
    FROM #mock_accounts ma
    WHERE ma.customer_id = c.customer_id
      AND ma.account_type = 'CHECKING'
      AND ma.currency = 'TWD'
      AND ma.status = 'ACTIVE'
    ORDER BY ma.account_number
) approved_account;

UPDATE p
SET
    latest_account_application_id = a.id,
    latest_account_application_no = a.application_no,
    latest_applied_account_type = a.account_type,
    latest_applied_currency = a.currency,
    latest_account_application_status = a.status,
    latest_account_application_risk_flag = a.risk_flag,
    latest_account_application_reviewed_at = a.reviewed_at,
    latest_account_application_reviewed_by = a.reviewed_by,
    latest_account_application_reject_reason = a.reject_reason,
    created_account_number = CASE WHEN LEN(a.created_account_number) <= 12 THEN a.created_account_number ELSE LEFT(a.created_account_number, 12) END,
    account_application_synced_at = SYSDATETIME(),
    updated_at = SYSDATETIME()
FROM CUSTOMER_PROFILE p
JOIN ACCOUNT_APPLICATION a ON a.customer_id = p.customer_id;

IF OBJECT_ID('ACCOUNT_STATUS_HISTORY', 'U') IS NOT NULL
BEGIN
    IF NOT EXISTS (SELECT 1 FROM ACCOUNT_STATUS_HISTORY)
    INSERT INTO ACCOUNT_STATUS_HISTORY (
        history_id, account_number, old_status, new_status, change_reason, changed_at, changed_by
    )
    SELECT
        LOWER(CONVERT(CHAR(36), NEWID())),
        account_number,
        NULL,
        status,
        N'Mock data initial status',
        created_at,
        N'總行'
    FROM #mock_accounts
    WHERE account_type <> 'BUSINESS';
END;

IF NOT EXISTS (SELECT 1 FROM FAVORITE_ACCOUNT)
INSERT INTO FAVORITE_ACCOUNT (customer_id, bank_code, account_number, alias, bank_name, created_at, updated_at)
SELECT TOP (24)
    customer_id,
    CASE rn % 4 WHEN 0 THEN '808' WHEN 1 THEN '812' WHEN 2 THEN '700' ELSE '004' END,
    CASE rn % 4
        WHEN 0 THEN '808' + RIGHT(REPLICATE('0', 13) + CAST(7000000000 + rn AS VARCHAR(13)), 13)
        WHEN 1 THEN '812' + RIGHT(REPLICATE('0', 13) + CAST(7000000000 + rn AS VARCHAR(13)), 13)
        WHEN 2 THEN '700' + RIGHT(REPLICATE('0', 13) + CAST(7000000000 + rn AS VARCHAR(13)), 13)
        ELSE '004' + RIGHT(REPLICATE('0', 13) + CAST(7000000000 + rn AS VARCHAR(13)), 13)
    END,
    N'常用轉帳對象',
    CASE rn % 4 WHEN 0 THEN N'玉山商業銀行' WHEN 1 THEN N'台新國際商業銀行' WHEN 2 THEN N'中華郵政股份有限公司' ELSE N'臺灣銀行' END,
    DATEADD(DAY, -120 + rn, CAST('2026-05-13 09:00:00' AS DATETIME2)),
    CAST('2026-05-13 09:00:00' AS DATETIME2)
FROM #customers
WHERE status = 'ACTIVE'
ORDER BY rn;

SELECT
    ROW_NUMBER() OVER (ORDER BY account_number) AS tx_rn,
    account_number,
    customer_id,
    account_type,
    currency,
    balance
INTO #tx_accounts
FROM #mock_accounts
WHERE account_type IN ('CHECKING', 'SAVINGS', 'SUB_ACCOUNT')
  AND status = 'ACTIVE';

DECLARE @accountCount INT = (SELECT COUNT(*) FROM #tx_accounts);
IF @accountCount = 0
    THROW 51102, 'account_mockdata.sql found no active transaction accounts.', 1;

DECLARE
    @i INT = 1,
    @slot INT,
    @counterpartSlot INT,
    @accountNumber VARCHAR(14),
    @currency CHAR(3),
    @currentBalance DECIMAL(19,4),
    @amount DECIMAL(19,4),
    @fee DECIMAL(19,4),
    @totalDebit DECIMAL(19,4),
    @balanceBefore DECIMAL(19,4),
    @balanceAfter DECIMAL(19,4),
    @entryType VARCHAR(10),
    @transactionType VARCHAR(25),
    @counterpartAccount VARCHAR(20),
    @counterpartBankCode VARCHAR(10),
    @counterpartBankName NVARCHAR(50),
    @isInterbank BIT,
    @note NVARCHAR(200),
    @referenceId VARCHAR(30),
    @createdAt DATETIME2(3);

IF NOT EXISTS (SELECT 1 FROM TRANS_LOG)
BEGIN
WHILE @i <= 500
BEGIN
    SET @slot = ((@i - 1) % @accountCount) + 1;
    SET @counterpartSlot = (@slot % @accountCount) + 1;

    SELECT
        @accountNumber = account_number,
        @currency = currency,
        @currentBalance = balance
    FROM #tx_accounts
    WHERE tx_rn = @slot;

    SET @amount = CASE
        WHEN @currency = 'JPY' THEN CAST(500 + ((@i * 137) % 80) * 100 AS DECIMAL(19,4))
        WHEN @currency = 'TWD' THEN CAST(300 + ((@i * 193) % 90) * 100 AS DECIMAL(19,4))
        ELSE CAST(20 + ((@i * 17) % 90) + ((@i % 4) * 0.25) AS DECIMAL(19,4))
    END;
    SET @fee = 0.0000;
    SET @totalDebit = NULL;
    SET @counterpartAccount = NULL;
    SET @counterpartBankCode = NULL;
    SET @counterpartBankName = NULL;
    SET @isInterbank = 0;
    SET @note = N'臨櫃存款';

    IF @i % 17 = 0
    BEGIN
        SET @transactionType = 'INTEREST';
        SET @entryType = 'CREDIT';
        SET @amount = CASE
            WHEN @currency = 'JPY' THEN CAST(1 + (@i % 8) AS DECIMAL(19,4))
            WHEN @currency = 'TWD' THEN CAST(3 + (@i % 20) AS DECIMAL(19,4))
            ELSE CAST(0.10 + (@i % 10) * 0.03 AS DECIMAL(19,4))
        END;
        SET @note = N'活存利息入帳';
    END
    ELSE IF @i % 11 = 0
    BEGIN
        SET @transactionType = 'EXCHANGE';
        SET @entryType = 'CREDIT';
        SET @note = N'換匯入帳';
    END
    ELSE IF @i % 7 = 0
    BEGIN
        SET @transactionType = 'WITHDRAW';
        SET @entryType = 'DEBIT';
        SET @note = N'ATM 提款';
    END
    ELSE IF @i % 5 = 0
    BEGIN
        SET @transactionType = 'TRANSFER';
        SET @entryType = 'DEBIT';
        SET @note = N'轉帳';

        IF @currency = 'TWD' AND @i % 20 = 0
        BEGIN
            SET @isInterbank = 1;
            SET @counterpartBankCode = CASE @i % 4 WHEN 0 THEN '808' WHEN 1 THEN '812' WHEN 2 THEN '700' ELSE '004' END;
            SET @counterpartBankName = CASE @counterpartBankCode
                WHEN '808' THEN N'玉山商業銀行'
                WHEN '812' THEN N'台新國際商業銀行'
                WHEN '700' THEN N'中華郵政股份有限公司'
                ELSE N'臺灣銀行'
            END;
            SET @counterpartAccount = @counterpartBankCode + RIGHT(REPLICATE('0', 13) + CAST(6000000000 + @i AS VARCHAR(13)), 13);
            SET @fee = CASE WHEN @amount <= 1000 THEN 10.0000 ELSE 15.0000 END;
            SET @note = N'跨行轉帳';
        END
        ELSE
        BEGIN
            SET @counterpartBankCode = '909';
            SET @counterpartBankName = N'爪哇銀行';
            SELECT @counterpartAccount = account_number
            FROM #tx_accounts
            WHERE tx_rn = @counterpartSlot;
        END
    END
    ELSE
    BEGIN
        SET @transactionType = 'DEPOSIT';
        SET @entryType = 'CREDIT';
        SET @note = N'薪資或現金存入';
    END

    SET @balanceBefore = @currentBalance;

    IF @entryType = 'DEBIT'
    BEGIN
        SET @totalDebit = @amount + @fee;
        IF @balanceBefore < @totalDebit
        BEGIN
            SET @transactionType = 'DEPOSIT';
            SET @entryType = 'CREDIT';
            SET @fee = 0.0000;
            SET @totalDebit = NULL;
            SET @counterpartAccount = NULL;
            SET @counterpartBankCode = NULL;
            SET @counterpartBankName = NULL;
            SET @isInterbank = 0;
            SET @note = N'餘額不足改為臨櫃存款 mock';
        END
    END

    SET @balanceAfter = CASE
        WHEN @entryType = 'DEBIT' THEN @balanceBefore - ISNULL(@totalDebit, @amount)
        ELSE @balanceBefore + @amount
    END;

    SET @referenceId = 'TXN-20260513-' + RIGHT(REPLICATE('0', 6) + CAST(@i AS VARCHAR(6)), 6);
    SET @createdAt = DATEADD(MINUTE, -(@i * 43), CAST('2026-05-13 10:00:00' AS DATETIME2(3)));

    INSERT INTO TRANS_LOG (
        transaction_id, reference_id, account_number, counterpart_account,
        bank_code, bank_name, counterpart_bank_code, counterpart_bank_name,
        is_interbank, entry_type, transaction_type, amount, fee_amount,
        total_debit_amount, balance_before, balance_after, currency, note, created_at
    ) VALUES (
        LOWER(CONVERT(CHAR(36), NEWID())),
        @referenceId,
        @accountNumber,
        @counterpartAccount,
        '909',
        N'爪哇銀行',
        @counterpartBankCode,
        @counterpartBankName,
        @isInterbank,
        @entryType,
        @transactionType,
        @amount,
        @fee,
        CASE WHEN @entryType = 'DEBIT' THEN @totalDebit ELSE NULL END,
        @balanceBefore,
        @balanceAfter,
        @currency,
        @note,
        @createdAt
    );

    UPDATE #tx_accounts
    SET balance = @balanceAfter
    WHERE account_number = @accountNumber;

    UPDATE #mock_accounts
    SET balance = @balanceAfter,
        changed_at = @createdAt,
        changed_by = 'mock'
    WHERE account_number = @accountNumber;

    SET @i += 1;
END;
END; -- IF NOT EXISTS TRANS_LOG

UPDATE a
SET
    a.balance = m.balance,
    a.changed_at = m.changed_at,
    a.changed_by = m.changed_by
FROM [ACCOUNT] a
JOIN #mock_accounts m ON m.account_number = a.account_number;

IF (SELECT COUNT(*) FROM TRANS_LOG) <> 500
    THROW 51103, 'account_mockdata.sql expected exactly 500 TRANS_LOG rows.', 1;

IF EXISTS (
    SELECT 1
    FROM [ACCOUNT]
    WHERE currency = 'JPY'
      AND (balance <> FLOOR(balance) OR ISNULL(liability, 0) <> FLOOR(ISNULL(liability, 0)))
)
    THROW 51104, 'account_mockdata.sql generated fractional JPY account money.', 1;

IF EXISTS (
    SELECT 1
    FROM TRANS_LOG
    WHERE currency = 'JPY'
      AND (
          amount <> FLOOR(amount)
          OR fee_amount <> FLOOR(fee_amount)
          OR ISNULL(total_debit_amount, 0) <> FLOOR(ISNULL(total_debit_amount, 0))
          OR balance_before <> FLOOR(balance_before)
          OR balance_after <> FLOOR(balance_after)
      )
)
    THROW 51105, 'account_mockdata.sql generated fractional JPY transaction money.', 1;

IF EXISTS (SELECT 1 FROM TRANS_LOG WHERE LEN(reference_id) > 30 OR LEN(account_number) > 14 OR LEN(ISNULL(counterpart_account, '')) > 20)
    THROW 51106, 'account_mockdata.sql generated data longer than account transaction column limits.', 1;

PRINT N'account_mockdata.sql completed: customer accounts and 500 transaction log rows generated.';

DROP TABLE #tx_accounts;
DROP TABLE #mock_accounts;
DROP TABLE #customers;

-- ===== Demo Test Accounts (seq 101-104) =====
-- These 4 accounts are NOT in the TOP(100) auto-generation above.
-- Insert them explicitly with controlled statuses.
INSERT INTO [ACCOUNT] (account_number, customer_id, account_type, currency, balance, liability, interest_rate, status, parent_account_number, created_at, changed_at, created_by, changed_by)
SELECT v.account_number, v.customer_id, v.account_type, v.currency, v.balance, v.liability, v.interest_rate, v.status, NULL, GETDATE(), GETDATE(), 'mock-demo', 'mock-demo'
FROM (VALUES
    ('090483761205', 'DM01NR01', 'CHECKING', 'TWD', 163842.3700, 0.0000, NULL, 'ACTIVE'),
    ('091726394518', 'DM01NR01', 'SAVINGS',  'TWD', 487915.6200, 0.0000, 1.50, 'ACTIVE'),
    ('090617248930', 'DM02NR02', 'CHECKING', 'TWD', 296731.0500, 0.0000, NULL, 'ACTIVE'),
    ('091382905764', 'DM02NR02', 'SAVINGS',  'TWD', 1136842.8100, 0.0000, 1.50, 'ACTIVE'),
    ('090295846731', 'DM03FZ01', 'CHECKING', 'TWD', 68219.4400, 0.0000, NULL, 'FROZEN'),
    ('091604173892', 'DM03FZ01', 'SAVINGS',  'TWD', 327508.1900, 0.0000, 1.50, 'FROZEN'),
    ('090839572416', 'DM04BK01', 'CHECKING', 'TWD', 104376.9200, 0.0000, NULL, 'ACTIVE'),
    ('091158630947', 'DM04BK01', 'SAVINGS',  'TWD', 398641.7300, 0.0000, 1.50, 'ACTIVE')
) AS v(account_number, customer_id, account_type, currency, balance, liability, interest_rate, status)
WHERE NOT EXISTS (SELECT 1 FROM [ACCOUNT] a WHERE a.account_number = v.account_number);

GO
