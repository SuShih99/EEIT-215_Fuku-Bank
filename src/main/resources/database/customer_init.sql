/*
===============================================================================
Java Easy Bank 客戶模組 (Customer Module)
===============================================================================
*/

-- [1] 進件申請表
CREATE TABLE CUSTOMER_APPLICATION (
    case_id VARCHAR(20) PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL,
    cif VARCHAR(20) NULL,
    name NVARCHAR(50) NOT NULL,
    birthday DATE NOT NULL,
    gender CHAR(1) NOT NULL,
    id_number VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address NVARCHAR(255) NOT NULL,
    username VARCHAR(50) NULL,
    password VARCHAR(255) NULL,
    avatar_url VARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',
    reset_pwd_token VARCHAR(255) NULL,
    token_expiry DATETIME2 NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT CHK_App_Gender CHECK (gender IN ('M', 'F'))
);

-- [2] 正式客戶視圖
CREATE TABLE CUSTOMER_PROFILE (
    customer_id VARCHAR(20) NOT NULL PRIMARY KEY, -- 預設建立叢集索引 (Clustered Index)
    cif VARCHAR(20) NOT NULL UNIQUE,
    id_number VARCHAR(20) NOT NULL UNIQUE,
    name NVARCHAR(50) NOT NULL,
    birthday DATE NOT NULL,
    gender CHAR(1) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL UNIQUE,
    address NVARCHAR(255) NOT NULL,
    nationality VARCHAR(10) NULL,
    registered_address NVARCHAR(255) NULL,
    current_address NVARCHAR(255) NULL,
    occupation NVARCHAR(50) NULL,
    employer NVARCHAR(100) NULL,
    estimated_monthly_tx INT NULL,
    account_purpose VARCHAR(30) NULL,
    fund_source VARCHAR(30) NULL,
    tax_residency VARCHAR(10) NULL,
    is_pep BIT NOT NULL DEFAULT 0,
    id_front_url VARCHAR(255) NULL,
    id_back_url VARCHAR(255) NULL,
    second_id_url VARCHAR(255) NULL,
    latest_account_application_id BIGINT NULL,
    latest_account_application_no VARCHAR(30) NULL,
    latest_applied_account_type VARCHAR(20) NULL,
    latest_applied_currency VARCHAR(3) NULL,
    latest_account_application_status VARCHAR(20) NULL,
    latest_account_application_risk_flag VARCHAR(30) NULL,
    latest_account_application_reviewed_at DATETIME2 NULL,
    latest_account_application_reviewed_by VARCHAR(50) NULL,
    latest_account_application_reject_reason NVARCHAR(500) NULL,
    created_account_number VARCHAR(12) NULL,
    account_application_synced_at DATETIME2 NULL,
    avatar_url VARCHAR(255) NULL,
    job NVARCHAR(100) NULL,
    annual_income INT NULL,
    risk_level VARCHAR(20) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT CHK_Profile_Gender CHECK (gender IN ('M', 'F'))
);

-- [3] 客戶認證表（帳號密碼 / JWT）
CREATE TABLE CUSTOMER_AUTH (
    auth_id VARCHAR(20) NOT NULL PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL UNIQUE,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    reset_token VARCHAR(255) NULL,
    reset_token_expiry DATETIME2 NULL,
    last_login_date DATETIME2 NULL,
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_Auth_Profile FOREIGN KEY (customer_id) REFERENCES CUSTOMER_PROFILE(customer_id)
);

-- [4] KYC 資料表
CREATE TABLE CUSTOMER_KYC (
    customer_id VARCHAR(20) NOT NULL PRIMARY KEY,
    id_issue_date DATE NOT NULL,
    id_issue_location NVARCHAR(20) NOT NULL,
    id_issue_type NVARCHAR(20) NOT NULL, 
    tax_residency VARCHAR(10) NOT NULL DEFAULT 'TW',
    marital_status CHAR(1) NOT NULL DEFAULT 'S',
    education_level NVARCHAR(20) NULL,
    occupation_category NVARCHAR(50) NULL,
    company_name NVARCHAR(100) NULL,
    annual_income INT NULL,
    source_of_wealth NVARCHAR(50) NULL,
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_KYC_Profile FOREIGN KEY (customer_id) REFERENCES CUSTOMER_PROFILE(customer_id),
    CONSTRAINT CHK_Issue_Type CHECK (id_issue_type IN (N'初發', N'補發', N'換發'))
);

-- [5] 風控標籤表
CREATE TABLE CUSTOMER_RISK_TAG (
    customer_id VARCHAR(20) NOT NULL PRIMARY KEY,
    aml_risk_level VARCHAR(10) NOT NULL DEFAULT 'LOW',
    pep_status CHAR(1) NOT NULL DEFAULT 'N',
    is_fraud_suspect CHAR(1) NOT NULL DEFAULT 'N',
    block_reason NVARCHAR(100) NULL,
    kyc_last_review_date DATE NOT NULL DEFAULT GETDATE(),
    kyc_next_review_date DATE NOT NULL,
    updated_by VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_Risk_Profile FOREIGN KEY (customer_id) REFERENCES CUSTOMER_PROFILE(customer_id),
    CONSTRAINT CHK_AML_Risk CHECK (aml_risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'BLACKLIST')),
    CONSTRAINT CHK_PEP_Status CHECK (pep_status IN ('Y', 'N'))
);
GO
