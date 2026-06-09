SET NOCOUNT ON;

CREATE TABLE AUTH_DEPT (
    dept_id   VARCHAR(10)   NOT NULL PRIMARY KEY,
    dept_code VARCHAR(20)   NOT NULL UNIQUE,
    dept_name NVARCHAR(50)  NOT NULL
);
GO

CREATE TABLE AUTH_ROLE (
    role_id    VARCHAR(10)  NOT NULL PRIMARY KEY,
    dept_id    VARCHAR(10)  NOT NULL,
    role_code  VARCHAR(20)  NOT NULL UNIQUE,
    role_name  NVARCHAR(50) NOT NULL,
    perm_level TINYINT      NOT NULL,
    perm_scope VARCHAR(10)  NULL,
    CONSTRAINT fk_auth_role_dept FOREIGN KEY (dept_id) REFERENCES AUTH_DEPT(dept_id),
    CONSTRAINT chk_auth_role_perm_level CHECK (perm_level IN (0, 1, 2, 3, 4))
);
GO

CREATE TABLE AUTH_EMP (
    emp_id            VARCHAR(10)  NOT NULL PRIMARY KEY,
    emp_name          NVARCHAR(50) NOT NULL,
    dept_id           VARCHAR(10)  NOT NULL,
    role_id           VARCHAR(10)  NOT NULL,
    email             VARCHAR(100) NOT NULL UNIQUE,
    password_hash     VARCHAR(255) NOT NULL,
    status            VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    contract_end_date DATETIME2    NULL,
    permission_expire DATETIME2    NOT NULL,
    failed_attempts   TINYINT      NOT NULL DEFAULT 0,
    pwd_updated_at    DATETIME2    NOT NULL DEFAULT GETDATE(),
    last_login_date   DATETIME2    NULL,
    created_at        DATETIME2    NOT NULL DEFAULT GETDATE(),
    updated_at        DATETIME2    NOT NULL DEFAULT GETDATE(),
    CONSTRAINT fk_auth_emp_dept FOREIGN KEY (dept_id) REFERENCES AUTH_DEPT(dept_id),
    CONSTRAINT fk_auth_emp_role FOREIGN KEY (role_id) REFERENCES AUTH_ROLE(role_id)
);
GO

CREATE TABLE AUTH_LOGIN_LOG (
    log_id        BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    attempt_email VARCHAR(100) NOT NULL,
    emp_id        VARCHAR(10)  NULL,
    login_time    DATETIME2    NOT NULL DEFAULT GETDATE(),
    login_result  VARCHAR(10)  NOT NULL,
    fail_reason   NVARCHAR(200) NULL,
    ip_address    VARCHAR(45)  NULL,
    CONSTRAINT fk_auth_login_log_emp FOREIGN KEY (emp_id) REFERENCES AUTH_EMP(emp_id)
);
GO

CREATE TABLE AUTH_ACTION_LOG (
    id          BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    emp_id      VARCHAR(10)   NULL,
    emp_name    NVARCHAR(50)  NULL,
    action      VARCHAR(50)   NOT NULL,
    target      VARCHAR(50)   NULL,
    details     NVARCHAR(500) NULL,
    action_time DATETIME2     NOT NULL DEFAULT GETDATE(),
    ip_address  VARCHAR(45)   NULL
);
GO
