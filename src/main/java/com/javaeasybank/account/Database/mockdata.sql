SET NOCOUNT ON;

-- 1. 設定參數
DECLARE @TotalAccounts INT = 10000;      -- 預計生成帳戶數
DECLARE @TotalTransactions INT = 20000;  -- 預計生成轉帳次數 (每筆產生 2 筆 LOG)
DECLARE @BatchSize INT = 5000;           -- 批次提交大小 (防止 Transaction Log 撐爆)

-- 2. 帳戶生成模組
DECLARE @CurrentAccount INT = 1;
DECLARE @AccNum VARCHAR(12);
DECLARE @CustId BIGINT;

PRINT '開始生成 ACCOUNT 相關資料...';

BEGIN TRAN;
WHILE @CurrentAccount <= @TotalAccounts
BEGIN
    SET @AccNum = '9000' + RIGHT('00000000' + CAST(@CurrentAccount AS VARCHAR(8)), 8);
    SET @CustId = 10000 + @CurrentAccount;

INSERT INTO ACCOUNT (
    account_number, customer_id, account_type, currency,
    balance, liability, interest_rate, status,
    parent_account_number, created_at, changed_at, created_by, changed_by
) VALUES (
             @AccNum, @CustId, 'CHECKING', 'TWD',
             100000.0000, 0.0000, 0.00150, 'ACTIVE',
             NULL, GETDATE(), GETDATE(), 'system_mock', 'system_mock'
         );

INSERT INTO ACCOUNT_STATUS_HISTORY (
    history_id, account_number, old_status, new_status, change_reason, changed_at, changed_by
) VALUES (
             CAST(NEWID() AS CHAR(36)), @AccNum, 'PENDING', 'ACTIVE', N'大量測試資料建立', GETDATE(), 'system_mock'
         );

INSERT INTO ACCOUNT_DAILY_SNAPSHOTS (
    snapshot_id, account_number, snapshot_date, balance, interest_rate, daily_interest, created_at
) VALUES (
             CAST(NEWID() AS CHAR(36)), @AccNum, CAST(GETDATE() AS DATE), 100000.0000, 0.00150, (100000.0000 * 0.00150 / 365), GETDATE()
         );

-- 批次提交邏輯
IF @CurrentAccount % @BatchSize = 0
BEGIN
COMMIT TRAN;
PRINT '已寫入 ' + CAST(@CurrentAccount AS VARCHAR) + ' 筆帳戶資料...';
BEGIN TRAN;
END

    SET @CurrentAccount = @CurrentAccount + 1;
END
COMMIT TRAN;

PRINT '帳戶生成完畢。開始生成 TRANS_LOG...';

-- 3. 交易紀錄生成模組
DECLARE @SuccessfulTx INT = 0; -- 改為計算成功寫入的次數，確保絕對數量正確
DECLARE @FromAcc VARCHAR(12);
DECLARE @ToAcc VARCHAR(12);
DECLARE @RefId VARCHAR(30);

BEGIN TRAN;
WHILE @SuccessfulTx < @TotalTransactions
BEGIN
    SET @FromAcc = '9000' + RIGHT('00000000' + CAST((ABS(CHECKSUM(NEWID())) % @TotalAccounts) + 1 AS VARCHAR(8)), 8);
    SET @ToAcc = '9000' + RIGHT('00000000' + CAST((ABS(CHECKSUM(NEWID())) % @TotalAccounts) + 1 AS VARCHAR(8)), 8);

    -- 確保不碰撞才執行寫入
    IF @FromAcc <> @ToAcc
BEGIN
        SET @RefId = 'TXN-' + REPLACE(CONVERT(VARCHAR, GETDATE(), 112), '-', '') + '-' +
                     REPLACE(CONVERT(VARCHAR, GETDATE(), 108), ':', '') + '-' + RIGHT(CONVERT(VARCHAR(36), NEWID()), 8);

INSERT INTO TRANS_LOG (
    transaction_id, reference_id, account_number, counterpart_account,
    counterpart_bank_code, entry_type, transaction_type, amount,
    balance_before, balance_after, currency, note, created_at
) VALUES
      (CAST(NEWID() AS CHAR(36)), @RefId, @FromAcc, @ToAcc, NULL, 'DEBIT', 'TRANSFER', 500.0000, 100000.0000, 99500.0000, 'TWD', N'Mock大量轉帳測試', GETDATE()),
      (CAST(NEWID() AS CHAR(36)), @RefId, @ToAcc, @FromAcc, NULL, 'CREDIT', 'TRANSFER', 500.0000, 100000.0000, 100500.0000, 'TWD', N'Mock大量轉帳測試', GETDATE());

SET @SuccessfulTx = @SuccessfulTx + 1;

        -- 批次提交邏輯
        IF @SuccessfulTx % @BatchSize = 0
BEGIN
COMMIT TRAN;
PRINT '已寫入 ' + CAST(@SuccessfulTx AS VARCHAR) + ' 筆轉帳交易...';
BEGIN TRAN;
END
END
END
COMMIT TRAN;

PRINT '所有測試資料生成完畢，交易精確收斂完成。';