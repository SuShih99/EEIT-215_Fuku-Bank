-- FukuBank schema generated from JPA entities
-- Generated on 2026-05-26

SET ANSI_NULLS ON;
GO
SET QUOTED_IDENTIFIER ON;
GO

IF OBJECT_ID(N'[dbo].[account]', N'U') IS NOT NULL
    DROP TABLE [dbo].[account];
GO
CREATE TABLE [dbo].[account] (
    [account_number] NVARCHAR(14) NOT NULL,
    [customer_id] NVARCHAR(20) NOT NULL,
    [account_type] NVARCHAR(20) NOT NULL,
    [currency] NVARCHAR(3) NOT NULL,
    [balance] DECIMAL(19, 4) NULL,
    [liability] DECIMAL(19, 4) NULL,
    [interest_rate] DECIMAL(7, 5) NULL,
    [status] NVARCHAR(20) NOT NULL,
    [parent_account_number] NVARCHAR(14) NULL,
    [created_at] DATETIME2 NOT NULL,
    [created_by] NVARCHAR(20) NULL,
    [changed_at] DATETIME2 NOT NULL,
    [changed_by] NVARCHAR(20) NULL,
    CONSTRAINT [PK_account] PRIMARY KEY ([account_number])
);
GO

IF OBJECT_ID(N'[dbo].[account_application]', N'U') IS NOT NULL
    DROP TABLE [dbo].[account_application];
GO
CREATE TABLE [dbo].[account_application] (
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [application_no] NVARCHAR(30) NOT NULL,
    [customer_id] NVARCHAR(20) NOT NULL,
    [account_type] NVARCHAR(20) NOT NULL,
    [currency] NVARCHAR(3) NULL,
    [customer_name] NVARCHAR(50) NOT NULL,
    [id_number] NVARCHAR(20) NOT NULL,
    [birthday] DATE NOT NULL,
    [gender] NVARCHAR(1) NOT NULL,
    [email] NVARCHAR(100) NOT NULL,
    [address] NVARCHAR(255) NOT NULL,
    [nationality] NVARCHAR(10) NOT NULL,
    [phone] NVARCHAR(20) NOT NULL,
    [registered_address] NVARCHAR(255) NOT NULL,
    [current_address] NVARCHAR(255) NOT NULL,
    [occupation] NVARCHAR(50) NULL,
    [employer] NVARCHAR(100) NULL,
    [estimated_monthly_tx] INT NULL,
    [account_purpose] NVARCHAR(30) NULL,
    [fund_source] NVARCHAR(30) NULL,
    [tax_residency] NVARCHAR(10) NULL,
    [is_pep] BIT NOT NULL DEFAULT 0,
    [id_front_url] NVARCHAR(255) NOT NULL,
    [id_back_url] NVARCHAR(255) NOT NULL,
    [second_id_url] NVARCHAR(255) NOT NULL,
    [risk_flag] NVARCHAR(30) NOT NULL,
    [apply_ip] NVARCHAR(45) NULL,
    [status] NVARCHAR(20) NOT NULL,
    [reject_reason] NVARCHAR(500) NULL,
    [reviewed_at] DATETIME2 NULL,
    [reviewed_by] NVARCHAR(50) NULL,
    [created_account_number] NVARCHAR(14) NULL,
    [created_at] DATETIME2 NOT NULL,
    [updated_at] DATETIME2 NOT NULL,
    CONSTRAINT [PK_account_application] PRIMARY KEY ([id])
);
GO
CREATE UNIQUE INDEX [UX_account_application_application_no] ON [dbo].[account_application] ([application_no]) WHERE [application_no] IS NOT NULL;
GO

IF OBJECT_ID(N'[dbo].[AUTH_ACTION_LOG]', N'U') IS NOT NULL
    DROP TABLE [dbo].[AUTH_ACTION_LOG];
GO
CREATE TABLE [dbo].[AUTH_ACTION_LOG] (
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [emp_id] NVARCHAR(10) NULL,
    [emp_name] NVARCHAR(50) NULL,
    [action] NVARCHAR(50) NOT NULL,
    [target] NVARCHAR(50) NULL,
    [details] NVARCHAR(500) NULL,
    [action_time] DATETIME2 NULL,
    [ip_address] NVARCHAR(45) NULL,
    CONSTRAINT [PK_AUTH_ACTION_LOG] PRIMARY KEY ([id])
);
GO

IF OBJECT_ID(N'[dbo].[AUTH_DEPT]', N'U') IS NOT NULL
    DROP TABLE [dbo].[AUTH_DEPT];
GO
CREATE TABLE [dbo].[AUTH_DEPT] (
    [dept_id] NVARCHAR(10) NOT NULL,
    [dept_code] NVARCHAR(20) NOT NULL,
    [dept_name] NVARCHAR(50) NOT NULL,
    CONSTRAINT [PK_AUTH_DEPT] PRIMARY KEY ([dept_id])
);
GO
CREATE UNIQUE INDEX [UX_AUTH_DEPT_dept_code] ON [dbo].[AUTH_DEPT] ([dept_code]) WHERE [dept_code] IS NOT NULL;
GO

IF OBJECT_ID(N'[dbo].[AUTH_EMP]', N'U') IS NOT NULL
    DROP TABLE [dbo].[AUTH_EMP];
GO
CREATE TABLE [dbo].[AUTH_EMP] (
    [emp_id] NVARCHAR(10) NOT NULL,
    [emp_name] NVARCHAR(50) NOT NULL,
    [dept_id] NVARCHAR(10) NOT NULL,
    [role_id] NVARCHAR(10) NOT NULL,
    [email] NVARCHAR(100) NOT NULL,
    [password_hash] NVARCHAR(255) NOT NULL,
    [status] NVARCHAR(10) NULL,
    [contract_end_date] DATETIME2 NULL,
    [permission_expire] DATETIME2 NOT NULL,
    [failed_attempts] INT NULL,
    [pwd_updated_at] DATETIME2 NULL,
    [last_login_date] DATETIME2 NULL,
    [created_at] DATETIME2 NULL,
    [updated_at] DATETIME2 NULL,
    CONSTRAINT [PK_AUTH_EMP] PRIMARY KEY ([emp_id])
);
GO
CREATE UNIQUE INDEX [UX_AUTH_EMP_email] ON [dbo].[AUTH_EMP] ([email]) WHERE [email] IS NOT NULL;
GO

IF OBJECT_ID(N'[dbo].[AUTH_LOGIN_LOG]', N'U') IS NOT NULL
    DROP TABLE [dbo].[AUTH_LOGIN_LOG];
GO
CREATE TABLE [dbo].[AUTH_LOGIN_LOG] (
    [log_id] BIGINT IDENTITY(1,1) NOT NULL,
    [attempt_email] NVARCHAR(100) NOT NULL,
    [emp_id] NVARCHAR(10) NULL,
    [login_time] DATETIME2 NULL,
    [login_result] NVARCHAR(10) NOT NULL,
    [fail_reason] NVARCHAR(200) NULL,
    [ip_address] NVARCHAR(45) NULL,
    CONSTRAINT [PK_AUTH_LOGIN_LOG] PRIMARY KEY ([log_id])
);
GO

IF OBJECT_ID(N'[dbo].[AUTH_ROLE]', N'U') IS NOT NULL
    DROP TABLE [dbo].[AUTH_ROLE];
GO
CREATE TABLE [dbo].[AUTH_ROLE] (
    [role_id] NVARCHAR(10) NOT NULL,
    [dept_id] NVARCHAR(10) NOT NULL,
    [role_code] NVARCHAR(20) NOT NULL,
    [role_name] NVARCHAR(50) NOT NULL,
    [perm_level] INT NOT NULL,
    [perm_scope] NVARCHAR(10) NULL,
    CONSTRAINT [PK_AUTH_ROLE] PRIMARY KEY ([role_id])
);
GO
CREATE UNIQUE INDEX [UX_AUTH_ROLE_role_code] ON [dbo].[AUTH_ROLE] ([role_code]) WHERE [role_code] IS NOT NULL;
GO

IF OBJECT_ID(N'[dbo].[BLACK_LIST]', N'U') IS NOT NULL
    DROP TABLE [dbo].[BLACK_LIST];
GO
CREATE TABLE [dbo].[BLACK_LIST] (
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [list_type] NVARCHAR(20) NOT NULL,
    [list_value] NVARCHAR(100) NOT NULL,
    [source] NVARCHAR(50) NULL,
    [reason] NVARCHAR(255) NULL,
    [status] BIT NOT NULL,
    [expires_at] DATETIME2 NULL,
    [created_at] DATETIME2 NULL,
    [updated_at] DATETIME2 NULL,
    CONSTRAINT [PK_BLACK_LIST] PRIMARY KEY ([id])
);
GO

IF OBJECT_ID(N'[dbo].[CARD_ACCOUNT]', N'U') IS NOT NULL
    DROP TABLE [dbo].[CARD_ACCOUNT];
GO
CREATE TABLE [dbo].[CARD_ACCOUNT] (
    [id] INT IDENTITY(1,1) NOT NULL,
    [account_number] NVARCHAR(20) NULL,
    [credit_limit] DECIMAL(15, 2) NULL,
    [statement_day] INT NULL,
    [due_days] INT NULL,
    [customer_id] NVARCHAR(20) NOT NULL,
    CONSTRAINT [PK_CARD_ACCOUNT] PRIMARY KEY ([id])
);
GO
CREATE UNIQUE INDEX [UX_CARD_ACCOUNT_customer_id] ON [dbo].[CARD_ACCOUNT] ([customer_id]) WHERE [customer_id] IS NOT NULL;
GO

IF OBJECT_ID(N'[dbo].[CARD_APPLICATION]', N'U') IS NOT NULL
    DROP TABLE [dbo].[CARD_APPLICATION];
GO
CREATE TABLE [dbo].[CARD_APPLICATION] (
    [application_id] INT IDENTITY(1,1) NOT NULL,
    [apply_date] DATETIME2 NULL,
    [status] NVARCHAR(50) NULL,
    [remark] NVARCHAR(200) NULL,
    [customer_id] NVARCHAR(20) NOT NULL,
    CONSTRAINT [PK_CARD_APPLICATION] PRIMARY KEY ([application_id])
);
GO

IF OBJECT_ID(N'[dbo].[CARD_APPLICATION_DOCUMENT]', N'U') IS NOT NULL
    DROP TABLE [dbo].[CARD_APPLICATION_DOCUMENT];
GO
CREATE TABLE [dbo].[CARD_APPLICATION_DOCUMENT] (
    [document_id] INT IDENTITY(1,1) NOT NULL,
    [file_url] NVARCHAR(500) NOT NULL,
    [file_name] NVARCHAR(255) NULL,
    [application_id] INT NOT NULL,
    [uploaded_at] DATETIME2 NULL,
    CONSTRAINT [PK_CARD_APPLICATION_DOCUMENT] PRIMARY KEY ([document_id])
);
GO

IF OBJECT_ID(N'[dbo].[CARD_APPLICATION_ITEM]', N'U') IS NOT NULL
    DROP TABLE [dbo].[CARD_APPLICATION_ITEM];
GO
CREATE TABLE [dbo].[CARD_APPLICATION_ITEM] (
    [item_id] INT IDENTITY(1,1) NOT NULL,
    [application_id] INT NULL,
    [card_type_id] INT NULL,
    [result] NVARCHAR(50) NULL,
    [approved_limit] DECIMAL(15, 2) NULL,
    [annual_fee] DECIMAL(10, 2) NULL,
    [create_card_flag] BIT NULL,
    [review_date] DATETIME2 NULL,
    [remark] NVARCHAR(200) NULL,
    CONSTRAINT [PK_CARD_APPLICATION_ITEM] PRIMARY KEY ([item_id])
);
GO

IF OBJECT_ID(N'[dbo].[CARD_BILL]', N'U') IS NOT NULL
    DROP TABLE [dbo].[CARD_BILL];
GO
CREATE TABLE [dbo].[CARD_BILL] (
    [bill_id] INT IDENTITY(1,1) NOT NULL,
    [billing_month] NVARCHAR(7) NULL,
    [bill_date] DATE NULL,
    [due_date] DATE NULL,
    [total_amount] DECIMAL(15, 2) NULL,
    [minimum_payment] DECIMAL(15, 2) NULL,
    [paid_amount] DECIMAL(15, 2) NULL,
    [bill_status] NVARCHAR(20) NULL,
    [cashback_amount] DECIMAL(15, 2) NULL,
    [reward_posted] BIT NULL,
    [reward_reference_id] NVARCHAR(100) NULL,
    [card_id] INT NULL,
    [card_account_id] INT NULL,
    CONSTRAINT [PK_CARD_BILL] PRIMARY KEY ([bill_id])
);
GO

IF OBJECT_ID(N'[dbo].[CARD_TRANSACTION]', N'U') IS NOT NULL
    DROP TABLE [dbo].[CARD_TRANSACTION];
GO
CREATE TABLE [dbo].[CARD_TRANSACTION] (
    [txn_id] INT IDENTITY(1,1) NOT NULL,
    [txn_amount] DECIMAL(15, 2) NOT NULL,
    [cashback_rate] DECIMAL(15, 2) NULL,
    [cashback_amount] DECIMAL(15, 2) NULL,
    [txn_type] NVARCHAR(20) NULL,
    [txn_date] DATETIME2 NULL,
    [description] NVARCHAR(200) NULL,
    [channel] NVARCHAR(50) NULL,
    [external_txn_id] NVARCHAR(100) NULL,
    [ref_txn_id] INT NULL,
    [card_id] INT NOT NULL,
    [merchant_id] INT NULL,
    [bill_id] INT NULL,
    CONSTRAINT [PK_CARD_TRANSACTION] PRIMARY KEY ([txn_id])
);
GO

IF OBJECT_ID(N'[dbo].[CARD_TYPE]', N'U') IS NOT NULL
    DROP TABLE [dbo].[CARD_TYPE];
GO
CREATE TABLE [dbo].[CARD_TYPE] (
    [card_type_id] INT IDENTITY(1,1) NOT NULL,
    [card_type_name] NVARCHAR(50) NOT NULL,
    [brand] NVARCHAR(20) NOT NULL,
    [annual_fee] DECIMAL(15, 2) NULL,
    [cashback_rate] DECIMAL(15, 2) NULL,
    [card_image_url] NVARCHAR(255) NULL,
    CONSTRAINT [PK_CARD_TYPE] PRIMARY KEY ([card_type_id])
);
GO

IF OBJECT_ID(N'[dbo].[CREDIT_CARD]', N'U') IS NOT NULL
    DROP TABLE [dbo].[CREDIT_CARD];
GO
CREATE TABLE [dbo].[CREDIT_CARD] (
    [card_id] INT IDENTITY(1,1) NOT NULL,
    [customer_id] NVARCHAR(20) NOT NULL,
    [card_type_id] INT NOT NULL,
    [card_number] NVARCHAR(16) NOT NULL,
    [expiry_date] DATE NOT NULL,
    [current_debt] DECIMAL(15, 2) NULL,
    [create_date] DATETIME2 NULL,
    [status] NVARCHAR(20) NULL,
    [application_item_id] INT NULL,
    [credit_card_account_number] NVARCHAR(20) NULL,
    [card_account_id] INT NULL,
    CONSTRAINT [PK_CREDIT_CARD] PRIMARY KEY ([card_id])
);
GO
CREATE UNIQUE INDEX [UX_CREDIT_CARD_card_number] ON [dbo].[CREDIT_CARD] ([card_number]) WHERE [card_number] IS NOT NULL;
GO

IF OBJECT_ID(N'[dbo].[CUSTOMER_AUTH]', N'U') IS NOT NULL
    DROP TABLE [dbo].[CUSTOMER_AUTH];
GO
CREATE TABLE [dbo].[CUSTOMER_AUTH] (
    [auth_id] NVARCHAR(20) NOT NULL,
    [customer_id] NVARCHAR(20) NOT NULL,
    [username] NVARCHAR(50) NOT NULL,
    [password_hash] NVARCHAR(255) NOT NULL,
    [role] NVARCHAR(20) NOT NULL,
    [status] NVARCHAR(20) NOT NULL,
    [reset_token] NVARCHAR(255) NULL,
    [reset_token_expiry] DATETIME2 NULL,
    [last_login_date] DATETIME2 NULL,
    [verification_token] NVARCHAR(255) NULL,
    [created_at] DATETIME2 NULL,
    [updated_at] DATETIME2 NULL,
    [unlocked_at] DATETIME2 NULL,
    CONSTRAINT [PK_CUSTOMER_AUTH] PRIMARY KEY ([auth_id])
);
GO
CREATE UNIQUE INDEX [UX_CUSTOMER_AUTH_customer_id] ON [dbo].[CUSTOMER_AUTH] ([customer_id]) WHERE [customer_id] IS NOT NULL;
GO
CREATE UNIQUE INDEX [UX_CUSTOMER_AUTH_username] ON [dbo].[CUSTOMER_AUTH] ([username]) WHERE [username] IS NOT NULL;
GO

IF OBJECT_ID(N'[dbo].[customer_credit_info]', N'U') IS NOT NULL
    DROP TABLE [dbo].[customer_credit_info];
GO
CREATE TABLE [dbo].[customer_credit_info] (
    [customer_id] NVARCHAR(20) NOT NULL,
    [annual_income] DECIMAL(15, 2) NULL,
    [occupation] NVARCHAR(50) NULL,
    [job] NVARCHAR(100) NULL,
    [external_score] INT NULL,
    [other_bank_debt] DECIMAL(15, 2) NULL,
    [has_real_estate] BIT NULL,
    [fund_source] NVARCHAR(30) NULL,
    [is_pep] BIT NOT NULL DEFAULT 0,
    [final_score] INT NULL,
    [risk_level] NVARCHAR(10) NULL,
    [last_updated_at] DATETIME2 NOT NULL,
    CONSTRAINT [PK_customer_credit_info] PRIMARY KEY ([customer_id])
);
GO

IF OBJECT_ID(N'[dbo].[CUSTOMER_DEVICE]', N'U') IS NOT NULL
    DROP TABLE [dbo].[CUSTOMER_DEVICE];
GO
CREATE TABLE [dbo].[CUSTOMER_DEVICE] (
    [device_id] BIGINT IDENTITY(1,1) NOT NULL,
    [customer_id] NVARCHAR(20) NOT NULL,
    [device_fingerprint] NVARCHAR(64) NOT NULL,
    [device_name] NVARCHAR(120) NOT NULL,
    [browser_name] NVARCHAR(60) NULL,
    [operating_system] NVARCHAR(60) NULL,
    [ip_address] NVARCHAR(45) NULL,
    [user_agent] NVARCHAR(512) NULL,
    [status] NVARCHAR(20) NOT NULL,
    [trusted] BIT NOT NULL,
    [first_seen_at] DATETIME2 NOT NULL,
    [last_seen_at] DATETIME2 NOT NULL,
    [revoked_at] DATETIME2 NULL,
    CONSTRAINT [PK_CUSTOMER_DEVICE] PRIMARY KEY ([device_id])
);
GO

IF OBJECT_ID(N'[dbo].[CUSTOMER_LOGIN_LOG]', N'U') IS NOT NULL
    DROP TABLE [dbo].[CUSTOMER_LOGIN_LOG];
GO
CREATE TABLE [dbo].[CUSTOMER_LOGIN_LOG] (
    [login_log_id] BIGINT IDENTITY(1,1) NOT NULL,
    [customer_id] NVARCHAR(20) NULL,
    [username] NVARCHAR(50) NOT NULL,
    [result] NVARCHAR(20) NOT NULL,
    [fail_reason] NVARCHAR(200) NULL,
    [ip_address] NVARCHAR(45) NULL,
    [user_agent] NVARCHAR(512) NULL,
    [device_name] NVARCHAR(120) NULL,
    [login_time] DATETIME2 NOT NULL,
    CONSTRAINT [PK_CUSTOMER_LOGIN_LOG] PRIMARY KEY ([login_log_id])
);
GO

IF OBJECT_ID(N'[dbo].[CUSTOMER_PROFILE]', N'U') IS NOT NULL
    DROP TABLE [dbo].[CUSTOMER_PROFILE];
GO
CREATE TABLE [dbo].[CUSTOMER_PROFILE] (
    [customer_id] NVARCHAR(20) NOT NULL,
    [cif] NVARCHAR(20) NOT NULL,
    [id_number] NVARCHAR(20) NOT NULL,
    [name] NVARCHAR(50) NOT NULL,
    [birthday] DATE NOT NULL,
    [gender] NVARCHAR(1) NOT NULL,
    [email] NVARCHAR(100) NOT NULL,
    [phone] NVARCHAR(20) NOT NULL,
    [address] NVARCHAR(255) NOT NULL,
    [nationality] NVARCHAR(10) NULL,
    [registered_address] NVARCHAR(255) NULL,
    [current_address] NVARCHAR(255) NULL,
    [occupation] NVARCHAR(50) NULL,
    [employer] NVARCHAR(100) NULL,
    [estimated_monthly_tx] INT NULL,
    [account_purpose] NVARCHAR(30) NULL,
    [fund_source] NVARCHAR(50) NULL,
    [tax_residency] NVARCHAR(10) NULL,
    [is_pep] BIT NOT NULL DEFAULT 0,
    [id_front_url] NVARCHAR(255) NULL,
    [id_back_url] NVARCHAR(255) NULL,
    [second_id_url] NVARCHAR(255) NULL,
    [latest_account_application_id] BIGINT NULL,
    [latest_account_application_no] NVARCHAR(30) NULL,
    [latest_applied_account_type] NVARCHAR(20) NULL,
    [latest_applied_currency] NVARCHAR(3) NULL,
    [latest_account_application_status] NVARCHAR(20) NULL,
    [latest_account_application_risk_flag] NVARCHAR(30) NULL,
    [latest_account_application_reviewed_at] DATETIME2 NULL,
    [latest_account_application_reviewed_by] NVARCHAR(50) NULL,
    [latest_account_application_reject_reason] NVARCHAR(500) NULL,
    [created_account_number] NVARCHAR(14) NULL,
    [account_application_synced_at] DATETIME2 NULL,
    [avatar_url] NVARCHAR(255) NULL,
    [status] NVARCHAR(20) NOT NULL,
    [created_at] DATETIME2 NULL,
    [updated_at] DATETIME2 NULL,
    [job] NVARCHAR(100) NULL,
    [annual_income] INT NULL,
    [risk_level] NVARCHAR(255) NULL,
    CONSTRAINT [PK_CUSTOMER_PROFILE] PRIMARY KEY ([customer_id])
);
GO
CREATE UNIQUE INDEX [UX_CUSTOMER_PROFILE_cif] ON [dbo].[CUSTOMER_PROFILE] ([cif]) WHERE [cif] IS NOT NULL;
GO

IF OBJECT_ID(N'[dbo].[favorite_account]', N'U') IS NOT NULL
    DROP TABLE [dbo].[favorite_account];
GO
CREATE TABLE [dbo].[favorite_account] (
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [customer_id] NVARCHAR(20) NOT NULL,
    [bank_code] NVARCHAR(10) NOT NULL,
    [account_number] NVARCHAR(20) NOT NULL,
    [alias] NVARCHAR(50) NOT NULL,
    [bank_name] NVARCHAR(50) NULL,
    [created_at] DATETIME2 NOT NULL,
    [updated_at] DATETIME2 NOT NULL,
    CONSTRAINT [PK_favorite_account] PRIMARY KEY ([id])
);
GO

IF OBJECT_ID(N'[dbo].[loan_account]', N'U') IS NOT NULL
    DROP TABLE [dbo].[loan_account];
GO
CREATE TABLE [dbo].[loan_account] (
    [account_id] NVARCHAR(255) NOT NULL,
    [account_number] NVARCHAR(255) NULL,
    [application_id] NVARCHAR(255) NULL,
    [customer_id] NVARCHAR(255) NULL,
    [apply_type] NVARCHAR(255) NULL,
    [principal_amount] BIGINT NULL,
    [confirmed_period] INT NULL,
    [rate] DECIMAL(19, 2) NULL,
    [monthly_payment] DECIMAL(19, 2) NULL,
    [paid_periods] INT NULL,
    [remaining_principal] DECIMAL(19, 2) NULL,
    [start_date] DATE NULL,
    [next_payment_date] DATE NULL,
    [account_status] NVARCHAR(50) NULL,
    [create_time] DATETIME2 NULL,
    [update_time] DATETIME2 NULL,
    CONSTRAINT [PK_loan_account] PRIMARY KEY ([account_id])
);
GO

IF OBJECT_ID(N'[dbo].[LOAN_APPLICATION]', N'U') IS NOT NULL
    DROP TABLE [dbo].[LOAN_APPLICATION];
GO
CREATE TABLE [dbo].[LOAN_APPLICATION] (
    [application_id] NVARCHAR(255) NOT NULL,
    [customer_id] NVARCHAR(255) NULL,
    [apply_type] NVARCHAR(255) NULL,
    [apply_amount] DECIMAL(19, 2) NULL,
    [apply_period] INT NULL,
    [rate] DECIMAL(19, 2) NULL,
    [disbursement_account] NVARCHAR(255) NULL,
    [application_status] NVARCHAR(50) NULL,
    [required_documents] NVARCHAR(MAX) NULL,
    [review_comment] NVARCHAR(50) NULL,
    [create_time] DATETIME2 NULL,
    [latest_contact_status] NVARCHAR(50) NULL,
    [latest_contact_time] DATETIME2 NULL,
    [update_time] DATETIME2 NULL,
    [documents_submitted_at] DATETIME2 NULL,
    [current_supplement_batch_no] INT NULL,
    CONSTRAINT [PK_LOAN_APPLICATION] PRIMARY KEY ([application_id])
);
GO

IF OBJECT_ID(N'[dbo].[LOAN_CONTACT_LOG]', N'U') IS NOT NULL
    DROP TABLE [dbo].[LOAN_CONTACT_LOG];
GO
CREATE TABLE [dbo].[LOAN_CONTACT_LOG] (
    [log_id] NVARCHAR(255) NOT NULL,
    [application_id] NVARCHAR(255) NULL,
    [emp_id] NVARCHAR(255) NULL,
    [contact_status] NVARCHAR(50) NULL,
    [contact_channel] NVARCHAR(50) NULL,
    [contact_time] DATETIME2 NULL,
    [note] NVARCHAR(255) NULL,
    CONSTRAINT [PK_LOAN_CONTACT_LOG] PRIMARY KEY ([log_id])
);
GO

IF OBJECT_ID(N'[dbo].[LOAN_DOCUMENT]', N'U') IS NOT NULL
    DROP TABLE [dbo].[LOAN_DOCUMENT];
GO
CREATE TABLE [dbo].[LOAN_DOCUMENT] (
    [document_id] NVARCHAR(255) NOT NULL,
    [application_id] NVARCHAR(255) NULL,
    [document_type] NVARCHAR(50) NULL,
    [file_url] NVARCHAR(255) NULL,
    [original_name] NVARCHAR(255) NULL,
    [uploaded_by] NVARCHAR(255) NULL,
    [upload_time] DATETIME2 NULL,
    [document_batch_type] NVARCHAR(255) NULL,
    [document_batch_no] INT NULL,
    [submitted_at] DATETIME2 NULL,
    CONSTRAINT [PK_LOAN_DOCUMENT] PRIMARY KEY ([document_id])
);
GO

IF OBJECT_ID(N'[dbo].[LOAN_REPAYMENT]', N'U') IS NOT NULL
    DROP TABLE [dbo].[LOAN_REPAYMENT];
GO
CREATE TABLE [dbo].[LOAN_REPAYMENT] (
    [repayment_id] NVARCHAR(255) NOT NULL,
    [account_id] NVARCHAR(255) NULL,
    [period_index] INT NULL,
    [scheduled_date] DATE NULL,
    [paid_date] DATE NULL,
    [total_amount] DECIMAL(19, 2) NULL,
    [principal_portion] DECIMAL(19, 2) NULL,
    [interest_portion] DECIMAL(19, 2) NULL,
    [remaining_after] DECIMAL(19, 2) NULL,
    [repayment_status] NVARCHAR(50) NULL,
    [create_time] DATETIME2 NULL,
    [update_time] DATETIME2 NULL,
    CONSTRAINT [PK_LOAN_REPAYMENT] PRIMARY KEY ([repayment_id])
);
GO

IF OBJECT_ID(N'[dbo].[LOAN_REVIEW_DETAIL]', N'U') IS NOT NULL
    DROP TABLE [dbo].[LOAN_REVIEW_DETAIL];
GO
CREATE TABLE [dbo].[LOAN_REVIEW_DETAIL] (
    [review_id] NVARCHAR(255) NOT NULL,
    [application_id] NVARCHAR(255) NULL,
    [confirmed_amount] DECIMAL(18, 2) NULL,
    [confirmed_period] INT NULL,
    [confirmed_rate] DECIMAL(10, 6) NULL,
    [collateral_note] NVARCHAR(255) NULL,
    [emp_id] NVARCHAR(255) NULL,
    [review_time] DATETIME2 NULL,
    [review_status] NVARCHAR(50) NULL,
    [submitted_time] DATETIME2 NULL,
    [review_note] NVARCHAR(255) NULL,
    CONSTRAINT [PK_LOAN_REVIEW_DETAIL] PRIMARY KEY ([review_id])
);
GO

IF OBJECT_ID(N'[dbo].[MERCHANT]', N'U') IS NOT NULL
    DROP TABLE [dbo].[MERCHANT];
GO
CREATE TABLE [dbo].[MERCHANT] (
    [merchant_id] INT NOT NULL,
    [merchant_name] NVARCHAR(100) NOT NULL,
    [merchant_category] NVARCHAR(50) NOT NULL,
    CONSTRAINT [PK_MERCHANT] PRIMARY KEY ([merchant_id])
);
GO

IF OBJECT_ID(N'[dbo].[notification_preferences]', N'U') IS NOT NULL
    DROP TABLE [dbo].[notification_preferences];
GO
CREATE TABLE [dbo].[notification_preferences] (
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [customer_id] NVARCHAR(20) NOT NULL,
    [notification_type] NVARCHAR(40) NOT NULL,
    [enabled] BIT NOT NULL,
    [updated_at] DATETIME2 NOT NULL,
    CONSTRAINT [PK_notification_preferences] PRIMARY KEY ([id])
);
GO

IF OBJECT_ID(N'[dbo].[notifications]', N'U') IS NOT NULL
    DROP TABLE [dbo].[notifications];
GO
CREATE TABLE [dbo].[notifications] (
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [customer_id] NVARCHAR(20) NOT NULL,
    [notification_type] NVARCHAR(40) NOT NULL,
    [title] NVARCHAR(100) NOT NULL,
    [message] NVARCHAR(1000) NOT NULL,
    [link_url] NVARCHAR(255) NULL,
    [is_read] BIT NOT NULL,
    [created_at] DATETIME2 NOT NULL,
    [read_at] DATETIME2 NULL,
    CONSTRAINT [PK_notifications] PRIMARY KEY ([id])
);
GO

IF OBJECT_ID(N'[dbo].[pending_transfer]', N'U') IS NOT NULL
    DROP TABLE [dbo].[pending_transfer];
GO
CREATE TABLE [dbo].[pending_transfer] (
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [reference_id] NVARCHAR(50) NOT NULL,
    [from_account_number] NVARCHAR(20) NOT NULL,
    [to_account_number] NVARCHAR(20) NOT NULL,
    [to_bank_code] NVARCHAR(10) NULL,
    [amount] DECIMAL(18, 4) NOT NULL,
    [currency] NVARCHAR(3) NULL,
    [note] NVARCHAR(200) NULL,
    [risk_log_id] BIGINT NULL,
    [review_task_id] BIGINT NULL,
    [hold_reason] NVARCHAR(200) NULL,
    [status] NVARCHAR(20) NOT NULL,
    [created_at] DATETIME2 NOT NULL,
    [processed_at] DATETIME2 NULL,
    [expires_at] DATETIME2 NOT NULL,
    CONSTRAINT [PK_pending_transfer] PRIMARY KEY ([id])
);
GO
CREATE UNIQUE INDEX [UX_pending_transfer_reference_id] ON [dbo].[pending_transfer] ([reference_id]) WHERE [reference_id] IS NOT NULL;
GO

IF OBJECT_ID(N'[dbo].[review_task]', N'U') IS NOT NULL
    DROP TABLE [dbo].[review_task];
GO
CREATE TABLE [dbo].[review_task] (
    [task_id] BIGINT IDENTITY(1,1) NOT NULL,
    [log_id] BIGINT NOT NULL,
    [business_id] NVARCHAR(64) NOT NULL,
    [scene] NVARCHAR(32) NULL,
    [status] NVARCHAR(255) NULL,
    [substatus] NVARCHAR(255) NULL,
    [review_result] NVARCHAR(20) NULL,
    [assignee] NVARCHAR(255) NULL,
    [admin_comment] NVARCHAR(500) NULL,
    [priority] INT NULL,
    [attachments] NVARCHAR(MAX) NULL,
    [required_documents_json] NVARCHAR(MAX) NULL,
    [create_at] DATETIME2 NULL,
    [processed_at] DATETIME2 NULL,
    [version] BIGINT NULL,
    CONSTRAINT [PK_review_task] PRIMARY KEY ([task_id])
);
GO

IF OBJECT_ID(N'[dbo].[RISK_EVENT_LOG]', N'U') IS NOT NULL
    DROP TABLE [dbo].[RISK_EVENT_LOG];
GO
CREATE TABLE [dbo].[RISK_EVENT_LOG] (
    [log_id] BIGINT IDENTITY(1,1) NOT NULL,
    [event_type] NVARCHAR(50) NOT NULL,
    [business_id] NVARCHAR(100) NOT NULL,
    [target_identifier] NVARCHAR(100) NOT NULL,
    [risk_level] NVARCHAR(20) NOT NULL,
    [disposition] NVARCHAR(50) NOT NULL,
    [trigger_reason] NVARCHAR(500) NULL,
    [meta_data] NVARCHAR(MAX) NULL,
    [transaction_amount] DECIMAL(18, 4) NULL,
    [callback_url] NVARCHAR(255) NULL,
    [created_at] DATETIME2 NOT NULL,
    CONSTRAINT [PK_RISK_EVENT_LOG] PRIMARY KEY ([log_id])
);
GO

IF OBJECT_ID(N'[dbo].[scheduled_transfer]', N'U') IS NOT NULL
    DROP TABLE [dbo].[scheduled_transfer];
GO
CREATE TABLE [dbo].[scheduled_transfer] (
    [id] BIGINT IDENTITY(1,1) NOT NULL,
    [customer_id] NVARCHAR(20) NOT NULL,
    [from_account_number] NVARCHAR(14) NOT NULL,
    [to_bank_code] NVARCHAR(3) NOT NULL,
    [to_bank_name] NVARCHAR(80) NOT NULL,
    [to_account_number] NVARCHAR(20) NOT NULL,
    [amount] DECIMAL(19, 4) NOT NULL,
    [scheduled_date] DATE NOT NULL,
    [note] NVARCHAR(200) NULL,
    [status] NVARCHAR(20) NOT NULL,
    [executed_at] DATETIME2 NULL,
    [fail_reason] NVARCHAR(500) NULL,
    [created_at] DATETIME2 NOT NULL,
    [updated_at] DATETIME2 NOT NULL,
    CONSTRAINT [PK_scheduled_transfer] PRIMARY KEY ([id])
);
GO

IF OBJECT_ID(N'[dbo].[trans_log]', N'U') IS NOT NULL
    DROP TABLE [dbo].[trans_log];
GO
CREATE TABLE [dbo].[trans_log] (
    [transaction_id] NVARCHAR(36) NOT NULL,
    [reference_id] NVARCHAR(30) NOT NULL,
    [account_number] NVARCHAR(14) NOT NULL,
    [counterpart_account] NVARCHAR(20) NULL,
    [bank_code] NVARCHAR(10) NOT NULL,
    [bank_name] NVARCHAR(50) NOT NULL,
    [counterpart_bank_code] NVARCHAR(10) NULL,
    [counterpart_bank_name] NVARCHAR(50) NULL,
    [is_interbank] BIT NOT NULL,
    [entry_type] NVARCHAR(10) NOT NULL,
    [transaction_type] NVARCHAR(25) NOT NULL,
    [amount] DECIMAL(19, 4) NOT NULL,
    [fee_amount] DECIMAL(19, 4) NOT NULL,
    [total_debit_amount] DECIMAL(19, 4) NULL,
    [balance_before] DECIMAL(19, 4) NOT NULL,
    [balance_after] DECIMAL(19, 4) NOT NULL,
    [currency] NVARCHAR(3) NOT NULL,
    [note] NVARCHAR(200) NULL,
    [created_at] DATETIME2 NOT NULL,
    CONSTRAINT [PK_trans_log] PRIMARY KEY ([transaction_id])
);
GO
