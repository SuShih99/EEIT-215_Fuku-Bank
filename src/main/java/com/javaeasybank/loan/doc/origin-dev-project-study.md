# origin/dev 專案整體學習文件

> 基準分支：`origin/dev`  
> 基準版本：`98d3953dc3adcaed1c9ec4fa5b9072952dcafc1b`  
> 最新 commit：`98d3953 [fix] 升級 Lombok 至 1.18.38 以解決 Java 21 編譯失敗的問題`  
> 整理日期：2026-05-24  
> 範圍：依目前最新 `origin/dev` 的檔案樹、`README.md`、`pom.xml`、`frontend/package.json`、前端 router、後端模組與既有 `ProjectOverview.md` 重新整理。  
> 備註：本文件整理整體專案；貸款模組細節請搭配同目錄的 `LoanModuleFileCatalog.md` 與 `LoanModuleFileCatalog-Summary.md`。

## 0. 最新同步摘要

本次已重新對齊最新 `origin/dev`，重點如下：

1. 後端基準仍是 Java 21 與 Spring Boot 4.0.5。
2. `pom.xml` 已將 Lombok 升級到 `1.18.38`，用來解決 Java 21 編譯相容性問題。
3. MapStruct 仍是 `1.5.5.Final`，並搭配 `lombok-mapstruct-binding 0.2.0` 讓 Lombok 與 MapStruct annotation processor 能一起工作。
4. 前端仍是 Vue 3 + Vite 8，`frontend/package.json` 指定 Node.js `^20.19.0 || >=22.12.0`。
5. 專案實際後端入口仍是 `FukuBankApplication.java`，README 部分文字仍可能出現舊的 `JavaEasyBankApplication` 名稱。

## 1. 專案定位

福庫銀行是一套銀行業務系統，提供兩個主要使用端：

| 使用端 | 對象 | 前端入口 | 後端 API 型態 |
|---|---|---|---|
| 客戶端 | 一般顧客 | `/`, `/login`, `/register`, `/user/**` | 多數為 `/api/customer/**`，信用卡客戶端多為 `/user/card-*` |
| 管理端 | 行員、管理者、資安或系統角色 | `/admin/login`, `/admin/**` | 多數為 `/api/auth/**`, `/api/customers`, `/api/admin/**` |

目前主模組如下：

| 模組 | 主要職責 |
|---|---|
| `common` | 共用設定、CORS、Security、JWT、統一回應、例外處理、Email、檔案儲存、匯率、Line Pay 設定 |
| `auth` | 管理端登入、員工帳號、角色、系統操作日誌 |
| `customer` | 顧客資料、註冊、登入、Email 驗證、密碼重設、個資、登入紀錄、裝置管理 |
| `account` | 帳戶、開戶申請、轉帳、常用帳號、預約轉帳、換匯、存提款、交易紀錄、沖正、電子存摺 |
| `loan` | 貸款申請、聯繫紀錄、二次填單、補件、送風控、撥款整合、還款 |
| `creditcard` | 卡別、信用卡申請、補件、明細審核、發卡、交易、刷退、帳單、繳款、Line Pay |
| `risk` | 黑名單、風險事件、信用資訊、風控檢查、人工審核任務、callback |
| `notification` | 客戶站內通知、未讀數、標記已讀、通知偏好 |

## 2. 技術棧

### 2.1 後端

| 類別 | 技術 | 目前用途 |
|---|---|---|
| 語言 | Java 21 | 後端主要開發語言 |
| Framework | Spring Boot 4.0.5 | 後端啟動、Bean 管理、Web / JPA / Security 整合 |
| Web | Spring Web / Spring MVC | REST API |
| ORM | Spring Data JPA / Hibernate | Entity、Repository、交易資料存取 |
| Security | Spring Security | 管理端 Session、客戶端 JWT、角色與 IP 限制 |
| Validation | Spring Validation | DTO 請求驗證 |
| DB Driver | Microsoft SQL Server JDBC | 連接 MS SQL Server |
| DTO / Entity 輔助 | Lombok 1.18.38 | 減少 getter、setter、constructor 樣板碼；目前版本用來支援 Java 21 編譯 |
| Mapper | MapStruct 1.5.5.Final + lombok-mapstruct-binding 0.2.0 | 信用卡模組 DTO / Entity 轉換，並讓 MapStruct 與 Lombok annotation processing 相容 |
| JWT | JJWT 0.12.6 | 客戶端 token 簽發與解析 |
| Mail / Template | Spring Mail、Thymeleaf | 通知信與 HTML mail template |
| PDF / Export | OpenHTMLtoPDF、iText、OpenPDF、OpenCSV | 電子存摺、貸款契約、信用卡帳單、CSV 匯出 |
| HTTP Client | RestTemplate + Apache HttpClient 5 | 呼叫風控、內部 callback、PATCH 支援 |
| Build | Maven Wrapper | 後端建置與測試 |

### 2.2 前端

| 類別 | 技術 | 目前用途 |
|---|---|---|
| Framework | Vue 3 | 前端單頁應用 |
| Build Tool | Vite 8 | dev server、production build |
| Runtime | Node.js `^20.19.0 || >=22.12.0` | 前端開發環境 |
| Router | Vue Router 5 | 客戶端與管理端路由 |
| Store | Pinia | 管理端與客戶端登入狀態 |
| UI | ant-design-vue、Font Awesome、Ant Design Icons | 後台元件、icon、提示 |
| HTTP | Axios | 與後端 API 溝通 |
| CSS | Tailwind CSS 4、自訂 theme css | 全站樣式與主題 |
| Chart / Export | Chart.js、vue-chartjs、xlsx、file-saver | 統計圖、Excel 匯出 |
| Test / Lint | Vitest、ESLint、Oxlint、Prettier | 前端測試、靜態檢查、格式化 |

### 2.3 資料庫與環境

| 項目 | 說明 |
|---|---|
| 目標 DB | MS SQL Server |
| 主設定檔 | `src/main/resources/application.properties` 只匯入 `application-local.properties` |
| 本機私密設定 | DB、SMTP、JWT secret、frontend URL、upload dir 應放在 `application-local.properties` |
| SQL 腳本 | `src/main/resources/database/*.sql` 依模組拆分 |
| 上傳檔案 | 預設透過 `FileStorageService` 存到本機 upload 目錄 |

## 3. 專案結構

```text
java-easy-bank/
├── README.md
├── pom.xml
├── src/main/java/com/javaeasybank/
│   ├── FukuBankApplication.java
│   ├── common/
│   ├── auth/
│   ├── customer/
│   ├── account/
│   ├── loan/
│   ├── creditcard/
│   ├── notification/
│   └── risk/
├── src/main/resources/
│   ├── application.properties
│   ├── database/
│   ├── doc/
│   ├── fonts/
│   ├── static/
│   └── templates/mail/
├── src/test/java/com/javaeasybank/
└── frontend/
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── api/
        ├── assets/
        ├── components/
        ├── data/
        ├── layouts/
        ├── router/
        ├── stores/
        ├── utils/
        └── views/
```

後端模組大多維持以下分層：

```text
module/
├── controller/
├── service/
├── repository/
├── entity/
├── dto/
├── enums/
└── utils 或 mapper
```

## 4. 啟動與常用指令

### 4.1 後端

```powershell
.\mvnw.cmd spring-boot:run
```

重點：

1. 後端入口是 `FukuBankApplication.java`。
2. `@EnableAsync` 啟用非同步工作，`@EnableJpaAuditing` 啟用 JPA auditing。
3. `application.properties` 不放私密資訊，只匯入 `application-local.properties`。
4. `FukuBankApplication` 中有 `restoreDatabase()` 工具方法，但目前在 `main` 中是註解狀態，不會自動重置 DB。

### 4.2 前端

```powershell
cd frontend
npm install
npm run dev
```

重點：

1. 前端預設 dev server 是 `http://localhost:5173`。
2. 後端預設是 `http://localhost:8080`。
3. 管理端使用 Session Cookie，開發時前後端都建議用 `localhost`，避免 cookie domain 不一致。
4. `frontend/src/api/axios.js` 負責 base URL、token、withCredentials 與錯誤攔截。

### 4.3 測試與建置

```powershell
.\mvnw.cmd test
```

```powershell
cd frontend
npm.cmd run test:unit
npm.cmd run build
```

在 Windows PowerShell 中若 `npm run ...` 被 `npm.ps1` 執行原則擋住，可改用 `npm.cmd run ...`。

## 5. 認證與授權

系統有兩套登入模型：

| 使用端 | 認證方式 | 前端保存 | 後端入口 | 權限依據 |
|---|---|---|---|---|
| 管理端 | Session-based | `localStorage.auth_user` + Cookie | `POST /api/auth/login` | Spring Security Session + role |
| 客戶端 | JWT-based | `localStorage.customer_user`, `localStorage.customer_token` | `POST /api/customer/auth/login` | JWT claim：`role`, `customerId` |

### 5.1 管理端 Session

1. 前端呼叫 `POST /api/auth/login`。
2. 後端用 `AuthenticationManager` 驗證帳密。
3. `CustomUserDetailsService` 查員工與角色。
4. 成功後把 `SecurityContext` 放入 HttpSession。
5. 前端保存 `auth_user`。
6. 管理端路由守衛進入 `/admin/**` 時會呼叫 `/api/auth/me` 確認 session。

主要檔案：

| 檔案 | 用途 |
|---|---|
| `common/config/SecurityConfig.java` | Session / JWT 共用安全設定 |
| `auth/controller/AuthController.java` | 管理端登入、登出、me |
| `auth/service/CustomUserDetailsService.java` | 管理端帳密驗證資料來源 |
| `frontend/src/stores/auth.js` | 管理端登入狀態 |
| `frontend/src/router/index.js` | 管理端路由守衛 |

### 5.2 客戶端 JWT

1. 前端呼叫 `POST /api/customer/auth/login`。
2. 後端驗證 `CustomerAuth`。
3. `JwtUtil` 產生 token，包含 `role` 與 `customerId`。
4. 前端把 token 存到 `customer_token`。
5. Axios request interceptor 加上 `Authorization: Bearer <token>`。
6. `JwtAuthenticationFilter` 解析 token，寫入 Spring Security context。

主要檔案：

| 檔案 | 用途 |
|---|---|
| `customer/controller/CustomerAuthController.java` | 客戶註冊、登入、驗證信、密碼重設 |
| `customer/service/CustomerAuthServiceImpl.java` | 客戶登入與註冊邏輯 |
| `common/util/JwtUtil.java` | JWT 產生與解析 |
| `common/config/JwtAuthenticationFilter.java` | 每次請求解析 JWT |
| `frontend/src/stores/customerAuth.js` | 客戶登入狀態 |

### 5.3 SecurityConfig 目前重點

| 規則 | 說明 |
|---|---|
| `/api/auth/login`, `/api/auth/logout` | 管理端登入登出公開 |
| `/api/customer/auth/register`, `/login`, `/request-reset`, `/reset-password`, `/verify-email` | 客戶註冊登入與密碼重設公開 |
| `/uploads/**`, `/img/**` | 靜態資源公開 |
| `/api/public/**`, `/api/linepay/**`, `/api/loan-applications/rate-rules` | 公開 API 或支付入口 |
| `/api/risk/**`, `/api/loan-callbacks/**`, `/api/transfer-callbacks/**`, `/api/account-callbacks/**` | 限制本機 IP |
| 其他 API | 需要登入 |

## 6. Common 模組

| 檔案 / 類別 | 用途 |
|---|---|
| `ApiResponse<T>` | 統一 API 回應格式 |
| `PageResponse` | 統一分頁回應格式 |
| `BusinessException` | 業務錯誤例外 |
| `GlobalExceptionHandler` | 統一例外轉 API 回應 |
| `SecurityConfig` | Spring Security 主設定 |
| `JwtAuthenticationFilter` | 客戶端 JWT 解析 |
| `CorsConfig` / `WebConfig` | CORS 與靜態資源設定 |
| `RestTemplateConfig` | 提供支援 PATCH 的 RestTemplate |
| `FileStorageService` | 上傳檔案儲存與刪除 |
| `EmailService` | 寄送驗證信、密碼重設、貸款、通知等 Email |
| `ExchangeRateController` / `ExchangeRateService` | 匯率查詢與換匯支援 |
| `LinePayProperties` / `LinePaySignatureUtil` | Line Pay 設定與簽章 |

## 7. 後端模組整理

### 7.1 Auth

| 分層 | 代表檔案 | 職責 |
|---|---|---|
| Controller | `AuthController`, `AuthActionLogController` | 管理端登入、登出、me、系統日誌 |
| Service | `AuthEmpService`, `CustomUserDetailsService`, `AuthActionLogService` | 員工、角色、登入驗證、操作紀錄 |
| Entity | `AuthEmp`, `AuthRole`, `AuthDept`, `AuthLoginLog`, `AuthActionLog` | 管理端員工與日誌資料 |
| Frontend | `AdminLoginView`, `EmployeeListView`, `SystemLogView` | 管理端登入、員工管理、系統日誌 |

學習重點：

1. 管理端採 Session-based 認證。
2. 系統日誌可匯出 CSV。
3. 權限透過 role 搭配前端 router meta 控制。

### 7.2 Customer

| 分層 | 代表檔案 | 職責 |
|---|---|---|
| Controller | `CustomerAuthController`, `CustomerController`, `CustomerSecurityController` | 客戶註冊登入、個資、裝置、登入紀錄 |
| Service | `CustomerAuthServiceImpl`, `CustomerServiceImpl`, `CustomerSecurityService`, `LoginAuditService` | 帳密驗證、Email 驗證、密碼重設、個資、登入稽核 |
| Entity | `CustomerProfile`, `CustomerAuth`, `CustomerLoginLog`, `CustomerDevice` | 客戶主資料、帳密、登入紀錄、裝置 |
| Frontend | `RegisterView`, `UserLoginView`, `UserProfileView`, `Security*.vue` | 客戶註冊登入與安全設定 |

學習重點：

1. 客戶端登入採 JWT。
2. 註冊與重設密碼會用 Email。
3. Account 開戶審核結果會同步回 `CustomerProfile`。
4. `customer_insert.sql` 是 demo customer seed data。

### 7.3 Account

| 分層 | 代表檔案 | 職責 |
|---|---|---|
| Controller | `AccountController`, `CustomerAccountController`, `TransferController`, `ScheduledTransferController`, `FavoriteAccountController`, `CashController`, `PassbookPdfController` | 帳戶查詢、轉帳、預約轉帳、常用帳號、存提款、電子存摺 |
| Service | `AccountService`, `TransferService`, `ScheduledTransferService`, `FavoriteAccountService`, `PassbookPdfService`, `AccountIntegrationService` | 帳務規則、交易紀錄、跨模組整合 |
| Entity | `Account`, `TransLog`, `AccountApplication`, `FavoriteAccount`, `ScheduledTransfer`, `PendingTransfer` | 帳戶、交易、開戶申請、常用帳號、預約與待審轉帳 |
| Frontend | `UserAccountsView`, `TransferView`, `ScheduledTransferView`, `FavoriteAccountsView`, `EPassbookView`, `TransLogView` | 客戶帳務功能與後台交易查詢 |

學習重點：

1. 帳務金額以 `BigDecimal` 為主。
2. 轉帳會接風控，可能直接通過、拒絕或進人工審核。
3. `AccountIntegrationService` 是信用卡與貸款整合帳務的核心入口。
4. 預約轉帳與待審轉帳分別用 `ScheduledTransfer` 與 `PendingTransfer`。

### 7.4 Loan

| 分層 | 代表檔案 | 職責 |
|---|---|---|
| Controller | `LoanApplicationController`, `LoanAdminController`, `LoanDocumentController`, `LoanAccountController`, `LoanCallbackController` | 貸款申請、行員審核、補件、帳戶查詢、callback |
| Service | `LoanApplicationService`, `LoanDocumentService`, `LoanAccountService`, `LoanRepaymentService`, `LoanContractPdfService` | 貸款生命週期、補件、建帳、還款、契約 PDF |
| Entity | `LoanApplication`, `LoanReviewDetail`, `LoanDocument`, `LoanAccount`, `LoanRepayment` | 申請、審核、補件、帳戶、還款期數 |
| Frontend | `LoanApplyView`, `LoanStatusView`, `LoanAccountView`, `LoanRepaymentView`, `LoanApplicationView` | 客戶申請/還款與行員審核 |

學習重點：

1. 貸款會串風控與帳戶兩個模組。
2. 行員送審後透過 `LoanRiskClient` 呼叫風控。
3. 核准後透過帳戶模組建貸款帳戶與撥款。
4. 還款會同步帳務與貸款期數狀態。

### 7.5 Credit Card

| 分層 | 代表檔案 | 職責 |
|---|---|---|
| Controller | `CardTypeController`, `CardApplicationController`, `CardApplicationDocumentController`, `CardTxnController`, `CardBillController`, `CardPaymentController`, `LinePayController` | 客戶端卡別、申請、補件、交易、帳單、繳款、Line Pay |
| Admin Controller | `CardTypeAdminController`, `CardApplicationAdminController`, `CardApplicationItemAdminController`, `CardTxnAdminController`, `CardBillAdminController` | 管理端卡別、申請審核、交易、帳單 |
| Service | `CardAppService`, `CardReviewService`, `CardTxnService`, `BillService`, `CardPaymentService`, `LinePayService` | 申請、審核、發卡、交易、帳單、繳款、Line Pay |
| Entity | `CardType`, `CardApplication`, `CardApplicationDocument`, `CreditCard`, `CardAccount`, `CardTransaction`, `CardBill`, `Merchant` | 信用卡業務資料 |
| Mapper | `creditcard/mapper/*` | MapStruct DTO 轉換 |
| Frontend | `CardTypeListView`, `CardApplicationForm`, `CardView`, `CardTxnView`, `CardBillView`, `LinePayConfirmView` | 客戶端與管理端信用卡畫面 |

學習重點：

1. 信用卡模組大量使用 MapStruct。
2. 客戶端信用卡 API 多使用 `/user/card-*` 路徑。
3. 帳單 PDF 可用 iText / PDF 工具處理。
4. `LinePayController` 與 `LinePayService` 是新版付款整合點。

### 7.6 Risk

| 分層 | 代表檔案 | 職責 |
|---|---|---|
| Controller | `RiskCheckController`, `RiskReviewController`, `RiskEventController`, `ReviewTaskController`, `BlackListController`, `CreditScoreController` | 風控檢查、人工審核、事件紀錄、黑名單、信用資訊 |
| Service | `RiskCheckService`, `RiskReviewService`, `ReviewTaskService`, `RiskEventService`, `BlackListService`, `CreditScoreService`, `CallbackService` | 風控判斷、任務建立、callback、信用評估 |
| Entity | `RiskEventLog`, `ReviewTask`, `Blacklist`, `CustomerCreditInfo` | 風控事件、審核任務、黑名單、信用資料 |
| Frontend | `RiskEventView`, `ReviewTask`, `BlackListView`, `CreditList`, `CreditDetail` | 管理端風控畫面 |

學習重點：

1. 風控可被登入、轉帳、貸款等流程呼叫。
2. 高風險或需人工判斷時會建立 `ReviewTask`。
3. callback 會把審核結果回傳業務模組。
4. 風控 API 多限制本機 IP，避免外部直接呼叫。

### 7.7 Notification

| 分層 | 代表檔案 | 職責 |
|---|---|---|
| Controller | `NotificationController`, `NotificationPreferenceController` | 客戶查通知、未讀數、標記已讀、通知偏好 |
| Service | `NotificationServiceImpl`, `NotificationPreferenceServiceImpl` | 建立通知、依偏好決定是否通知、偏好更新 |
| Entity | `Notification`, `NotificationPreference` | 站內通知與通知偏好 |
| Frontend | `UserLayout`, `NotificationSettingsView`, `api/notification.js` | 客戶通知鈴鐺與偏好設定 |

學習重點：

1. 站內通知與 Email 分開，Email 仍由 `EmailService` 負責。
2. 通知偏好可控制部分通知類型，安全類通知不應任意關閉。
3. 通知目前主要依 JPA entity 建表，沒有獨立 notification SQL init。

## 8. 前端架構整理

### 8.1 路由

| 區域 | 代表路由 | 代表畫面 |
|---|---|---|
| Landing | `/` | `LandingView.vue` |
| 客戶登入前 | `/login`, `/register`, `/verify-email`, `/reset-password` | 登入、註冊、驗證信、重設密碼 |
| 客戶首頁 | `/user/home` | `UserHomeView.vue` |
| 客戶個資與安全 | `/user/profile`, `/user/security/**` | 個資、密碼、登入紀錄、裝置 |
| 帳務 | `/user/accounts`, `/user/transfer`, `/user/scheduled-transfer`, `/user/exchange` | 帳戶、轉帳、預約轉帳、換匯 |
| 貸款 | `/user/loan-apply`, `/user/loan-status`, `/user/loan-accounts`, `/user/loan-repayment` | 申請、進度、帳戶、還款 |
| 信用卡 | `/user/card-*`, `/user/linepay/confirm` | 卡別、申請、卡片、交易、帳單、Line Pay |
| 管理端 | `/admin/**` | 員工、客戶、帳戶、信用卡、貸款、風控、日誌 |

### 8.2 Store

| Store | 用途 |
|---|---|
| `stores/auth.js` | 管理端登入使用者、session 驗證 |
| `stores/customerAuth.js` | 客戶端 token、customer user、登入登出 |
| `stores/counter.js` | Pinia 範例 / 基礎 store |

### 8.3 API 檔案

| 檔案 | 用途 |
|---|---|
| `api/axios.js` | 共用 axios instance、token、withCredentials、錯誤攔截 |
| `api/auth.js` | 管理端登入 / me / 登出 |
| `api/customerAuth.js` | 客戶註冊、登入、驗證信、密碼重設 |
| `api/customer.js` | 客戶資料 |
| `api/account*.js` | 帳戶、開戶、帳戶查詢 |
| `api/scheduledTransfer.js` | 預約轉帳 |
| `api/favoriteAccount.js` | 常用帳號 |
| `api/card*.js`, `api/userCard*.js` | 信用卡管理端與客戶端功能 |
| `api/notification.js` | 站內通知 |
| `api/notificationPreference.js` | 通知偏好 |

### 8.4 視覺與資源

| 類別 | 檔案 | 用途 |
|---|---|---|
| Landing | `LandingView.vue`, `landing-wabi-bg.webp` | 首頁與新版 landing 視覺 |
| Layout | `UserLayout.vue`, `AdminLayout.vue` | 客戶端與管理端殼層 |
| Theme | `customer-theme.css`, `admin-theme.css`, `main.css`, `tailwind.css` | 客戶端與後台樣式 |
| Demo data | `customerDemoAccounts.js`, `frontend/public/demo-documents/*` | Demo 顧客資料與文件素材 |
| Logo | `JbLogo.vue` | 共用品牌標誌元件 |

## 9. 資料庫腳本

### 9.1 建表腳本

| 檔案 | 模組 |
|---|---|
| `auth_init.sql` | Auth |
| `customer_init.sql` | Customer |
| `account_init.sql` | Account |
| `loan_init.sql` | Loan |
| `card_init.sql` | Credit Card |
| `risk_init.sql` | Risk |
| `ddl_PendingTransfer.sql` | Account 待審轉帳補充 |
| `loan_document_batch_migration.sql` | Loan 補件批次欄位調整 |

### 9.2 種子與 mock data

| 檔案 | 模組 |
|---|---|
| `auth_insert.sql` | 管理端員工 / 角色資料 |
| `customer_insert.sql` | 客戶 demo data |
| `account_mockdata.sql` | 帳戶與交易 demo data |
| `loan_mockdata.sql` | 貸款 demo data |
| `card_mockdata.sql` | 信用卡 demo data |
| `risk_mockdata.sql` | 風控 demo data |

### 9.3 建議初始化順序

1. 先跑 `auth_init.sql`, `customer_init.sql`, `account_init.sql`, `loan_init.sql`, `card_init.sql`, `risk_init.sql`。
2. 再跑 `auth_insert.sql`。
3. 再跑 `customer_insert.sql`。
4. 最後依需求跑 `account_mockdata.sql`, `loan_mockdata.sql`, `card_mockdata.sql`, `risk_mockdata.sql`。

## 10. 重要運行流程

### 10.1 客戶登入

```text
UserLoginView.vue
-> api/customerAuth.js
-> CustomerAuthController
-> CustomerAuthServiceImpl
-> JwtUtil
-> customerAuth store
-> axios Authorization header
```

### 10.2 管理端登入

```text
AdminLoginView.vue
-> api/auth.js
-> AuthController
-> AuthenticationManager
-> CustomUserDetailsService
-> HttpSession
-> auth store
-> router guard /api/auth/me
```

### 10.3 開戶申請

```text
AccountApplicationView.vue
-> AccountApplicationController
-> AccountApplicationService
-> AccountApplicationRepository
-> FileStorageService
-> 管理端 AccountApplicationListView.vue 審核
-> CustomerServiceImpl.syncAccountApplicationProfile
```

### 10.4 轉帳與風控

```text
TransferView.vue
-> TransferController
-> TransferService
-> TransferRiskClient
-> RiskCheckController / RiskCheckService
-> 通過：更新 Account + TransLog
-> 人工審核：PendingTransfer + ReviewTask
-> NotificationService
```

### 10.5 預約轉帳

```text
ScheduledTransferView.vue
-> ScheduledTransferController
-> ScheduledTransferService
-> ScheduledTransferRepository
-> 到期後執行轉帳或標記失敗
```

### 10.6 貸款

```text
LoanApplyView.vue
-> LoanApplicationController
-> LoanApplicationService
-> LoanAdminController / LoanReviewModal.vue
-> LoanRiskClient
-> RiskReviewService
-> LoanCallbackController
-> AccountIntegrationService
-> LoanAccountService / LoanRepaymentService
```

### 10.7 信用卡

```text
CardApplicationForm.vue
-> CardApplicationController
-> CardAppService
-> 管理端 CardApplicationList / CardApplicationDetailView
-> CardReviewService
-> CreditCardService / CardAccountService
-> CardTxnService / BillService / CardPaymentService
```

### 10.8 Line Pay

```text
LinePayConfirmView.vue
-> LinePayController
-> LinePayService
-> LinePayProperties
-> LinePaySignatureUtil
-> CardPayment / payment result
```

### 10.9 通知

```text
業務 service
-> NotificationService
-> NotificationPreferenceService
-> NotificationRepository
-> UserLayout 通知鈴鐺 / NotificationSettingsView
```

## 11. 技術使用方法與學習時機

| 技術 | 什麼時候看 | 建議入口 |
|---|---|---|
| Spring Boot | 想理解後端怎麼啟動 | `FukuBankApplication.java`, `pom.xml` |
| Spring MVC | 想理解 API 怎麼接前端 | 任一 `controller` |
| Spring Security | 想理解登入、角色、API 為何被擋 | `SecurityConfig`, `JwtAuthenticationFilter` |
| JWT | 想理解客戶端登入狀態 | `JwtUtil`, `CustomerAuthServiceImpl`, `axios.js` |
| Session | 想理解管理端登入狀態 | `AuthController`, `CustomUserDetailsService`, `auth.js` |
| Spring Data JPA | 想理解資料庫怎麼映射 | 各模組 `entity` + `repository` |
| Transaction | 想理解多表更新一致性 | `TransferService`, `AccountIntegrationService`, `LoanApplicationService` |
| RestTemplate | 想理解模組對模組呼叫 | `LoanRiskClient`, `TransferRiskClient`, `LoginRiskClient` |
| MultipartFile / FormData | 想理解檔案上傳 | `FileStorageService`, 補件或開戶申請頁 |
| JavaMailSender / Thymeleaf | 想理解 Email 通知 | `EmailService`, `templates/mail` |
| OpenHTMLtoPDF / iText | 想理解 PDF | `PassbookPdfService`, `LoanContractPdfService`, `CardBillPDFService` |
| Lombok 1.18.38 | 想理解 entity / dto / service 為什麼沒有手寫 getter、setter、constructor，或處理 Java 21 編譯相容問題 | `pom.xml`, 各模組 entity / dto / service |
| MapStruct | 想理解 DTO 轉換 | `creditcard/mapper/*` |
| Vue Router | 想理解頁面怎麼切 | `frontend/src/router/index.js` |
| Pinia | 想理解登入狀態 | `stores/auth.js`, `stores/customerAuth.js` |
| Axios | 想理解 API 呼叫 | `frontend/src/api/axios.js` |
| Tailwind / theme CSS | 想理解畫面樣式 | `frontend/src/assets/*.css` |
| Vitest / JUnit / Mockito | 想補測試 | `src/test/java`, `frontend/src/api/customerAuth.spec.js` |

## 12. 建議閱讀路線

### 12.1 想先懂整體

1. `README.md`
2. `src/main/resources/doc/ProjectOverview.md`
3. `pom.xml`
4. `frontend/package.json`
5. `FukuBankApplication.java`
6. `frontend/src/router/index.js`

### 12.2 想懂登入權限

1. `SecurityConfig`
2. `AuthController`
3. `CustomerAuthController`
4. `JwtAuthenticationFilter`
5. `JwtUtil`
6. `frontend/src/stores/auth.js`
7. `frontend/src/stores/customerAuth.js`
8. `frontend/src/api/axios.js`

### 12.3 想懂一條完整業務線

| 業務 | 建議順序 |
|---|---|
| 帳戶 | `UserAccountsView` -> `AccountController` -> `AccountService` -> `AccountRepository` -> `Account` |
| 轉帳 | `TransferView` -> `TransferController` -> `TransferService` -> `TransferRiskClient` -> `TransLog` |
| 貸款 | `LoanApplyView` -> `LoanApplicationController` -> `LoanApplicationService` -> `LoanRiskClient` -> `AccountIntegrationService` |
| 信用卡 | `CardApplicationForm` -> `CardApplicationController` -> `CardAppService` -> `CardReviewService` -> `CreditCardService` |
| 風控 | `RiskCheckController` -> `RiskCheckService` -> `RiskEventService` -> `ReviewTaskService` |
| 通知 | `NotificationController` -> `NotificationServiceImpl` -> `NotificationPreferenceServiceImpl` -> `UserLayout` |

## 13. 目前版本注意事項

1. 最新 `origin/dev` 已把 Lombok 升級到 `1.18.38`，這是 Java 21 編譯相容性的關鍵修正；若本機仍編譯失敗，先確認 Maven 有抓到新版 Lombok。
2. `README.md` 的專案結構仍寫 `JavaEasyBankApplication.java`，但目前實際入口檔是 `FukuBankApplication.java`。
3. `application.properties` 只有匯入本機設定，真正 DB、SMTP、JWT、Line Pay 等設定要放 `application-local.properties`。
4. 管理端 Session 和客戶端 JWT 共用同一組 Spring Security 設定，debug 權限問題時要先分清楚使用端。
5. 信用卡客戶端 API 路徑多為 `/user/card-*`，和多數 `/api/**` API 命名風格不同。
6. 風控與 callback API 有 IP 限制，本機測試通常要走 `localhost` 或 `127.0.0.1`。
7. Notification 目前主要由 JPA entity 管表，尚未看到獨立 notification SQL init。
8. 前端新增 landing 視覺與 demo assets，可從 `LandingView.vue`, `landing-wabi-bg.webp`, `customerDemoAccounts.js` 開始看。
9. 同一個業務通常會跨前端 view、api 檔、controller、service、repository、entity、SQL 與通知層，建議用一條流程追，不要只按資料夾看。
