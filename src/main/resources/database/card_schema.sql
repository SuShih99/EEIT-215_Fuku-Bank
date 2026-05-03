-- ============================================================
-- Card Module Schema
-- 依賴：customer_profile 表必須先存在
-- ============================================================

-- 卡別資料表
CREATE TABLE [CARD_TYPE] (
    card_type_id INT IDENTITY(1,1) PRIMARY KEY,
    card_type_name NVARCHAR(50) NOT NULL,
    brand NVARCHAR(20) NOT NULL,
    annual_fee DECIMAL(10,2),
    cashback_rate DECIMAL(5,2),
    card_image_url VARCHAR(255)
);

-- 商家資料表
CREATE TABLE [MERCHANT] (
    merchant_id INT PRIMARY KEY,
    merchant_name NVARCHAR(100) NOT NULL,
    merchant_category NVARCHAR(50) NOT NULL
);

-- 信用卡申請主表
CREATE TABLE [CARD_APPLICATION] (
    application_id INT IDENTITY(1,1) PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL,
    apply_date DATETIME DEFAULT GETDATE(),
    status NVARCHAR(20) DEFAULT 'PENDING',
    remark NVARCHAR(200),
    FOREIGN KEY (customer_id) REFERENCES customer_profile(customer_id)
);

-- 信用卡申請明細（可一次申請多張卡）
CREATE TABLE [CARD_APPLICATION_ITEM] (
    item_id INT IDENTITY(1,1) PRIMARY KEY,
    application_id INT NOT NULL,
    card_type_id INT NOT NULL,
    result NVARCHAR(20) DEFAULT 'PENDING',
    approved_limit DECIMAL(15,2),
    annual_fee DECIMAL(10,2),
    create_card_flag BIT DEFAULT 0,
    remark NVARCHAR(200),
    FOREIGN KEY (application_id) REFERENCES CARD_APPLICATION(application_id),
    FOREIGN KEY (card_type_id) REFERENCES CARD_TYPE(card_type_id)
);

-- 信用卡主表
CREATE TABLE [CREDIT_CARD] (
    card_id INT IDENTITY(1,1) PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL,
    card_type_id INT NOT NULL,
    application_item_id INT NULL,
    card_number VARCHAR(16) UNIQUE NOT NULL,
    expiry_date DATE NOT NULL,
    credit_limit DECIMAL(15,2),
    current_balance DECIMAL(15,2) DEFAULT 0,
    create_date DATETIME DEFAULT GETDATE(),
    status NVARCHAR(20) CHECK (status IN ('ACTIVE','BLOCKED')),
    FOREIGN KEY (customer_id) REFERENCES customer_profile(customer_id),
    FOREIGN KEY (card_type_id) REFERENCES CARD_TYPE(card_type_id),
    FOREIGN KEY (application_item_id) REFERENCES CARD_APPLICATION_ITEM(item_id)
);

-- 信用卡帳單
CREATE TABLE [CARD_BILL] (
    bill_id INT IDENTITY(1,1) PRIMARY KEY,
    card_id INT NOT NULL,
    billing_month VARCHAR(7),
    bill_date DATE,
    due_date DATE,
    total_amount DECIMAL(15,2),
    minimum_payment DECIMAL(15,2),
    paid_amount DECIMAL(15,2),
    bill_status NVARCHAR(20),
    FOREIGN KEY (card_id) REFERENCES CREDIT_CARD(card_id)
);

-- 信用卡交易紀錄
CREATE TABLE [CARD_TRANSACTION] (
    txn_id INT IDENTITY(1,1) PRIMARY KEY,
    card_id INT NOT NULL,
    merchant_id INT NULL,
    ref_txn_id INT NULL,
    txn_amount DECIMAL(15,2) NOT NULL,
    txn_type NVARCHAR(20),
    txn_date DATETIME,
    description NVARCHAR(200),
    FOREIGN KEY (card_id) REFERENCES CREDIT_CARD(card_id),
    FOREIGN KEY (merchant_id) REFERENCES MERCHANT(merchant_id),
    FOREIGN KEY (ref_txn_id) REFERENCES CARD_TRANSACTION(txn_id)
);
