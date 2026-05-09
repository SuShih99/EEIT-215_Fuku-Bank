/*
===============================================================================
Java Easy Bank 系統權限模組 (Auth Module)
===============================================================================
*/

-- 寫入 5 大實體部門
INSERT INTO AUTH_DEPT (dept_id, dept_code, dept_name) VALUES
('DPT001', 'CF',  N'消費金融部'),
('DPT002', 'CS',  N'客戶服務部'),
('DPT003', 'CR',  N'授信審查部'),
('DPT004', 'OPS', N'營運企劃部'),
('DPT005', 'IS',  N'資訊安全部');
GO

-- 寫入 11 個標準職務角色
INSERT INTO AUTH_ROLE (role_id, dept_id, role_code, role_name, perm_level, perm_scope) VALUES
('R001', 'DPT001', 'CFSO',   N'消金業務專員',   0, NULL),
('R002', 'DPT001', 'CFDM',   N'消金部經理',     2, NULL),
('R003', 'DPT002', 'CSVO',   N'客服照會專員',   1, NULL),
('R004', 'DPT002', 'CSDM',   N'客服部經理',     2, NULL),
('R005', 'DPT003', 'JCRO',   N'初階授信審查員', 1, NULL),
('R006', 'DPT003', 'CRDM',   N'授信部經理',     2, NULL),
('R007', 'DPT003', 'CRO',    N'風控長',         3, NULL),
('R008', 'DPT004', 'OPS_PA', N'營運企劃專員',   3, 'RO'),
('R009', 'DPT004', 'COO',    N'營運長',         4, 'BIZ'),
('R010', 'DPT005', 'ISSA',   N'資安監控分析師', 3, NULL),
('R011', 'DPT005', 'CISO',   N'資安長',         4, 'SYS'),
('R012', 'DPT005', 'SYS_STAFF', N'職員',           0, NULL),
('R013', 'DPT005', 'SYS_SUPER', N'超級管理員',     1, NULL);
GO

-- 寫入 11 位擬真員工帳號（密碼皆為 123456）
INSERT INTO AUTH_EMP (emp_id, emp_name, dept_id, role_id, email, password_hash, status, contract_end_date, permission_expire) VALUES
-- 消費金融部
('E26001', N'林家豪', 'DPT001', 'R001', 'chiahao.lin@javabank.com',  '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31'),
('E26002', N'王淑芬', 'DPT001', 'R002', 'shufen.wang@javabank.com', '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31'),
-- 客戶服務部
('E26003', N'陳建志', 'DPT002', 'R003', 'chienchih.chen@javabank.com', '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31'),
('E26004', N'張雅婷', 'DPT002', 'R004', 'yating.chang@javabank.com','$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31'),
-- 授信審查部
('E26005', N'劉冠宇', 'DPT003', 'R005', 'kuanyu.liu@javabank.com',  '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31'),
('E26006', N'吳建國', 'DPT003', 'R006', 'chienkuo.wu@javabank.com',   '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'SUSPENDED', NULL, '2024-01-01'), -- 離職停用
('E26007', N'李志明', 'DPT003', 'R007', 'chihming.lee@javabank.com',   '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31'),
-- 營運企劃部
('E26008', N'趙宇軒', 'DPT004', 'R008', 'yuhsuan.chao@javabank.com','$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', '2026-06-30', '2026-06-30'), -- 約聘人員
('E26009', N'黃志成', 'DPT004', 'R009', 'chihcheng.huang@javabank.com', '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31'),
-- 資訊安全部
('E26010', N'蔡宗翰', 'DPT005', 'R010', 'tsunghan.tsai@javabank.com', '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'LOCKED', NULL, '2026-12-31'), -- 密碼鎖定
('E26011', N'鄭文華', 'DPT005', 'R011', 'wenhua.cheng@javabank.com','$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31'),
('E26012', N'管理員甲', 'DPT005', 'R012', 'admin.a@javabank.com', '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31'),
('E26013', N'管理員乙', 'DPT005', 'R013', 'admin.b@javabank.com', '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS', 'ACTIVE', NULL, '2026-12-31');
GO

-- 寫入登入日誌模擬資料
INSERT INTO AUTH_LOGIN_LOG (attempt_email, emp_id, login_result, fail_reason, ip_address) VALUES
('wenhua.cheng@javabank.com', 'E26011', 'SUCCESS', NULL, '192.168.1.100'),
('chiahao.lin@javabank.com', 'E26001', 'SUCCESS', NULL, '192.168.1.101'),
('chienkuo.wu@javabank.com', 'E26006', 'FAILED', N'ACCOUNT_SUSPENDED', '192.168.1.102'),
('tsunghan.tsai@javabank.com', 'E26010', 'FAILED', N'INVALID_PASSWORD', '192.168.1.103'),
('hacker.test@javabank.com', NULL, 'FAILED', N'USER_NOT_FOUND', '203.0.113.50');
GO

-- 查詢測試：查看全行員工與對應權限
SELECT e.emp_name, d.dept_name, r.role_name, r.role_code, r.perm_level, e.status
FROM AUTH_EMP e
JOIN auth_dept d ON e.dept_id = d.dept_id
JOIN auth_role r ON e.role_id = r.role_id;
GO
