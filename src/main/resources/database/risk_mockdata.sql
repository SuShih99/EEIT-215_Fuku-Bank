-- ============================================================
-- Risk Module Mock Data (Entity 完全對齊版)
-- ============================================================

-- 使用 CTE 產生 200 筆風控事件
WITH Numbers AS (SELECT TOP 200 ROW_NUMBER() OVER (ORDER BY (SELECT NULL)) AS n
                 FROM master.sys.all_columns a
                          CROSS JOIN master.sys.all_columns b)
INSERT
INTO RISK_EVENT_LOG (
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
END
AS risk_level,

    -- 5. disposition (Enum: PASS, REJECT, MANUAL_REVIEW)
    CASE
        WHEN n % 10 IN (0, 1, 2, 3, 4, 5, 6) THEN 'PASS'
        WHEN n % 10 IN (7, 8) THEN 'MANUAL_REVIEW'
        ELSE 'REJECT'
END
AS disposition,

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
END
AS trigger_reason,

    -- 7. transaction_amount (18, 4)
    CASE
        WHEN (n % 6) = 1 THEN NULL
        ELSE CAST((RAND(n) * 100000) AS DECIMAL(18, 4))
END
AS transaction_amount,

    -- 8. created_at
    DATEADD(SECOND, -n * 4320, GETDATE()) AS created_at
FROM Numbers;

PRINT
N'資料已成功生成：200 筆紀錄。欄位已對齊 RiskEventLog Entity。';
INSERT INTO [java_easy_bank].[dbo].[customer_credit_info] ([annual_income], [external_score], [final_score], [has_real_estate], [is_pep],
    [other_bank_debt], [last_updated_at], [risk_level], [customer_id], [fund_source],
    [occupation], [job])
VALUES
    (940000.00, 618, 65, 1, 0, 316148.77, '2026-05-17 17:00:24.4097539', 'MEDIUM', 'A6R3M8J2', 'SAVINGS', 'SERVICE_SALES', '物流運輸業'),
    (880000.00, 530, 58, 0, 0, 198784.42, '2026-05-17 17:00:24.4309143', 'MEDIUM', 'B3N8T5P9', 'SAVINGS', 'SERVICE_SALES', '物流運輸業'),
    (580000.00, 544, 60, 1, 0, 186647.12, '2026-05-17 17:00:24.4215010', 'MEDIUM', 'B9P5N2W6', 'SAVINGS', 'SERVICE_SALES', '餐飲服務業'),
    (1300000.00, 698, 78, 0, 0, 169364.39, '2026-05-17 17:00:24.4225328', 'LOW', 'C6T8R4J3', 'SAVINGS', 'PROFESSIONAL', '法律會計業'),
    (700000.00, 543, 59, 0, 0, 177140.35, '2026-05-17 17:00:24.4309143', 'MEDIUM', 'C9W2M6R4', 'SAVINGS', 'SERVICE_SALES', '餐飲服務業'),
    (760000.00, 542, 59, 1, 0, 288162.23, '2026-05-17 17:00:24.4107578', 'MEDIUM', 'D5Q9T2W7', 'SAVINGS', 'SERVICE_SALES', '餐飲服務業'),
    (1500000.00, 617, 48, 0, 0, 1846024.21, '2026-05-17 17:00:24.4225328', 'MEDIUM', 'E2V7D9M5', 'BUSINESS_INCOME', 'TECHNICIAN', '建築營造業'),
    (1420000.00, 657, 75, 1, 0, 434111.33, '2026-05-17 17:00:24.4319813', 'LOW', 'E5J7Q3D8', 'SAVINGS', 'PROFESSIONAL', '法律會計業'),
    (1320000.00, 624, 59, 1, 0, 1744697.25, '2026-05-17 17:00:24.4319813', 'MEDIUM', 'F2P9V4K6', 'BUSINESS_INCOME', 'TECHNICIAN', '建築營造業'),
    (1180000.00, 748, 82, 0, 0, 284252.96, '2026-05-17 17:00:24.4118864', 'LOW', 'F7V4C8N2', 'SAVINGS', 'PROFESSIONAL', '法律會計業'),
    (1380000.00, 682, 74, 0, 0, 296127.33, '2026-05-17 17:00:24.4118864', 'LOW', 'G3K6P9M5', 'BUSINESS_INCOME', 'TECHNICIAN', '建築營造業'),
    (1020000.00, 659, 65, 0, 0, 474998.32, '2026-05-17 17:00:24.4333257', 'MEDIUM', 'G8A5C2N7', 'SAVINGS', 'PROFESSIONAL', '資訊科技業'),
    (1200000.00, 651, 74, 0, 0, 209117.12, '2026-05-17 17:00:24.4333257', 'LOW', 'H4D7R9M3', 'SAVINGS', 'PROFESSIONAL', '金融保險業'),
    (900000.00, 676, 73, 1, 0, 328426.19, '2026-05-17 17:00:24.4037683', 'LOW', 'H7C2P8D4', 'SAVINGS', 'TECHNICIAN', '製造業'),
    (960000.00, 680, 83, 1, 0, 257354.96, '2026-05-17 17:00:24.4345166', 'LOW', 'J6K3W8Q5', 'SAVINGS', 'TECHNICIAN', '製造業'),
    (1080000.00, 699, 78, 1, 0, 487177.00, '2026-05-17 17:00:24.4129161', 'LOW', 'J8R2D5A7', 'SAVINGS', 'PROFESSIONAL', '資訊科技業'),
    (960000.00, 607, 58, 0, 0, 422309.72, '2026-05-17 17:00:24.4052452', 'MEDIUM', 'K2T8B4R7', 'SAVINGS', 'CLERICAL', '教育服務業'),
    (1260000.00, 722, 80, 0, 0, 293944.09, '2026-05-17 17:00:24.4139313', 'LOW', 'L4N9T6Q3', 'SAVINGS', 'PROFESSIONAL', '金融保險業'),
    (1140000.00, 512, 52, 1, 0, 563217.63, '2026-05-17 17:00:24.4345166', 'MEDIUM', 'L9M2T7A4', 'SAVINGS', 'OTHER', '醫療保健業'),
    (540000.00, 551, 60, 1, 0, 162384.18, '2026-05-17 17:00:24.4052452', 'MEDIUM', 'M9A5D2Q8', 'SAVINGS', 'SERVICE_SALES', '零售服務業'),
    (1020000.00, 709, 76, 0, 1, 291915.60, '2026-05-17 17:00:24.4149566', 'HIGH', 'N2W5H8C4', 'SAVINGS', 'TECHNICIAN', '製造業'),
    (720000.00, 658, 72, 1, 0, 349215.92, '2026-05-17 17:00:24.4355301', 'LOW', 'N5V8C3P6', 'SAVINGS', 'CLERICAL', '教育服務業'),
    (880000.00, 625, 69, 0, 0, 80669.15, '2026-05-17 17:00:24.4066606', 'MEDIUM', 'P4W7N6C3', 'SAVINGS', 'CLERICAL', '公務機關'),
    (600000.00, 610, 75, 1, 0, 18246.45, '2026-05-17 17:00:24.4355301', 'LOW', 'P7Q4J9D2', 'SAVINGS', 'SERVICE_SALES', '零售服務業'),
    (940000.00, 647, 71, 0, 0, 90866.08, '2026-05-17 17:00:24.4355301', 'LOW', 'Q2R6M8W5', 'SAVINGS', 'CLERICAL', '公務機關'),
    (1200000.00, 734, 71, 0, 0, 367013.78, '2026-05-17 17:00:24.4235346', 'LOW', 'Q5H3K8A7', 'SAVINGS', 'PROFESSIONAL', '資訊科技業'),
    (433534.00, 717, 68, 0, 0, 225437.00, '2026-05-17 17:00:24.3940111', 'MEDIUM', 'Q8M4T7K2', 'SAVINGS', 'CLERICAL', '資訊科技業'),
    (1140000.00, 726, 80, 1, 0, 439442.93, '2026-05-17 17:00:24.4026984', 'LOW', 'R5N9W3A6', 'SAVINGS', 'PROFESSIONAL', '金融保險業'),
    (1080000.00, 742, 81, 1, 0, 452550.83, '2026-05-17 17:00:24.4235346', 'LOW', 'R8J6N2C4', 'SAVINGS', 'PROFESSIONAL', '金融保險業'),
    (1380000.00, 750, 84, 0, 0, 251155.83, '2026-05-17 17:00:24.4370350', 'LOW', 'R9T3A7K4', 'BUSINESS_INCOME', 'LEGISLATOR_MANAGER', '自營商'),
    (1000000.00, 605, 74, 1, 0, 195213.75, '2026-05-17 17:00:24.4370350', 'LOW', 'S4C8N5V2', 'SAVINGS', 'SERVICE_SALES', '物流運輸業'),
    (900000.00, 532, 54, 0, 0, 23434.66, '2026-05-17 17:00:24.4149566', 'MEDIUM', 'S7A3K9P6', 'SAVINGS', 'OTHER', '醫療保健業'),
    (840000.00, 590, 76, 1, 0, 7781.42, '2026-05-17 17:00:24.4256305', 'LOW', 'T3M9P5W7', 'SAVINGS', 'TECHNICIAN', '製造業'),
    (520000.00, 546, 50, 0, 0, 206926.91, '2026-05-17 17:00:24.4381931', 'MEDIUM', 'T6D2P9M7', 'SAVINGS', 'SERVICE_SALES', '餐飲服務業'),
    (1320000.00, 713, 61, 0, 0, 1314847.22, '2026-05-17 17:00:24.4097539', 'MEDIUM', 'T8H2K5V9', 'BUSINESS_INCOME', 'LEGISLATOR_MANAGER', '自營商'),
    (780000.00, 681, 73, 0, 0, 91524.11, '2026-05-17 17:00:24.4159667', 'LOW', 'U5D8M2R4', 'SAVINGS', 'CLERICAL', '教育服務業'),
    (1240000.00, 743, 81, 0, 0, 28185.42, '2026-05-17 17:00:24.4381931', 'LOW', 'U8H5Q3R6', 'SAVINGS', 'PROFESSIONAL', '法律會計業'),
    (1440000.00, 680, 53, 0, 0, 1239361.87, '2026-05-17 17:00:24.4414904', 'MEDIUM', 'V3K7W4A9', 'BUSINESS_INCOME', 'TECHNICIAN', '建築營造業'),
    (1080000.00, 497, 51, 1, 0, 404440.63, '2026-05-17 17:00:24.4037683', 'MEDIUM', 'V6J3X9M5', 'SAVINGS', 'OTHER', '醫療保健業'),
    (1020000.00, 562, 46, 0, 0, 462511.50, '2026-05-17 17:00:24.4256305', 'MEDIUM', 'V7A4D8Q2', 'SAVINGS', 'OTHER', '醫療保健業'),
    (900000.00, 645, 71, 1, 0, 382931.05, '2026-05-17 17:00:24.4266559', 'LOW', 'W2K6T9N5', 'SAVINGS', 'CLERICAL', '教育服務業'),
    (1140000.00, 667, 75, 0, 0, 145383.06, '2026-05-17 17:00:24.4414904', 'LOW', 'W6M2C8D5', 'SAVINGS', 'PROFESSIONAL', '資訊科技業'),
    (660000.00, 533, 59, 1, 0, 268952.19, '2026-05-17 17:00:24.4159667', 'MEDIUM', 'W9F4T7N2', 'SAVINGS', 'SERVICE_SALES', '零售服務業'),
    (1000000.00, 629, 69, 0, 0, 275041.78, '2026-05-17 17:00:24.4173727', 'MEDIUM', 'X3J6Q8C5', 'SAVINGS', 'CLERICAL', '公務機關'),
    (780000.00, 606, 64, 0, 0, 169850.32, '2026-05-17 17:00:24.4266559', 'MEDIUM', 'X8C3R7M4', 'SAVINGS', 'SERVICE_SALES', '零售服務業'),
    (1320000.00, 705, 78, 0, 0, 288281.30, '2026-05-17 17:00:24.4425705', 'LOW', 'X9N5T3Q7', 'SAVINGS', 'PROFESSIONAL', '金融保險業'),
    (820000.00, 595, 67, 0, 0, 235274.20, '2026-05-17 17:00:24.4277177', 'MEDIUM', 'Y5Q9V2H6', 'SAVINGS', 'CLERICAL', '公務機關'),
    (1440000.00, 721, 62, 0, 0, 1532013.58, '2026-05-17 17:00:24.4204681', 'MEDIUM', 'Y8L2V5D9', 'BUSINESS_INCOME', 'LEGISLATOR_MANAGER', '自營商'),
    (760000.00, 549, 70, 1, 0, 93067.79, '2026-05-17 17:00:24.4204681', 'LOW', 'Z4M7A3K8', 'SAVINGS', 'SERVICE_SALES', '物流運輸業'),
    (1260000.00, 745, 74, 0, 0, 532161.01, '2026-05-17 17:00:24.4287313', 'LOW', 'Z7D4A8K3', 'BUSINESS_INCOME', 'LEGISLATOR_MANAGER', '自營商');