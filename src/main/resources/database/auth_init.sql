/*
===============================================================================
Java Easy Bank 系統權限模組 (Auth Module)
===============================================================================
*/

-- [1] 實體部門表 (AUTH_DEPT)
CREATE TABLE AUTH_DEPT (
    dept_id       VARCHAR(10)   PRIMARY KEY,
    dept_code     VARCHAR(20)   NOT NULL UNIQUE, -- 如: CF, CS, CR
    dept_name     NVARCHAR(50)  NOT NULL         -- 如: 消費金融部
);

-- [2] 角色權限表 (AUTH_ROLE)
CREATE TABLE AUTH_ROLE (
    role_id       VARCHAR(10)   PRIMARY KEY,
    dept_id       VARCHAR(10)   NOT NULL,     
    role_code     VARCHAR(20)   NOT NULL UNIQUE, -- 系統代碼 (如: CFSO, CISO)
    role_name     NVARCHAR(50)  NOT NULL,        -- 職務名稱
    perm_level    TINYINT       NOT NULL,        -- 0(Maker), 1(Checker), 2(Approver), 3(Sr.Approver), 4(Admin)
    perm_scope    VARCHAR(10)   NULL,            -- 擴充範圍: 'RO'(唯讀), 'BIZ'(業務), 'SYS'(系統)
    
    CONSTRAINT FK_Role_Dept FOREIGN KEY (dept_id) REFERENCES AUTH_DEPT(dept_id),
    CONSTRAINT CHK_Perm_Level CHECK (perm_level IN (0, 1, 2, 3, 4))
);

-- [3] 員工帳號表 (AUTH_EMP)
CREATE TABLE AUTH_EMP (
    emp_id              VARCHAR(10)     PRIMARY KEY,
    emp_name            NVARCHAR(50)    NOT NULL,
    dept_id             VARCHAR(10)     NOT NULL,     
    role_id             VARCHAR(10)     NOT NULL,     
    email               VARCHAR(100)    NOT NULL UNIQUE, 
    password_hash       VARCHAR(255)    NOT NULL,     
    
    status              VARCHAR(10)     DEFAULT 'ACTIVE',   -- ACTIVE, SUSPENDED, LOCKED
    contract_end_date   DATETIME2       NULL,               -- 約聘到期日
    permission_expire   DATETIME2       NOT NULL,           -- 權限覆審到期日
    
    failed_attempts     TINYINT         DEFAULT 0,          -- 密碼連續錯誤次數
    pwd_updated_at      DATETIME2       DEFAULT GETDATE(),  -- 密碼最後更新時間 (90天換密碼用)
    last_login_date     DATETIME2       NULL,               -- 最後登入時間
    created_at          DATETIME2       DEFAULT GETDATE(),
    updated_at          DATETIME2       DEFAULT GETDATE(),
    
    CONSTRAINT FK_Emp_Dept FOREIGN KEY (dept_id) REFERENCES AUTH_DEPT(dept_id),
    CONSTRAINT FK_Emp_Role FOREIGN KEY (role_id) REFERENCES AUTH_ROLE(role_id)
);

-- [4] 登入日誌表 (AUTH_LOGIN_LOG)
CREATE TABLE AUTH_LOGIN_LOG (
    log_id          BIGINT IDENTITY(1,1) PRIMARY KEY, 
    attempt_email   VARCHAR(100)    NOT NULL,         -- 紀錄當下輸入的信箱
    emp_id          VARCHAR(10)     NULL,             -- 允許 NULL (防假帳號登入報錯)
    login_time      DATETIME2       DEFAULT GETDATE(),
    login_result    VARCHAR(10)     NOT NULL,         
    fail_reason     NVARCHAR(200)   NULL,             
    ip_address      VARCHAR(45)     NULL,             
    
    CONSTRAINT FK_Log_Emp FOREIGN KEY (emp_id) REFERENCES AUTH_EMP(emp_id)
);

-- [5] 操作行為日誌表 (AUTH_ACTION_LOG)
CREATE TABLE AUTH_ACTION_LOG (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    emp_id          VARCHAR(10)     NULL,
    emp_name        NVARCHAR(50)    NULL,
    action          VARCHAR(50)     NOT NULL,
    target          VARCHAR(50)     NULL,
    details         NVARCHAR(500)   NULL,
    action_time     DATETIME2       DEFAULT GETDATE(),
    ip_address      VARCHAR(45)     NULL
);
GO
