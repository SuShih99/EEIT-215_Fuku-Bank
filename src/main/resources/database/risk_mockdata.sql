-- ============================================================
-- Risk Module Mock Data (Entity 完全對齊版)
-- ============================================================

-- 清除舊資料
DELETE FROM RISK_EVENT_LOG;

-- 使用 CTE 產生 200 筆風控事件
WITH Numbers AS (
    SELECT TOP 200 ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
    FROM master.sys.all_columns a
             CROSS JOIN master.sys.all_columns b
)
INSERT INTO RISK_EVENT_LOG (
    event_type,
    business_id,        -- Entity 中是 nullable = false，必須補上
    target_identifier,
    risk_level,
    disposition,       -- 對應 Entity 中的 private Disposition disposition
    trigger_reason,
    transaction_amount,
    created_at
)
SELECT
    -- 1. event_type (String)
    CASE (n % 6)
        WHEN 0 THEN 'TRANSFER'
        WHEN 1 THEN 'LOGIN'
        WHEN 2 THEN 'LOAN_APPLY'
        WHEN 3 THEN 'CREATE_CUSTOMER'
        WHEN 4 THEN 'UPDATE_CUSTOMER'
        ELSE 'GENERAL'
        END AS event_type,

    -- 2. business_id (必須有值，模擬業務案號)
    CASE (n % 3)
        WHEN 0 THEN 'TXN-' + CAST(2026000 + n AS VARCHAR)
        WHEN 1 THEN 'CUST-' + CAST(9000 + n AS VARCHAR)
        ELSE 'APPLY-' + CAST(8000 + n AS VARCHAR)
        END AS business_id,

    -- 3. target_identifier
    CASE (n % 4)
        WHEN 0 THEN '822' + CAST(100000000 + n AS VARCHAR)
        WHEN 1 THEN '4579-' + CAST(1000 + n AS VARCHAR) + '-0000-1234'
        ELSE 'USER-' + CAST(5000 + n AS VARCHAR)
        END AS target_identifier,

    -- 4. risk_level (Enum: LOW, MEDIUM, HIGH, SUSPENDED)
    CASE
        WHEN n % 15 = 0 THEN 'SUSPENDED'
        WHEN n % 10 IN (0, 1, 2, 3, 4, 5) THEN 'LOW'
        WHEN n % 10 IN (6, 7, 8) THEN 'MEDIUM'
        ELSE 'HIGH'
END AS risk_level,

    -- 5. disposition (Enum: PASS, REJECT, MANUAL_REVIEW)
    CASE
        WHEN n % 10 IN (0, 1, 2, 3, 4, 5, 6) THEN 'PASS'
        WHEN n % 10 IN (7, 8) THEN 'MANUAL_REVIEW'
        ELSE 'REJECT'
END AS disposition,

    -- 6. trigger_reason
    CASE (n % 8)
        WHEN 0 THEN N'系統例行掃描正常'
        WHEN 1 THEN N'偵測到常用裝置'
        WHEN 2 THEN N'交易頻率略高於平均'
        WHEN 3 THEN N'觸發大額預警規則'
        WHEN 4 THEN N'黑名單特徵比對成功'
        WHEN 5 THEN N'跨境交易風險偵測'
        WHEN 6 THEN N'異常時段登入警示'
        ELSE N'多重帳戶關聯偵測'
END AS trigger_reason,

    -- 7. transaction_amount (18, 4)
    CASE
        WHEN (n % 6) = 1 THEN NULL
        ELSE CAST((RAND(n) * 100000) AS DECIMAL(18, 4))
END AS transaction_amount,

    -- 8. created_at
    DATEADD(SECOND, -n * 4320, GETDATE()) AS created_at
FROM Numbers;

PRINT N'資料已成功生成：200 筆紀錄。欄位已對齊 RiskEventLog Entity。';