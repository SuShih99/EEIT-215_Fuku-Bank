-- Card module schema

CREATE TABLE [CARD_TYPE] (
    card_type_id INT IDENTITY(1,1) PRIMARY KEY,
    card_type_name NVARCHAR(50) NOT NULL,
    brand NVARCHAR(20) NOT NULL,
    annual_fee DECIMAL(15,2),
    cashback_rate DECIMAL(15,2),
    card_image_url VARCHAR(255)
);

CREATE TABLE [MERCHANT] (
    merchant_id INT PRIMARY KEY,
    merchant_name NVARCHAR(100) NOT NULL,
    merchant_category NVARCHAR(50) NOT NULL
);

CREATE TABLE [CARD_APPLICATION] (
    application_id INT IDENTITY(1,1) PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL,
    apply_date DATETIME2 DEFAULT SYSDATETIME(),
    status NVARCHAR(20) DEFAULT 'PENDING',
    remark NVARCHAR(200),
    FOREIGN KEY (customer_id) REFERENCES customer_profile(customer_id)
);

CREATE TABLE [CARD_APPLICATION_ITEM] (
    item_id INT IDENTITY(1,1) PRIMARY KEY,
    application_id INT NOT NULL,
    card_type_id INT NOT NULL,
    result NVARCHAR(20) DEFAULT 'PENDING',
    approved_limit DECIMAL(15,2),
    annual_fee DECIMAL(10,2),
    create_card_flag BIT DEFAULT 0,
    review_date DATETIME2 NULL,
    remark NVARCHAR(200),
    FOREIGN KEY (application_id) REFERENCES CARD_APPLICATION(application_id),
    FOREIGN KEY (card_type_id) REFERENCES CARD_TYPE(card_type_id)
);

CREATE TABLE [CARD_ACCOUNT] (
    id INT IDENTITY(1,1) PRIMARY KEY,
    account_number VARCHAR(20),
    credit_limit DECIMAL(15,2),
    statement_day INT,
    due_days INT,
    customer_id VARCHAR(20) NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customer_profile(customer_id)
);

CREATE TABLE [CREDIT_CARD] (
    card_id INT IDENTITY(1,1) PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL,
    card_type_id INT NOT NULL,
    application_item_id INT NULL,
    card_number VARCHAR(16) UNIQUE NOT NULL,
    expiry_date DATE NOT NULL,
    current_debt DECIMAL(15,2) DEFAULT 0,
    create_date DATETIME2 DEFAULT SYSDATETIME(),
    status NVARCHAR(20) CHECK (status IN ('ACTIVE','BLOCKED','INACTIVE')),
    credit_card_account_number VARCHAR(20),
    card_account_id INT NULL,
    FOREIGN KEY (customer_id) REFERENCES customer_profile(customer_id),
    FOREIGN KEY (card_type_id) REFERENCES CARD_TYPE(card_type_id),
    FOREIGN KEY (application_item_id) REFERENCES CARD_APPLICATION_ITEM(item_id),
    FOREIGN KEY (card_account_id) REFERENCES CARD_ACCOUNT(id)
);

CREATE TABLE [CARD_BILL] (
    bill_id INT IDENTITY(1,1) PRIMARY KEY,
    card_id INT NULL,
    card_account_id INT NULL,
    billing_month VARCHAR(7),
    bill_date DATE,
    due_date DATE,
    total_amount DECIMAL(15,2),
    minimum_payment DECIMAL(15,2),
    paid_amount DECIMAL(15,2),
    bill_status NVARCHAR(20),
    cashback_amount DECIMAL(15,2) DEFAULT 0,
    reward_posted BIT DEFAULT 0,
    reward_reference_id VARCHAR(100),
    FOREIGN KEY (card_id) REFERENCES CREDIT_CARD(card_id),
    FOREIGN KEY (card_account_id) REFERENCES CARD_ACCOUNT(id)
);

CREATE TABLE [CARD_TRANSACTION] (
    txn_id INT IDENTITY(1,1) PRIMARY KEY,
    card_id INT NOT NULL,
    merchant_id INT NULL,
    bill_id INT NULL,
    ref_txn_id INT NULL,
    txn_amount DECIMAL(15,2) NOT NULL,
    cashback_rate DECIMAL(15,2),
    cashback_amount DECIMAL(15,2),
    txn_type NVARCHAR(20),
    txn_date DATETIME2,
    description NVARCHAR(200),
    channel NVARCHAR(50),
    external_txn_id VARCHAR(100),
    FOREIGN KEY (card_id) REFERENCES CREDIT_CARD(card_id),
    FOREIGN KEY (merchant_id) REFERENCES MERCHANT(merchant_id),
    FOREIGN KEY (bill_id) REFERENCES CARD_BILL(bill_id),
    FOREIGN KEY (ref_txn_id) REFERENCES CARD_TRANSACTION(txn_id)
);
