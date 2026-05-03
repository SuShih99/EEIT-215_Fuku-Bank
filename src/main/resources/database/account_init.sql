-- ============================================================
-- 1. ACCOUNT 帳戶
-- ============================================================
CREATE TABLE [ACCOUNT] (
    [account_number]        VARCHAR(12)     PRIMARY KEY,            -- 帳戶號碼 (PK, 業務編號, Java端生成)
    [customer_id]           VARCHAR(20)     NOT NULL,               -- 客戶識別碼 (FK → customer_profile)
    [account_type]          VARCHAR(20)     NOT NULL,               -- 帳戶型別 (CHECKING/TIME_DEPOSIT/LOAN/SUB_ACCOUNT)
    [currency]              CHAR(3)         NOT NULL,               -- 幣別 (ISO 4217, 固定3碼)
    [balance]               DECIMAL(19,4)   NULL DEFAULT 0,         -- 餘額 (活存/定存用, 預設0防止NULL運算錯誤)
    [liability]             DECIMAL(19,4)   NULL DEFAULT 0,         -- 負債 (貸款用, 預設0防止NULL運算錯誤)
    [interest_rate]         DECIMAL(7,5)    NULL,                   -- 年利率
    [status]                VARCHAR(20)     NOT NULL,               -- 狀態 (PENDING/ACTIVE/FROZEN/DORMANT/CLOSED)
    [parent_account_number] VARCHAR(12)     NULL,                   -- 父帳戶 (僅子帳戶使用, FK → ACCOUNT)
    [created_at]            DATETIME2       NOT NULL,               -- 建立時間 (Java端生成)
    [changed_at]            DATETIME2       NOT NULL,               -- 最後變更時間
    [created_by]            VARCHAR(20)     NULL,                   -- 建立者 (等員工表再改)
    [changed_by]            VARCHAR(20)     NULL,                   -- 最後變更者 (等員工表再改)

    FOREIGN KEY ([parent_account_number]) REFERENCES [ACCOUNT]([account_number])
    )
    GO

-- ACCOUNT INDEX
CREATE INDEX idx_account_customer ON [ACCOUNT]([customer_id]);
CREATE INDEX idx_account_status ON [ACCOUNT]([status]);
GO

-- ACCOUNT 欄位註解
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '帳戶號碼 (PK, 業務編號, 12碼, Java端生成)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT', @level2type = N'Column', @level2name = 'account_number';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '客戶識別碼 (未來補FK至CUSTOMER表)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT', @level2type = N'Column', @level2name = 'customer_id';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '帳戶型別 (CHECKING/TIME_DEPOSIT/LOAN/SUB_ACCOUNT)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT', @level2type = N'Column', @level2name = 'account_type';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '幣別 (ISO 4217, 如: TWD/USD/JPY)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT', @level2type = N'Column', @level2name = 'currency';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '餘額 (活存/定存適用, 貸款帳戶程式端設NULL, DEFAULT 0防止NULL運算錯誤)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT', @level2type = N'Column', @level2name = 'balance';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '負債 (貸款帳戶適用, 還款時直接扣除, DEFAULT 0防止NULL運算錯誤)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT', @level2type = N'Column', @level2name = 'liability';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '年利率 (活存寫死/定存行員輸入/貸款核貸時寫入)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT', @level2type = N'Column', @level2name = 'interest_rate';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '帳戶狀態 (PENDING/ACTIVE/FROZEN/DORMANT/CLOSED)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT', @level2type = N'Column', @level2name = 'status';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '父帳戶帳號 (僅子帳戶使用, 其他為NULL)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT', @level2type = N'Column', @level2name = 'parent_account_number';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '建立時間 (Java端生成)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT', @level2type = N'Column', @level2name = 'created_at';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '最後變更時間', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT', @level2type = N'Column', @level2name = 'changed_at';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '建立者 (暫用VARCHAR(20), 等員工表主鍵確定後改FK)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT', @level2type = N'Column', @level2name = 'created_by';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '最後變更者 (暫用VARCHAR(20), 等員工表主鍵確定後改FK)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT', @level2type = N'Column', @level2name = 'changed_by';
GO


-- ============================================================
-- 2. ACCOUNT_STATUS_HISTORY 帳戶狀態變更紀錄
-- ============================================================
CREATE TABLE [ACCOUNT_STATUS_HISTORY] (
    [history_id]        CHAR(36)        PRIMARY KEY,                -- 歷史紀錄 ID (UUID, Java端生成)
    [account_number]    VARCHAR(12)     NOT NULL,                   -- 帳戶號碼 (FK)
    [old_status]        VARCHAR(20)     NULL,                       -- 變更前狀態 (首次建立為NULL)
    [new_status]        VARCHAR(20)     NOT NULL,                   -- 變更後狀態
    [change_reason]     NVARCHAR(200)   NOT NULL,                   -- 變更原因 (強制填入, 前端提供預設選項)
    [changed_at]        DATETIME2       NOT NULL,                   -- 變更時間
    [changed_by]        VARCHAR(20)     NULL,                       -- 變更者 (等員工表再改)

    FOREIGN KEY ([account_number]) REFERENCES [ACCOUNT]([account_number])
    )
    GO

-- ACCOUNT_STATUS_HISTORY INDEX
CREATE INDEX idx_ash_account ON [ACCOUNT_STATUS_HISTORY]([account_number]);
CREATE INDEX idx_ash_changed_at ON [ACCOUNT_STATUS_HISTORY]([changed_at]);
GO

-- ACCOUNT_STATUS_HISTORY 欄位註解
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '歷史紀錄 ID (PK, UUID, Java端生成)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT_STATUS_HISTORY', @level2type = N'Column', @level2name = 'history_id';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '帳戶號碼 (FK → ACCOUNT)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT_STATUS_HISTORY', @level2type = N'Column', @level2name = 'account_number';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '變更前狀態 (首次建立帳戶時為NULL)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT_STATUS_HISTORY', @level2type = N'Column', @level2name = 'old_status';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '變更後狀態 (PENDING/ACTIVE/FROZEN/DORMANT/CLOSED)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT_STATUS_HISTORY', @level2type = N'Column', @level2name = 'new_status';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '變更原因 (強制填入, 前端提供預設下拉選項)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT_STATUS_HISTORY', @level2type = N'Column', @level2name = 'change_reason';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '變更時間', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT_STATUS_HISTORY', @level2type = N'Column', @level2name = 'changed_at';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '變更者 (暫用VARCHAR(20), 等員工表主鍵確定後改FK)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT_STATUS_HISTORY', @level2type = N'Column', @level2name = 'changed_by';
GO


-- ============================================================
-- 3. ACCOUNT_DAILY_SNAPSHOTS 帳戶每日快照
-- ============================================================
CREATE TABLE [ACCOUNT_DAILY_SNAPSHOTS] (
    [snapshot_id]       CHAR(36)        PRIMARY KEY,                -- 快照 ID (UUID, Java端生成)
    [account_number]    VARCHAR(12)     NOT NULL,                   -- 帳戶號碼 (FK)
    [snapshot_date]     DATE            NOT NULL,                   -- 快照日期 (每天只拍一次, 用DATE不用DATETIME2)
    [balance]           DECIMAL(19,4)   NOT NULL,                   -- 當日日終餘額
    [interest_rate]     DECIMAL(7,5)    NOT NULL,                   -- 當日適用年利率 (存快照當下的值)
    [daily_interest]    DECIMAL(19,4)   NOT NULL,                   -- 當日產生利息
    [created_at]        DATETIME2       NOT NULL,                   -- 快照建立時間

    FOREIGN KEY ([account_number]) REFERENCES [ACCOUNT]([account_number])
    )
    GO

-- ACCOUNT_DAILY_SNAPSHOTS INDEX
CREATE UNIQUE INDEX idx_ads_account_date ON [ACCOUNT_DAILY_SNAPSHOTS]([account_number], [snapshot_date]);
CREATE INDEX idx_ads_snapshot_date ON [ACCOUNT_DAILY_SNAPSHOTS]([snapshot_date]);
GO

-- ACCOUNT_DAILY_SNAPSHOTS 欄位註解
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '快照 ID (PK, UUID, Java端生成)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT_DAILY_SNAPSHOTS', @level2type = N'Column', @level2name = 'snapshot_id';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '帳戶號碼 (FK → ACCOUNT)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT_DAILY_SNAPSHOTS', @level2type = N'Column', @level2name = 'account_number';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '快照日期 (每天只拍一次, 用DATE不用DATETIME2)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT_DAILY_SNAPSHOTS', @level2type = N'Column', @level2name = 'snapshot_date';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '當日日終餘額 (結帳點當下的ACCOUNT.balance)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT_DAILY_SNAPSHOTS', @level2type = N'Column', @level2name = 'balance';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '當日適用年利率 (存快照當下的值, 未來調整利率不影響歷史)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT_DAILY_SNAPSHOTS', @level2type = N'Column', @level2name = 'interest_rate';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '當日產生利息 (公式: balance × interest_rate ÷ dayCountBasis)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT_DAILY_SNAPSHOTS', @level2type = N'Column', @level2name = 'daily_interest';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '快照建立時間 (Java端生成)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'ACCOUNT_DAILY_SNAPSHOTS', @level2type = N'Column', @level2name = 'created_at';
GO


-- ============================================================
-- 4. TRANS_LOG 交易紀錄
-- ============================================================
CREATE TABLE [TRANS_LOG] (
    [transaction_id]        CHAR(36)        PRIMARY KEY,            -- 交易 ID (UUID, Java端生成, 內部用)
    [reference_id]          VARCHAR(30)     NOT NULL,               -- 業務交易編號 (TXN-yyyyMMdd-HHmmss-8碼hex, 對外用)
    [account_number]        VARCHAR(12)     NOT NULL,               -- 影響帳號 (FK)
    [counterpart_account]   VARCHAR(20)     NULL,                   -- 對手方帳號 (行內12碼/跨行最長16碼, 存提款NULL)
    [counterpart_bank_code] VARCHAR(10)     NULL,                   -- 對手方銀行代碼 (跨行轉帳用, 行內NULL)
    [entry_type]            VARCHAR(10)     NOT NULL,               -- 記帳方向 (DEBIT/CREDIT)
    [transaction_type]      VARCHAR(25)     NOT NULL,               -- 交易類型 (TRANSFER/DEPOSIT/WITHDRAW/INTEREST/LOAN_DISBURSEMENT/LOAN_REPAYMENT)
    [amount]                DECIMAL(19,4)   NOT NULL,               -- 交易金額 (永遠正數, 方向由entry_type決定)
    [balance_before]        DECIMAL(19,4)   NOT NULL,               -- 交易前餘額
    [balance_after]         DECIMAL(19,4)   NOT NULL,               -- 交易後餘額
    [currency]              CHAR(3)         NOT NULL,               -- 幣別 (冗餘欄位, 避免每次JOIN帳戶表)
    [note]                  NVARCHAR(200)   NULL,                   -- 註記 (用NVARCHAR支援中文)
    [created_at]            DATETIME2(3)    NOT NULL,               -- 操作時間 (毫秒級, Java端生成)

    FOREIGN KEY ([account_number]) REFERENCES [ACCOUNT]([account_number])
    )
    GO

-- TRANSACTION INDEX
CREATE INDEX idx_tx_reference ON [TRANSACTION]([reference_id]);
CREATE INDEX idx_tx_account_time ON [TRANSACTION]([account_number], [created_at]);
CREATE INDEX idx_tx_counterpart ON [TRANSACTION]([counterpart_account]);
GO

-- TRANSACTION 欄位註解
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '交易 ID (PK, UUID, Java端生成, 內部關聯用)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'TRANSACTION', @level2type = N'Column', @level2name = 'transaction_id';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '業務交易編號 (格式: TXN-yyyyMMdd-HHmmss-8碼hex, 轉帳時兩筆共用同一個)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'TRANSACTION', @level2type = N'Column', @level2name = 'reference_id';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '影響帳號 (FK → ACCOUNT, 這筆交易影響的帳戶)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'TRANSACTION', @level2type = N'Column', @level2name = 'account_number';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '對手方帳號 (行內12碼/跨行最長16碼, 存提款為NULL)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'TRANSACTION', @level2type = N'Column', @level2name = 'counterpart_account';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '對手方銀行代碼 (跨行轉帳用如004台灣銀行, 行內轉帳為NULL)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'TRANSACTION', @level2type = N'Column', @level2name = 'counterpart_bank_code';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '記帳方向 (DEBIT=扣款/CREDIT=入帳, 銀行不出現負數)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'TRANSACTION', @level2type = N'Column', @level2name = 'entry_type';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '交易類型 (TRANSFER/DEPOSIT/WITHDRAW/INTEREST/LOAN_DISBURSEMENT/LOAN_REPAYMENT)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'TRANSACTION', @level2type = N'Column', @level2name = 'transaction_type';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '交易金額 (永遠正數, 正負由entry_type決定)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'TRANSACTION', @level2type = N'Column', @level2name = 'amount';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '交易前餘額 (貸款帳戶存liability值, 由transaction_type判斷語意)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'TRANSACTION', @level2type = N'Column', @level2name = 'balance_before';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '交易後餘額 (像存摺每行印餘額, 對帳用)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'TRANSACTION', @level2type = N'Column', @level2name = 'balance_after';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '幣別 (ISO 4217, 冗餘欄位避免每次JOIN)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'TRANSACTION', @level2type = N'Column', @level2name = 'currency';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '註記 (用NVARCHAR支援中文備註, 用戶轉帳可填)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'TRANSACTION', @level2type = N'Column', @level2name = 'note';
EXEC sp_addextendedproperty @name = N'Column_Description', @value = '操作時間 (DATETIME2(3)毫秒級, Java端生成)', @level0type = N'Schema', @level0name = 'dbo', @level1type = N'Table', @level1name = 'TRANSACTION', @level2type = N'Column', @level2name = 'created_at';
GO