# 爪哇銀行 (Java Easy Bank)

資展國際 EEIT-215 第四組專題

銀行業務系統，提供**管理端（行員）**與**客戶端（顧客）**兩套介面，涵蓋顧客管理、帳務監控、授信放款、信用卡業務、保險業務等核心功能。

---

## 技術棧

| 項目 | 版本 |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.5 |
| Spring Data JPA / Hibernate | 隨 Spring Boot |
| Spring Security | 隨 Spring Boot |
| Vue | 3 |
| MS SQL Server | - |
| Maven | - |

---

## 系統架構

```
管理端（行員）  →  /api/admin/**
客戶端（顧客）  →  /api/client/**
```

### 模組分工

| 模組 | 負責人 |
|---|---|
| 共用元件 | 漢億（組長） |
| 系統與權限 (Auth) + 顧客管理 (Customer) | 以琳（副組長） |
| 帳務監控 (Account) | 漢億 |
| 授信放款 (Loan) | 泓翔 |
| 信用卡 (Credit Card) | 王昶 |
| 保險 (Insurance) | 世帆 |

---

## 本地端啟動指南

### 1. 環境準備

- JDK 21
- Maven 3.8+
- MS SQL Server
- Node.js 18+（前端）

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

在 `src/main/resources/` 建立 `application.properties`，填入自己的資料庫帳號密碼：

```properties
# Server
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
```

> ⚠️ `application.properties` 已加入 `.gitignore`，不會推上 GitHub，請勿將密碼推上遠端。

### 5. 初始化資料

執行 `src/main/resources/data.sql`（待補）

### 6. 啟動後端

用 IntelliJ 或 Eclipse 載入 Maven 專案後啟動，或在終端機執行：

```bash
./mvnw spring-boot:run
```

啟動成功後 console 會顯示：
```
Started JavaEasyBankApplication in X.XX seconds
```

### 7. 啟動前端（待補）

```bash
cd frontend
npm install
npm run dev
```

前端預設跑在 `http://localhost:5173`

---

## 專案結構

```
src/main/java/com/javaeasybank/
├── common/              # 共用元件（組長負責，禁止組員修改）
│   ├── config/          # SecurityConfig、CorsConfig
│   ├── dto/response/    # ApiResponse、PageResponse
│   ├── entity/          # BaseEntity
│   ├── exception/       # BusinessException、GlobalExceptionHandler
│   └── util/            # SessionUtil
├── auth/                # 系統與權限管理
├── customer/            # 顧客管理
├── account/             # 帳務監控
├── loan/                # 授信放款
├── creditcard/          # 信用卡業務
└── insurance/           # 保險業務
```

---

## 開發規範

詳見 Notion 開發指南（內部文件）

- Git branch 命名：`feature/{模組}-{功能}`、`fix/{模組}-{問題}`
- Commit 格式：`feat account：新增轉帳 API`
- 所有 API 回傳統一使用 `ApiResponse<T>` 格式
- 業務邏輯錯誤統一 `throw new BusinessException("說明")`
- 金額欄位一律使用 `BigDecimal` / `DECIMAL(15,2)`

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
