IF NOT EXISTS (SELECT 1 FROM AUTH_DEPT)
INSERT INTO AUTH_DEPT (dept_id, dept_code, dept_name) VALUES
('DPT001', 'CF',  N'Consumer Finance'),
('DPT002', 'CS',  N'Customer Service'),
('DPT003', 'CR',  N'Credit Risk'),
('DPT004', 'OPS', N'Operations'),
('DPT005', 'IS',  N'Information Security');
GO

IF NOT EXISTS (SELECT 1 FROM AUTH_ROLE)
INSERT INTO AUTH_ROLE (role_id, dept_id, role_code, role_name, perm_level, perm_scope) VALUES
('R001', 'DPT001', 'CFSO',   N'CF Senior Officer',   0, NULL),
('R002', 'DPT001', 'CFDM',    N'CF Department Manager', 2, NULL),
('R003', 'DPT002', 'CSVO',    N'CS Officer',          1, NULL),
('R004', 'DPT002', 'CSDM',    N'CS Department Manager', 2, NULL),
('R005', 'DPT003', 'JCRO',    N'Junior Credit Risk Officer', 1, NULL),
('R006', 'DPT003', 'CRDM',    N'Credit Risk Manager', 2, NULL),
('R007', 'DPT003', 'CRO',     N'Chief Risk Officer',  3, NULL),
('R008', 'DPT004', 'OPS_PA',  N'Operations Approver', 3, 'RO'),
('R009', 'DPT004', 'COO',     N'Chief Operating Officer', 4, 'BIZ'),
('R010', 'DPT005', 'ISSA',    N'Information Security Analyst', 3, NULL),
('R011', 'DPT005', 'CISO',    N'Chief Information Security Officer', 4, 'SYS'),
('R012', 'DPT005', 'SYS_STAFF', N'System Staff',      0, NULL),
('R013', 'DPT005', 'SYS_SUPER', N'System Supervisor', 1, NULL);
GO

IF NOT EXISTS (SELECT 1 FROM AUTH_EMP)
INSERT INTO AUTH_EMP (emp_id, emp_name, dept_id, role_id, email, password_hash, status, contract_end_date, permission_expire) VALUES
('E26001', N'Chia-Hao Lin', 'DPT001', 'R001', 'chiahao.lin@javabank.com',  '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31'),
('E26002', N'Shu-Fen Wang', 'DPT001', 'R002', 'shufen.wang@javabank.com', '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31'),
('E26003', N'Chien-Chih Chen', 'DPT002', 'R003', 'chienchih.chen@javabank.com', '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31'),
('E26004', N'Ya-Ting Chang', 'DPT002', 'R004', 'yating.chang@javabank.com', '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31'),
('E26005', N'Kuanyu Liu', 'DPT003', 'R005', 'kuanyu.liu@javabank.com',  '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31'),
('E26006', N'Chien-Kuo Wu', 'DPT003', 'R006', 'chienkuo.wu@javabank.com',   '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31'),
('E26007', N'Chih-Ming Lee', 'DPT003', 'R007', 'chihming.lee@javabank.com',   '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31'),
('E26008', N'Yu-Hsuan Chao', 'DPT004', 'R008', 'yuhsuan.chao@javabank.com', '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', '2026-06-30', '2026-06-30'),
('E26009', N'Chih-Cheng Huang', 'DPT004', 'R009', 'chihcheng.huang@javabank.com', '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31'),
('E26010', N'Tsunghan Tsai', 'DPT005', 'R010', 'tsunghan.tsai@javabank.com', '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31'),
('E26011', N'Wen-Hua Cheng', 'DPT005', 'R011', 'wenhua.cheng@javabank.com', '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31'),
('E26012', N'Admin A', 'DPT005', 'R012', 'admin.a@javabank.com', '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31'),
('E26013', N'Admin B', 'DPT005', 'R013', 'admin.b@javabank.com', '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31');
GO

IF NOT EXISTS (SELECT 1 FROM AUTH_LOGIN_LOG)
INSERT INTO AUTH_LOGIN_LOG (attempt_email, emp_id, login_result, fail_reason, ip_address) VALUES
('wenhua.cheng@javabank.com', 'E26011', 'SUCCESS', NULL, '192.168.1.100'),
('chiahao.lin@javabank.com', 'E26001', 'SUCCESS', NULL, '192.168.1.101'),
('chienkuo.wu@javabank.com', 'E26006', 'FAILED', N'ACCOUNT_SUSPENDED', '192.168.1.102'),
('tsunghan.tsai@javabank.com', 'E26010', 'FAILED', N'INVALID_PASSWORD', '192.168.1.103'),
('hacker.test@javabank.com', NULL, 'FAILED', N'USER_NOT_FOUND', '203.0.113.50');
GO

IF NOT EXISTS (SELECT 1 FROM AUTH_ACTION_LOG)
INSERT INTO AUTH_ACTION_LOG (emp_id, emp_name, action, target, details, action_time, ip_address) VALUES
('E26003', N'Chien-Chih Chen', 'LOGIN', 'E26003', N'Login to auth admin', DATEADD(MINUTE, -56, GETDATE()), '192.168.10.23'),
('E26003', N'Chien-Chih Chen', 'LOGOUT', 'E26003', N'Logout from auth admin', DATEADD(MINUTE, -52, GETDATE()), '192.168.10.23'),
('E26005', N'Kuanyu Liu', 'LOGIN', 'E26005', N'Login to auth admin', DATEADD(MINUTE, -48, GETDATE()), '192.168.10.45'),
('E26004', N'Ya-Ting Chang', 'LOGIN', 'E26004', N'Login to auth admin', DATEADD(MINUTE, -44, GETDATE()), '192.168.10.31'),
('E26007', N'Chih-Ming Lee', 'LOGIN', 'E26007', N'Login to auth admin', DATEADD(MINUTE, -40, GETDATE()), '192.168.10.52'),
('E26005', N'Kuanyu Liu', 'LOGOUT', 'E26005', N'Logout from auth admin', DATEADD(MINUTE, -36, GETDATE()), '192.168.10.45'),
('E26010', N'Tsunghan Tsai', 'LOGIN', 'E26010', N'Login to auth admin', DATEADD(MINUTE, -32, GETDATE()), '192.168.10.66'),
('E26004', N'Ya-Ting Chang', 'LOGOUT', 'E26004', N'Logout from auth admin', DATEADD(MINUTE, -28, GETDATE()), '192.168.10.31');
GO
