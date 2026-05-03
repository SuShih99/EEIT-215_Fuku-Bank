-- ============================================================
-- Card Module Mock Data
-- 依賴：customer_profile 表必須先有資料（執行 auth_customer_insert.sql）
-- 清除順序：由下往上刪（FK 依賴）
-- ============================================================

-- ===== 清除舊資料（依 FK 順序由下往上刪）=====
DELETE FROM CARD_BILL;
DELETE FROM CARD_TRANSACTION;
DELETE FROM CREDIT_CARD;
DELETE FROM CARD_APPLICATION_ITEM;
DELETE FROM CARD_APPLICATION;
DELETE FROM MERCHANT;
DELETE FROM CARD_TYPE;

-- ===== 重置 IDENTITY 種子 =====
DBCC CHECKIDENT ('CARD_TYPE', RESEED, 0);
DBCC CHECKIDENT ('CARD_APPLICATION', RESEED, 0);
DBCC CHECKIDENT ('CARD_APPLICATION_ITEM', RESEED, 0);
DBCC CHECKIDENT ('CREDIT_CARD', RESEED, 0);
DBCC CHECKIDENT ('CARD_TRANSACTION', RESEED, 0);
DBCC CHECKIDENT ('CARD_BILL', RESEED, 0);

-- ===== 1. CARD_TYPE =====
SET IDENTITY_INSERT CARD_TYPE ON;
INSERT INTO CARD_TYPE (card_type_id, card_type_name, brand, annual_fee, cashback_rate, card_image_url) VALUES
(1, N'現金回饋卡', 'VISA', 1000, 1.5, 'img/cashback1.png'),
(2, N'旅遊卡', 'Master', 2000, 2.0, 'img/travel1.png'),
(3, N'百貨聯名卡', 'JCB', 1500, 1.2, 'img/store1.png'),
(4, N'加油卡', 'VISA', 800, 3.0, 'img/gas1.png'),
(5, N'學生卡', 'Master', 0, 0.5, 'img/student1.png'),
(6, N'商務卡', 'VISA', 5000, 2.5, 'img/business1.png'),
(7, N'航空哩程卡', 'JCB', 3000, 2.2, 'img/airline1.png'),
(8, N'購物卡', 'Master', 1200, 1.8, 'img/shop1.png'),
(9, N'美食卡', 'VISA', 900, 2.0, 'img/food1.png'),
(10, N'頂級黑卡', 'Master', 10000, 3.5, 'img/black1.png');
SET IDENTITY_INSERT CARD_TYPE OFF;

-- ===== 2. MERCHANT（merchant_id 非 IDENTITY，直接 INSERT）=====
INSERT INTO MERCHANT (merchant_id, merchant_name, merchant_category) VALUES
(1, '7-11', 'SHOPPING'),
(2, 'FamilyMart', 'SHOPPING'),
(3, 'Starbucks', 'FOOD'),
(4, 'McDonalds', 'FOOD'),
(5, 'TSR', 'TRAVEL'),
(6, 'Uber', 'TRAVEL'),
(7, 'Shopee', 'SHOPPING'),
(8, 'Netflix', 'ENTERTAINMENT'),
(9, 'Eslite Bookstore', 'SHOPPING'),
(10, 'Carrefour', 'SHOPPING');

-- ===== 3. CARD_APPLICATION（使用 customer_profile 的真實 customer_id）=====
SET IDENTITY_INSERT CARD_APPLICATION ON;
INSERT INTO CARD_APPLICATION (application_id, customer_id, apply_date, status, remark) VALUES
(1, 'X7K9P2M4', GETDATE(), 'PENDING', NULL),
(2, 'V4L6T1Y8', GETDATE(), 'APPROVED', N'信用良好'),
(3, 'D3H8F5G2', GETDATE(), 'REJECTED', N'收入不足'),
(4, 'B9W1C7R5', GETDATE(), 'PENDING', NULL),
(5, 'P6M4N2Q8', GETDATE(), 'APPROVED', NULL),
(6, 'K1T9V5L3', GETDATE(), 'PENDING', NULL),
(7, 'E8C2X7J4', GETDATE(), 'REJECTED', N'信用紀錄不良'),
(8, 'Y5R4W1H6', GETDATE(), 'APPROVED', NULL),
(9, 'G7N3M8P2', GETDATE(), 'PENDING', NULL),
(10, 'J2F6K9V1', GETDATE(), 'APPROVED', NULL);
SET IDENTITY_INSERT CARD_APPLICATION OFF;

-- ===== 4. CARD_APPLICATION_ITEM =====
SET IDENTITY_INSERT CARD_APPLICATION_ITEM ON;
INSERT INTO CARD_APPLICATION_ITEM
(item_id, application_id, card_type_id, result, approved_limit, annual_fee, create_card_flag, remark) VALUES
(1, 1, 1, 'PENDING', NULL, NULL, 0, NULL),
(2, 2, 2, 'APPROVED', 100000, 2000, 1, NULL),
(3, 3, 3, 'REJECTED', NULL, NULL, 0, N'信用評分不足'),
(4, 4, 4, 'PENDING', NULL, NULL, 0, NULL),
(5, 5, 5, 'APPROVED', 50000, 0, 1, NULL),
(6, 6, 6, 'PENDING', NULL, NULL, 0, NULL),
(7, 7, 7, 'REJECTED', NULL, NULL, 0, N'分數過低'),
(8, 8, 8, 'APPROVED', 120000, 1200, 1, NULL),
(9, 9, 9, 'PENDING', NULL, NULL, 0, NULL),
(10, 10, 10, 'APPROVED', 500000, 10000, 1, NULL);
SET IDENTITY_INSERT CARD_APPLICATION_ITEM OFF;

-- ===== 5. CREDIT_CARD（只有 APPROVED 的客戶才有卡）=====
SET IDENTITY_INSERT CREDIT_CARD ON;
INSERT INTO CREDIT_CARD
(card_id, customer_id, card_type_id, application_item_id, card_number, expiry_date, credit_limit, current_balance, status) VALUES
(1, 'V4L6T1Y8', 2, 2, '4000000000000001', '2028-12-31', 100000, 2000, 'ACTIVE'),
(2, 'P6M4N2Q8', 5, 5, '4000000000000002', '2027-06-30', 50000, 1000, 'ACTIVE'),
(3, 'Y5R4W1H6', 8, 8, '4000000000000003', '2029-01-31', 120000, 5000, 'ACTIVE'),
(4, 'J2F6K9V1', 10, 10, '4000000000000004', '2030-12-31', 500000, 20000, 'ACTIVE'),
(5, 'V4L6T1Y8', 1, NULL, '4000000000000005', '2027-05-31', 80000, 3000, 'ACTIVE'),
(6, 'P6M4N2Q8', 4, NULL, '4000000000000006', '2026-11-30', 60000, 4000, 'BLOCKED'),
(7, 'Y5R4W1H6', 6, NULL, '4000000000000007', '2028-09-30', 150000, 7000, 'ACTIVE'),
(8, 'J2F6K9V1', 7, NULL, '4000000000000008', '2029-03-31', 200000, 10000, 'ACTIVE'),
(9, 'V4L6T1Y8', 3, NULL, '4000000000000009', '2027-08-31', 90000, 2000, 'ACTIVE'),
(10, 'P6M4N2Q8', 9, NULL, '4000000000000010', '2026-12-31', 70000, 1500, 'ACTIVE');
SET IDENTITY_INSERT CREDIT_CARD OFF;

-- ===== 6. CARD_TRANSACTION =====
SET IDENTITY_INSERT CARD_TRANSACTION ON;
INSERT INTO CARD_TRANSACTION
(txn_id, card_id, merchant_id, ref_txn_id, txn_amount, txn_type, description) VALUES
(1, 1, 1, NULL, 150, 'PURCHASE', N'7-11 消費'),
(2, 1, 3, NULL, 200, 'PURCHASE', N'星巴克咖啡'),
(3, 2, 4, NULL, 300, 'PURCHASE', N'麥當勞用餐'),
(4, 2, 5, NULL, 1200, 'PURCHASE', N'高鐵車票'),
(5, 3, 7, NULL, 2500, 'PURCHASE', N'蝦皮購物'),
(6, 3, 8, NULL, 390, 'PURCHASE', N'Netflix 訂閱'),
(7, 4, 6, NULL, 500, 'PURCHASE', N'Uber 叫車'),
(8, 4, 2, NULL, 180, 'PURCHASE', N'全家消費'),
(9, 5, 9, NULL, 600, 'PURCHASE', N'誠品購書'),
(10, 5, 10, NULL, 2200, 'PURCHASE', N'家樂福採購');
SET IDENTITY_INSERT CARD_TRANSACTION OFF;

-- ===== 7. CARD_BILL =====
SET IDENTITY_INSERT CARD_BILL ON;
INSERT INTO CARD_BILL
(bill_id, card_id, billing_month, bill_date, due_date, total_amount, minimum_payment, paid_amount, bill_status) VALUES
(1, 1, '2026-03', '2026-03-25', '2026-04-10', 2000, 200, 2000, 'PAID'),
(2, 2, '2026-03', '2026-03-25', '2026-04-10', 1000, 100, 500, 'PARTIAL'),
(3, 3, '2026-03', '2026-03-25', '2026-04-10', 5000, 500, 0, 'UNPAID'),
(4, 4, '2026-03', '2026-03-25', '2026-04-10', 20000, 2000, 20000, 'PAID'),
(5, 5, '2026-03', '2026-03-25', '2026-04-10', 3000, 300, 1000, 'PARTIAL'),
(6, 6, '2026-03', '2026-03-25', '2026-04-10', 4000, 400, 0, 'UNPAID'),
(7, 7, '2026-03', '2026-03-25', '2026-04-10', 7000, 700, 7000, 'PAID'),
(8, 8, '2026-03', '2026-03-25', '2026-04-10', 10000, 1000, 5000, 'PARTIAL'),
(9, 9, '2026-03', '2026-03-25', '2026-04-10', 2000, 200, 0, 'UNPAID'),
(10, 10, '2026-03', '2026-03-25', '2026-04-10', 1500, 150, 1500, 'PAID');
SET IDENTITY_INSERT CARD_BILL OFF;
