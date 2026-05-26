/*
===============================================================================
Java Easy Bank Account Mock Data
- Requires customer_insert.sql first
- Creates realistic customer accounts from 100 customer profiles
- Generates exactly 10000 transaction log rows
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

-- Wang Daming demo FX accounts: realistic small travel/investment balances.
UPDATE #mock_accounts
SET
    balance = 3280.5000,
    changed_at = CAST('2026-05-13 09:00:00' AS DATETIME2),
    changed_by = N'總行'
WHERE customer_id = 'Q8M4T7K2'
  AND account_type = 'SAVINGS'
  AND currency = 'USD';

INSERT INTO #mock_accounts (
    account_number, customer_id, account_type, currency, balance, liability,
    interest_rate, status, parent_account_number, created_at, changed_at, created_by, changed_by
)
SELECT
    v.account_number,
    v.customer_id,
    v.account_type,
    v.currency,
    v.balance,
    v.liability,
    v.interest_rate,
    v.status,
    NULL,
    v.created_at,
    CAST('2026-05-13 09:00:00' AS DATETIME2),
    N'總行',
    N'總行'
FROM (VALUES
    ('071260501392', 'Q8M4T7K2', 'SAVINGS', 'JPY', CAST(186000.0000 AS DECIMAL(19,4)), 0.0000, 0.00100, 'ACTIVE', CAST('2026-02-16 09:00:00' AS DATETIME2)),
    ('071260501840', 'Q8M4T7K2', 'SAVINGS', 'EUR', CAST(950.7500 AS DECIMAL(19,4)), 0.0000, 0.00250, 'ACTIVE', CAST('2026-03-04 09:00:00' AS DATETIME2)),
    ('071260501826', 'Q8M4T7K2', 'SAVINGS', 'GBP', CAST(420.2500 AS DECIMAL(19,4)), 0.0000, 0.00250, 'ACTIVE', CAST('2026-03-18 09:00:00' AS DATETIME2))
) AS v(account_number, customer_id, account_type, currency, balance, liability, interest_rate, status, created_at)
JOIN #customers c ON c.customer_id = v.customer_id
WHERE c.status = 'ACTIVE'
  AND NOT EXISTS (
      SELECT 1
      FROM #mock_accounts m
      WHERE m.account_number = v.account_number
  );

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
    CASE
        WHEN customer_id = 'Q8M4T7K2' THEN CAST(2500000.0000 AS DECIMAL(19,4))
        ELSE CAST(80000 + ((rn * 67411) % 3600000) AS DECIMAL(19,4))
    END,
    CASE WHEN customer_id = 'Q8M4T7K2' THEN 0.01800 ELSE 0.02100 END,
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

-- Additional Wang Daming loan account: production logic allocates 902... when 901... already exists.
INSERT INTO #mock_accounts (
    account_number, customer_id, account_type, currency, balance, liability,
    interest_rate, status, parent_account_number, created_at, changed_at, created_by, changed_by
)
SELECT
    '902'
        + RIGHT('00' + CAST(ASCII(UPPER(LEFT(id_number, 1))) - ASCII('A') + 1 AS VARCHAR(2)), 2)
        + SUBSTRING(id_number, 2, LEN(id_number)),
    customer_id,
    'LOAN',
    'TWD',
    0.0000,
    CAST(150000.0000 AS DECIMAL(19,4)),
    0.04000,
    'ACTIVE',
    NULL,
    CAST('2026-05-22 11:00:00' AS DATETIME2),
    CAST('2026-05-22 12:00:00' AS DATETIME2),
    N'總行',
    N'總行'
FROM #customers
WHERE customer_id = 'Q8M4T7K2'
  AND status = 'ACTIVE';

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

-- Keep Wang Daming's demo loan accounts aligned on repeated mock-data runs.
UPDATE a
SET
    a.liability = m.liability,
    a.interest_rate = m.interest_rate,
    a.status = m.status,
    a.changed_at = m.changed_at,
    a.changed_by = m.changed_by
FROM [ACCOUNT] a
JOIN #mock_accounts m
  ON m.account_number = a.account_number
WHERE m.customer_id = 'Q8M4T7K2'
  AND m.account_type = 'LOAN';

INSERT INTO [ACCOUNT] (
    account_number, customer_id, account_type, currency, balance, liability,
    interest_rate, status, parent_account_number, created_at, changed_at, created_by, changed_by
)
SELECT
    m.account_number, m.customer_id, m.account_type, m.currency, m.balance, m.liability,
    m.interest_rate, m.status, m.parent_account_number, m.created_at, m.changed_at, m.created_by, m.changed_by
FROM #mock_accounts m
WHERE m.customer_id = 'Q8M4T7K2'
  AND m.account_type = 'LOAN'
  AND NOT EXISTS (
      SELECT 1
      FROM [ACCOUNT] a
      WHERE a.account_number = m.account_number
  );

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
BEGIN
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

    UPDATE FAVORITE_ACCOUNT
    SET bank_code = '822',
        account_number = '820991991',
        bank_name = N'中國信託商業銀行',
        created_at = CAST('2026-01-14 09:00:00' AS DATETIME2),
        updated_at = CAST('2026-01-14 09:00:00' AS DATETIME2)
    WHERE customer_id = 'Q8M4T7K2';
END;

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
WHILE @i <= 9880
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

    SET @createdAt = DATEADD(MINUTE, -(@i * 43), CAST('2026-05-13 10:00:00' AS DATETIME2(3)));
    SET @referenceId = 'TXN-' + CONVERT(VARCHAR(8), @createdAt, 112) + '-' + REPLACE(CONVERT(VARCHAR(8), @createdAt, 108), ':', '') + '-' + LOWER(LEFT(CONVERT(CHAR(36), NEWID()), 8));

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

DECLARE @wangTransactions TABLE (
    tx_rn INT IDENTITY(1,1) PRIMARY KEY,
    account_type VARCHAR(20) NOT NULL,
    currency CHAR(3) NOT NULL,
    entry_type VARCHAR(10) NOT NULL,
    transaction_type VARCHAR(25) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    fee_amount DECIMAL(19,4) NOT NULL,
    counterpart_account VARCHAR(20) NULL,
    counterpart_bank_code VARCHAR(10) NULL,
    counterpart_bank_name NVARCHAR(50) NULL,
    is_interbank BIT NOT NULL,
    note NVARCHAR(200) NOT NULL,
    created_at DATETIME2(3) NOT NULL
);

DECLARE
    @wangMonth INT = 0,
    @wangBaseDate DATETIME2(3),
    @vipFxCurrency CHAR(3),
    @vipFxAmount DECIMAL(19,4),
    @vipFxNote NVARCHAR(200);

WHILE @wangMonth < 12
BEGIN
    SET @wangBaseDate = DATEADD(MONTH, @wangMonth, CAST('2025-06-01 00:00:00.000' AS DATETIME2(3)));

    INSERT INTO @wangTransactions (
        account_type, currency, entry_type, transaction_type, amount, fee_amount,
        counterpart_account, counterpart_bank_code, counterpart_bank_name,
        is_interbank, note, created_at
    )
    VALUES
    ('CHECKING', 'TWD', 'CREDIT', 'DEPOSIT', CAST(82000 + (@wangMonth % 4) * 1500 AS DECIMAL(19,4)), 0.0000, NULL, NULL, NULL, 0, N'薪資入帳', DATEADD(HOUR, 9, DATEADD(DAY, 5, @wangBaseDate))),
    ('CHECKING', 'TWD', 'DEBIT', 'LOAN_REPAYMENT', CAST(23500 + (@wangMonth % 3) * 500 AS DECIMAL(19,4)), 0.0000, '90101100260501', '909', N'福庫銀行', 0, N'房貸自動扣繳', DATEADD(HOUR, 8, DATEADD(DAY, 8, @wangBaseDate))),
    ('CHECKING', 'TWD', 'DEBIT', 'CARD_PAYMENT', CAST(12800 + (@wangMonth % 5) * 920 AS DECIMAL(19,4)), 0.0000, '80100000000001', '909', N'福庫銀行', 0, N'信用卡自動扣款', DATEADD(HOUR, 8, DATEADD(DAY, 12, @wangBaseDate))),
    ('CHECKING', 'TWD', 'DEBIT', 'TRANSFER', CAST(12000 + (@wangMonth % 4) * 1000 AS DECIMAL(19,4)), 15.0000, '812000070000001', '812', N'台新國際商業銀行', 1, N'家庭生活費轉帳', DATEADD(HOUR, 20, DATEADD(DAY, 15, @wangBaseDate))),
    ('CHECKING', 'TWD', 'DEBIT', 'TRANSFER', CAST(15000 + (@wangMonth % 3) * 2000 AS DECIMAL(19,4)), 0.0000, '071260501840', '909', N'福庫銀行', 0, N'理財資金調撥', DATEADD(HOUR, 10, DATEADD(DAY, 18, @wangBaseDate))),
    ('CHECKING', 'TWD', 'CREDIT', 'INTEREST', CAST(18 + (@wangMonth % 6) AS DECIMAL(19,4)), 0.0000, NULL, NULL, NULL, 0, N'活存利息入帳', DATEADD(HOUR, 1, DATEADD(DAY, 22, @wangBaseDate))),
    ('SAVINGS', 'USD', 'CREDIT', 'EXCHANGE', CAST(220 + (@wangMonth % 5) * 35 + 0.50 AS DECIMAL(19,4)), 0.0000, NULL, NULL, NULL, 0, N'美元定期換匯入帳', DATEADD(HOUR, 10, DATEADD(DAY, 19, @wangBaseDate))),
    ('SAVINGS', 'JPY', 'CREDIT', 'EXCHANGE', CAST(30000 + (@wangMonth % 6) * 3000 AS DECIMAL(19,4)), 0.0000, NULL, NULL, NULL, 0, N'日圓旅遊基金換匯', DATEADD(HOUR, 10, DATEADD(DAY, 20, @wangBaseDate))),
    ('SAVINGS',
     CASE WHEN @wangMonth % 2 = 0 THEN 'EUR' ELSE 'GBP' END,
     'CREDIT',
     'EXCHANGE',
     CASE WHEN @wangMonth % 2 = 0
          THEN CAST(160 + (@wangMonth % 4) * 18 + 0.75 AS DECIMAL(19,4))
          ELSE CAST(90 + (@wangMonth % 4) * 12 + 0.25 AS DECIMAL(19,4))
     END,
     0.0000, NULL, NULL, NULL, 0,
     CASE WHEN @wangMonth % 2 = 0 THEN N'歐元分批換匯入帳' ELSE N'英鎊分批換匯入帳' END,
     DATEADD(HOUR, 14, DATEADD(DAY, 21, @wangBaseDate)));

    SET @vipFxCurrency = CASE @wangMonth % 4 WHEN 0 THEN 'USD' WHEN 1 THEN 'JPY' WHEN 2 THEN 'EUR' ELSE 'GBP' END;
    SET @vipFxAmount = CASE @vipFxCurrency
        WHEN 'USD' THEN CAST(80 + (@wangMonth % 3) * 20 AS DECIMAL(19,4))
        WHEN 'JPY' THEN CAST(12000 + (@wangMonth % 3) * 4000 AS DECIMAL(19,4))
        WHEN 'EUR' THEN CAST(70 + (@wangMonth % 3) * 15 + 0.25 AS DECIMAL(19,4))
        ELSE CAST(45 + (@wangMonth % 3) * 10 AS DECIMAL(19,4))
    END;
    SET @vipFxNote = CASE @vipFxCurrency
        WHEN 'USD' THEN N'美元現鈔提領'
        WHEN 'JPY' THEN N'日圓旅遊現鈔提領'
        WHEN 'EUR' THEN N'歐元資金調撥'
        ELSE N'英鎊資金調撥'
    END;

    INSERT INTO @wangTransactions (
        account_type, currency, entry_type, transaction_type, amount, fee_amount,
        counterpart_account, counterpart_bank_code, counterpart_bank_name,
        is_interbank, note, created_at
    )
    VALUES (
        'SAVINGS',
        @vipFxCurrency,
        'DEBIT',
        CASE WHEN @vipFxCurrency IN ('USD', 'JPY') THEN 'WITHDRAW' ELSE 'TRANSFER' END,
        @vipFxAmount,
        0.0000,
        CASE WHEN @vipFxCurrency IN ('USD', 'JPY') THEN NULL ELSE '071260501840' END,
        CASE WHEN @vipFxCurrency IN ('USD', 'JPY') THEN NULL ELSE '909' END,
        CASE WHEN @vipFxCurrency IN ('USD', 'JPY') THEN NULL ELSE N'福庫銀行' END,
        0,
        @vipFxNote,
        DATEADD(HOUR, 16, DATEADD(DAY, 22, @wangBaseDate))
    );

    SET @wangMonth += 1;
END;

DECLARE
    @wangTxIndex INT = 1,
    @wangTxCount INT = (SELECT COUNT(*) FROM @wangTransactions),
    @wangAccountType VARCHAR(20);

IF @wangTxCount <> 120
    THROW 51108, 'account_mockdata.sql expected exactly 120 Wang Daming VIP transaction rows.', 1;

WHILE @wangTxIndex <= @wangTxCount
BEGIN
    SET @accountNumber = NULL;

    SELECT
        @wangAccountType = account_type,
        @currency = currency,
        @entryType = entry_type,
        @transactionType = transaction_type,
        @amount = amount,
        @fee = fee_amount,
        @counterpartAccount = counterpart_account,
        @counterpartBankCode = counterpart_bank_code,
        @counterpartBankName = counterpart_bank_name,
        @isInterbank = is_interbank,
        @note = note,
        @createdAt = created_at
    FROM @wangTransactions
    WHERE tx_rn = @wangTxIndex;

    SELECT TOP 1
        @accountNumber = account_number,
        @currentBalance = balance
    FROM #tx_accounts
    WHERE customer_id = 'Q8M4T7K2'
      AND account_type = @wangAccountType
      AND currency = @currency
    ORDER BY account_number;

    IF @accountNumber IS NULL
        THROW 51107, 'account_mockdata.sql expected Wang Daming demo transaction account.', 1;

    SET @balanceBefore = @currentBalance;
    SET @totalDebit = CASE WHEN @entryType = 'DEBIT' THEN @amount + @fee ELSE NULL END;
    SET @balanceAfter = CASE
        WHEN @entryType = 'DEBIT' THEN @balanceBefore - @totalDebit
        ELSE @balanceBefore + @amount
    END;
    SET @referenceId = 'TXN-' + CONVERT(VARCHAR(8), @createdAt, 112) + '-' + REPLACE(CONVERT(VARCHAR(8), @createdAt, 108), ':', '') + '-' + LOWER(LEFT(CONVERT(CHAR(36), NEWID()), 8));

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
        N'福庫銀行',
        @counterpartBankCode,
        @counterpartBankName,
        @isInterbank,
        @entryType,
        @transactionType,
        @amount,
        @fee,
        @totalDebit,
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

    SET @wangTxIndex += 1;
END;
END; -- IF NOT EXISTS TRANS_LOG

UPDATE a
SET
    a.balance = m.balance,
    a.changed_at = m.changed_at,
    a.changed_by = m.changed_by
FROM [ACCOUNT] a
JOIN #mock_accounts m ON m.account_number = a.account_number;

IF (SELECT COUNT(*) FROM TRANS_LOG) <> 10000
    THROW 51103, 'account_mockdata.sql expected exactly 10000 TRANS_LOG rows.', 1;

IF EXISTS (
    SELECT 1
    FROM TRANS_LOG
    WHERE created_at >= CAST('2026-05-24 00:00:00.000' AS DATETIME2(3))
)
    THROW 51109, 'account_mockdata.sql generated future-dated transactions.', 1;

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

PRINT N'account_mockdata.sql completed: customer accounts and 10000 transaction log rows generated.';

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
