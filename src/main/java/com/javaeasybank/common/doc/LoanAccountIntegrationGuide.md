# Loan 與 Account 模組交接文件

本文提供 Loan 模組呼叫 Account 模組的整合方式。Loan 模組負責貸款申請、審核、期別與業務狀態；Account 模組負責貸款帳戶、撥款、還款、利息、交易紀錄與悲觀鎖一致性。

## 一、Loan 相關帳戶

| AccountType | 用途 | balance | liability | rate | 是否顯示於一般帳戶查詢 |
|---|---|---:|---:|---:|---|
| `BUSINESS` | 銀行內部撥款/收款帳戶 | 可變動 | 0 | NULL | 否 |
| `LOAN` | 貸款負債帳戶 | 永遠 0 | 可變動 | 可有 | 否 |

一般 user 端帳戶查詢會排除 `BUSINESS` 與 `LOAN`。一般轉帳、存提款、換匯、沖正也不可把這兩種特殊帳戶當一般餘額帳戶操作。

## 二、系統業務帳戶

`account_init.sql` 會初始化兩個 `BUSINESS` 帳戶：

| 帳號 | 用途 | 初始餘額 |
|---|---|---:|
| `909000000001` | 銀行撥款帳戶 | `999999999999.0000` |
| `909000000002` | 銀行收款帳戶 | `0.0000` |

兩者 `customer_id` 皆為 `BANK_INTERNAL`，幣別為 `TWD`，狀態為 `ACTIVE`，沒有利率。

## 三、貸款帳號編碼

Account 端會讀取 `CUSTOMER_PROFILE.id_number`，把英文字母轉為 `01` 到 `26`，數字保留。

範例：

```txt
A123456789 -> 01123456789
901 + 01123456789 = 90101123456789
```

貸款帳戶使用 `901` 起跳；若 `901...` 已存在，會依序嘗試 `902...`、`903...`，最多到 `999...`。

## 四、Loan 模組呼叫入口

主要 service：

```java
com.javaeasybank.account.service.AccountIntegrationService
```

### 1. 建立貸款帳戶

```java
LoanAccountResponse createLoanAccount(LoanAccountCreateRequest request)
```

Request：

| 欄位 | 說明 |
|---|---|
| `customerId` | 顧客 ID |
| `liability` | 初始負債 |
| `rate` | 貸款利率 |

回傳 `loanAccountNumber`。Account 端會建立 `AccountType.LOAN`，幣別固定 `TWD`，`balance` 固定為 0。

### 2. 貸款撥款

```java
LoanAccountTransactionResponse disburseLoan(LoanDisbursementRequest request)
```

流程：

1. 銀行撥款帳戶 `909000000001` 扣款。
2. 指定的顧客台幣活存帳戶入帳。
3. 寫入兩筆 `LOAN_DISBURSEMENT` 交易紀錄，同一個 `referenceId`。
4. 貸款帳戶的 `balance` 不會變動。

### 3. 新增貸款利息

```java
LoanAccountTransactionResponse addLoanInterest(LoanInterestRequest request)
```

流程：

1. 檢查貸款帳戶為 `LOAN`、`ACTIVE`、`balance = 0`。
2. `liability += amount`。
3. 寫入一筆 `INTEREST` 交易紀錄。

### 4. 查顧客可扣款帳戶

```java
List<AccountResponse> getActiveTwdCheckingAccounts(String customerId)
```

只回傳該顧客名下 `ACTIVE + CHECKING + TWD` 帳戶，供 Loan 前端選擇扣款帳戶。

### 5. 顧客還款

```java
LoanAccountTransactionResponse repayLoan(String customerId, LoanRepaymentRequest request)
```

流程使用悲觀鎖鎖住：

1. 顧客扣款帳戶。
2. 貸款帳戶。
3. 銀行收款帳戶 `909000000002`。

交易必須三件事全部成功才 commit：

1. 顧客台幣活存 `balance -= amount`。
2. 貸款帳戶 `liability -= amount`，且 `balance` 維持 0。
3. 銀行收款帳戶 `balance += amount`。

若金額大於剩餘負債，Account 端會拒絕還款。

### 6. 查還款紀錄

```java
Page<TransLogResponse> getLoanRepaymentRecords(
    String customerId,
    String loanAccountNumber,
    LocalDate startDate,
    LocalDate endDate,
    Pageable pageable
)
```

可用 `customerId` 查該客戶所有貸款還款紀錄，也可用 `loanAccountNumber` 查單一貸款帳戶。回傳的是貸款帳戶上的 `LOAN_REPAYMENT + CREDIT` 紀錄。

## 五、風控核准後的撥款流程

貸款申請完成二次填單並送審後，Loan 模組會將審核條件送到 Risk 模組。Risk 模組確認核准後，應透過 callback 推進撥款流程。

建議主流程：

1. 行員在 Loan 管理端完成二次填單，送出 `PATCH /api/admin/loan-applications/{id}/review/submit`。
2. Loan 模組將申請狀態由 `IN_CONTACT` 推進為 `PENDING_REVIEW`，並送出風控審核資料。
3. Risk 模組審核通過後，呼叫 `POST /api/loan-callbacks/{applicationId}/status`，body 帶入：

```json
{
  "newStatus": "APPROVED",
  "callerModule": "RISK",
  "note": "風控審核通過"
}
```

4. Loan 模組收到 Risk callback 後，必須確認目前狀態為 `PENDING_REVIEW`，才可套用 `APPROVED`。
5. Loan 模組以二次填單的核准金額、核准期數、利率與原申請客戶建立貸款負債帳戶：

```java
LoanAccountResponse loanAccount = accountIntegrationService.createLoanAccount(
    new LoanAccountCreateRequest(customerId, confirmedAmount, confirmedRate)
);
```

6. Loan 模組再呼叫 Account 撥款：

```java
accountIntegrationService.disburseLoan(
    new LoanDisbursementRequest(
        loanAccount.getLoanAccountNumber(),
        disbursementAccount,
        confirmedAmount,
        "貸款核准撥款 applicationId=" + applicationId
    )
);
```

7. Account 模組在同一筆撥款交易中完成：
   - 鎖定銀行撥款帳戶、貸款帳戶、客戶入帳帳戶。
   - 驗證貸款帳戶與入帳帳戶屬於同一客戶。
   - 銀行撥款帳戶 `909000000001` 扣款。
   - 客戶台幣活存帳戶入帳。
   - 寫入兩筆 `LOAN_DISBURSEMENT` 交易紀錄。
8. 撥款成功後，Loan 模組將申請狀態由 `APPROVED` 推進為 `DISBURSED`，並可建立 Loan 模組自己的 `loan_account` / repayment schedule 資料。
9. 若建立貸款帳戶或撥款任一步失敗，申請不可標記為 `DISBURSED`；應保留在 `APPROVED` 或記錄待補償狀態，供後續重試或人工處理。

目前程式狀態：

- `LoanApplicationService.handleStatusCallback` 目前只處理 `RISK` callback，並只更新 `APPROVED` / `REJECTED`。
- `AccountIntegrationService.createLoanAccount` 與 `disburseLoan` 已存在，但尚未由 Loan callback 串接。
- 前端貸款申請頁已送出 `disbursementAccount`，但 `LoanMemberRequestDTO` 與 `LoanApplication` 尚未保存此欄位，後端目前無法知道核准後要撥到哪個帳戶。
- `LoanStatusCallbackRequestDTO` 已註明帳戶模組可做 `APPROVED -> DISBURSED`，但 controller/service 尚未實作 `callerModule = ACCOUNT` 分流。

## 六、Loan User 端 API

| Method | Path | 用途 |
|---|---|---|
| `GET` | `/api/customer/loan-repayments/debit-accounts` | 查可扣款台幣活存 |
| `POST` | `/api/customer/loan-repayments` | 執行貸款還款 |
| `GET` | `/api/customer/loan-repayments` | 查還款紀錄 |

Customer API 會從 JWT 解析 `customerId`，後端會再次檢查扣款帳戶與貸款帳戶是否屬於本人。

## 七、交易紀錄規則

| TransactionType | 說明 |
|---|---|
| `LOAN_DISBURSEMENT` | 貸款撥款 |
| `LOAN_REPAYMENT` | 貸款還款 |
| `INTEREST` | 貸款利息增加 |

同一業務動作產生的多筆 `TRANS_LOG` 會共用同一個 `referenceId`。

## 八、注意事項

1. Loan 帳戶的 `balance` 永遠必須是 0；Account 端在貸款操作前會檢查，若 DB 已有異常值會拒絕交易。
2. Loan 還款不允許超過剩餘 `liability`。
3. `BUSINESS`、`LOAN` 不會出現在一般 user 帳戶查詢，不可用一般轉帳/存提款/換匯操作。
4. SQL schema 已將 account 相關 FK 欄位放寬為 `VARCHAR(14)`，以支援 `901 + 身分證編碼` 的貸款帳號。
