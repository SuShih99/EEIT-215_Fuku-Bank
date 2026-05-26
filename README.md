# 福庫銀行 (fuku-bank)

資展國際 EEIT-215 第四組專題。

福庫銀行是一套銀行業務系統，提供**管理端（行員）**與**客戶端（顧客）**兩套介面，涵蓋員工登入與權限、顧客管理、開戶與帳務、貸款、信用卡、風險控管等核心業務。

---

## 系統概覽

| 使用端 | 前端入口 | 後端 API |
|---|---|---|
| 客戶端 | `/`, `/login`, `/user/**` | 主要為 `/api/customer/**`，信用卡客戶端目前為 `/user/card-*` |
| 管理端 | `/admin/login`, `/admin/**` | 主要為 `/api/auth/**`, `/api/customers`, `/api/admin/**` |

目前有兩套認證模型：

- 管理端：Session-based，登入入口 `POST /api/auth/login`，前端保存 `auth_user` 並靠 Cookie 維持 session。
- 客戶端：JWT-based，登入入口 `POST /api/customer/auth/login`，前端保存 `customer_user` 與 `customer_token`。

---

## 技術棧

| 層級 | 技術 |
|---|---|
| 後端 | Java 21, Spring Boot 4.0.5, Spring Web, Spring Data JPA, Spring Security |
| 資料庫 | MS SQL Server |
| 前端 | Vue 3, Vite, Vue Router, Pinia, ant-design-vue, axios, Tailwind CSS |
| 建置 | Maven Wrapper, npm |

前端 `frontend/package.json` 指定 Node.js 版本為 `^20.19.0 || >=22.12.0`。

---

## 模組分工

| 模組 | 主要職責 | 負責人 |
|---|---|---|
| Common | 共用設定、統一回應、例外處理、JWT、CORS、檔案上傳、Email、匯率 API | 漢億 |
| Auth | 管理端登入、員工、角色、系統日誌 | 以琳 |
| Customer | 顧客資料、註冊驗證信、顧客登入、個資、密碼重設、開戶申請資料同步 | 以琳 |
| Account | 帳戶、開戶申請、轉帳、存提款、交易紀錄、沖正、常用帳號、預約轉帳 | 漢億 |
| Loan | 貸款申請、聯繫紀錄、二次填單、送審 | 泓翔 |
| Credit Card | 卡別、信用卡申請、明細審核、發卡、交易、刷退、帳單查詢 | 王昶 |
| Risk | 黑名單、風險事件、AOP 風控框架 | 世帆 |
| Notification | 站內通知中心、未讀管理、通知偏好設定、mail 事件同步通知 | 漢億 |

Account 與 Customer 目前有一個明確整合點：客戶送出開戶申請、管理端核准、補件或駁回後，Account 會呼叫 Customer 的 `syncAccountApplicationProfile`，把申請表欄位與審核結果同步到 `CUSTOMER_PROFILE`。

---

## 本地端啟動指南

### 1. 環境準備

- JDK 21
- Maven 3.8+，或直接使用專案內的 `./mvnw`
- MS SQL Server
- Node.js `^20.19.0 || >=22.12.0`
- npm

### 2. Clone 專案

```bash
git clone https://github.com/han369k/java-easy-bank.git
cd java-easy-bank
```

### 3. 建立資料庫

在 SQL Server 建立資料庫：

```sql
CREATE DATABASE java_easy_bank;
```

### 4. 建立本機設定檔

`src/main/resources/application.properties` 目前只負責匯入本機設定，內容不要更動。請在 `src/main/resources/` 建立自己的 `application-local.properties`，放 DB、SMTP、JWT 等本機或私密設定：

```properties
server.port=8080

# Database
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=java_easy_bank;encrypt=false;trustServerCertificate=true
spring.datasource.username=你的帳號
spring.datasource.password=你的密碼
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Jackson
spring.jackson.time-zone=Asia/Taipei
spring.jackson.serialization.write-dates-as-timestamps=false

# App
app.frontend-url=http://localhost:5173
app.jwt.secret=請放至少 32 字元以上的本機密鑰
app.jwt.expiration-ms=86400000
app.upload.dir=uploads

# SMTP / Mail
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=你的寄件信箱
spring.mail.password=你的應用程式密碼
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.ssl.protocols=TLSv1.2
spring.mail.properties.mail.smtp.ssl.trust=smtp.gmail.com
```

`application-local.properties` 會包含資料庫密碼、SMTP 密碼與 JWT secret，請勿提交到遠端。

### 5. 初始化資料

SQL 腳本位於 `src/main/resources/database/`，目前已依模組拆分。

建表腳本：

- `auth_init.sql`
- `customer_init.sql`
- `account_init.sql`
- `loan_init.sql`
- `card_init.sql`
- `risk_init.sql`

種子資料 / mock data：

- `auth_insert.sql`
- `customer_insert.sql`
- `account_mockdata.sql`
- `loan_mockdata.sql`
- `card_mockdata.sql`
- `risk_mockdata.sql`

Customer 腳本已更新為新版 `CUSTOMER_PROFILE` 結構，包含開戶申請同步欄位，例如國籍、戶籍地址、現居地址、職業、任職機構、PEP、稅務居民、最近一次開戶申請狀態、審核結果與建立帳號。

### 6. 啟動後端

```bash
./mvnw spring-boot:run
```

啟動成功後 console 會顯示 `Started JavaEasyBankApplication`。

### 7. 啟動前端

```bash
cd frontend
npm install
npm run dev
```

前端預設跑在 `http://localhost:5173`，後端預設為 `http://localhost:8080`。管理端 Session Cookie 依賴 credentials，開發時建議前後端都使用 `localhost`，避免 `localhost` / `127.0.0.1` 混用造成登入狀態不一致。

---

## 專案結構

```text
java-easy-bank/
├── README.md
├── pom.xml
├── src/main/java/com/javaeasybank/
│   ├── JavaEasyBankApplication.java
│   ├── common/          # 共用元件
│   ├── auth/            # 管理端登入、員工、角色、日誌
│   ├── customer/        # 顧客資料與顧客登入
│   ├── account/         # 帳戶、開戶、轉帳、交易
│   ├── loan/            # 貸款申請與審核
│   ├── creditcard/      # 信用卡業務
│   ├── notification/    # 站內通知與通知偏好
│   └── risk/            # 風控與黑名單
├── src/main/resources/
│   ├── application.properties
│   ├── database/
│   ├── doc/
│   └── static/img/
├── src/test/java/com/javaeasybank/
└── frontend/
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── api/
        ├── layouts/
        ├── router/
        ├── stores/
        └── views/
```

---

## 常用驗證指令

```bash
./mvnw -q test
```

```bash
cd frontend
npm run test:unit
```

```bash
cd frontend
npm run build
```

目前 `./mvnw -q test` 已調整為可在無本機 SQL Server 的環境下執行完整單元測試（`JavaEasyBankApplicationTests` 為 class load smoke test）。

---

## 開發規範

- 所有 API 原則上回傳 `ApiResponse<T>`。
- 業務邏輯錯誤使用 `BusinessException`，Account 模組可使用帶錯誤碼的 `AccountException`。
- 金額型別以 `BigDecimal` 為主，DB 依模組目前多使用 `DECIMAL(19,4)` 或信用卡交易常見的 `DECIMAL(15,2)`。
- 後端模組維持 controller / service / repository / entity / dto / enums 分層。
- 跨模組資料寫入應透過對方 service 暴露的方法，不直接改別的模組資料表。
- Git branch 命名建議：`feature/{模組}-{功能}`、`fix/{模組}-{問題}`。
- Commit 格式建議：`feat account: 新增轉帳 API`。

---

## 文件

- 詳細專案總覽：`src/main/resources/doc/ProjectOverview.md`
- Account 模組開發文件：`src/main/java/com/javaeasybank/account/doc/DeveloperDoc.md`

---

## 開發團隊

| 成員 | 角色 |
|---|---|
| 漢億 | 組長 / Tech Lead |
| 以琳 | 副組長 |
| 泓翔 | 組員 |
| 王昶 | 組員 |
| 世帆 | 組員 |

---

*資展國際 EEIT-215 第四組 — 2026*
