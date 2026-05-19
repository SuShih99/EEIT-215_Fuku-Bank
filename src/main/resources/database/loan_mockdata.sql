/*
===============================================================================
Java Easy Bank Loan Demo Data
- Purpose: small, presentation-friendly loan dataset.
- Requires: customer_insert.sql and account_mockdata.sql first.
- Primary demo customer: Q8M4T7K2 / cust0001 / password 123456.
- Covers:
  1. PENDING_CONTACT
  2. IN_CONTACT with uploaded initial documents
  3. RETURNED with required supplement documents
  4. PENDING_REVIEW after document submission
  5. REJECTED
  6. DISBURSED + ACTIVE repayment
  7. DISBURSED + OVERDUE repayment
  8. CLOSED + PAID_OFF account
===============================================================================
*/

SET NOCOUNT ON;

IF OBJECT_ID('LOAN_REPAYMENT', 'U') IS NOT NULL DELETE FROM LOAN_REPAYMENT;
IF OBJECT_ID('LOAN_DOCUMENT', 'U') IS NOT NULL DELETE FROM LOAN_DOCUMENT;
IF OBJECT_ID('LOAN_ACCOUNT', 'U') IS NOT NULL DELETE FROM LOAN_ACCOUNT;
IF OBJECT_ID('LOAN_REVIEW_DETAIL', 'U') IS NOT NULL DELETE FROM LOAN_REVIEW_DETAIL;
IF OBJECT_ID('LOAN_CONTACT_LOG', 'U') IS NOT NULL DELETE FROM LOAN_CONTACT_LOG;
IF OBJECT_ID('LOAN_APPLICATION', 'U') IS NOT NULL DELETE FROM LOAN_APPLICATION;

DECLARE @demoCustomerId VARCHAR(20) = 'Q8M4T7K2';
DECLARE @demoCheckingAccount VARCHAR(14) = '070000000001';

IF NOT EXISTS (SELECT 1 FROM CUSTOMER_PROFILE WHERE customer_id = @demoCustomerId)
    THROW 51201, 'loan_mockdata.sql requires customer Q8M4T7K2 from customer_insert.sql.', 1;

IF NOT EXISTS (SELECT 1 FROM [ACCOUNT] WHERE account_number = '909000000002')
BEGIN
    INSERT INTO [ACCOUNT] (
        account_number, customer_id, account_type, currency, balance, liability,
        interest_rate, status, parent_account_number, created_at, changed_at,
        created_by, changed_by
    ) VALUES (
        '909000000002', 'BANK_INTERNAL', 'BUSINESS', 'TWD', 0.0000, 0.0000,
        NULL, 'ACTIVE', NULL, '2026-01-01 09:00:00', '2026-05-19 09:00:00',
        'loan-demo', 'loan-demo'
    );
END;

IF EXISTS (SELECT 1 FROM [ACCOUNT] WHERE account_number = @demoCheckingAccount)
BEGIN
    UPDATE [ACCOUNT]
    SET
        customer_id = @demoCustomerId,
        account_type = 'CHECKING',
        currency = 'TWD',
        balance = CASE WHEN ISNULL(balance, 0) < 250000 THEN 250000.0000 ELSE balance END,
        liability = 0.0000,
        interest_rate = 0.00500,
        status = 'ACTIVE',
        parent_account_number = NULL,
        changed_at = '2026-05-19 09:00:00',
        changed_by = 'loan-demo'
    WHERE account_number = @demoCheckingAccount;
END
ELSE
BEGIN
    INSERT INTO [ACCOUNT] (
        account_number, customer_id, account_type, currency, balance, liability,
        interest_rate, status, parent_account_number, created_at, changed_at,
        created_by, changed_by
    ) VALUES (
        @demoCheckingAccount, @demoCustomerId, 'CHECKING', 'TWD', 250000.0000, 0.0000,
        0.00500, 'ACTIVE', NULL, '2026-01-02 09:00:00', '2026-05-19 09:00:00',
        'loan-demo', 'loan-demo'
    );
END;

CREATE TABLE #demo_loan_accounts (
    account_number VARCHAR(14) NOT NULL PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL,
    liability DECIMAL(19,4) NOT NULL,
    interest_rate DECIMAL(7,5) NOT NULL,
    created_at DATETIME2 NOT NULL,
    changed_at DATETIME2 NOT NULL
);

INSERT INTO #demo_loan_accounts (
    account_number, customer_id, liability, interest_rate, created_at, changed_at
) VALUES
('90126050000001', @demoCustomerId,  9010.0000, 0.04000, '2026-04-19 10:30:00', '2026-05-19 09:30:00'),
('90126050000002', @demoCustomerId, 12020.0000, 0.04500, '2026-02-19 10:40:00', '2026-05-19 09:40:00'),
('90126050000003', @demoCustomerId,     0.0000, 0.03800, '2025-11-19 10:50:00', '2026-05-19 09:50:00');

UPDATE a
SET
    a.customer_id = d.customer_id,
    a.account_type = 'LOAN',
    a.currency = 'TWD',
    a.balance = 0.0000,
    a.liability = d.liability,
    a.interest_rate = d.interest_rate,
    a.status = 'ACTIVE',
    a.parent_account_number = NULL,
    a.changed_at = d.changed_at,
    a.changed_by = 'loan-demo'
FROM [ACCOUNT] a
JOIN #demo_loan_accounts d ON d.account_number = a.account_number;

INSERT INTO [ACCOUNT] (
    account_number, customer_id, account_type, currency, balance, liability,
    interest_rate, status, parent_account_number, created_at, changed_at,
    created_by, changed_by
)
SELECT
    d.account_number, d.customer_id, 'LOAN', 'TWD', 0.0000, d.liability,
    d.interest_rate, 'ACTIVE', NULL, d.created_at, d.changed_at,
    'loan-demo', 'loan-demo'
FROM #demo_loan_accounts d
WHERE NOT EXISTS (
    SELECT 1
    FROM [ACCOUNT] a
    WHERE a.account_number = d.account_number
);

INSERT INTO LOAN_APPLICATION (
    application_id,
    customer_id,
    apply_type,
    apply_amount,
    apply_period,
    rate,
    disbursement_account,
    application_status,
    create_time,
    latest_contact_status,
    latest_contact_time,
    update_time,
    documents_submitted_at,
    current_supplement_batch_no,
    review_comment,
    required_documents
) VALUES
('LADEMO2605001', @demoCustomerId, 'PERSONAL', 100000.00, 12, 0.040000, @demoCheckingAccount, 'PENDING_CONTACT', '2026-05-19 09:00:00', 'NOT_CONTACTED', NULL, NULL, NULL, 0, NULL, NULL),
('LADEMO2605002', @demoCustomerId, 'CAR',      300000.00, 36, 0.032000, @demoCheckingAccount, 'IN_CONTACT',      '2026-05-18 09:10:00', 'REACHED', '2026-05-18 11:00:00', '2026-05-18 11:00:00', NULL, 0, NULL, NULL),
('LADEMO2605003', @demoCustomerId, 'BUSINESS', 500000.00, 48, 0.035000, @demoCheckingAccount, 'RETURNED',        '2026-05-17 09:20:00', 'CONFIRMED', '2026-05-17 10:30:00', '2026-05-18 15:00:00', NULL, 1, N'Need bank statement and income cert.', N'["BANK_STATEMENT","INCOME_CERT"]'),
('LADEMO2605004', @demoCustomerId, 'PERSONAL', 160000.00, 24, 0.042000, @demoCheckingAccount, 'PENDING_REVIEW',  '2026-05-16 09:30:00', 'CONFIRMED', '2026-05-16 10:30:00', '2026-05-17 13:00:00', '2026-05-17 13:00:00', 0, NULL, NULL),
('LADEMO2605005', @demoCustomerId, 'PERSONAL',  80000.00, 12, 0.050000, @demoCheckingAccount, 'REJECTED',        '2026-05-15 09:40:00', 'CONFIRMED', '2026-05-15 10:10:00', '2026-05-16 16:00:00', '2026-05-15 15:00:00', 0, N'Debt burden exceeds limit.', NULL),
('LADEMO2605006', @demoCustomerId, 'PERSONAL',  12000.00,  4, 0.040000, @demoCheckingAccount, 'DISBURSED',       '2026-04-18 09:50:00', 'CONFIRMED', '2026-04-18 11:00:00', '2026-04-19 10:30:00', '2026-04-18 14:00:00', 0, NULL, NULL),
('LADEMO2605007', @demoCustomerId, 'PERSONAL',  16000.00,  4, 0.045000, @demoCheckingAccount, 'DISBURSED',       '2026-02-18 09:55:00', 'CONFIRMED', '2026-02-18 11:00:00', '2026-05-19 09:40:00', '2026-02-18 14:00:00', 0, NULL, NULL),
('LADEMO2605008', @demoCustomerId, 'PERSONAL',   8000.00,  4, 0.038000, @demoCheckingAccount, 'CLOSED',          '2025-11-18 09:58:00', 'CONFIRMED', '2025-11-18 11:00:00', '2026-05-19 09:50:00', '2025-11-18 14:00:00', 0, NULL, NULL);

INSERT INTO LOAN_CONTACT_LOG (
    log_id, application_id, emp_id, contact_status, contact_channel, contact_time, note
) VALUES
('LCD2605002001', 'LADEMO2605002', 'EMP001', 'REACHED',   'PHONE', '2026-05-18 11:00:00', N'Customer confirmed vehicle loan intent.'),
('LCD2605003001', 'LADEMO2605003', 'EMP001', 'CONFIRMED', 'PHONE', '2026-05-17 10:30:00', N'Customer confirmed business loan details.'),
('LCD2605003002', 'LADEMO2605003', 'EMP002', 'CONFIRMED', 'EMAIL', '2026-05-18 15:00:00', N'Supplement request sent.'),
('LCD2605004001', 'LADEMO2605004', 'EMP001', 'CONFIRMED', 'PHONE', '2026-05-16 10:30:00', N'Initial documents submitted for review.'),
('LCD2605005001', 'LADEMO2605005', 'EMP003', 'CONFIRMED', 'PHONE', '2026-05-15 10:10:00', N'Customer reached before rejection.'),
('LCD2605006001', 'LADEMO2605006', 'EMP001', 'CONFIRMED', 'PHONE', '2026-04-18 11:00:00', N'Loan approved and disbursed.'),
('LCD2605007001', 'LADEMO2605007', 'EMP001', 'CONFIRMED', 'PHONE', '2026-02-18 11:00:00', N'Loan approved and disbursed.'),
('LCD2605008001', 'LADEMO2605008', 'EMP001', 'CONFIRMED', 'PHONE', '2025-11-18 11:00:00', N'Loan approved and later paid off.');

INSERT INTO LOAN_REVIEW_DETAIL (
    review_id,
    application_id,
    confirmed_amount,
    confirmed_period,
    confirmed_rate,
    collateral_note,
    emp_id,
    review_time,
    review_status,
    submitted_time,
    review_note
) VALUES
('LRD2605004001', 'LADEMO2605004', 160000.00, 24, 0.042000, N'No collateral. Salary transfer record available.', 'EMP001', '2026-05-17 13:00:00', 'SUBMITTED', '2026-05-17 13:00:00', N'Waiting for risk review result.'),
('LRD2605005001', 'LADEMO2605005',  80000.00, 12, 0.050000, N'No collateral.', 'EMP003', '2026-05-16 16:00:00', 'SUBMITTED', '2026-05-16 16:00:00', N'Rejected due to high debt burden.'),
('LRD2605006001', 'LADEMO2605006',  12000.00,  4, 0.040000, N'Approved for short-term personal loan demo.', 'EMP001', '2026-04-19 10:30:00', 'SUBMITTED', '2026-04-19 10:30:00', N'Disbursed. One installment already paid.'),
('LRD2605007001', 'LADEMO2605007',  16000.00,  4, 0.045000, N'Approved for overdue repayment demo.', 'EMP001', '2026-02-19 10:40:00', 'SUBMITTED', '2026-02-19 10:40:00', N'Disbursed. Second installment is overdue.'),
('LRD2605008001', 'LADEMO2605008',   8000.00,  4, 0.038000, N'Approved for paid-off account demo.', 'EMP001', '2025-11-19 10:50:00', 'SUBMITTED', '2025-11-19 10:50:00', N'Fully repaid and closed.');

INSERT INTO LOAN_DOCUMENT (
    document_id,
    application_id,
    document_type,
    file_url,
    original_name,
    uploaded_by,
    upload_time,
    document_batch_type,
    document_batch_no,
    submitted_at
) VALUES
('LDD2605002001', 'LADEMO2605002', 'ID_CARD',        '/uploads/mock/loan/LADEMO2605002/id-card.pdf',        N'id-card.pdf',        @demoCustomerId, '2026-05-18 10:40:00', 'INITIAL',    0, NULL),
('LDD2605002002', 'LADEMO2605002', 'INCOME_CERT',    '/uploads/mock/loan/LADEMO2605002/income-cert.pdf',    N'income-cert.pdf',    @demoCustomerId, '2026-05-18 10:45:00', 'INITIAL',    0, NULL),
('LDD2605003001', 'LADEMO2605003', 'ID_CARD',        '/uploads/mock/loan/LADEMO2605003/id-card.pdf',        N'id-card.pdf',        @demoCustomerId, '2026-05-17 11:20:00', 'INITIAL',    0, '2026-05-17 12:00:00'),
('LDD2605003002', 'LADEMO2605003', 'BANK_STATEMENT', '/uploads/mock/loan/LADEMO2605003/bank-statement.pdf', N'bank-statement.pdf', @demoCustomerId, '2026-05-18 16:00:00', 'SUPPLEMENT', 1, NULL),
('LDD2605004001', 'LADEMO2605004', 'ID_CARD',        '/uploads/mock/loan/LADEMO2605004/id-card.pdf',        N'id-card.pdf',        @demoCustomerId, '2026-05-16 12:00:00', 'INITIAL',    0, '2026-05-17 13:00:00'),
('LDD2605004002', 'LADEMO2605004', 'INCOME_CERT',    '/uploads/mock/loan/LADEMO2605004/income-cert.pdf',    N'income-cert.pdf',    @demoCustomerId, '2026-05-16 12:05:00', 'INITIAL',    0, '2026-05-17 13:00:00'),
('LDD2605005001', 'LADEMO2605005', 'ID_CARD',        '/uploads/mock/loan/LADEMO2605005/id-card.pdf',        N'id-card.pdf',        @demoCustomerId, '2026-05-15 12:00:00', 'INITIAL',    0, '2026-05-15 15:00:00'),
('LDD2605006001', 'LADEMO2605006', 'ID_CARD',        '/uploads/mock/loan/LADEMO2605006/id-card.pdf',        N'id-card.pdf',        @demoCustomerId, '2026-04-18 12:00:00', 'INITIAL',    0, '2026-04-18 14:00:00'),
('LDD2605007001', 'LADEMO2605007', 'ID_CARD',        '/uploads/mock/loan/LADEMO2605007/id-card.pdf',        N'id-card.pdf',        @demoCustomerId, '2026-02-18 12:00:00', 'INITIAL',    0, '2026-02-18 14:00:00'),
('LDD2605008001', 'LADEMO2605008', 'ID_CARD',        '/uploads/mock/loan/LADEMO2605008/id-card.pdf',        N'id-card.pdf',        @demoCustomerId, '2025-11-18 12:00:00', 'INITIAL',    0, '2025-11-18 14:00:00');

INSERT INTO LOAN_ACCOUNT (
    account_id,
    account_number,
    application_id,
    customer_id,
    apply_type,
    principal_amount,
    confirmed_period,
    rate,
    monthly_payment,
    paid_periods,
    remaining_principal,
    start_date,
    next_payment_date,
    account_status,
    create_time,
    update_time
) VALUES
('LACDEMOACTIVE01',  '90126050000001', 'LADEMO2605006', @demoCustomerId, 'PERSONAL', 12000, 4, 0.040000, 3030.00, 1,  9010.00, '2026-04-19', '2026-06-19', 'ACTIVE',   '2026-04-19 10:30:00', '2026-05-19 09:30:00'),
('LACDEMOOVERDUE01', '90126050000002', 'LADEMO2605007', @demoCustomerId, 'PERSONAL', 16000, 4, 0.045000, 4030.00, 1, 12020.00, '2026-02-19', '2026-04-19', 'OVERDUE',  '2026-02-19 10:40:00', '2026-05-19 09:40:00'),
('LACDEMOPAIDOFF01', '90126050000003', 'LADEMO2605008', @demoCustomerId, 'PERSONAL',  8000, 4, 0.038000, 2020.00, 4,     0.00, '2025-11-19', NULL,         'PAID_OFF', '2025-11-19 10:50:00', '2026-05-19 09:50:00');

INSERT INTO LOAN_REPAYMENT (
    repayment_id,
    account_id,
    period_index,
    scheduled_date,
    paid_date,
    total_amount,
    principal_portion,
    interest_portion,
    remaining_after,
    repayment_status,
    create_time,
    update_time
) VALUES
('RPD_ACTIVE_001', 'LACDEMOACTIVE01', 1, '2026-05-19', '2026-05-19', 3030.00, 2990.00, 40.00, 9010.00, 'PAID',      '2026-04-19 10:31:00', '2026-05-19 09:30:00'),
('RPD_ACTIVE_002', 'LACDEMOACTIVE01', 2, '2026-06-19', NULL,         3030.00, 3000.00, 30.00, 6010.00, 'SCHEDULED', '2026-04-19 10:31:00', NULL),
('RPD_ACTIVE_003', 'LACDEMOACTIVE01', 3, '2026-07-19', NULL,         3030.00, 3010.00, 20.00, 3000.00, 'SCHEDULED', '2026-04-19 10:31:00', NULL),
('RPD_ACTIVE_004', 'LACDEMOACTIVE01', 4, '2026-08-19', NULL,         3010.00, 3000.00, 10.00,    0.00, 'SCHEDULED', '2026-04-19 10:31:00', NULL),

('RPD_OVERDUE_001', 'LACDEMOOVERDUE01', 1, '2026-03-19', '2026-03-19', 4030.00, 3980.00, 50.00, 12020.00, 'PAID',      '2026-02-19 10:41:00', '2026-03-19 09:00:00'),
('RPD_OVERDUE_002', 'LACDEMOOVERDUE01', 2, '2026-04-19', NULL,         4030.00, 4000.00, 30.00,  8020.00, 'OVERDUE',   '2026-02-19 10:41:00', '2026-05-19 09:40:00'),
('RPD_OVERDUE_003', 'LACDEMOOVERDUE01', 3, '2026-05-19', NULL,         4030.00, 4010.00, 20.00,  4010.00, 'SCHEDULED', '2026-02-19 10:41:00', NULL),
('RPD_OVERDUE_004', 'LACDEMOOVERDUE01', 4, '2026-06-19', NULL,         4020.00, 4010.00, 10.00,     0.00, 'SCHEDULED', '2026-02-19 10:41:00', NULL),

('RPD_PAIDOFF_001', 'LACDEMOPAIDOFF01', 1, '2025-12-19', '2025-12-19', 2020.00, 1990.00, 30.00, 6010.00, 'PAID', '2025-11-19 10:51:00', '2025-12-19 09:00:00'),
('RPD_PAIDOFF_002', 'LACDEMOPAIDOFF01', 2, '2026-01-19', '2026-01-19', 2020.00, 2000.00, 20.00, 4010.00, 'PAID', '2025-11-19 10:51:00', '2026-01-19 09:00:00'),
('RPD_PAIDOFF_003', 'LACDEMOPAIDOFF01', 3, '2026-02-19', '2026-02-19', 2020.00, 2010.00, 10.00, 2000.00, 'PAID', '2025-11-19 10:51:00', '2026-02-19 09:00:00'),
('RPD_PAIDOFF_004', 'LACDEMOPAIDOFF01', 4, '2026-03-19', '2026-03-19', 2000.00, 2000.00,  0.00,    0.00, 'PAID', '2025-11-19 10:51:00', '2026-03-19 09:00:00');

DROP TABLE #demo_loan_accounts;

PRINT N'loan_mockdata.sql completed: seeded compact loan demo data for Q8M4T7K2.';

SET NOCOUNT OFF;
GO
