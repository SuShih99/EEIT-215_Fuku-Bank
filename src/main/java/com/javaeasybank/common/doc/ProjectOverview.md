# Java Easy Bank 專案理解文件

> 本文件依照目前程式碼靜態閱讀整理，時間點：2026-05-09。  
> 目的：作為資訊交接、後續開發定位、API 對接與模組責任切分的總覽文件。  
> 範圍：後端 Spring Boot、前端 Vue、資料庫初始化腳本、目前可觀察到的業務流程與注意事項。

---

## 1. 專案定位

Java Easy Bank 是一套銀行業務系統，提供兩個主要使用端：

| 使用端 | 主要對象 | 前端路徑 | 後端 API 特徵 |
|---|---|---|---|
| 客戶端 | 一般顧客 | `/`, `/login`, `/user/**` | 多數為 `/api/customer/**`，信用卡部分為 `/user/**` |
| 管理端 | 行員、管理者、資安/系統角色 | `/admin/login`, `/admin/**` | 多數為 `/api/admin/**` 或 `/api/auth/**` |

目前業務模組涵蓋：

- Auth：員工登入、員工管理、系統操作日誌。
- Customer：客戶資料、客戶註冊登入、個人資料、密碼重設、大頭照。
- Account：帳戶、開戶申請、轉帳、存提款、交易紀錄、沖正、常用帳號、預約轉帳。
- Loan：貸款申請、聯繫紀錄、二次填單、送審、利率規則。
- Credit Card：信用卡卡別、申請、申請明細審核、發卡、交易、刷退。
- Risk：黑名單、風險事件、AOP 風控檢查框架。
- Common：共用回應格式、例外處理、安全設定、JWT、CORS、檔案上傳等。

---

## 2. 技術棧

### 後端

| 類別 | 技術 |
|---|---|
| 語言 | Java 21 |
| Framework | Spring Boot 4.0.5 |
| Web | Spring Web |
| ORM | Spring Data JPA / Hibernate |
| Security | Spring Security |
| Validation | Spring Validation |
| AOP | Spring Boot Starter AspectJ |
| DB Driver | Microsoft SQL Server JDBC |
| DTO/Entity 輔助 | Lombok |
| Mapper | MapStruct |
| JWT | jjwt 0.12.6 |
| Export | OpenCSV, OpenPDF |
| Mail | Spring Boot Starter Mail |
| Build | Maven |

### 前端

| 類別 | 技術 |
|---|---|
| Framework | Vue 3 |
| Build Tool | Vite |
| Runtime | Node.js `^20.19.0 || >=22.12.0` |
| Router | Vue Router |
| Store | Pinia |
| UI | ant-design-vue |
| HTTP | axios |
| CSS | Tailwind CSS + 自訂 theme css |
| Chart / Export | Chart.js, vue-chartjs, xlsx, file-saver |

### 資料庫

- 目標資料庫：MS SQL Server。
- `src/main/resources/application.properties` 僅匯入本機設定：
  - `spring.config.import=optional:classpath:application-local.properties`
- DB 連線資訊預期放在本機 `application-local.properties`，不應提交。
- 初始化與 mock data 位於 `src/main/resources/database/`。

---

## 3. 專案結構總覽

```text
java-easy-bank/
├── pom.xml
├── README.md
├── src/main/java/com/javaeasybank/
│   ├── JavaEasyBankApplication.java
│   ├── common/
│   ├── auth/
│   ├── customer/
│   ├── account/
│   ├── loan/
│   ├── creditcard/
│   └── risk/
├── src/main/resources/
│   ├── application.properties
│   ├── banner.txt
│   ├── database/
│   └── static/img/
├── src/test/java/com/javaeasybank/
└── frontend/
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── api/
        ├── assets/
        ├── components/
        ├── layouts/
        ├── router/
        ├── stores/
        └── views/
```

後端各模組大多採用以下分層：

```text
module/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
└── enums/
```

---

## 4. 啟動與設定

### 後端

README 建議流程：

```bash
./mvnw spring-boot:run
```

後端預設設定：

- Server：通常使用 `8080`。
- DB：由 `application-local.properties` 補上。
- Jackson 時區建議：`Asia/Taipei`。
- JPA 建議本機可用 `ddl-auto=update`，依 README 範例設定。

### 前端

```bash
cd frontend
npm install
npm run dev
```

前端預設：

- Vite dev server：`http://localhost:5173`
- Node.js 版本依 `frontend/package.json` 為 `^20.19.0 || >=22.12.0`。
- `frontend/src/api/axios.js` 的 `BASE_URL` 固定為 `http://localhost:8080`
- axios 啟用 `withCredentials: true`，讓管理端 Session Cookie 能跟後端互通。
- Vite proxy 目前代理：
  - `/api` -> `http://localhost:8080`
  - `/uploads` -> `http://localhost:8080`

管理端登入依賴 session cookie；本機開發時建議前後端都使用 `localhost`，避免前端用 `127.0.0.1`、後端 base URL 用 `localhost` 時 cookie domain 不一致。

---

## 5. 認證與授權模型

系統目前有兩套登入模型，並由同一個 Spring Security filter chain 管理：

| 使用端 | 認證方式 | 前端儲存 | 後端入口 | 權限依據 |
|---|---|---|---|---|
| 管理端 | Session-based | `localStorage.auth_user` + Cookie | `/api/auth/login` | Spring Security Session + role |
| 客戶端 | JWT-based | `localStorage.customer_user`, `localStorage.customer_token` | `/api/customer/auth/login` | JWT claim: `role`, `customerId` |

### 管理端 Session 流程

1. 前端呼叫 `POST /api/auth/login`。
2. 後端使用 `AuthenticationManager` 驗證 email/password。
3. `CustomUserDetailsService` 從 `AUTH_EMP` 查員工，再從 `AUTH_ROLE` 查角色。
4. 驗證成功後把 `SecurityContext` 放入 HttpSession。
5. 前端儲存後端回傳的員工資訊至 `auth_user`。
6. 管理端路由守衛每次進入 `/admin/**` 會呼叫 `GET /api/auth/me` 確認 session 仍有效。

相關檔案：

- `common/config/SecurityConfig.java`
- `auth/controller/AuthController.java`
- `auth/service/CustomUserDetailsService.java`
- `frontend/src/stores/auth.js`
- `frontend/src/router/index.js`

### 客戶端 JWT 流程

1. 前端呼叫 `POST /api/customer/auth/login`。
2. 後端驗證 `CUSTOMER_AUTH` 帳密與狀態。
3. `JwtUtil` 產生 JWT，claim 包含：
   - subject：username
   - `role`
   - `customerId`
4. 前端儲存 token 至 `customer_token`。
5. axios request interceptor 自動加上 `Authorization: Bearer <token>`。
6. `JwtAuthenticationFilter` 驗證 token 後把 `ROLE_CUSTOMER` 放入 Spring Security context。

相關檔案：

- `common/util/JwtUtil.java`
- `common/config/JwtAuthenticationFilter.java`
- `customer/controller/CustomerAuthController.java`
- `customer/service/CustomerAuthServiceImpl.java`
- `frontend/src/stores/customerAuth.js`
- `frontend/src/api/axios.js`

### 路由守衛

前端守衛位於 `frontend/src/router/index.js`。

- 客戶端 `/user/**` 需要 `customer_token`。
- 管理端 `/admin/**` 需要 `auth_user`，且會呼叫 `/api/auth/me`。
- `requiresAdmin` 目前保留給員工管理與系統日誌，允許角色：
  - `ISSA`
  - `CISO`
  - `SYS_SUPER`
  - `SYS_STAFF`
- 若管理端使用者 role 為 `CUSTOMER`，會導向 `/forbidden`。

---

## 6. Common 模組

### 統一 API 回應

`common/dto/response/ApiResponse.java`

所有 API 原則上應回傳：

```json
{
  "success": true,
  "message": "操作成功",
  "errorCode": null,
  "data": {}
}
```

常用工廠方法：

- `ApiResponse.success(data)`
- `ApiResponse.success(message, data)`
- `ApiResponse.fail(message)`
- `ApiResponse.fail(errorCode, message)`

### 分頁回應

`common/dto/response/PageResponse.java`

用於把 Spring Data `Page` 轉成前端較穩定的格式：

- `content`
- `page`
- `size`
- `totalElements`
- `totalPages`

部分模組仍直接回傳 Spring Data `Page`，例如 Risk 與部分信用卡申請 API。

### 全域例外處理

`common/exception/GlobalExceptionHandler.java`

目前統一處理：

- `AccountException` -> HTTP 400，保留 `errorCode`
- `BusinessException` -> HTTP 400
- `MethodArgumentNotValidException` -> HTTP 400
- `AuthorizationDeniedException` -> HTTP 403
- `AuthenticationException` -> HTTP 401
- 其他 `Exception` -> HTTP 500

### CORS

目前有兩處 CORS 設定：

- `common/config/SecurityConfig.java`：Security filter chain 使用的 `CorsConfigurationSource`
- `common/config/CorsConfig.java`：MVC 層的 `/api/**` CORS mapping

兩者都允許 `http://localhost:5173`，並允許 credentials。

### 檔案上傳與靜態資源

`common/service/FileStorageService.java`

- 預設上傳根目錄：`uploads`
- 支援 JPG / JPEG / PNG
- 檔案大小限制：5 MB
- 回傳 URL 格式：`/uploads/{subDir}/{uuid}.{ext}`

`common/config/WebConfig.java`

- 將 `/uploads/**` 對應到本機 `file:uploads/`

目前使用場景：

- 客戶大頭照：`/uploads/avatars/**`
- 開戶證件：`/uploads/id-cards/**`
- 信用卡卡別圖片：部分 controller 直接寫到 `uploads/`

### SessionUtil

`common/util/SessionUtil.java` 是早期手動 session 驗證工具，註解顯示屬於「階段一」設計。現在主要驗證已轉向 Spring Security + JWT，但此工具仍留在 common。

---

## 7. 後端模組詳解

## 7.1 Auth 模組

### 職責

- 管理端員工登入 / 登出 / session 檢查。
- 員工 CRUD、停用、啟用。
- 系統操作日誌查詢與 CSV/PDF 匯出。
- Spring Security `UserDetailsService` 資料來源。

### 主要 Entity

| Entity | Table | 用途 |
|---|---|---|
| `AuthEmp` | `AUTH_EMP` | 員工帳號、密碼 hash、角色、狀態 |
| `AuthRole` | `AUTH_ROLE` | 角色代碼與角色資料 |
| `AuthDept` | `AUTH_DEPT` | 部門 |
| `AuthLoginLog` | `AUTH_LOGIN_LOG` | 登入登出紀錄 |
| `AuthActionLog` | `AUTH_ACTION_LOG` | 系統操作日誌 |

### API

| Method | Path | 說明 |
|---|---|---|
| POST | `/api/auth/login` | 管理端登入 |
| GET | `/api/auth/me` | 檢查 session 是否有效 |
| POST | `/api/auth/logout` | 管理端登出 |
| GET | `/api/auth/employees/count` | 查員工數 |
| GET | `/api/auth/employees?keyword=` | 查員工 / 模糊搜尋 |
| POST | `/api/auth/employees` | 新增員工 |
| PUT | `/api/auth/employees/{empId}` | 修改員工 |
| DELETE | `/api/auth/employees/{empId}/suspend` | 停用員工 |
| PUT | `/api/auth/employees/{empId}/resume` | 重新啟用員工 |
| POST | `/api/auth/employees/seed` | 一鍵建立測試員工資料 |
| GET | `/api/auth/logs` | 查系統日誌 |
| GET | `/api/auth/logs/export/csv` | 匯出 CSV |
| GET | `/api/auth/logs/export/pdf` | 匯出 PDF |

---

## 7.2 Customer 模組

### 職責

- 客戶資料管理。
- 客戶註冊 / 登入 / JWT 發放。
- 客戶個人資料維護。
- 大頭照上傳。
- 密碼重設 token 產生與重設。
- 提供 `syncAccountApplicationProfile`，讓帳戶模組同步開戶申請與審核結果。

### 主要 Entity

| Entity | Table | 用途 |
|---|---|---|
| `CustomerProfile` | `CUSTOMER_PROFILE` | 客戶 KYC / 基本資料 / CIF / 聯絡方式 / 最近一次開戶申請同步資料 |
| `CustomerAuth` | `CUSTOMER_AUTH` | 客戶登入帳號、密碼 hash、角色、狀態、reset token |

### CustomerProfile 欄位範圍

`CustomerProfile` 是顧客模組目前最核心的正式客戶資料表。新版結構除了原本的基本資料，也承接開戶申請表同步欄位，讓管理端顧客管理能直接看到最近一次開戶申請與審核結果。

| 類別 | 欄位 |
|---|---|
| 識別與基本資料 | `customerId`, `cif`, `idNumber`, `name`, `birthday`, `gender`, `email`, `phone`, `address`, `avatarUrl`, `status`, `createdAt`, `updatedAt` |
| 地址與 KYC | `nationality`, `registeredAddress`, `currentAddress` |
| 職業與交易概況 | `occupation`, `employer`, `estimatedMonthlyTx`, `job`, `annualIncome`, `riskLevel` |
| 開戶目的與法遵 | `accountPurpose`, `fundSource`, `taxResidency`, `isPep` |
| 證件附件 | `idFrontUrl`, `idBackUrl`, `secondIdUrl` |
| 最近一次開戶申請 | `latestAccountApplicationId`, `latestAccountApplicationNo`, `latestAppliedAccountType`, `latestAppliedCurrency`, `latestAccountApplicationStatus`, `latestAccountApplicationRiskFlag`, `latestAccountApplicationReviewedAt`, `latestAccountApplicationReviewedBy`, `latestAccountApplicationRejectReason`, `createdAccountNumber`, `accountApplicationSyncedAt` |

目前 `address` 仍是舊版相容欄位；開戶申請同步時會優先使用 `currentAddress`，沒有現居地址時才用 `registeredAddress` 或原本的 `address`。

### 開戶申請同步責任邊界

帳戶模組不直接寫 `CUSTOMER_PROFILE`。同步流程由 Account 組裝 `AccountApplicationProfileSyncRequest`，再呼叫 Customer 的 `CustomerService.syncAccountApplicationProfile(customerId, request)`。

同步時 Customer 模組負責：

- 檢查 `customerId` 是否存在。
- 檢查 `idNumber`、`phone` 是否和其他客戶重複。
- 更新姓名、身分證、生日、電話、地址、職業、法遵、證件 URL。
- 更新最近一次開戶申請編號、狀態、風險標記、審核人員、審核時間、補件或駁回原因、建立帳號。
- 寫入 `accountApplicationSyncedAt`，表示最後同步時間。

| 觸發時機 | 開戶申請狀態 | CustomerProfile 結果 |
|---|---|---|
| 客戶送出申請 | `PENDING` | 立即同步申請表欄位與證件 URL，讓顧客表先具備最新 KYC 資料 |
| 管理端核准 | `APPROVED` | 同步審核結果與 `createdAccountNumber` |
| 管理端要求補件 | `SUPPLEMENT_REQUIRED` | 同步補件狀態、審核人員、審核時間與原因 |
| 管理端駁回 | `REJECTED` | 同步駁回狀態、審核人員、審核時間與原因 |

因為補件後可能重新送審，所以不論核准、補件或駁回，都會把該次審核完成後的結果同步回 CustomerProfile。

### 客戶認證 API

| Method | Path | 說明 |
|---|---|---|
| POST | `/api/customer/auth/register` | 客戶註冊，建立 profile + auth，回傳 JWT |
| POST | `/api/customer/auth/login` | 客戶登入，回傳 JWT |
| GET | `/api/customer/auth/me` | 取得目前客戶資料 |
| PUT | `/api/customer/auth/profile` | 修改個人資料 |
| POST | `/api/customer/auth/avatar` | 上傳大頭照 |
| POST | `/api/customer/auth/request-reset` | 請求密碼重設 |
| POST | `/api/customer/auth/reset-password` | 執行密碼重設 |
| POST | `/api/customer/auth/seed` | 建立客戶認證測試資料 |

### 管理端客戶 API

| Method | Path | 說明 |
|---|---|---|
| GET | `/api/customers?keyword=` | 查詢客戶 / 模糊搜尋 |
| POST | `/api/customers` | 新增客戶 |
| PUT | `/api/customers/{customerId}` | 修改客戶 |
| PUT | `/api/customers/{customerId}/deactivate` | 停用客戶 |
| PUT | `/api/customers/{customerId}/activate` | 啟用客戶 |
| POST | `/api/customers/seed` | 建立客戶測試資料 |

### 註冊流程

1. 檢查 username 是否重複。
2. 檢查身分證字號是否重複。
3. 產生 `customerId` 與 CIF。
4. 建立 `CustomerProfile`。
5. 使用 BCrypt hash 建立 `CustomerAuth`。
6. 產生 JWT 並回傳登入資料。

### 密碼重設現況

目前 `requestPasswordReset` 會產生 reset token，效期 30 分鐘，並將連結印到 console。SMTP 寄信仍是 TODO。

---

## 7.3 Account 模組

### 職責

- 帳戶建立與管理。
- 客戶查詢自己的帳戶與交易。
- 開戶申請與 KYC 文件上傳。
- 轉帳、存款、提款。
- 交易紀錄查詢。
- 沖正。
- 常用帳號。
- 預約轉帳。

### 主要 Entity

| Entity | Table | 用途 |
|---|---|---|
| `Account` | `ACCOUNT` | 帳號、客戶 ID、帳戶類型、幣別、餘額、狀態 |
| `TransLog` | `TRANS_LOG` | 交易紀錄，雙邊記帳與沖正紀錄 |
| `AccountApplication` | `ACCOUNT_APPLICATION` | 開戶申請、KYC、證件、風險標記、審核狀態 |
| `FavoriteAccount` | `FAVORITE_ACCOUNT` | 客戶常用收款帳號 |
| `ScheduledTransfer` | `SCHEDULED_TRANSFER` | 預約轉帳 |

### 主要 Enum

| Enum | 值 |
|---|---|
| `AccountStatus` | `PENDING`, `ACTIVE`, `FROZEN`, `DORMANT`, `CLOSED` |
| `AccountType` | `CHECKING`, `SAVINGS`, `TIME_DEPOSIT`, `LOAN`, `SUB_ACCOUNT` |
| `Currency` | `TWD`, `USD`, `EUR`, `JPY`, `GBP`, `CNY`, `AUD`, `CAD`, `CHF`, `HKD` |
| `ApplicationStatus` | `PENDING`, `SUPPLEMENT_REQUIRED`, `APPROVED`, `REJECTED`, `CANCELLED` |
| `TransactionType` | `TRANSFER`, `DEPOSIT`, `WITHDRAW`, `INTEREST`, `LOAN_DISBURSEMENT`, `LOAN_REPAYMENT`, `REVERSAL` |
| `EntryType` | `DEBIT`, `CREDIT` |
| `RiskFlag` | `NORMAL`, `WATCH`, `PEP`, `HIGH_RISK`, `HIGH_FREQUENCY`, `PEP_HIGH_FREQUENCY` |

### 帳戶建立與狀態規則

目前帳戶建立有兩條路：

| 路徑 | 入口 | 建立後狀態 | 說明 |
|---|---|---|---|
| 直接建立 | `POST /api/accounts` | `PENDING` | 管理端 / 內部使用，會檢查客戶存在與帳戶型別規則 |
| 開戶申請核准 | `PATCH /api/admin/account-applications/{id}/approve` | `ACTIVE` | 客戶申請經管理端核准後自動建立帳戶 |

直接建立帳戶規則：

- `CHECKING`：同一 customer + 同一 currency 只能有一個。
- `SUB_ACCOUNT`：限定 TWD；需已有 ACTIVE 的 TWD checking；需提供 parent account；parent account 必須存在且同屬該 customer。
- `TIME_DEPOSIT`、`LOAN`：目前不檢查重複，可多開。
- `CHECKING` 初始餘額 1000，利率 0.0015。
- `SUB_ACCOUNT` 利率比照活存，但不給初始 1000。

狀態流轉由 `AccountService.updateAccountStatus` 控制：

- `PENDING` -> `ACTIVE`
- `ACTIVE` -> `FROZEN`, `DORMANT`, `CLOSED`
- `FROZEN` -> `ACTIVE`, `CLOSED`
- `DORMANT` -> `ACTIVE`, `CLOSED`
- `CLOSED` 不可再轉換

若將某帳戶改為 `FROZEN`，目前會連動凍結同一 customer 其他 `ACTIVE` 帳戶。狀態歷史表仍未落地。

### 帳戶管理 API

| Method | Path | 說明 |
|---|---|---|
| POST | `/api/accounts` | 建立帳戶 |
| GET | `/api/accounts/{accountNumber}` | 查單一帳戶 |
| GET | `/api/accounts?customerId=&page=&size=` | 依客戶查帳戶 |
| GET | `/api/accounts/status/{status}` | 依狀態查帳戶 |
| GET | `/api/accounts/filter?type=&currency=` | 依帳戶類型與幣別查帳戶 |
| GET | `/api/accounts/latest` | 查最新帳戶 |
| PATCH | `/api/accounts/{accountNumber}/status?newStatus=` | 變更帳戶狀態 |

### 客戶帳戶查詢 API

| Method | Path | 說明 |
|---|---|---|
| GET | `/api/customer/accounts` | 查目前登入客戶的所有帳戶 |
| GET | `/api/customer/transactions` | 查目前客戶交易紀錄，可依帳號或日期篩選 |

### 開戶申請 API

| Method | Path | 說明 |
|---|---|---|
| POST | `/api/customer/account-applications` | 客戶提交開戶申請，multipart，含三張證件 |
| GET | `/api/customer/account-applications` | 客戶查自己的申請 |
| GET | `/api/admin/account-applications?status=&page=&size=` | 管理端查申請列表 |
| GET | `/api/admin/account-applications/{id}` | 管理端查單筆申請 |
| PATCH | `/api/admin/account-applications/{id}/approve` | 核准申請，自動建立 Account |
| PATCH | `/api/admin/account-applications/{id}/supplement` | 要求補件 |
| PATCH | `/api/admin/account-applications/{id}/reject` | 駁回申請 |

### 開戶申請表欄位

`AccountApplicationRequest` 是客戶送出開戶申請時的文字欄位，證件檔案則在 Controller 層以 multipart file 接收。

| 類別 | 欄位 | 說明 |
|---|---|---|
| 帳戶 | `accountType`, `currency` | 帳戶類型必填；外幣帳戶需帶幣別 |
| 基本 KYC | `customerName`, `idNumber`, `birthday`, `nationality`, `phone` | 用來同步到 CustomerProfile |
| 地址 | `registeredAddress`, `currentAddress` | 戶籍地址與現居地址分開存放 |
| 職業 | `occupation`, `employer`, `estimatedMonthlyTx` | 職業、任職機構、預估月交易量 |
| 開戶目的 | `accountPurpose`, `fundSource` | Enum 轉字串後同步到 CustomerProfile |
| 法遵 | `taxResidency`, `isPep` | 稅務居民與 PEP 聲明 |
| 證件 | `idFront`, `idBack`, `secondId` | Controller 上傳至 `/uploads/id-cards/**`，URL 寫入申請單與 CustomerProfile |

管理端審核後，`AccountApplication` 也會保留 `reviewedAt`、`reviewedBy`、`rejectReason`、`riskFlag`、`createdAccountNumber` 等審核結果欄位，並同步成 CustomerProfile 的最近一次開戶申請資訊。

### 交易 API

| Method | Path | 說明 |
|---|---|---|
| POST | `/api/customer/transfers` | 客戶轉帳 |
| POST | `/api/customer/cash/deposit` | 存款 |
| POST | `/api/customer/cash/withdraw` | 提款 |
| POST | `/api/admin/transfers/reversal` | 管理端沖正 |

### 交易紀錄 API

| Method | Path | 說明 |
|---|---|---|
| GET | `/api/trans-logs/reference/{referenceId}` | 依 referenceId 查交易 |
| GET | `/api/trans-logs/account/{accountNumber}` | 依帳號查交易 |
| GET | `/api/trans-logs/customer/{customerId}` | 依客戶查交易 |
| GET | `/api/trans-logs/customer/{customerId}/range` | 依客戶與日期區間查交易 |
| GET | `/api/trans-logs/latest` | 查最新交易 |

### 常用帳號 API

| Method | Path | 說明 |
|---|---|---|
| GET | `/api/customer/favorite-accounts` | 查常用帳號 |
| POST | `/api/customer/favorite-accounts` | 新增常用帳號 |
| PUT | `/api/customer/favorite-accounts/{id}` | 修改常用帳號 |
| DELETE | `/api/customer/favorite-accounts/{id}` | 刪除常用帳號 |

### 預約轉帳 API

| Method | Path | 說明 |
|---|---|---|
| GET | `/api/customer/scheduled-transfers` | 查預約轉帳 |
| POST | `/api/customer/scheduled-transfers` | 建立預約轉帳 |
| PATCH | `/api/customer/scheduled-transfers/{id}/cancel` | 取消預約轉帳 |

注意：`ScheduledTransferService.executeDueTransfers` 目前只有 service 方法，專案內尚未看到 `@Scheduled` 入口；該方法目前也只是更新狀態為 `EXECUTED`，尚未真正呼叫 `TransferService.transfer`。

### 開戶申請流程

1. 客戶上傳身分證正面、反面、第二證件，並提交 KYC 資料。
2. Controller 從 JWT 取得 `customerId`，從 request 取得 IP。
3. `FileStorageService` 儲存三張證件到 `uploads/id-cards`。
4. `AccountApplicationService.submit` 檢查：
   - 同客戶不可已有 `PENDING` 申請。
   - 同 IP 24 小時內申請數是否達 3 次以上。
   - 同手機 24 小時內申請數是否達 3 次以上。
   - PEP 聲明。
5. 依風險情境設定 `RiskFlag`。
6. 儲存 `AccountApplication`，狀態為 `PENDING`。
7. 送件後同步申請資料至 `CustomerProfile`，最近一次開戶申請狀態為 `PENDING`。
8. 管理端可核准、補件、駁回；每次審核完成都會再次同步結果至 `CustomerProfile`。
9. 核准時自動建立 `Account`：
   - 帳號由 `AccountNumberGenerator` 產生。
   - 狀態設為 `ACTIVE`。
   - 活存 `CHECKING` 會給初始餘額 1000 與利率 0.0015。
   - 申請單回寫 `createdAccountNumber`。

注意：目前高頻申請是「標記」風險，不是直接阻擋送件。

### 轉帳流程

1. `TransferService.transfer` 被 `@RiskCheck(scene = RiskScene.TRANSFER)` 標註。
2. 驗證來源/目的帳號不可空。
3. 金額需大於 0。
4. 不可自轉。
5. 查來源與目的帳戶。
6. 兩方帳戶狀態都必須是 `ACTIVE`。
7. 兩方幣別必須一致。
8. 來源餘額需足夠。
9. 在同一個 transaction 內扣款與入帳。
10. 產生同一個 `referenceId`。
11. 寫兩筆 `TransLog`：
    - 來源帳戶：`DEBIT` + `TRANSFER`
    - 目的帳戶：`CREDIT` + `TRANSFER`
12. 回傳 referenceId、雙方餘額與時間。

注意：雖然轉帳 API 位於 `/api/customer/transfers`，目前 `TransferController` 沒有從 JWT 取出 `customerId` 驗證 `fromAccountNumber` 是否屬於目前登入客戶；實際 ownership check 需要後續補強。`CashController` 的存提款 API 也同樣未驗證帳戶歸屬。

### 沖正流程

1. 依原始 `referenceId` 查所有原始交易紀錄。
2. 若找不到，丟出 `TRANSACTION_NOT_FOUND`。
3. 以 note 包含 `沖正 ref: {originalRefId}` 防止重複沖正。
4. 對每筆原交易做反向操作：
   - 原 `DEBIT` -> 沖正 `CREDIT`，餘額加回。
   - 原 `CREDIT` -> 沖正 `DEBIT`，餘額扣回。
5. 不修改或刪除原交易，而是新增沖正 `TransLog`。

---

## 7.4 Loan 模組

### 職責

- 會員貸款申請。
- 非會員貸款申請。
- 利率規則提供給前端。
- 管理端依狀態查申請。
- 行員聯繫紀錄。
- 二次填單草稿。
- 送審流程。

### 主要 Entity

| Entity | Table | 用途 |
|---|---|---|
| `LoanApplication` | `LOAN_APPLICATION` | 貸款主申請 |
| `LoanContactLog` | `LOAN_CONTACT_LOG` | 行員聯繫紀錄 |
| `LoanReviewDetail` | `LOAN_REVIEW_DETAIL` | 二次填單 / 審核資料 |

### 主要 Enum

| Enum | 值 |
|---|---|
| `LoanApplicationStatus` | `PENDING_CONTACT`, `IN_CONTACT`, `PENDING_REVIEW`, `APPROVED`, `REJECTED`, `CANCELLED`, `DISBURSED`, `CLOSED` |
| `LoanReviewStatus` | `DRAFT`, `SUBMITTED` |
| `LoanContactStatus` | `NOT_CONTACTED`, `ATTEMPTED`, `REACHED`, `CONFIRMED`, `DECLINED` |
| `LoanContactChannel` | `PHONE`, `EMAIL`, `SMS` |

### 客戶端 API

| Method | Path | 說明 |
|---|---|---|
| POST | `/api/loan-applications/member` | 會員貸款申請 |
| POST | `/api/loan-applications/non-member` | 非會員貸款申請 |
| GET | `/api/loan-applications/rate-rules` | 取得利率規則 |

### 管理端 API

| Method | Path | 說明 |
|---|---|---|
| GET | `/api/admin/loan-applications?status=` | 依狀態查申請，預設 `PENDING_CONTACT` |
| POST | `/api/admin/loan-applications/{id}/contact-logs` | 新增聯繫紀錄 |
| GET | `/api/admin/loan-applications/{id}/contact-logs` | 查聯繫紀錄 |
| POST | `/api/admin/loan-applications/{id}/review` | 儲存二次填單草稿 |
| PATCH | `/api/admin/loan-applications/{id}/review/submit` | 送審 |
| GET | `/api/admin/loan-applications/{id}/review` | 查二次填單內容 |

### 貸款流程

1. 客戶或非會員送出申請。
2. 後端產生 applicationId，格式約為 `LAyyyyMMddHHmmss####`。
3. 初始狀態為 `PENDING_CONTACT`。
4. 行員新增聯繫紀錄後：
   - 主表更新最新聯繫狀態與時間。
   - 若原本是 `PENDING_CONTACT`，推進為 `IN_CONTACT`。
   - 若客戶放棄，推進為 `CANCELLED`。
5. 行員可建立或覆蓋二次填單草稿。
6. 送審條件：
   - 主表必須是 `IN_CONTACT`。
   - 已有 review draft。
   - draft 狀態必須是 `DRAFT`。
7. 送審後 review 狀態改為 `SUBMITTED`，主表狀態改為 `PENDING_REVIEW`。

### 利率規則

`LoanApplicationService.getRateRules` 回傳前端用規則：

- 貸款類型包含 `PERSONAL`, `CAR`, `MOTOR`, `STUDENT`, `BUSINESS`, `HOUSE`, `LAND`。
- 各類型有 baseRate 與可選期數。
- `STUDENT` 目前有 `fixedRate=true`。
- termRates 依期數加碼。

---

## 7.5 Credit Card 模組

### 職責

- 卡別管理與圖片上傳。
- 客戶信用卡申請。
- 管理端申請查詢、狀態更新、備註。
- 申請明細審核。
- 審核通過後發卡。
- 卡片管理。
- 信用卡交易、額度檢查、刷退。

### 主要 Entity

| Entity | 用途 |
|---|---|
| `CardType` | 信用卡卡別、權益、圖片等 |
| `CardApplication` | 信用卡申請主檔 |
| `CardApplicationItem` | 申請明細 / 審核項目 |
| `CreditCard` | 實體信用卡資料 |
| `CardTransaction` | 信用卡交易 |
| `CardBill` | 帳單 |
| `Merchant` | 商家 |

### 主要 Enum

| Enum | 值 |
|---|---|
| `CardApplicationStatus` | `PENDING`, `APPROVED`, `REJECTED`, `PARTIAL` |
| `CardApplicationItemResult` | `PENDING`, `APPROVED`, `REJECTED` |
| `CardStatus` | `INACTIVE`, `ACTIVE`, `BLOCKED` |
| `TxnType` | `PURCHASE`, `REFUND`, `PAYMENT` |
| `BillStatus` | 帳單狀態 |
| `MerchantStatus` | 商家狀態 |
| `MerchantCategory` | 商家分類 |

### 客戶端卡別 / 申請 API

| Method | Path | 說明 |
|---|---|---|
| GET | `/user/card-types` | 查全部卡別 |
| GET | `/user/card-types/{id}` | 查單一卡別 |
| GET | `/user/card-applications` | 查目前客戶的信用卡申請 |
| POST | `/user/card-applications` | 送出信用卡申請 |

注意：以上信用卡客戶端路徑目前沒有 `/api` 前綴。

### 管理端卡別 API

| Method | Path | 說明 |
|---|---|---|
| GET | `/api/admin/card-types` | 查全部卡別 |
| GET | `/api/admin/card-types/{id}` | 查單一卡別 |
| POST | `/api/admin/card-types` | 新增卡別 |
| POST | `/api/admin/card-types/upload` | 上傳卡別圖片 |
| PUT | `/api/admin/card-types/{id}` | 修改卡別 |
| DELETE | `/api/admin/card-types/{id}` | 刪除卡別 |

### 管理端信用卡申請 API

| Method | Path | 說明 |
|---|---|---|
| GET | `/api/admin/card-applications?keyword=&status=` | 查信用卡申請，可搜尋 |
| GET | `/api/admin/card-applications/{id}` | 查單筆申請 |
| PUT | `/api/admin/card-applications/{id}/status` | 更新申請狀態 |
| DELETE | `/api/admin/card-applications/{id}` | 刪除申請 |
| PUT | `/api/admin/card-applications/{id}/remark` | 更新備註 |

### 管理端申請明細審核 API

| Method | Path | 說明 |
|---|---|---|
| GET | `/api/admin/card-application-items/{applicationId}` | 查某申請的明細 |
| POST | `/api/admin/card-application-items/{id}/approve` | 核准明細並發卡 |
| POST | `/api/admin/card-application-items/{id}/reject?remark=` | 拒絕明細 |

### 卡片 API

| Method | Path | 說明 |
|---|---|---|
| GET | `/api/cards` | 查卡片列表 |
| GET | `/api/cards/{id}` | 查單一卡片 |
| GET | `/api/admin/cards` | 管理端查卡片列表 |
| GET | `/api/admin/cards/{id}` | 管理端查單一卡片 |
| POST | `/api/admin/cards` | 新增卡片 |
| PUT | `/api/admin/cards/{id}` | 更新卡片 |
| DELETE | `/api/admin/cards/{id}` | 刪除卡片 |

### 信用卡交易 API

| Method | Path | 說明 |
|---|---|---|
| GET | `/user/card-txns` | 客戶端查交易 |
| GET | `/user/card-txns/{id}` | 客戶端查單筆交易 |
| POST | `/user/card-txns` | 客戶端建立交易 |
| GET | `/api/admin/card-txns` | 管理端查交易 |
| GET | `/api/admin/card-txns/{id}` | 管理端查單筆交易 |
| POST | `/api/admin/card-txns` | 管理端新增交易 |
| PUT | `/api/admin/card-txns/{id}` | 管理端更新交易 |
| POST | `/api/admin/card-txns/{id}/refund` | 刷退 |
| DELETE | `/api/admin/card-txns/{id}` | 刪除交易 |

### 信用卡申請與發卡流程

1. 客戶送出 `CardApplication`。
2. 後端設定申請狀態為 `PENDING`。
3. 管理端可搜尋申請、更新狀態、備註。
4. 管理端查申請明細。
5. 明細審核：
   - 若 item 不是 `PENDING`，不可重複審核。
   - 核准時設為 `APPROVED`，寫入 review date，並呼叫 `CreditCardService.createFromApplicationItem` 發卡。
   - 拒絕時設為 `REJECTED`，寫入 remark 與 review date。

### 信用卡交易流程

1. 建立交易時先查信用卡與商家。
2. 計算可用額度：`creditLimit - currentBalance`。
3. 若交易金額大於可用額度，丟出「信用額度不足」。
4. 建立 `CardTransaction`。
5. 更新 `CreditCard.currentBalance += txnAmount`。
6. 刷退時建立一筆負數金額的 `REFUND` 交易，並扣回 currentBalance。

---

## 7.6 Risk 模組

### 職責

- 黑名單管理。
- 風險事件查詢與搜尋。
- `@RiskCheck` AOP 風控切面。
- `RiskHandler` 抽象介面，依 `RiskScene` 分派不同業務場景。

### 主要 Entity

| Entity | Table | 用途 |
|---|---|---|
| `Blacklist` | `BLACK_LIST` | 黑名單資料 |
| `RiskEventLog` | `RISK_EVENT_LOG` | 風險事件紀錄 |

### 主要 Enum

| Enum | 值 |
|---|---|
| `RiskScene` | `GENERAL`, `TRANSFER`, `LOAN`, `CREDIT_CARD`, `LOGIN` |
| `BlacklistType` | `ID_CARD`, `ACCOUNT_NO`, `IP_ADDRESS`, `PHONE`, `EMAIL` |
| `AmlRiskLevel` | AML 風險等級 |
| `RiskDisposition` | 風控處置 |

### 黑名單 API

| Method | Path | 說明 |
|---|---|---|
| GET | `/api/blacklist` | 查黑名單 |
| POST | `/api/blacklist/create` | 新增黑名單 |
| GET | `/api/blacklist/{type}/{value}` | 依業務鍵查黑名單 |
| PUT | `/api/blacklist/{type}/{value}/status?enabled=` | 啟用 / 停用黑名單 |
| POST | `/api/blacklist/{type}/{value}/update` | 更新黑名單 |

### 風險事件 API

| Method | Path | 說明 |
|---|---|---|
| GET | `/api/riskevent` | 查全部風險事件 |
| GET | `/api/riskevent/search?eventType=&actionTaken=&riskLevel=` | 搜尋風險事件 |

### AOP 風控設計

核心介面：

- `RiskTarget`
  - `getTargetIdentifier()`
  - `getAmount()`
- `RiskHandler`
  - `getScene()`
  - `check(identifier, amount)`
- `RiskCheck`
  - method annotation，指定 `RiskScene`
- `RiskCheckAspect`
  - 攔截標註 `@RiskCheck` 的方法
  - 掃描 method args 中實作 `RiskTarget` 的物件
  - 依 scene 找 handler 執行檢查

目前 `TransferService.transfer` 標註：

```java
@RiskCheck(scene = RiskScene.TRANSFER)
```

目前觀察：

- `TransferRiskHandler` 的 `check` 邏輯尚未實作。
- `TransferRiskHandler` 目前檔案內未看到 `@Component` / `@Service`，因此可能尚未被 Spring 掃描注入到 `RiskCheckAspect`。
- `TransferService` 內仍留有大額交易、高頻交易、24 小時累計金額檢查 TODO。

---

## 8. 前端架構

### 入口

- `frontend/src/main.js`
  - 載入 `main.css`
  - 載入 `customer-theme.css`
  - 載入 `admin-theme.css`
  - 建立 Vue app
  - 掛載 Pinia、Router、Ant Design Vue

### Layout

| Layout | 用途 |
|---|---|
| `layouts/UserLayout.vue` | 客戶端登入後主版面，含頂部 logo、會員資訊、mega nav |
| `layouts/AdminLayout.vue` | 管理端主版面，左側固定 sidebar、上方員工資訊、登出 |

### Store

| Store | 用途 |
|---|---|
| `stores/auth.js` | 管理端員工登入狀態，localStorage key: `auth_user` |
| `stores/customerAuth.js` | 客戶端登入狀態與 JWT，localStorage keys: `customer_user`, `customer_token` |
| `stores/counter.js` | Vue 預設示範 store，目前非核心 |

### Axios

`frontend/src/api/axios.js`

- `BASE_URL = http://localhost:8080`
- `withCredentials = true`
- Request interceptor：
  - 若 localStorage 有 `customer_token`，自動放入 `Authorization` header。
- Response interceptor：
  - 401 時依目前 route 判斷客戶端或管理端，清除 localStorage 並導向登入頁。

### 主要前端 API 檔

| 檔案 | 對應模組 |
|---|---|
| `api/auth.js` | 管理端登入、員工、日誌 |
| `api/customerAuth.js` | 客戶登入、註冊、個資、密碼重設 |
| `api/customer.js` | 管理端客戶管理 |
| `api/account.js` | 帳戶、轉帳、交易紀錄、存提款、沖正 |
| `api/customerAccount.js` | 客戶端帳戶與交易 |
| `api/accountApplication.js` | 開戶申請 |
| `api/favoriteAccount.js` | 常用帳號 |
| `api/scheduledTransfer.js` | 預約轉帳 |
| `api/cardtype.js` | 信用卡卡別 |
| `api/cardApplication.js` | 信用卡申請與明細審核 |
| `api/card.js` | 信用卡卡片 |
| `api/cardTxn.js` | 信用卡交易 |

### 客戶端路由

| Path | Name | View |
|---|---|---|
| `/` | `landing` | `LandingView.vue` |
| `/login` | `user-login` | `UserLoginView.vue` |
| `/register` | `user-register` | `RegisterView.vue` |
| `/reset-password` | `reset-password` | `ResetPasswordView.vue` |
| `/user/home` | `user-home` | `UserHomeView.vue` |
| `/user/profile` | `user-profile` | `UserProfileView.vue` |
| `/user/card-types` | `user-card-types` | `CardTypeListView.vue` |
| `/user/card-applications` | `user-card-applications` | `CardApplicationForm.vue` |
| `/user/account-application` | `user-account-application` | `AccountApplicationView.vue` |
| `/user/accounts` | `user-accounts` | `UserAccountsView.vue` |
| `/user/transactions` | `user-transactions` | `UserTransactionsView.vue` |
| `/user/transfer` | `user-transfer` | `TransferView.vue` |
| `/user/scheduled-transfer` | `user-scheduled-transfer` | `ScheduledTransferView.vue` |
| `/user/favorite-accounts` | `user-favorite-accounts` | `FavoriteAccountsView.vue` |

### 管理端路由

| Path | Name | View |
|---|---|---|
| `/admin/login` | `admin-login` | `AdminLoginView.vue` |
| `/admin` | `admin-home` | `AdminHomeView.vue` |
| `/admin/accounts` | `admin-accounts` | `AccountListView.vue` |
| `/admin/account-applications` | `admin-account-applications` | `AccountApplicationListView.vue` |
| `/admin/trans-logs` | `admin-trans-logs` | `TransLogView.vue` |
| `/admin/employees` | `admin-employees` | `EmployeeListView.vue` |
| `/admin/employees/create` | `admin-employees-create` | `EmployeeCreateView.vue` |
| `/admin/customers` | `admin-customers` | `CustomerListView.vue` |
| `/admin/customers/create` | `admin-customers-create` | `CustomerCreateView.vue` |
| `/admin/card-types` | `admin-card-types` | `CardTypeListView.vue` |
| `/admin/card-applications` | `admin-card-applications` | `CardApplicationList.vue` |
| `/admin/card-applications/:id` | `admin-card-application-detail` | `CardApplicationDetailView.vue` |
| `/admin/card-txns` | `admin-card-txns` | `CardTxnView.vue` |
| `/admin/risk-events` | `admin-risk-events` | `RiskEventView.vue` |
| `/admin/cards` | `admin-cards` | `CardView.vue` |
| `/admin/loan-apply` | `loan-apply` | `LoanApplyView.vue` |
| `/admin/loan-applications` | `loan-applications` | `LoanApplicationView.vue` |
| `/admin/blacklist` | `admin-blacklist` | `BlackListView.vue` |
| `/admin/logs` | `admin-logs` | `SystemLogView.vue` |
| `/forbidden` | `forbidden` | inline component |

### 管理端顧客管理畫面

`frontend/src/views/admin/CustomerListView.vue` 對應 `GET /api/customers`，目前已配合新版 CustomerProfile 欄位顯示。

主表欄位重點：

- 客戶資訊：姓名、Email、頭像。
- CIF。
- 身分證字號。
- 國籍。
- 性別。
- 電話。
- 現居地址。
- 職業 / 任職機構。
- 法遵：PEP 與稅務居民。
- 最近開戶申請：狀態與申請編號。
- 建立帳號。
- 顧客狀態。
- 操作：停用、啟用、展開明細。

展開列會顯示三組明細：

| 區塊 | 內容 |
|---|---|
| 基本與地址 | 國籍、生日、Email、戶籍地址、現居地址 |
| 職業與法遵 | 職業、任職機構、預估月交易量、開戶目的、資金來源、稅務居民 |
| 最近開戶申請 | 申請編號、申請狀態、帳戶類型、幣別、風險標記、審核人員、審核時間、駁回或補件原因、建立帳號、同步時間 |

因此後端 `CustomerResponse` 若再新增欄位，需要同步確認 `CustomerListView.vue` 的欄位、展開明細與格式化 map 是否要調整。

---

## 9. 資料庫腳本

主要位置：`src/main/resources/database/`

目前資料庫腳本已依模組拆分，Auth 與 Customer 不再共用 `auth_customer_*` 檔案。

目前可見腳本：

| 檔案 | 用途 |
|---|---|
| `auth_init.sql` | Auth 初始化 |
| `auth_insert.sql` | Auth seed |
| `auth_drop.sql` | Auth drop |
| `customer_init.sql` | Customer 初始化，含開戶申請同步欄位 |
| `customer_insert.sql` | Customer seed，會補齊新版 Customer Profile 基礎欄位 |
| `customer_drop.sql` | Customer drop |
| `account_init.sql` | Account 初始化 |
| `account_mockdata.sql` | Account mock data |
| `loan_init.sql` | Loan 初始化 |
| `loan_mockdata.sql` | Loan mock data |
| `card_init.sql` | Credit Card 初始化 |
| `card_mockdata.sql` | Credit Card mock data |
| `risk_init.sql` | Risk 初始化 |
| `risk_mockdata.sql` | Risk mock data |

建議初始化順序：

1. 先建立模組 table：`auth_init.sql`、`customer_init.sql`、`account_init.sql`、`loan_init.sql`、`card_init.sql`、`risk_init.sql`。
2. 再建立種子資料：`auth_insert.sql`、`customer_insert.sql`、各模組 `*_mockdata.sql`。
3. 若要重建資料，先確認 FK 依賴再執行對應的 `*_drop.sql`。目前可見 drop 腳本包含 `auth_drop.sql`、`customer_drop.sql`。

Customer 腳本重點：

- `customer_init.sql` 的 `CUSTOMER_PROFILE` 已包含開戶申請同步欄位。
- `CUSTOMER_AUTH` 目前仍放在 customer 腳本，因為它是客戶端登入帳密表，與管理端 Auth 模組的 `AUTH_EMP`、`AUTH_ROLE` 不同。
- `customer_insert.sql` 會以既有 KYC / Risk seed 資料補齊新版 profile 欄位，例如職業、任職機構、年收入、稅務居民、PEP、風險等級與同步時間。

其他 SQL：

- `ddl_Blacklist.sql`
- `src/main/java/com/javaeasybank/loan/database/LoanTable.sql`

---

## 10. 測試現況

目前 `src/test/java` 可見：

| Test | 內容 |
|---|---|
| `JavaEasyBankApplicationTests` | Spring Boot context load |
| `AccountApplicationServiceTest` | 開戶申請 submit / approve 等單元測試 |
| `ReferenceIdGeneratorTest` | 交易 referenceId 格式與唯一性 |

測試輔助設定：

- `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`：本機 JDK 21 測試時使用 Mockito subclass mock maker，降低 inline mock maker 自動 attach 的環境問題。

常用驗證指令：

```bash
./mvnw -q test
```

```bash
cd frontend
npm run build
```

目前閱讀範圍內測試主要集中在 account 模組，其他模組測試覆蓋較少。完整後端測試會載入 Spring context，若本機沒有 SQL Server 或 `application-local.properties` 設定不完整，可能會在 context load 階段失敗。

---

## 11. 重要業務規則摘要

### 帳務金額

- 金額型別以 `BigDecimal` 為主。
- DB 欄位多以 `precision = 19, scale = 4` 或信用卡 `precision = 15, scale = 2`。
- README 規範提到金額欄位一律使用 `BigDecimal` / `DECIMAL(15,2)`，但 account entity 目前使用 `19,4`。

### 交易記帳

- 轉帳採雙邊記帳。
- 同一筆業務交易使用同一個 `referenceId`。
- 每一筆 `TransLog` 自己有 `transactionId` UUID。
- 沖正不修改原紀錄，只新增反向交易紀錄。

### 帳戶狀態

- 轉帳、存款、提款都要求帳戶為 `ACTIVE`。
- 轉帳要求來源與目的帳戶幣別一致。
- 開戶核准後建立帳戶為 `ACTIVE`。

### 信用卡額度

- 建立信用卡交易時檢查 `creditLimit - currentBalance`。
- 刷退以負數交易表示，並扣回 `currentBalance`。

### 客戶資料

- `CustomerProfile.idNumber`, `email`, `phone`, `cif` 具 unique 意圖。
- 客戶註冊時會同步建立 profile 與 auth。
- 客戶登入後主要依 JWT 中的 `customerId` 取得資料。
- 開戶申請是 CustomerProfile 新版 KYC / 法遵欄位的重要來源。
- Account 模組只能透過 `CustomerService.syncAccountApplicationProfile` 同步開戶申請資料，不應直接操作 Customer 資料表。
- 每次管理端審核開戶申請後，無論核准、要求補件或駁回，都要同步最新審核結果，支援後續補件重送與歷程判讀。

---

## 12. 目前觀察到的注意事項

以下不是一定要立即修正的錯誤，而是後續開發時需要知道的狀態。

1. `TransferRiskHandler` 目前看起來尚未實作檢查邏輯，且未標註 Spring component，風控 AOP 對轉帳可能尚未真正生效。
2. `TransferService` 留有大額、高頻、24 小時累計金額風控 TODO。
3. `CustomerAuthServiceImpl.requestPasswordReset` 目前只在 console 印出重設連結，尚未接 SMTP。
4. `SessionUtil` 是舊式 session 工具，註解顯示已被 Spring Security 方案取代，但檔案仍存在。
5. 信用卡客戶端 API 使用 `/user/card-*`，沒有 `/api` 前綴；目前 axios 使用完整 `BASE_URL` 可正常打到後端，但與多數 API 命名風格不同。
6. 部分 Controller 直接回傳 Spring Data `Page`，部分轉成 `PageResponse`；前端解包時要注意格式不完全一致。
7. 信用卡交易的更新 / 刪除 API 存在，但交易類資料通常應避免物理刪除；程式註解也寫「不可刪除交易」，後續可確認是否只保留查詢與刷退。
8. `CardTxnService.update` 更新交易金額時未同步調整信用卡 `currentBalance`，若此 API 真的會被使用，需要確認業務規則。
9. `CardController.getMyCards` 目前呼叫 `findAll()`，名稱像客戶查自己的卡，但實際上可能回傳全部卡片。
10. `SecurityConfig` 目前 permit seed endpoints，適合開發期；正式環境需確認是否關閉。
11. JWT secret 有預設值，正式部署應以環境設定覆蓋。
12. `GlobalExceptionHandler` 對 `AccountException` 會保留 errorCode，但 `TransferException` 繼承 `BusinessException`，若前端需要轉帳錯誤碼，需要確認是否有專用 handler。
13. `TransferController` 與 `CashController` 位於 customer API 路徑，但目前沒有檢查操作帳戶是否屬於 JWT 中的 customerId。
14. `ScheduledTransferService.executeDueTransfers` 目前尚未接 `@Scheduled`，且到期執行只更新狀態，未真正執行轉帳。

---

## 13. 建議閱讀順序

如果要快速接手專案，建議照這個順序看：

1. `README.md`
2. `pom.xml`
3. `src/main/resources/application.properties`
4. `common/config/SecurityConfig.java`
5. `common/config/JwtAuthenticationFilter.java`
6. `common/util/JwtUtil.java`
7. `common/dto/response/ApiResponse.java`
8. `common/exception/GlobalExceptionHandler.java`
9. `frontend/src/api/axios.js`
10. `frontend/src/router/index.js`
11. `frontend/src/stores/auth.js`
12. `frontend/src/stores/customerAuth.js`
13. 各模組依需求進入：
    - 管理端登入 / 員工：`auth`
    - 客戶登入 / 註冊：`customer`
    - 帳務：`account`
    - 貸款：`loan`
    - 信用卡：`creditcard`
    - 風控：`risk`

若要理解「開戶申請同步顧客資料」這條線，建議接著看：

1. `account/dto/request/AccountApplicationRequest.java`
2. `account/service/AccountApplicationService.java`
3. `customer/service/CustomerService.java`
4. `customer/service/CustomerServiceImpl.java`
5. `customer/entity/CustomerProfile.java`
6. `frontend/src/views/admin/CustomerListView.vue`

---

## 14. 模組責任邊界

| 模組 | 負責內容 | 不建議直接跨越的地方 |
|---|---|---|
| `common` | 共用 config、DTO、例外、工具、檔案服務 | 不放具體業務邏輯 |
| `auth` | 行員、角色、登入、日誌 | 不處理客戶 JWT |
| `customer` | 客戶基本資料、客戶登入、開戶申請資料同步落點 | 不直接操作帳戶餘額 |
| `account` | 帳戶、交易、開戶、常用帳號、預約轉帳 | 不直接修改客戶認證資料；同步客戶資料需走 CustomerService |
| `loan` | 貸款申請與審核流程 | 不直接撥款到 account，除非設計整合點 |
| `creditcard` | 信用卡業務 | 不直接操作 account 交易紀錄 |
| `risk` | 風控規則、黑名單、風險事件 | 不承擔業務流程本身 |

---

## 15. 後續可補強文件

本文件是專案總覽。後續可拆出更細文件：

- API 規格文件：依模組列 request / response body。
- DB ERD：整理 entity 關聯與 table key。
- 權限矩陣：列出 roleCode 可操作的頁面與 API。
- 前端畫面地圖：route -> view -> api -> store。
- 交易一致性設計：轉帳、沖正、信用卡刷退的不可變紀錄規範。
- 風控設計文件：RiskScene、RiskTarget、RiskHandler、事件落庫規則。
