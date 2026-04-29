CREATE TABLE RiskEventLog (
    LogId BIGINT IDENTITY(1,1) PRIMARY KEY,       -- 流水號主鍵
    
    -- 1. 來源與目標 (Who & Where)
    EventType VARCHAR(50) NOT NULL,               -- 事件類型：'TRANSFER', 'LOAN', 'CREDIT_CARD', 'USER_LOGIN'
    TargetIdentifier VARCHAR(100) NOT NULL,       -- 目標識別碼：可以是帳號(10碼)、卡號(16碼)或用戶ID(UUID)
    
    -- 2. 風控判定結果 (What happened)
    RiskLevel VARCHAR(20) NOT NULL,               -- 風險等級：'LOW' (低), 'MEDIUM' (中), 'HIGH' (高)
    ActionTaken VARCHAR(50) NOT NULL,             -- 系統處置：'PASSED' (放行), 'BLOCKED' (攔截), 'MANUAL_REVIEW' (人工審核)
    TriggerReason NVARCHAR(500),                  -- 觸發原因 (使用 NVARCHAR 支援中文，例如："單筆金額超過50萬")
    
    -- 3. 交易細節 (Details - 允許 NULL，因為有些風控不涉及金額，例如異常登入)
    TransactionAmount DECIMAL(18, 4) NULL,        -- 交易金額 
    
    -- 4. 時間戳記 (When)
    CreatedAt DATETIME DEFAULT GETDATE() NOT NULL -- 發生時間
);

-- 建立索引 (Index) 讓你的「風控儀表板」查詢更快 (加分項)
CREATE INDEX IX_RiskEvent_EventType ON RiskEventLog(EventType);
CREATE INDEX IX_RiskEvent_CreatedAt ON RiskEventLog(CreatedAt DESC);