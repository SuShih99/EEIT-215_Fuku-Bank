/*
===============================================================================
Java Easy Bank 系統權限模組 (Auth Module)
===============================================================================
*/

-- 寫入 5 大實體部門
INSERT INTO auth_dept (dept_id, dept_code, dept_name) VALUES
('DPT001', 'CF',  N'消費金融部'),
('DPT002', 'CS',  N'客戶服務部'),
('DPT003', 'CR',  N'授信審查部'),
('DPT004', 'OPS', N'營運企劃部'),
('DPT005', 'IS',  N'資訊安全部');
GO

-- 寫入 11 個標準職務角色
INSERT INTO auth_role (role_id, dept_id, role_code, role_name, perm_level, perm_scope) VALUES
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
INSERT INTO auth_emp (emp_id, emp_name, dept_id, role_id, email, password_hash, status, contract_end_date, permission_expire) VALUES
-- 消費金融部
('E26001', N'林家豪', 'DPT001', 'R001', 'chiahao.lin@javabank.com',  '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Fy0dS8qWOdLAyFwPiyMCu', 'ACTIVE', NULL, '2026-12-31'),
('E26002', N'王淑芬', 'DPT001', 'R002', 'shufen.wang@javabank.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Fy0dS8qWOdLAyFwPiyMCu', 'ACTIVE', NULL, '2026-12-31'),
-- 客戶服務部
('E26003', N'陳建志', 'DPT002', 'R003', 'chienchih.chen@javabank.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Fy0dS8qWOdLAyFwPiyMCu', 'ACTIVE', NULL, '2026-12-31'),
('E26004', N'張雅婷', 'DPT002', 'R004', 'yating.chang@javabank.com','$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Fy0dS8qWOdLAyFwPiyMCu', 'ACTIVE', NULL, '2026-12-31'),
-- 授信審查部
('E26005', N'劉冠宇', 'DPT003', 'R005', 'kuanyu.liu@javabank.com',  '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Fy0dS8qWOdLAyFwPiyMCu', 'ACTIVE', NULL, '2026-12-31'),
('E26006', N'吳建國', 'DPT003', 'R006', 'chienkuo.wu@javabank.com',   '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Fy0dS8qWOdLAyFwPiyMCu', 'SUSPENDED', NULL, '2024-01-01'), -- 離職停用
('E26007', N'李志明', 'DPT003', 'R007', 'chihming.lee@javabank.com',   '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Fy0dS8qWOdLAyFwPiyMCu', 'ACTIVE', NULL, '2026-12-31'),
-- 營運企劃部
('E26008', N'趙宇軒', 'DPT004', 'R008', 'yuhsuan.chao@javabank.com','$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Fy0dS8qWOdLAyFwPiyMCu', 'ACTIVE', '2026-06-30', '2026-06-30'), -- 約聘人員
('E26009', N'黃志成', 'DPT004', 'R009', 'chihcheng.huang@javabank.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Fy0dS8qWOdLAyFwPiyMCu', 'ACTIVE', NULL, '2026-12-31'),
-- 資訊安全部
('E26010', N'蔡宗翰', 'DPT005', 'R010', 'tsunghan.tsai@javabank.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Fy0dS8qWOdLAyFwPiyMCu', 'LOCKED', NULL, '2026-12-31'), -- 密碼鎖定
('E26011', N'鄭文華', 'DPT005', 'R011', 'wenhua.cheng@javabank.com','$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Fy0dS8qWOdLAyFwPiyMCu', 'ACTIVE', NULL, '2026-12-31'),
('E26012', N'管理員甲', 'DPT005', 'R012', 'admin.a@javabank.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Fy0dS8qWOdLAyFwPiyMCu', 'ACTIVE', NULL, '2026-12-31'),
('E26013', N'管理員乙', 'DPT005', 'R013', 'admin.b@javabank.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36Fy0dS8qWOdLAyFwPiyMCu', 'ACTIVE', NULL, '2026-12-31');
GO

-- 寫入登入日誌模擬資料
INSERT INTO auth_login_log (attempt_email, emp_id, login_result, fail_reason, ip_address) VALUES
('wenhua.cheng@javabank.com', 'E26011', 'SUCCESS', NULL, '192.168.1.100'),
('chiahao.lin@javabank.com', 'E26001', 'SUCCESS', NULL, '192.168.1.101'),
('chienkuo.wu@javabank.com', 'E26006', 'FAILED', N'ACCOUNT_SUSPENDED', '192.168.1.102'),
('tsunghan.tsai@javabank.com', 'E26010', 'FAILED', N'INVALID_PASSWORD', '192.168.1.103'),
('hacker.test@javabank.com', NULL, 'FAILED', N'USER_NOT_FOUND', '203.0.113.50');
GO

-- 查詢測試：查看全行員工與對應權限
SELECT e.emp_name, d.dept_name, r.role_name, r.role_code, r.perm_level, e.status
FROM auth_emp e
JOIN auth_dept d ON e.dept_id = d.dept_id
JOIN auth_role r ON e.role_id = r.role_id;
GO

/*
===============================================================================
Java Easy Bank 客戶模組 (Customer Module)
===============================================================================
*/

-- 寫入 20 筆 customer_application
INSERT INTO customer_application (case_id, customer_id, cif, name, birthday, gender, id_number, email, phone, address, username, status) VALUES
('APP260428001', 'X7K9P2M4', '2605-A3R5W8J1', N'王大明', '1985-05-15', 'M', 'A123456789', 'ming.wang@email.com', '0912000001', N'台北市信義區信義路五段1號', 'mingwang85', 'APPROVED'),
('APP260428002', 'V4L6T1Y8', '2605-N9Q2E7C4', N'林小華', '1990-08-22', 'F', 'B223456789', 'hua.lin@email.com', '0912000002', N'台中市西屯區台灣大道三段99號', 'hualin90', 'APPROVED'),
('APP260428003', 'D3H8F5G2', '2605-M1V6P4K9', N'陳建國', '1978-11-03', 'M', 'C123456789', 'chien.chen@email.com', '0912000003', N'高雄市三民區建國一路1號', 'chienchen78', 'APPROVED'),
('APP260428004', 'B9W1C7R5', '2605-H5T2J8X3', N'張雅婷', '1995-02-14', 'F', 'D223456789', 'ya.chang@email.com', '0912000004', N'台南市東區大學路1號', 'yachang95', 'APPROVED'),
('APP260428005', 'P6M4N2Q8', '2605-L7F3Y1W6', N'李志明', '1982-07-30', 'M', 'E123456789', 'chih.lee@email.com', '0912000005', N'新北市板橋區中山路一段161號', 'chihlee82', 'APPROVED'),
('APP260428006', 'K1T9V5L3', '2605-C4G8R2M5', N'吳美玲', '1988-12-10', 'F', 'F223456789', 'mei.wu@email.com', '0912000006', N'桃園市中壢區中大路300號', 'meiwu88', 'APPROVED'),
('APP260428007', 'E8C2X7J4', '2605-T6N1P9V8', N'黃文輝', '1975-04-05', 'M', 'G123456789', 'wen.huang@email.com', '0912000007', N'新竹市東區光復路二段101號', 'wenhuang75', 'APPROVED'),
('APP260428008', 'Y5R4W1H6', '2605-F2K7M3L9', N'蔡佳蓉', '1992-09-18', 'F', 'H223456789', 'chia.tsai@email.com', '0912000008', N'苗栗縣苗栗市縣府路100號', 'chiatsai92', 'APPROVED'),
('APP260428009', 'G7N3M8P2', '2605-X1W5C4T6', N'劉冠宇', '1980-01-25', 'M', 'I123456789', 'kuan.liu@email.com', '0912000009', N'彰化縣彰化市中山路二段416號', 'kuanliu80', 'APPROVED'),
('APP260428010', 'J2F6K9V1', '2605-R8Y4H7N2', N'許家瑩', '1998-06-08', 'F', 'J223456789', 'chia.hsu@email.com', '0912000010', N'南投縣南投市中興路660號', 'chiahsu98', 'APPROVED'),
('APP260428011', 'Q4W8C1T7', '2605-V3L9M2P5', N'鄭宗翰', '1986-10-12', 'M', 'K123456789', 'tsung.cheng@email.com', '0912000011', N'雲林縣斗六市雲林路二段515號', 'tsungcheng86', 'APPROVED'),
('APP260428012', 'M9P2R5N4', '2605-J7X1W6C8', N'洪玉婷', '1991-03-20', 'F', 'L223456789', 'yu.hung@email.com', '0912000012', N'嘉義市東區中山路199號', 'yuhung91', 'APPROVED'),
('APP260428013', 'H3T7J1V9', '2605-K4F8N5R2', N'邱信宏', '1979-08-05', 'M', 'M123456789', 'hsin.chiu@email.com', '0912000013', N'屏東縣屏東市自由路527號', 'hsinchiu79', 'APPROVED'),
('APP260428014', 'L5V1M6P3', '2605-W9T2C7H4', N'曾婉茹', '1994-11-28', 'F', 'N223456789', 'wan.tseng@email.com', '0912000014', N'宜蘭縣宜蘭市縣政北路1號', 'wantseng94', 'APPROVED'),
('APP260428015', 'R8K4N9M2', '2605-P1V5J3X6', N'廖偉翔', '1983-05-10', 'M', 'O123456789', 'wei.liao@email.com', '0912000015', N'花蓮縣花蓮市府前路17號', 'weiliao83', 'APPROVED'),
('APP260428016', 'T1C7W4R8', '2605-N6M2L9F5', N'賴怡君', '1987-12-01', 'F', 'P223456789', 'yi.lai@email.com', '0912000016', N'台東縣台東市中山路276號', 'yilai87', 'APPROVED'),
('APP260428017', 'N6M9P2V5', '2605-C8H1T4K7', N'徐俊傑', '1976-02-18', 'M', 'Q123456789', 'chun.hsu@email.com', '0912000017', N'澎湖縣馬公市治平路32號', 'chunhsu76', 'APPROVED'),
('APP260428018', 'W2R5T8C1', '2605-M7N3P6V9', N'卓佩樺', '1996-07-07', 'F', 'R223456789', 'pei.cho@email.com', '0912000018', N'基隆市中正區義一路1號', 'peicho96', 'APPROVED'),
('APP260428019', 'C4H1K7N9', '2605-V5L2W8R3', N'江宇軒', '1989-09-30', 'M', 'S123456789', 'yu.chiang@email.com', '0912000019', N'台北市中正區重慶南路一段122號', 'yuchiang89', 'APPROVED'),
('APP260428020', 'V7P3M1R6', '2605-T9C4F2J8', N'郭欣儀', '1993-04-25', 'F', 'T223456789', 'hsin.kuo@email.com', '0912000020', N'新北市三重區重新路一段1號', 'hsinkuo93', 'APPROVED');
GO

-- 寫入 20 筆 customer_profile
INSERT INTO customer_profile (customer_id, cif, id_number, name, birthday, gender, email, phone, address, status) VALUES
('X7K9P2M4', '2605-A3R5W8J1', 'A123456789', N'王大明', '1985-05-15', 'M', 'ming.wang@email.com', '0912000001', N'台北市信義區信義路五段1號', 'ACTIVE'),
('V4L6T1Y8', '2605-N9Q2E7C4', 'B223456789', N'林小華', '1990-08-22', 'F', 'hua.lin@email.com', '0912000002', N'台中市西屯區台灣大道三段99號', 'ACTIVE'),
('D3H8F5G2', '2605-M1V6P4K9', 'C123456789', N'陳建國', '1978-11-03', 'M', 'chien.chen@email.com', '0912000003', N'高雄市三民區建國一路1號', 'ACTIVE'),
('B9W1C7R5', '2605-H5T2J8X3', 'D223456789', N'張雅婷', '1995-02-14', 'F', 'ya.chang@email.com', '0912000004', N'台南市東區大學路1號', 'ACTIVE'),
('P6M4N2Q8', '2605-L7F3Y1W6', 'E123456789', N'李志明', '1982-07-30', 'M', 'chih.lee@email.com', '0912000005', N'新北市板橋區中山路一段161號', 'ACTIVE'),
('K1T9V5L3', '2605-C4G8R2M5', 'F223456789', N'吳美玲', '1988-12-10', 'F', 'mei.wu@email.com', '0912000006', N'桃園市中壢區中大路300號', 'ACTIVE'),
('E8C2X7J4', '2605-T6N1P9V8', 'G123456789', N'黃文輝', '1975-04-05', 'M', 'wen.huang@email.com', '0912000007', N'新竹市東區光復路二段101號', 'INACTIVE'),
('Y5R4W1H6', '2605-F2K7M3L9', 'H223456789', N'蔡佳蓉', '1992-09-18', 'F', 'chia.tsai@email.com', '0912000008', N'苗栗縣苗栗市縣府路100號', 'ACTIVE'),
('G7N3M8P2', '2605-X1W5C4T6', 'I123456789', N'劉冠宇', '1980-01-25', 'M', 'kuan.liu@email.com', '0912000009', N'彰化縣彰化市中山路二段416號', 'ACTIVE'),
('J2F6K9V1', '2605-R8Y4H7N2', 'J223456789', N'許家瑩', '1998-06-08', 'F', 'chia.hsu@email.com', '0912000010', N'南投縣南投市中興路660號', 'ACTIVE'),
('Q4W8C1T7', '2605-V3L9M2P5', 'K123456789', N'鄭宗翰', '1986-10-12', 'M', 'tsung.cheng@email.com', '0912000011', N'雲林縣斗六市雲林路二段515號', 'ACTIVE'),
('M9P2R5N4', '2605-J7X1W6C8', 'L223456789', N'洪玉婷', '1991-03-20', 'F', 'yu.hung@email.com', '0912000012', N'嘉義市東區中山路199號', 'ACTIVE'),
('H3T7J1V9', '2605-K4F8N5R2', 'M123456789', N'邱信宏', '1979-08-05', 'M', 'hsin.chiu@email.com', '0912000013', N'屏東縣屏東市自由路527號', 'ACTIVE'),
('L5V1M6P3', '2605-W9T2C7H4', 'N223456789', N'曾婉茹', '1994-11-28', 'F', 'wan.tseng@email.com', '0912000014', N'宜蘭縣宜蘭市縣政北路1號', 'ACTIVE'),
('R8K4N9M2', '2605-P1V5J3X6', 'O123456789', N'廖偉翔', '1983-05-10', 'M', 'wei.liao@email.com', '0912000015', N'花蓮縣花蓮市府前路17號', 'ACTIVE'),
('T1C7W4R8', '2605-N6M2L9F5', 'P223456789', N'賴怡君', '1987-12-01', 'F', 'yi.lai@email.com', '0912000016', N'台東縣台東市中山路276號', 'ACTIVE'),
('N6M9P2V5', '2605-C8H1T4K7', 'Q123456789', N'徐俊傑', '1976-02-18', 'M', 'chun.hsu@email.com', '0912000017', N'澎湖縣馬公市治平路32號', 'ACTIVE'),
('W2R5T8C1', '2605-M7N3P6V9', 'R223456789', N'卓佩樺', '1996-07-07', 'F', 'pei.cho@email.com', '0912000018', N'基隆市中正區義一路1號', 'ACTIVE'),
('C4H1K7N9', '2605-V5L2W8R3', 'S123456789', N'江宇軒', '1989-09-30', 'M', 'yu.chiang@email.com', '0912000019', N'台北市中正區重慶南路一段122號', 'ACTIVE'),
('V7P3M1R6', '2605-T9C4F2J8', 'T223456789', N'郭欣儀', '1993-04-25', 'F', 'hsin.kuo@email.com', '0912000020', N'新北市三重區重新路一段1號', 'ACTIVE');
GO

-- 寫入 20 筆 customer_kyc
INSERT INTO customer_kyc (customer_id, id_issue_date, id_issue_location, id_issue_type, marital_status, education_level, occupation_category, company_name, annual_income, source_of_wealth) VALUES
('X7K9P2M4', '2015-10-01', N'台北市', N'初發', 'M', N'大學', N'資訊科技業', N'台積電', 150, N'薪資收入'),
('V4L6T1Y8', '2018-05-12', N'台中市', N'換發', 'S', N'碩士', N'金融保險業', N'國泰金控', 120, N'薪資收入'),
('D3H8F5G2', '2010-12-20', N'高雄市', N'換發', 'M', N'高中', N'製造業', N'中鋼', 90, N'薪資與投資'),
('B9W1C7R5', '2020-03-15', N'台南市', N'初發', 'S', N'大學', N'服務業', N'統一超商', 50, N'薪資收入'),
('P6M4N2Q8', '2016-08-08', N'新北市', N'補發', 'D', N'專科', N'營造建築業', N'遠雄建設', 200, N'營利所得'),
('K1T9V5L3', '2019-11-11', N'桃園市', N'換發', 'M', N'大學', N'運輸物流業', N'長榮航空', 110, N'薪資收入'),
('E8C2X7J4', '2014-02-28', N'新竹市', N'換發', 'M', N'博士', N'教育研究', N'清華大學', 130, N'薪資收入'),
('Y5R4W1H6', '2021-07-05', N'苗栗縣', N'初發', 'S', N'大學', N'醫療保健業', N'長庚醫院', 80, N'薪資收入'),
('G7N3M8P2', '2012-09-15', N'彰化縣', N'補發', 'M', N'高中', N'農林漁牧業', N'自營農場', 60, N'營業收入'),
('J2F6K9V1', '2022-01-10', N'南投縣', N'初發', 'S', N'大學', N'藝術娛樂業', N'自由業', 40, N'執行業務所得'),
('Q4W8C1T7', '2017-06-22', N'雲林縣', N'換發', 'M', N'碩士', N'公用事業', N'台電', 100, N'薪資收入'),
('M9P2R5N4', '2019-04-18', N'嘉義市', N'補發', 'S', N'專科', N'批發零售業', N'全聯實業', 55, N'薪資收入'),
('H3T7J1V9', '2011-10-30', N'屏東縣', N'換發', 'M', N'高中', N'住宿餐飲業', N'自營餐廳', 85, N'營業收入'),
('L5V1M6P3', '2020-08-14', N'宜蘭縣', N'初發', 'S', N'大學', N'出版傳媒業', N'天下雜誌', 65, N'薪資收入'),
('R8K4N9M2', '2015-03-25', N'花蓮縣', N'換發', 'D', N'碩士', N'政府機關', N'花蓮縣政府', 95, N'薪資收入'),
('T1C7W4R8', '2018-12-05', N'台東縣', N'補發', 'M', N'大學', N'休閒旅遊業', N'雄獅旅遊', 50, N'薪資收入'),
('N6M9P2V5', '2013-07-19', N'澎湖縣', N'換發', 'M', N'高中', N'漁業', N'遠洋漁業公司', 140, N'薪資收入'),
('W2R5T8C1', '2021-11-02', N'基隆市', N'初發', 'S', N'大學', N'專業科學技術', N'理律法律事務所', 120, N'執行業務所得'),
('C4H1K7N9', '2016-01-08', N'台北市', N'換發', 'M', N'博士', N'資訊科技業', N'Google 台灣', 350, N'薪資與股票'),
('V7P3M1R6', '2019-09-12', N'新北市', N'補發', 'S', N'碩士', N'金融保險業', N'富邦金控', 115, N'薪資收入');
GO

-- 寫入 20 筆 customer_risk_tag
INSERT INTO customer_risk_tag (customer_id, aml_risk_level, pep_status, is_fraud_suspect, block_reason, kyc_next_review_date) VALUES
('X7K9P2M4', 'LOW', 'N', 'N', NULL, '2028-04-28'),
('V4L6T1Y8', 'LOW', 'N', 'N', NULL, '2028-04-28'),
('D3H8F5G2', 'MEDIUM', 'N', 'N', NULL, '2027-04-28'),
('B9W1C7R5', 'LOW', 'N', 'N', NULL, '2028-04-28'),
('P6M4N2Q8', 'HIGH', 'N', 'N', NULL, '2026-10-28'),
('K1T9V5L3', 'LOW', 'N', 'N', NULL, '2028-04-28'),
('E8C2X7J4', 'LOW', 'N', 'Y', N'多次小額異常匯出被警示', '2026-05-28'),
('Y5R4W1H6', 'LOW', 'N', 'N', NULL, '2028-04-28'),
('G7N3M8P2', 'MEDIUM', 'N', 'N', NULL, '2027-04-28'),
('J2F6K9V1', 'LOW', 'N', 'N', NULL, '2028-04-28'),
('Q4W8C1T7', 'LOW', 'N', 'N', NULL, '2028-04-28'),
('M9P2R5N4', 'LOW', 'N', 'N', NULL, '2028-04-28'),
('H3T7J1V9', 'LOW', 'N', 'N', NULL, '2028-04-28'),
('L5V1M6P3', 'LOW', 'N', 'N', NULL, '2028-04-28'),
('R8K4N9M2', 'HIGH', 'Y', 'N', N'重要政治性職務人士', '2026-10-28'),
('T1C7W4R8', 'LOW', 'N', 'N', NULL, '2028-04-28'),
('N6M9P2V5', 'LOW', 'N', 'N', NULL, '2028-04-28'),
('W2R5T8C1', 'LOW', 'N', 'N', NULL, '2028-04-28'),
('C4H1K7N9', 'LOW', 'N', 'N', NULL, '2028-04-28'),
('V7P3M1R6', 'LOW', 'N', 'N', NULL, '2028-04-28');
GO