UPDATE la
SET la.account_number = a.account_number
FROM LOAN_ACCOUNT la
JOIN [ACCOUNT] a ON a.customer_id = la.customer_id AND a.account_type = 'LOAN';
