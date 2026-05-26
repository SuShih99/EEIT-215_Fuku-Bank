# Card 與 Account 模組交接文件

本文提供 Card 模組呼叫 Account 模組的整合方式。Card 模組負責信用卡申請、發卡、消費、帳單與應繳金額計算；Account 模組負責信用卡繳款帳戶、顧客繳款、已繳金額查詢、結算入銀行收款帳戶與交易紀錄。

## 一、Card 相關帳戶

| AccountType | 用途 | balance | liability | rate | 是否顯示於一般帳戶查詢 |
|---|---|---:|---:|---:|---|
| `BUSINESS` | 銀行內部收款帳戶 | 可變動 | 0 | NULL | 否 |
| `CREDIT_CARD` | 信用卡繳款暫存帳戶 | 可變動 | 0 | NULL | 否 |

一般 user 端帳戶查詢會排除 `BUSINESS` 與 `CREDIT_CARD`。一般轉帳、存提款、換匯、沖正也不可把這兩種特殊帳戶當一般餘額帳戶操作。

## 二、銀行收款帳戶

`account_init.sql` 會初始化銀行收款帳戶：

| 帳號 | 用途 | 初始餘額 |
|---|---|---:|
| `909000000002` | 銀行收款帳戶 | `0.0000` |

`customer_id` 為 `BANK_INTERNAL`，幣別為 `TWD`，狀態為 `ACTIVE`，沒有利率。Card 模組結算時會把信用卡繳款帳戶餘額轉入此帳戶。

## 三、信用卡帳號編碼

Account 端會讀取 `CUSTOMER_PROFILE.id_number`，把英文字母轉為 `01` 到 `26`，數字保留。

範例：

```txt
A123456789 -> 01123456789
801 + 01123456789 = 80101123456789
```

信用卡繳款帳戶使用 `801` 起跳；若 `801...` 已存在，會依序嘗試 `802...`、`803...`，最多到 `899...`。同一 customer 只能建立一個 `CREDIT_CARD` 帳戶。

## 四、Card 模組呼叫入口

主要 service：

```java
com.javaeasybank.account.service.AccountIntegrationService
```

信用卡回饋入帳使用：

```java
com.javaeasybank.account.service.TransferService
```

### 1. 建立信用卡繳款帳戶

```java
CreditCardAccountResponse createCreditCardAccount(CreditCardAccountCreateRequest request)
```

Request：

| 欄位 | 說明 |
|---|---|
| `customerId` | 顧客 ID |

同一顧客只能有一個 `CREDIT_CARD` 帳戶。幣別固定 `TWD`，`rate = NULL`，初始 `balance = 0`。

### 2. 顧客繳信用卡款

```java
CreditCardPaymentResponse payCreditCard(String customerId, CreditCardPaymentRequest request)
```

流程：

1. 顧客台幣活存扣款。
2. 顧客信用卡繳款帳戶入帳。
3. 寫入兩筆 `CARD_PAYMENT` 交易紀錄，同一個 `referenceId`。

成功後回傳 `referenceId`、繳款金額、扣款後活存餘額與信用卡帳戶餘額。

### 3. 查已繳金額

```java
CreditCardPaidAmountResponse getCreditCardPaidAmount(
    String customerId,
    String creditCardAccountNumber,
    String billingMonth,
    LocalDate startDate,
    LocalDate endDate
)
```

支援：

| 查詢方式 | 說明 |
|---|---|
| 不帶月份與日期 | 查全部已繳金額 |
| `billingMonth=yyyy-MM` | 查指定帳單月份 |
| `startDate` / `endDate` | 查指定日期區間 |

回傳的 `paidAmount` 是 `CARD_PAYMENT + CREDIT` 交易加總；`currentCreditCardBalance` 是目前可被 Card 模組結算扣除的餘額。

### 4. Card 模組結算扣款

```java
CreditCardSettlementResponse settleCreditCard(CreditCardSettlementRequest request)
```

此方法給 Card 模組依帳單金額呼叫：

1. 信用卡繳款帳戶 `balance -= amount`。
2. 銀行收款帳戶 `909000000002 balance += amount`。
3. 寫入兩筆 `CARD_SETTLEMENT` 交易紀錄，同一個 `referenceId`。

若信用卡繳款帳戶餘額不足，Account 端會拒絕結算。

### 5. 信用卡回饋入帳

```java
CashResponse creditCardReward(CashRequest request)
```

用途：Card 模組計算回饋後，將回饋金入帳到顧客指定的一般餘額帳戶。

Request：

| 欄位 | 說明 |
|---|---|
| `accountNumber` | 回饋入帳帳號，需為一般可動用餘額帳戶 |
| `amount` | 回饋金額，必須大於 0 |
| `note` | 備註，建議放 Card 模組的回饋批號、帳單月份或來源交易編號 |

流程：

1. 驗證帳戶存在且狀態為 `ACTIVE`。
2. 驗證帳戶不可為 `BUSINESS`、`LOAN`、`CREDIT_CARD` 等特殊帳戶。
3. 將回饋金額入帳到該帳戶。
4. 寫入一筆 `CREDIT + CARD_REWARD` 交易紀錄。

成功後回傳 `referenceId`、入帳帳號、入帳金額、入帳後餘額與交易時間。

## 五、Card User 端 API

| Method | Path | 用途 |
|---|---|---|
| `POST` | `/api/customer/card-payments` | 信用卡繳款 |
| `GET` | `/api/customer/card-payments/paid-amount` | 查已繳金額 |

Customer API 會從 JWT 解析 `customerId`，後端會再次檢查扣款帳戶與信用卡帳戶是否屬於本人。

## 六、交易紀錄規則

| TransactionType | 說明 |
|---|---|
| `CARD_PAYMENT` | 顧客繳信用卡款 |
| `CARD_SETTLEMENT` | Card 模組結算已繳金額到銀行收款帳戶 |
| `CARD_REWARD` | Card 模組信用卡回饋入帳 |

同一業務動作產生的多筆 `TRANS_LOG` 會共用同一個 `referenceId`。

## 七、注意事項

1. Credit Card 帳戶只能透過專用繳款方法增加 balance，透過 Card 結算方法扣除 balance。
2. 同一 customer 只能建立一個 `CREDIT_CARD` 帳戶。
3. 信用卡回饋不可入帳到 `BUSINESS`、`LOAN`、`CREDIT_CARD` 等特殊帳戶，只能入帳一般可動用餘額帳戶。
4. `BUSINESS`、`CREDIT_CARD` 不會出現在一般 user 帳戶查詢，不可用一般轉帳/存提款/換匯操作。
5. SQL schema 已將 account 相關 FK 欄位放寬為 `VARCHAR(14)`，以支援 `801 + 身分證編碼` 的信用卡帳號。
