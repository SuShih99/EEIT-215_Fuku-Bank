/*
===============================================================================
Java Easy Bank Customer Mock Data
- 50 realistic customer profiles
- Includes auth, KYC, risk tags, login logs, and authorized devices
- Keep this file customer-only. Account mock data lives in account_mockdata.sql.
===============================================================================
*/

SET NOCOUNT ON;

-- Clear customer mock data in FK-safe order.
IF OBJECT_ID('CUSTOMER_DEVICE', 'U') IS NOT NULL DELETE FROM CUSTOMER_DEVICE;
IF OBJECT_ID('CUSTOMER_LOGIN_LOG', 'U') IS NOT NULL DELETE FROM CUSTOMER_LOGIN_LOG;
IF OBJECT_ID('CUSTOMER_AUTH', 'U') IS NOT NULL DELETE FROM CUSTOMER_AUTH;
IF OBJECT_ID('CUSTOMER_RISK_TAG', 'U') IS NOT NULL DELETE FROM CUSTOMER_RISK_TAG;
IF OBJECT_ID('CUSTOMER_KYC', 'U') IS NOT NULL DELETE FROM CUSTOMER_KYC;
IF OBJECT_ID('CUSTOMER_APPLICATION', 'U') IS NOT NULL DELETE FROM CUSTOMER_APPLICATION;
IF OBJECT_ID('CUSTOMER_PROFILE', 'U') IS NOT NULL DELETE FROM CUSTOMER_PROFILE;
GO

DECLARE @customers TABLE (
    seq INT PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL,
    cif VARCHAR(20) NOT NULL,
    auth_id VARCHAR(20) NOT NULL,
    case_id VARCHAR(20) NOT NULL,
    name NVARCHAR(50) NOT NULL,
    birthday DATE NOT NULL,
    gender CHAR(1) NOT NULL,
    id_number VARCHAR(20) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address NVARCHAR(255) NOT NULL,
    username VARCHAR(50) NOT NULL,
    profile_status VARCHAR(20) NOT NULL,
    auth_status VARCHAR(20) NOT NULL,
    application_status VARCHAR(20) NOT NULL,
    nationality VARCHAR(10) NOT NULL,
    registered_address NVARCHAR(255) NOT NULL,
    current_address NVARCHAR(255) NOT NULL,
    occupation NVARCHAR(50) NOT NULL,
    employer NVARCHAR(100) NOT NULL,
    estimated_monthly_tx INT NOT NULL,
    account_purpose VARCHAR(30) NOT NULL,
    fund_source VARCHAR(30) NOT NULL,
    tax_residency VARCHAR(10) NOT NULL,
    is_pep BIT NOT NULL,
    id_front_url VARCHAR(255) NOT NULL,
    id_back_url VARCHAR(255) NOT NULL,
    second_id_url VARCHAR(255) NOT NULL,
    avatar_url VARCHAR(255) NOT NULL,
    job NVARCHAR(100) NOT NULL,
    annual_income INT NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    issue_location NVARCHAR(20) NOT NULL,
    education_level NVARCHAR(20) NOT NULL,
    marital_status CHAR(1) NOT NULL,
    id_issue_type NVARCHAR(20) NOT NULL,
    source_of_wealth NVARCHAR(50) NOT NULL,
    created_at DATETIME2 NOT NULL,
    updated_at DATETIME2 NOT NULL
);

INSERT INTO @customers (
    seq, customer_id, cif, auth_id, case_id, name, birthday, gender, id_number,
    email, phone, address, username, profile_status, auth_status, application_status,
    nationality, registered_address, current_address, occupation, employer,
    estimated_monthly_tx, account_purpose, fund_source, tax_residency, is_pep,
    id_front_url, id_back_url, second_id_url, avatar_url, job, annual_income,
    risk_level, issue_location, education_level, marital_status, id_issue_type,
    source_of_wealth, created_at, updated_at
) VALUES
    (1, 'C26050001', '2605-C000001', 'AUTH26050001', 'CASE26050001', N'王大明', '1968-01-01', 'M', 'A100260501', 'customer001@java-bank.demo', '0913000001', N'台北市信義區信義路五段7號', 'cust0001', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'台北市信義區信義路五段7號', N'台北市信義區信義路五段7號', N'資訊科技業', N'台積電', 5, 'SALARY', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050001-front.jpg', '/uploads/mock/customer/C26050001-back.jpg', '/uploads/mock/customer/C26050001-second.jpg', '/avatars/mock/C26050001.png', N'資訊科技業', 960000, 'LOW', N'台北市', N'大學', 'M', N'初發', N'薪資收入', '2026-01-02 09:00:00', '2026-01-02 09:00:00'),
    (2, 'C26050002', '2605-C000002', 'AUTH26050002', 'CASE26050002', N'林雅婷', '1975-02-02', 'F', 'B200260502', 'customer002@java-bank.demo', '0913000002', N'新北市板橋區中山路一段161號', 'cust0002', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'新北市板橋區中山路一段161號', N'新北市板橋區中山路一段161號', N'金融保險業', N'國泰金控', 10, 'INVESTMENT', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050002-front.jpg', '/uploads/mock/customer/C26050002-back.jpg', '/uploads/mock/customer/C26050002-second.jpg', '/avatars/mock/C26050002.png', N'金融保險業', 1140000, 'LOW', N'新北市', N'碩士', 'S', N'換發', N'薪資收入', '2026-02-03 10:07:00', '2026-02-03 10:07:00'),
    (3, 'C26050003', '2605-C000003', 'AUTH26050003', 'CASE26050003', N'陳柏翰', '1982-03-03', 'M', 'C100260503', 'customer003@java-bank.demo', '0913000003', N'桃園市中壢區中大路300號', 'cust0003', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'桃園市中壢區中大路300號', N'桃園市中壢區中大路300號', N'製造業', N'中鋼公司', 20, 'SAVINGS', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050003-front.jpg', '/uploads/mock/customer/C26050003-back.jpg', '/uploads/mock/customer/C26050003-second.jpg', '/avatars/mock/C26050003.png', N'製造業', 900000, 'LOW', N'桃園市', N'專科', 'S', N'補發', N'薪資收入', '2026-03-04 11:14:00', '2026-03-04 11:14:00'),
    (4, 'C26050004', '2605-C000004', 'AUTH26050004', 'CASE26050004', N'張怡君', '1989-04-04', 'F', 'D200260504', 'customer004@java-bank.demo', '0913000004', N'台中市西屯區台灣大道三段99號', 'cust0004', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'台中市西屯區台灣大道三段99號', N'台中市西屯區台灣大道三段99號', N'醫療保健業', N'長庚醫院', 30, 'SAVINGS', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050004-front.jpg', '/uploads/mock/customer/C26050004-back.jpg', '/uploads/mock/customer/C26050004-second.jpg', '/avatars/mock/C26050004.png', N'醫療保健業', 1080000, 'LOW', N'台中市', N'高中', 'M', N'初發', N'薪資收入', '2026-04-05 12:21:00', '2026-04-05 12:21:00'),
    (5, 'C26050005', '2605-C000005', 'AUTH26050005', 'CASE26050005', N'李冠宇', '1996-05-05', 'M', 'E100260505', 'customer005@java-bank.demo', '0913000005', N'台南市東區大學路1號', 'cust0005', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'台南市東區大學路1號', N'台南市東區大學路1號', N'教育服務業', N'台北市立大學', 50, 'SAVINGS', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050005-front.jpg', '/uploads/mock/customer/C26050005-back.jpg', '/uploads/mock/customer/C26050005-second.jpg', '/avatars/mock/C26050005.png', N'教育服務業', 960000, 'LOW', N'台南市', N'大學', 'S', N'換發', N'薪資收入', '2026-05-06 13:28:00', '2026-05-06 13:28:00'),
    (6, 'C26050006', '2605-C000006', 'AUTH26050006', 'CASE26050006', N'吳佳穎', '1969-06-06', 'F', 'F200260506', 'customer006@java-bank.demo', '0913000006', N'高雄市前鎮區中山二路2號', 'cust0006', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'高雄市前鎮區中山二路2號', N'高雄市前鎮區中山二路2號', N'零售服務業', N'統一超商', 80, 'DAILY_EXPENSE', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050006-front.jpg', '/uploads/mock/customer/C26050006-back.jpg', '/uploads/mock/customer/C26050006-second.jpg', '/avatars/mock/C26050006.png', N'零售服務業', 540000, 'LOW', N'高雄市', N'大學', 'S', N'補發', N'薪資收入', '2026-01-07 14:35:00', '2026-01-07 14:35:00'),
    (7, 'C26050007', '2605-C000007', 'AUTH26050007', 'CASE26050007', N'黃俊傑', '1976-07-07', 'M', 'G100260507', 'customer007@java-bank.demo', '0913000007', N'基隆市中正區義一路1號', 'cust0007', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'基隆市中正區義一路1號', N'基隆市中正區義一路1號', N'公務機關', N'台北市政府', 100, 'SALARY', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050007-front.jpg', '/uploads/mock/customer/C26050007-back.jpg', '/uploads/mock/customer/C26050007-second.jpg', '/avatars/mock/C26050007.png', N'公務機關', 880000, 'LOW', N'基隆市', N'碩士', 'M', N'初發', N'薪資收入', '2026-02-08 15:42:00', '2026-02-08 15:42:00'),
    (8, 'C26050008', '2605-C000008', 'AUTH26050008', 'CASE26050008', N'蔡佩珊', '1983-08-08', 'F', 'H200260508', 'customer008@java-bank.demo', '0913000008', N'新竹市東區光復路二段101號', 'cust0008', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'新竹市東區光復路二段101號', N'新竹市東區光復路二段101號', N'自營商', N'自營工作室', 5, 'BUSINESS', 'BUSINESS_INCOME', 'TW', 0, '/uploads/mock/customer/C26050008-front.jpg', '/uploads/mock/customer/C26050008-back.jpg', '/uploads/mock/customer/C26050008-second.jpg', '/avatars/mock/C26050008.png', N'自營商', 1320000, 'LOW', N'新竹市', N'專科', 'S', N'換發', N'營業收入', '2026-03-09 16:49:00', '2026-03-09 16:49:00'),
    (9, 'C26050009', '2605-C000009', 'AUTH26050009', 'CASE26050009', N'劉信宏', '1990-09-09', 'M', 'J100260509', 'customer009@java-bank.demo', '0913000009', N'新竹縣竹北市縣政九路146號', 'cust0009', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'新竹縣竹北市縣政九路146號', N'新竹縣竹北市縣政九路146號', N'物流運輸業', N'長榮航空', 10, 'PAYMENT', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050009-front.jpg', '/uploads/mock/customer/C26050009-back.jpg', '/uploads/mock/customer/C26050009-second.jpg', '/avatars/mock/C26050009.png', N'物流運輸業', 940000, 'MEDIUM', N'新竹縣', N'高中', 'S', N'補發', N'薪資收入', '2026-04-10 17:56:00', '2026-04-10 17:56:00'),
    (10, 'C26050010', '2605-C000010', 'AUTH26050010', 'CASE26050010', N'許家瑩', '1997-10-10', 'F', 'K200260510', 'customer010@java-bank.demo', '0913000010', N'苗栗縣苗栗市縣府路100號', 'cust0010', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'苗栗縣苗栗市縣府路100號', N'苗栗縣苗栗市縣府路100號', N'餐飲服務業', N'王品集團', 20, 'DAILY_EXPENSE', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050010-front.jpg', '/uploads/mock/customer/C26050010-back.jpg', '/uploads/mock/customer/C26050010-second.jpg', '/avatars/mock/C26050010.png', N'餐飲服務業', 760000, 'LOW', N'苗栗縣', N'大學', 'M', N'初發', N'薪資收入', '2026-05-11 09:03:00', '2026-05-11 09:03:00'),
    (11, 'C26050011', '2605-C000011', 'AUTH26050011', 'CASE26050011', N'鄭宗翰', '1970-11-11', 'M', 'L100260511', 'customer011@java-bank.demo', '0913000011', N'彰化縣彰化市中山路二段416號', 'cust0011', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'彰化縣彰化市中山路二段416號', N'彰化縣彰化市中山路二段416號', N'法律會計業', N'勤業眾信', 30, 'INVESTMENT', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050011-front.jpg', '/uploads/mock/customer/C26050011-back.jpg', '/uploads/mock/customer/C26050011-second.jpg', '/avatars/mock/C26050011.png', N'法律會計業', 1180000, 'LOW', N'彰化縣', N'大學', 'S', N'換發', N'薪資收入', '2026-01-12 10:10:00', '2026-01-12 10:10:00'),
    (12, 'C26050012', '2605-C000012', 'AUTH26050012', 'CASE26050012', N'洪玉婷', '1977-12-12', 'F', 'M200260512', 'customer012@java-bank.demo', '0913000012', N'南投縣南投市中興路660號', 'cust0012', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'南投縣南投市中興路660號', N'南投縣南投市中興路660號', N'建築營造業', N'遠雄建設', 50, 'BUSINESS', 'BUSINESS_INCOME', 'TW', 0, '/uploads/mock/customer/C26050012-front.jpg', '/uploads/mock/customer/C26050012-back.jpg', '/uploads/mock/customer/C26050012-second.jpg', '/avatars/mock/C26050012.png', N'建築營造業', 1380000, 'LOW', N'南投縣', N'碩士', 'S', N'補發', N'營業收入', '2026-02-13 11:17:00', '2026-02-13 11:17:00'),
    (13, 'C26050013', '2605-C000013', 'AUTH26050013', 'CASE26050013', N'邱信宏', '1984-01-13', 'M', 'N100260513', 'customer013@java-bank.demo', '0913000013', N'雲林縣斗六市雲林路二段515號', 'cust0013', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'雲林縣斗六市雲林路二段515號', N'雲林縣斗六市雲林路二段515號', N'資訊科技業', N'台積電', 80, 'SALARY', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050013-front.jpg', '/uploads/mock/customer/C26050013-back.jpg', '/uploads/mock/customer/C26050013-second.jpg', '/avatars/mock/C26050013.png', N'資訊科技業', 1080000, 'LOW', N'雲林縣', N'專科', 'M', N'初發', N'薪資收入', '2026-03-14 12:24:00', '2026-03-14 12:24:00'),
    (14, 'C26050014', '2605-C000014', 'AUTH26050014', 'CASE26050014', N'曾婉茹', '1991-02-14', 'F', 'P200260514', 'customer014@java-bank.demo', '0913000014', N'嘉義市東區中山路199號', 'cust0014', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'嘉義市東區中山路199號', N'嘉義市東區中山路199號', N'金融保險業', N'國泰金控', 100, 'INVESTMENT', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050014-front.jpg', '/uploads/mock/customer/C26050014-back.jpg', '/uploads/mock/customer/C26050014-second.jpg', '/avatars/mock/C26050014.png', N'金融保險業', 1260000, 'LOW', N'嘉義市', N'高中', 'S', N'換發', N'薪資收入', '2026-04-15 13:31:00', '2026-04-15 13:31:00'),
    (15, 'C26050015', '2605-C000015', 'AUTH26050015', 'CASE26050015', N'廖偉翔', '1998-03-15', 'M', 'Q100260515', 'customer015@java-bank.demo', '0913000015', N'嘉義縣太保市祥和一路東段1號', 'cust0015', 'FROZEN', 'LOCKED', 'APPROVED', 'TW', N'嘉義縣太保市祥和一路東段1號', N'嘉義縣太保市祥和一路東段1號', N'製造業', N'中鋼公司', 5, 'SAVINGS', 'SALARY', 'TW', 1, '/uploads/mock/customer/C26050015-front.jpg', '/uploads/mock/customer/C26050015-back.jpg', '/uploads/mock/customer/C26050015-second.jpg', '/avatars/mock/C26050015.png', N'製造業', 1020000, 'HIGH', N'嘉義縣', N'大學', 'S', N'補發', N'薪資收入', '2026-04-16 14:38:00', '2026-04-16 14:38:00'),
    (16, 'C26050016', '2605-C000016', 'AUTH26050016', 'CASE26050016', N'賴怡君', '1971-04-16', 'F', 'R200260516', 'customer016@java-bank.demo', '0913000016', N'屏東縣屏東市自由路527號', 'cust0016', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'屏東縣屏東市自由路527號', N'屏東縣屏東市自由路527號', N'醫療保健業', N'長庚醫院', 10, 'SAVINGS', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050016-front.jpg', '/uploads/mock/customer/C26050016-back.jpg', '/uploads/mock/customer/C26050016-second.jpg', '/avatars/mock/C26050016.png', N'醫療保健業', 900000, 'LOW', N'屏東縣', N'大學', 'M', N'初發', N'薪資收入', '2026-01-17 15:45:00', '2026-01-17 15:45:00'),
    (17, 'C26050017', '2605-C000017', 'AUTH26050017', 'CASE26050017', N'徐俊傑', '1978-05-17', 'M', 'S100260517', 'customer017@java-bank.demo', '0913000017', N'宜蘭縣宜蘭市縣政北路1號', 'cust0017', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'宜蘭縣宜蘭市縣政北路1號', N'宜蘭縣宜蘭市縣政北路1號', N'教育服務業', N'台北市立大學', 20, 'SAVINGS', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050017-front.jpg', '/uploads/mock/customer/C26050017-back.jpg', '/uploads/mock/customer/C26050017-second.jpg', '/avatars/mock/C26050017.png', N'教育服務業', 780000, 'LOW', N'宜蘭縣', N'碩士', 'S', N'換發', N'薪資收入', '2026-02-18 16:52:00', '2026-02-18 16:52:00'),
    (18, 'C26050018', '2605-C000018', 'AUTH26050018', 'CASE26050018', N'卓佩樺', '1985-06-18', 'F', 'T200260518', 'customer018@java-bank.demo', '0913000018', N'花蓮縣花蓮市府前路17號', 'cust0018', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'花蓮縣花蓮市府前路17號', N'花蓮縣花蓮市府前路17號', N'零售服務業', N'統一超商', 30, 'DAILY_EXPENSE', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050018-front.jpg', '/uploads/mock/customer/C26050018-back.jpg', '/uploads/mock/customer/C26050018-second.jpg', '/avatars/mock/C26050018.png', N'零售服務業', 660000, 'LOW', N'花蓮縣', N'專科', 'S', N'補發', N'薪資收入', '2026-03-19 17:59:00', '2026-03-19 17:59:00'),
    (19, 'C26050019', '2605-C000019', 'AUTH26050019', 'CASE26050019', N'江宇軒', '1992-07-19', 'M', 'U100260519', 'customer019@java-bank.demo', '0913000019', N'台東縣台東市中山路276號', 'cust0019', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'台東縣台東市中山路276號', N'台東縣台東市中山路276號', N'公務機關', N'台北市政府', 50, 'SALARY', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050019-front.jpg', '/uploads/mock/customer/C26050019-back.jpg', '/uploads/mock/customer/C26050019-second.jpg', '/avatars/mock/C26050019.png', N'公務機關', 1000000, 'LOW', N'台東縣', N'高中', 'M', N'初發', N'薪資收入', '2026-04-20 09:06:00', '2026-04-20 09:06:00'),
    (20, 'C26050020', '2605-C000020', 'AUTH26050020', 'CASE26050020', N'郭欣儀', '1999-08-20', 'F', 'V200260520', 'customer020@java-bank.demo', '0913000020', N'澎湖縣馬公市治平路32號', 'cust0020', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'澎湖縣馬公市治平路32號', N'澎湖縣馬公市治平路32號', N'自營商', N'自營工作室', 80, 'BUSINESS', 'BUSINESS_INCOME', 'TW', 0, '/uploads/mock/customer/C26050020-front.jpg', '/uploads/mock/customer/C26050020-back.jpg', '/uploads/mock/customer/C26050020-second.jpg', '/avatars/mock/C26050020.png', N'自營商', 1440000, 'LOW', N'澎湖縣', N'大學', 'S', N'換發', N'營業收入', '2026-04-21 10:13:00', '2026-04-21 10:13:00'),
    (21, 'C26050021', '2605-C000021', 'AUTH26050021', 'CASE26050021', N'謝文凱', '1972-09-21', 'M', 'X100260521', 'customer021@java-bank.demo', '0913000021', N'台北市信義區信義路五段7號', 'cust0021', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'台北市信義區信義路五段7號', N'台北市信義區信義路五段7號', N'物流運輸業', N'長榮航空', 100, 'PAYMENT', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050021-front.jpg', '/uploads/mock/customer/C26050021-back.jpg', '/uploads/mock/customer/C26050021-second.jpg', '/avatars/mock/C26050021.png', N'物流運輸業', 760000, 'LOW', N'台北市', N'大學', 'S', N'補發', N'薪資收入', '2026-01-22 11:20:00', '2026-01-22 11:20:00'),
    (22, 'C26050022', '2605-C000022', 'AUTH26050022', 'CASE26050022', N'楊欣妤', '1979-10-22', 'F', 'Y200260522', 'customer022@java-bank.demo', '0913000022', N'新北市板橋區中山路一段161號', 'cust0022', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'新北市板橋區中山路一段161號', N'新北市板橋區中山路一段161號', N'餐飲服務業', N'王品集團', 5, 'DAILY_EXPENSE', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050022-front.jpg', '/uploads/mock/customer/C26050022-back.jpg', '/uploads/mock/customer/C26050022-second.jpg', '/avatars/mock/C26050022.png', N'餐飲服務業', 580000, 'LOW', N'新北市', N'碩士', 'M', N'初發', N'薪資收入', '2026-02-23 12:27:00', '2026-02-23 12:27:00'),
    (23, 'C26050023', '2605-C000023', 'AUTH26050023', 'CASE26050023', N'何承恩', '1986-11-23', 'M', 'W100260523', 'customer023@java-bank.demo', '0913000023', N'桃園市中壢區中大路300號', 'cust0023', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'桃園市中壢區中大路300號', N'桃園市中壢區中大路300號', N'法律會計業', N'勤業眾信', 10, 'INVESTMENT', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050023-front.jpg', '/uploads/mock/customer/C26050023-back.jpg', '/uploads/mock/customer/C26050023-second.jpg', '/avatars/mock/C26050023.png', N'法律會計業', 1300000, 'MEDIUM', N'桃園市', N'專科', 'S', N'換發', N'薪資收入', '2026-03-24 13:34:00', '2026-03-24 13:34:00'),
    (24, 'C26050024', '2605-C000024', 'AUTH26050024', 'CASE26050024', N'周雅琪', '1993-12-24', 'F', 'Z200260524', 'customer024@java-bank.demo', '0913000024', N'台中市西屯區台灣大道三段99號', 'cust0024', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'台中市西屯區台灣大道三段99號', N'台中市西屯區台灣大道三段99號', N'建築營造業', N'遠雄建設', 20, 'BUSINESS', 'BUSINESS_INCOME', 'TW', 0, '/uploads/mock/customer/C26050024-front.jpg', '/uploads/mock/customer/C26050024-back.jpg', '/uploads/mock/customer/C26050024-second.jpg', '/avatars/mock/C26050024.png', N'建築營造業', 1500000, 'LOW', N'台中市', N'高中', 'S', N'補發', N'營業收入', '2026-04-25 14:41:00', '2026-04-25 14:41:00'),
    (25, 'C26050025', '2605-C000025', 'AUTH26050025', 'CASE26050025', N'蘇柏安', '2000-01-25', 'M', 'A100260525', 'customer025@java-bank.demo', '0913000025', N'台南市東區大學路1號', 'cust0025', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'台南市東區大學路1號', N'台南市東區大學路1號', N'資訊科技業', N'台積電', 30, 'SALARY', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050025-front.jpg', '/uploads/mock/customer/C26050025-back.jpg', '/uploads/mock/customer/C26050025-second.jpg', '/avatars/mock/C26050025.png', N'資訊科技業', 1200000, 'LOW', N'台南市', N'大學', 'M', N'初發', N'薪資收入', '2026-05-02 15:48:00', '2026-05-02 15:48:00'),
    (26, 'C26050026', '2605-C000026', 'AUTH26050026', 'CASE26050026', N'盧品妤', '1973-02-26', 'F', 'B200260526', 'customer026@java-bank.demo', '0913000026', N'高雄市前鎮區中山二路2號', 'cust0026', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'高雄市前鎮區中山二路2號', N'高雄市前鎮區中山二路2號', N'金融保險業', N'國泰金控', 50, 'INVESTMENT', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050026-front.jpg', '/uploads/mock/customer/C26050026-back.jpg', '/uploads/mock/customer/C26050026-second.jpg', '/avatars/mock/C26050026.png', N'金融保險業', 1080000, 'LOW', N'高雄市', N'大學', 'S', N'換發', N'薪資收入', '2026-01-03 16:55:00', '2026-01-03 16:55:00'),
    (27, 'C26050027', '2605-C000027', 'AUTH26050027', 'CASE26050027', N'方建廷', '1980-03-27', 'M', 'C100260527', 'customer027@java-bank.demo', '0913000027', N'基隆市中正區義一路1號', 'cust0027', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'基隆市中正區義一路1號', N'基隆市中正區義一路1號', N'製造業', N'中鋼公司', 80, 'SAVINGS', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050027-front.jpg', '/uploads/mock/customer/C26050027-back.jpg', '/uploads/mock/customer/C26050027-second.jpg', '/avatars/mock/C26050027.png', N'製造業', 840000, 'LOW', N'基隆市', N'碩士', 'S', N'補發', N'薪資收入', '2026-02-04 17:02:00', '2026-02-04 17:02:00'),
    (28, 'C26050028', '2605-C000028', 'AUTH26050028', 'CASE26050028', N'呂怡萱', '1987-04-01', 'F', 'D200260528', 'customer028@java-bank.demo', '0913000028', N'新竹市東區光復路二段101號', 'cust0028', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'新竹市東區光復路二段101號', N'新竹市東區光復路二段101號', N'醫療保健業', N'長庚醫院', 100, 'SAVINGS', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050028-front.jpg', '/uploads/mock/customer/C26050028-back.jpg', '/uploads/mock/customer/C26050028-second.jpg', '/avatars/mock/C26050028.png', N'醫療保健業', 1020000, 'LOW', N'新竹市', N'專科', 'M', N'初發', N'薪資收入', '2026-03-05 09:09:00', '2026-03-05 09:09:00'),
    (29, 'C26050029', '2605-C000029', 'AUTH26050029', 'CASE26050029', N'施冠廷', '1994-05-02', 'M', 'E100260529', 'customer029@java-bank.demo', '0913000029', N'新竹縣竹北市縣政九路146號', 'cust0029', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'新竹縣竹北市縣政九路146號', N'新竹縣竹北市縣政九路146號', N'教育服務業', N'台北市立大學', 5, 'SAVINGS', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050029-front.jpg', '/uploads/mock/customer/C26050029-back.jpg', '/uploads/mock/customer/C26050029-second.jpg', '/avatars/mock/C26050029.png', N'教育服務業', 900000, 'LOW', N'新竹縣', N'高中', 'S', N'換發', N'薪資收入', '2026-04-06 10:16:00', '2026-04-06 10:16:00'),
    (30, 'C26050030', '2605-C000030', 'AUTH26050030', 'CASE26050030', N'朱佩君', '2001-06-03', 'F', 'F200260530', 'customer030@java-bank.demo', '0913000030', N'苗栗縣苗栗市縣府路100號', 'cust0030', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'苗栗縣苗栗市縣府路100號', N'苗栗縣苗栗市縣府路100號', N'零售服務業', N'統一超商', 10, 'DAILY_EXPENSE', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050030-front.jpg', '/uploads/mock/customer/C26050030-back.jpg', '/uploads/mock/customer/C26050030-second.jpg', '/avatars/mock/C26050030.png', N'零售服務業', 780000, 'LOW', N'苗栗縣', N'大學', 'S', N'補發', N'薪資收入', '2026-05-07 11:23:00', '2026-05-07 11:23:00'),
    (31, 'C26050031', '2605-C000031', 'AUTH26050031', 'CASE26050031', N'高志豪', '1974-07-04', 'M', 'G100260531', 'customer031@java-bank.demo', '0913000031', N'彰化縣彰化市中山路二段416號', 'cust0031', 'INACTIVE', 'INACTIVE', 'APPROVED', 'TW', N'彰化縣彰化市中山路二段416號', N'彰化縣彰化市中山路二段416號', N'公務機關', N'台北市政府', 20, 'SALARY', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050031-front.jpg', '/uploads/mock/customer/C26050031-back.jpg', '/uploads/mock/customer/C26050031-second.jpg', '/avatars/mock/C26050031.png', N'公務機關', 820000, 'LOW', N'彰化縣', N'大學', 'M', N'初發', N'薪資收入', '2026-01-08 12:30:00', '2026-01-08 12:30:00'),
    (32, 'C26050032', '2605-C000032', 'AUTH26050032', 'CASE26050032', N'宋筱涵', '1981-08-05', 'F', 'H200260532', 'customer032@java-bank.demo', '0913000032', N'南投縣南投市中興路660號', 'cust0032', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'南投縣南投市中興路660號', N'南投縣南投市中興路660號', N'自營商', N'自營工作室', 30, 'BUSINESS', 'BUSINESS_INCOME', 'TW', 0, '/uploads/mock/customer/C26050032-front.jpg', '/uploads/mock/customer/C26050032-back.jpg', '/uploads/mock/customer/C26050032-second.jpg', '/avatars/mock/C26050032.png', N'自營商', 1260000, 'LOW', N'南投縣', N'碩士', 'S', N'換發', N'營業收入', '2026-02-09 13:37:00', '2026-02-09 13:37:00'),
    (33, 'C26050033', '2605-C000033', 'AUTH26050033', 'CASE26050033', N'羅哲維', '1988-09-06', 'M', 'J100260533', 'customer033@java-bank.demo', '0913000033', N'雲林縣斗六市雲林路二段515號', 'cust0033', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'雲林縣斗六市雲林路二段515號', N'雲林縣斗六市雲林路二段515號', N'物流運輸業', N'長榮航空', 50, 'PAYMENT', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050033-front.jpg', '/uploads/mock/customer/C26050033-back.jpg', '/uploads/mock/customer/C26050033-second.jpg', '/avatars/mock/C26050033.png', N'物流運輸業', 880000, 'LOW', N'雲林縣', N'專科', 'S', N'補發', N'薪資收入', '2026-03-10 14:44:00', '2026-03-10 14:44:00'),
    (34, 'C26050034', '2605-C000034', 'AUTH26050034', 'CASE26050034', N'葉心怡', '1995-10-07', 'F', 'K200260534', 'customer034@java-bank.demo', '0913000034', N'嘉義市東區中山路199號', 'cust0034', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'嘉義市東區中山路199號', N'嘉義市東區中山路199號', N'餐飲服務業', N'王品集團', 80, 'DAILY_EXPENSE', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050034-front.jpg', '/uploads/mock/customer/C26050034-back.jpg', '/uploads/mock/customer/C26050034-second.jpg', '/avatars/mock/C26050034.png', N'餐飲服務業', 700000, 'LOW', N'嘉義市', N'高中', 'M', N'初發', N'薪資收入', '2026-04-11 15:51:00', '2026-04-11 15:51:00'),
    (35, 'C26050035', '2605-C000035', 'AUTH26050035', 'CASE26050035', N'潘子豪', '1968-11-08', 'M', 'L100260535', 'customer035@java-bank.demo', '0913000035', N'嘉義縣太保市祥和一路東段1號', 'cust0035', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'嘉義縣太保市祥和一路東段1號', N'嘉義縣太保市祥和一路東段1號', N'法律會計業', N'勤業眾信', 100, 'INVESTMENT', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050035-front.jpg', '/uploads/mock/customer/C26050035-back.jpg', '/uploads/mock/customer/C26050035-second.jpg', '/avatars/mock/C26050035.png', N'法律會計業', 1420000, 'LOW', N'嘉義縣', N'大學', 'S', N'換發', N'薪資收入', '2026-05-12 16:58:00', '2026-05-12 16:58:00'),
    (36, 'C26050036', '2605-C000036', 'AUTH26050036', 'CASE26050036', N'馮欣妤', '1975-12-09', 'F', 'M200260536', 'customer036@java-bank.demo', '0913000036', N'屏東縣屏東市自由路527號', 'cust0036', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'屏東縣屏東市自由路527號', N'屏東縣屏東市自由路527號', N'建築營造業', N'遠雄建設', 5, 'BUSINESS', 'BUSINESS_INCOME', 'TW', 0, '/uploads/mock/customer/C26050036-front.jpg', '/uploads/mock/customer/C26050036-back.jpg', '/uploads/mock/customer/C26050036-second.jpg', '/avatars/mock/C26050036.png', N'建築營造業', 1320000, 'LOW', N'屏東縣', N'大學', 'S', N'補發', N'營業收入', '2026-01-13 17:05:00', '2026-01-13 17:05:00'),
    (37, 'C26050037', '2605-C000037', 'AUTH26050037', 'CASE26050037', N'戴建宏', '1982-01-10', 'M', 'N100260537', 'customer037@java-bank.demo', '0913000037', N'宜蘭縣宜蘭市縣政北路1號', 'cust0037', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'宜蘭縣宜蘭市縣政北路1號', N'宜蘭縣宜蘭市縣政北路1號', N'資訊科技業', N'台積電', 10, 'SALARY', 'SALARY', 'US', 0, '/uploads/mock/customer/C26050037-front.jpg', '/uploads/mock/customer/C26050037-back.jpg', '/uploads/mock/customer/C26050037-second.jpg', '/avatars/mock/C26050037.png', N'資訊科技業', 1020000, 'MEDIUM', N'宜蘭縣', N'碩士', 'M', N'初發', N'薪資收入', '2026-02-14 09:12:00', '2026-02-14 09:12:00'),
    (38, 'C26050038', '2605-C000038', 'AUTH26050038', 'CASE26050038', N'袁佳蓉', '1989-02-11', 'F', 'P200260538', 'customer038@java-bank.demo', '0913000038', N'花蓮縣花蓮市府前路17號', 'cust0038', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'花蓮縣花蓮市府前路17號', N'花蓮縣花蓮市府前路17號', N'金融保險業', N'國泰金控', 20, 'INVESTMENT', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050038-front.jpg', '/uploads/mock/customer/C26050038-back.jpg', '/uploads/mock/customer/C26050038-second.jpg', '/avatars/mock/C26050038.png', N'金融保險業', 1200000, 'LOW', N'花蓮縣', N'專科', 'S', N'換發', N'薪資收入', '2026-03-15 10:19:00', '2026-03-15 10:19:00'),
    (39, 'C26050039', '2605-C000039', 'AUTH26050039', 'CASE26050039', N'侯彥廷', '1996-03-12', 'M', 'Q100260539', 'customer039@java-bank.demo', '0913000039', N'台東縣台東市中山路276號', 'cust0039', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'台東縣台東市中山路276號', N'台東縣台東市中山路276號', N'製造業', N'中鋼公司', 30, 'SAVINGS', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050039-front.jpg', '/uploads/mock/customer/C26050039-back.jpg', '/uploads/mock/customer/C26050039-second.jpg', '/avatars/mock/C26050039.png', N'製造業', 960000, 'LOW', N'台東縣', N'高中', 'S', N'補發', N'薪資收入', '2026-04-16 11:26:00', '2026-04-16 11:26:00'),
    (40, 'C26050040', '2605-C000040', 'AUTH26050040', 'CASE26050040', N'丁婉如', '1969-04-13', 'F', 'R200260540', 'customer040@java-bank.demo', '0913000040', N'澎湖縣馬公市治平路32號', 'cust0040', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'澎湖縣馬公市治平路32號', N'澎湖縣馬公市治平路32號', N'醫療保健業', N'長庚醫院', 50, 'SAVINGS', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050040-front.jpg', '/uploads/mock/customer/C26050040-back.jpg', '/uploads/mock/customer/C26050040-second.jpg', '/avatars/mock/C26050040.png', N'醫療保健業', 1140000, 'LOW', N'澎湖縣', N'大學', 'M', N'初發', N'薪資收入', '2026-04-17 12:33:00', '2026-04-17 12:33:00'),
    (41, 'C26050041', '2605-C000041', 'AUTH26050041', 'CASE26050041', N'詹博文', '1976-05-14', 'M', 'S100260541', 'customer041@java-bank.demo', '0913000041', N'台北市信義區信義路五段7號', 'cust0041', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'台北市信義區信義路五段7號', N'台北市信義區信義路五段7號', N'教育服務業', N'台北市立大學', 80, 'SAVINGS', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050041-front.jpg', '/uploads/mock/customer/C26050041-back.jpg', '/uploads/mock/customer/C26050041-second.jpg', '/avatars/mock/C26050041.png', N'教育服務業', 720000, 'LOW', N'台北市', N'大學', 'S', N'換發', N'薪資收入', '2026-01-18 13:40:00', '2026-01-18 13:40:00'),
    (42, 'C26050042', '2605-C000042', 'AUTH26050042', 'CASE26050042', N'梁詠晴', '1983-06-15', 'F', 'T200260542', 'customer042@java-bank.demo', '0913000042', N'新北市板橋區中山路一段161號', 'cust0042', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'新北市板橋區中山路一段161號', N'新北市板橋區中山路一段161號', N'零售服務業', N'統一超商', 100, 'DAILY_EXPENSE', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050042-front.jpg', '/uploads/mock/customer/C26050042-back.jpg', '/uploads/mock/customer/C26050042-second.jpg', '/avatars/mock/C26050042.png', N'零售服務業', 600000, 'LOW', N'新北市', N'碩士', 'S', N'補發', N'薪資收入', '2026-02-19 14:47:00', '2026-02-19 14:47:00'),
    (43, 'C26050043', '2605-C000043', 'AUTH26050043', 'CASE26050043', N'沈志偉', '1990-07-16', 'M', 'U100260543', 'customer043@java-bank.demo', '0913000043', N'桃園市中壢區中大路300號', 'cust0043', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'桃園市中壢區中大路300號', N'桃園市中壢區中大路300號', N'公務機關', N'台北市政府', 5, 'SALARY', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050043-front.jpg', '/uploads/mock/customer/C26050043-back.jpg', '/uploads/mock/customer/C26050043-second.jpg', '/avatars/mock/C26050043.png', N'公務機關', 940000, 'LOW', N'桃園市', N'專科', 'M', N'初發', N'薪資收入', '2026-03-20 15:54:00', '2026-03-20 15:54:00'),
    (44, 'C26050044', '2605-C000044', 'AUTH26050044', 'CASE26050044', N'謝佳玲', '1997-08-17', 'F', 'V200260544', 'customer044@java-bank.demo', '0913000044', N'台中市西屯區台灣大道三段99號', 'cust0044', 'INACTIVE', 'INACTIVE', 'APPROVED', 'TW', N'台中市西屯區台灣大道三段99號', N'台中市西屯區台灣大道三段99號', N'自營商', N'自營工作室', 10, 'BUSINESS', 'BUSINESS_INCOME', 'TW', 0, '/uploads/mock/customer/C26050044-front.jpg', '/uploads/mock/customer/C26050044-back.jpg', '/uploads/mock/customer/C26050044-second.jpg', '/avatars/mock/C26050044.png', N'自營商', 1380000, 'LOW', N'台中市', N'高中', 'S', N'換發', N'營業收入', '2026-04-21 16:01:00', '2026-04-21 16:01:00'),
    (45, 'C26050045', '2605-C000045', 'AUTH26050045', 'CASE26050045', N'彭宗翰', '1970-09-18', 'M', 'X100260545', 'customer045@java-bank.demo', '0913000045', N'台南市東區大學路1號', 'cust0045', 'ACTIVE', 'ACTIVE', 'PENDING', 'TW', N'台南市東區大學路1號', N'台南市東區大學路1號', N'物流運輸業', N'長榮航空', 20, 'PAYMENT', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050045-front.jpg', '/uploads/mock/customer/C26050045-back.jpg', '/uploads/mock/customer/C26050045-second.jpg', '/avatars/mock/C26050045.png', N'物流運輸業', 1000000, 'LOW', N'台南市', N'大學', 'S', N'補發', N'薪資收入', '2026-04-22 17:08:00', '2026-04-22 17:08:00'),
    (46, 'C26050046', '2605-C000046', 'AUTH26050046', 'CASE26050046', N'董宜臻', '1977-10-19', 'F', 'Y200260546', 'customer046@java-bank.demo', '0913000046', N'高雄市前鎮區中山二路2號', 'cust0046', 'ACTIVE', 'ACTIVE', 'SUPPLEMENT_REQUIRED', 'TW', N'高雄市前鎮區中山二路2號', N'高雄市前鎮區中山二路2號', N'餐飲服務業', N'王品集團', 30, 'DAILY_EXPENSE', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050046-front.jpg', '/uploads/mock/customer/C26050046-back.jpg', '/uploads/mock/customer/C26050046-second.jpg', '/avatars/mock/C26050046.png', N'餐飲服務業', 520000, 'LOW', N'高雄市', N'大學', 'M', N'初發', N'薪資收入', '2026-01-23 09:15:00', '2026-01-23 09:15:00'),
    (47, 'C26050047', '2605-C000047', 'AUTH26050047', 'CASE26050047', N'唐柏霖', '1984-11-20', 'M', 'W100260547', 'customer047@java-bank.demo', '0913000047', N'基隆市中正區義一路1號', 'cust0047', 'ACTIVE', 'ACTIVE', 'REJECTED', 'TW', N'基隆市中正區義一路1號', N'基隆市中正區義一路1號', N'法律會計業', N'勤業眾信', 50, 'INVESTMENT', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050047-front.jpg', '/uploads/mock/customer/C26050047-back.jpg', '/uploads/mock/customer/C26050047-second.jpg', '/avatars/mock/C26050047.png', N'法律會計業', 1240000, 'LOW', N'基隆市', N'碩士', 'S', N'換發', N'薪資收入', '2026-02-24 10:22:00', '2026-02-24 10:22:00'),
    (48, 'C26050048', '2605-C000048', 'AUTH26050048', 'CASE26050048', N'魏思妤', '1991-12-21', 'F', 'Z200260548', 'customer048@java-bank.demo', '0913000048', N'新竹市東區光復路二段101號', 'cust0048', 'ACTIVE', 'ACTIVE', 'CANCELLED', 'TW', N'新竹市東區光復路二段101號', N'新竹市東區光復路二段101號', N'建築營造業', N'遠雄建設', 80, 'BUSINESS', 'BUSINESS_INCOME', 'TW', 0, '/uploads/mock/customer/C26050048-front.jpg', '/uploads/mock/customer/C26050048-back.jpg', '/uploads/mock/customer/C26050048-second.jpg', '/avatars/mock/C26050048.png', N'建築營造業', 1440000, 'MEDIUM', N'新竹市', N'專科', 'S', N'補發', N'營業收入', '2026-03-25 11:29:00', '2026-03-25 11:29:00'),
    (49, 'C26050049', '2605-C000049', 'AUTH26050049', 'CASE26050049', N'簡宏達', '1998-01-22', 'M', 'A100260549', 'customer049@java-bank.demo', '0913000049', N'新竹縣竹北市縣政九路146號', 'cust0049', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'新竹縣竹北市縣政九路146號', N'新竹縣竹北市縣政九路146號', N'資訊科技業', N'台積電', 100, 'SALARY', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050049-front.jpg', '/uploads/mock/customer/C26050049-back.jpg', '/uploads/mock/customer/C26050049-second.jpg', '/avatars/mock/C26050049.png', N'資訊科技業', 1140000, 'LOW', N'新竹縣', N'高中', 'M', N'初發', N'薪資收入', '2026-04-02 12:36:00', '2026-04-02 12:36:00'),
    (50, 'C26050050', '2605-C000050', 'AUTH26050050', 'CASE26050050', N'尤芷晴', '1971-02-23', 'F', 'B200260550', 'customer050@java-bank.demo', '0913000050', N'苗栗縣苗栗市縣府路100號', 'cust0050', 'ACTIVE', 'ACTIVE', 'APPROVED', 'TW', N'苗栗縣苗栗市縣府路100號', N'苗栗縣苗栗市縣府路100號', N'金融保險業', N'國泰金控', 5, 'INVESTMENT', 'SALARY', 'TW', 0, '/uploads/mock/customer/C26050050-front.jpg', '/uploads/mock/customer/C26050050-back.jpg', '/uploads/mock/customer/C26050050-second.jpg', '/avatars/mock/C26050050.png', N'金融保險業', 1320000, 'LOW', N'苗栗縣', N'大學', 'S', N'換發', N'薪資收入', '2026-05-03 13:43:00', '2026-05-03 13:43:00');

-- Customer registration/application records.
INSERT INTO CUSTOMER_APPLICATION (
    case_id, customer_id, cif, name, birthday, gender, id_number,
    email, phone, address, username, avatar_url, status, created_at, updated_at
)
SELECT
    case_id, customer_id, cif, name, birthday, gender, id_number,
    email, phone, address, username, avatar_url, application_status, created_at, updated_at
FROM @customers;

-- Formal customer profiles.
INSERT INTO CUSTOMER_PROFILE (
    customer_id, cif, id_number, name, birthday, gender, email, phone, address,
    nationality, registered_address, current_address, occupation, employer,
    estimated_monthly_tx, account_purpose, fund_source, tax_residency, is_pep,
    id_front_url, id_back_url, second_id_url, avatar_url, job, annual_income,
    risk_level, status, created_at, updated_at
)
SELECT
    customer_id, cif, id_number, name, birthday, gender, email, phone, address,
    nationality, registered_address, current_address, occupation, employer,
    estimated_monthly_tx, account_purpose, fund_source, tax_residency, is_pep,
    id_front_url, id_back_url, second_id_url, avatar_url, job, annual_income,
    risk_level, profile_status, created_at, updated_at
FROM @customers;

-- Authentication accounts. Password for every mock customer is 123456.
INSERT INTO CUSTOMER_AUTH (
    auth_id, customer_id, username, password_hash, role, status,
    last_login_date, verification_token, created_at, updated_at
)
SELECT
    auth_id,
    customer_id,
    username,
    '$2a$10$1Wa/uOR8VZXCrOwfd9.2ZOtgSeATa0xVvdSsYudUgtCSsQJ0M2cDS',
    'CUSTOMER',
    auth_status,
    DATEADD(DAY, -1 * (seq % 12), CAST('2026-05-13 09:00:00' AS DATETIME2)),
    NULL,
    created_at,
    updated_at
FROM @customers;

INSERT INTO CUSTOMER_KYC (
    customer_id, id_issue_date, id_issue_location, id_issue_type,
    tax_residency, marital_status, education_level, occupation_category,
    company_name, annual_income, source_of_wealth, updated_at
)
SELECT
    customer_id,
    DATEFROMPARTS(YEAR(birthday) + 20, ((seq - 1) % 12) + 1, ((seq - 1) % 27) + 1),
    issue_location,
    id_issue_type,
    tax_residency,
    marital_status,
    education_level,
    occupation,
    employer,
    annual_income,
    source_of_wealth,
    updated_at
FROM @customers;

INSERT INTO CUSTOMER_RISK_TAG (
    customer_id, aml_risk_level, pep_status, is_fraud_suspect,
    block_reason, kyc_last_review_date, kyc_next_review_date, updated_by, updated_at
)
SELECT
    customer_id,
    CASE risk_level WHEN 'HIGH' THEN 'HIGH' WHEN 'MEDIUM' THEN 'MEDIUM' ELSE 'LOW' END,
    CASE WHEN is_pep = 1 THEN 'Y' ELSE 'N' END,
    CASE WHEN profile_status = 'FROZEN' THEN 'Y' ELSE 'N' END,
    CASE
        WHEN is_pep = 1 THEN N'重要政治性職務人士，需加強審查'
        WHEN profile_status = 'FROZEN' THEN N'帳戶異常活動暫停服務'
        ELSE NULL
    END,
    CAST('2026-05-13' AS DATE),
    DATEADD(YEAR, CASE risk_level WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END, CAST('2026-05-13' AS DATE)),
    'mock-seed',
    updated_at
FROM @customers;

INSERT INTO CUSTOMER_LOGIN_LOG (
    customer_id, username, result, fail_reason, ip_address, user_agent, device_name, login_time
)
SELECT
    customer_id,
    username,
    CASE WHEN seq % 13 = 0 THEN N'失敗' ELSE N'成功' END,
    CASE WHEN seq % 13 = 0 THEN N'密碼輸入錯誤' ELSE NULL END,
    CONCAT('203.0.113.', (seq % 200) + 10),
    CASE WHEN seq % 2 = 0
        THEN N'Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/537.36 Chrome/124.0 Safari/537.36'
        ELSE N'Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148 Safari/604.1'
    END,
    CASE WHEN seq % 2 = 0 THEN N'Chrome / macOS' ELSE N'Safari / iOS' END,
    DATEADD(HOUR, -1 * seq, CAST('2026-05-13 10:00:00' AS DATETIME2))
FROM @customers;

INSERT INTO CUSTOMER_DEVICE (
    customer_id, device_fingerprint, device_name, browser_name, operating_system,
    ip_address, user_agent, status, trusted, first_seen_at, last_seen_at, revoked_at
)
SELECT
    customer_id,
    CONCAT('mock-fingerprint-', RIGHT(CONCAT('0000', seq), 4)),
    CASE WHEN seq % 2 = 0 THEN N'個人筆電' ELSE N'個人手機' END,
    CASE WHEN seq % 2 = 0 THEN N'Chrome' ELSE N'Safari' END,
    CASE WHEN seq % 2 = 0 THEN N'macOS' ELSE N'iOS' END,
    CONCAT('203.0.113.', (seq % 200) + 10),
    CASE WHEN seq % 2 = 0
        THEN N'Mozilla/5.0 (Macintosh; Intel Mac OS X 14_5) AppleWebKit/537.36 Chrome/124.0 Safari/537.36'
        ELSE N'Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148 Safari/604.1'
    END,
    CASE WHEN profile_status = 'INACTIVE' THEN 'REVOKED' ELSE 'ACTIVE' END,
    CASE WHEN profile_status = 'INACTIVE' THEN 0 ELSE 1 END,
    DATEADD(DAY, -30 - seq, CAST('2026-05-13 10:00:00' AS DATETIME2)),
    DATEADD(HOUR, -1 * seq, CAST('2026-05-13 10:00:00' AS DATETIME2)),
    CASE WHEN profile_status = 'INACTIVE' THEN DATEADD(DAY, -2, CAST('2026-05-13 10:00:00' AS DATETIME2)) ELSE NULL END
FROM @customers;

IF (SELECT COUNT(*) FROM CUSTOMER_PROFILE) <> 50
    THROW 51001, 'customer_insert.sql expected exactly 50 CUSTOMER_PROFILE rows.', 1;

IF EXISTS (
    SELECT 1
    FROM CUSTOMER_PROFILE
    GROUP BY email
    HAVING COUNT(*) > 1
)
    THROW 51002, 'customer_insert.sql generated duplicated customer emails.', 1;

PRINT N'customer_insert.sql mock data completed: 50 customer profiles.';
GO
