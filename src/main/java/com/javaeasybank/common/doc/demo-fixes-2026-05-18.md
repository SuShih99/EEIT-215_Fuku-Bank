# Demo 展示修復記錄 — 2026-05-18

## 一、前端改動

### 1. 客戶註冊頁（`frontend/src/views/user/RegisterView.vue`）

**問題**：「一鍵帶入測試帳號並註冊」按鈕同時執行填表＋送出，導致無法在送出前手動修改 email 欄位。

**修改內容**：
- 按鈕文字：`一鍵帶入測試帳號並註冊` → `一鍵帶入測試帳號`
- 函式 `fillDemoAndRegister()` 改名為 `fillDemoAccount()`
- 填表時跳過 `email` 欄位（留空讓使用者自行輸入真實信箱）
- 移除 `await handleRegister()`，改為只填表單，不自動送出

```js
// 改後邏輯
async function fillDemoAccount() {
  const { email: _email, ...rest } = account  // 排除 email
  Object.assign(form, rest)
  form.email = ''                             // email 留空
  await nextTick()
  // 不再呼叫 handleRegister()
}
```

---

### 2. 客戶登入頁（`frontend/src/views/user/UserLoginView.vue`）

**問題**：「一鍵帶入測試帳號並登入」自動登入，無法展示手動按登入的步驟。

**修改內容**：
- 「一鍵帶入測試帳號並登入」→ `一鍵帶入測試帳號`（只填表，不登入）
- `fillTestAccount()` 移除 `await handleLogin(true)`
- 「一般登入」按鈕文字改為 `登入示範帳號`（維持自動登入行為不變）

---

### 3. 管理員登入（取消擴充，維持原版）

原本規劃將快速帶入帳號擴充至全部員工，經確認後取消，維持原本 3 個帳號的版本。

---

## 二、後端資料庫 Seed 改動

### `src/main/resources/database/auth_insert.sql`

**問題**：E26006 吳建國（SUSPENDED）、E26010 蔡宗翰（LOCKED）無法登入，
Demo 時需由鄭文華示範手動停權，所以需要這兩個帳號先處於正常狀態。

**修改內容**：

| emp_id | 姓名   | 改前狀態    | 改後狀態 | permission_expire 改後 |
|--------|--------|------------|----------|------------------------|
| E26006 | 吳建國 | SUSPENDED  | ACTIVE   | 2026-12-31             |
| E26010 | 蔡宗翰 | LOCKED     | ACTIVE   | 2026-12-31             |

> ⚠️ 此修改只影響資料庫重新初始化時的狀態。
> 若資料庫已在運行，需手動執行：
> ```sql
> UPDATE AUTH_EMP
> SET status = 'ACTIVE', permission_expire = '2026-12-31'
> WHERE emp_id IN ('E26006', 'E26010');
> ```

---

## 三、後端 Entity 改動

### `CustomerProfile.java`

**問題**：`customerProfileRepository.save(profile)` 在新建 entity 時，
Spring Data JPA 看到 ID 已設定，誤判為可能是 update，呼叫 `entityManager.merge()`。
Hibernate 6 + SQL Server JDBC 在此情況下 INSERT 後回傳 row count = `-1`，
Hibernate 期待收到 `1`，因此拋出 `ObjectOptimisticLockingFailureException`。

**修改內容**：

1. 加入 `@DynamicInsert` / `@DynamicUpdate`：只傳有值的欄位，減少與 DB DEFAULT 值的衝突
2. 實作 `Persistable<String>` 介面：強制 Spring Data JPA 走 `persist()` 而非 `merge()`

```java
@DynamicInsert
@DynamicUpdate
public class CustomerProfile implements Persistable<String> {

    @Transient
    private boolean isNew = true;  // 新建時為 true

    @Override
    public String getId() { return customerId; }

    @Override
    public boolean isNew() { return isNew; }

    @PostPersist
    @PostLoad
    void markNotNew() { this.isNew = false; }  // 儲存或載入後設為 false
}
```

---

## 四、後端例外處理改動

### `GlobalExceptionHandler.java`

**問題**：`ObjectOptimisticLockingFailureException`（INSERT row count 不符）
與 `OptimisticLockingFailureException`（業務層 UPDATE 衝突）被同一個 handler 攔截，
導致新會員「註冊失敗」時顯示「該案件已被他人搶先處理」，語意錯誤。

**修改內容**：新增獨立的 `ObjectOptimisticLockingFailureException` handler

```java
// DB 層 INSERT/UPDATE row count 不符（SQL Server + Hibernate 相容問題）→ 500
@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
public ResponseEntity<ApiResponse<Void>> handleObjectOptimisticLock(...) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.fail("資料寫入失敗，請稍後再試。若問題持續發生請聯繫系統管理員"));
}

// 業務層 UPDATE 衝突（真正的樂觀鎖衝突）→ 409
@ExceptionHandler(OptimisticLockingFailureException.class)
public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(...) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.fail("該案件已被他人搶先處理，請重新整理"));
}
```

---

## 五、後端設定檔改動

### `src/main/resources/application-local.properties`

#### 5-1. `ddl-auto` 模式調整

```properties
# 改前
spring.jpa.hibernate.ddl-auto=create

# 改後
spring.jpa.hibernate.ddl-auto=update
```

**原因**：`create` 模式會在每次啟動時 DROP 並重建所有資料表，
Hibernate 自動建出的 DDL 缺少 SQL Server 的 `DEFAULT GETDATE()` 等限制，
與手動維護的 `xxx_init.sql` 不一致，導致 INSERT 異常。

`update` 模式改為：只補缺少的欄位，不清表，保留手動建表的結構。

> ⚠️ 改為 `update` 後，若需重置資料庫必須手動執行 `xxx_drop.sql` → `xxx_init.sql`，
> 不能靠重啟 Spring Boot 自動清表。

#### 5-2. HikariCP 連線初始化（根本修法）

```properties
spring.datasource.hikari.connection-init-sql=SET NOCOUNT OFF
```

**原因**：SQL Server 在某些連線狀態下，`INSERT`/`UPDATE` 後不回傳 affected row count，
JDBC driver 回傳 `-1`，Hibernate 6 嚴格驗證 row count 導致拋出例外。

`SET NOCOUNT OFF` 是 SQL Server 的 T-SQL 指令，確保每次 DML 都回傳影響行數。
此設定透過 HikariCP 在每條新連線建立時執行，屬於全局修正，
**影響範圍**：所有 JPA `save()` 的 INSERT / UPDATE 操作，包含貸款、信用卡、風控等模組。

---

## 六、Email 發送限制說明（非程式 bug）

```
550 5.4.5 Daily user sending limit exceeded
```

**原因**：系統使用的 Gmail 帳號（`programmereeit@gmail.com`）當日寄件量達到上限（免費帳號每日 500 封）。
多次測試觸發驗證信、登入通知、失敗警告等，累積超過上限。

**影響**：`EmailService.sendEmail()` 是 `@Async` + `try-catch`，
Email 失敗**只印 log，不影響 API 回傳、不觸發 rollback**。

**解決方式**：
1. 等待 Gmail 配額在 UTC 午夜重置（約台灣時間每天早上 8 點）
2. 換一組 Gmail 應用程式密碼（Google 帳號 → 安全性 → 應用程式密碼）

---

## 七、RBAC 角色說明（給各組同學參考）

### 系統認證機制

本系統管理員端使用 **Spring Session（Server-side Session）+ Cookie**，非 JWT。
登入後 Cookie 自動攜帶 `JSESSIONID`，後端自動識別身份。

客戶端使用 **JWT Token**，每次 API 請求需在 Header 帶 `Authorization: Bearer <token>`。

### 管理員角色對應表

| role_code | perm_level | 姓名   | email                           | 主要權限 |
|-----------|-----------|--------|---------------------------------|---------|
| CFSO      | 0         | 林家豪  | chiahao.lin@javabank.com        | 基本業務操作 |
| CFDM      | 2         | 王淑芬  | shufen.wang@javabank.com        | 消金部主管 |
| CSVO      | 1         | 陳建志  | chienchih.chen@javabank.com     | 客服查詢 |
| CSDM      | 2         | 張雅婷  | yating.chang@javabank.com       | 客服部主管 |
| JCRO      | 1         | 劉冠宇  | kuanyu.liu@javabank.com         | 初階授信審查 |
| CRDM      | 2         | 吳建國  | chienkuo.wu@javabank.com        | 授信部主管 |
| CRO       | 3         | 李志明  | chihming.lee@javabank.com       | 風控長 |
| OPS_PA    | 3         | 趙宇軒  | yuhsuan.chao@javabank.com       | 營運企劃 |
| COO       | 4         | 黃志成  | chihcheng.huang@javabank.com    | 營運長 |
| ISSA      | 3         | 蔡宗翰  | tsunghan.tsai@javabank.com      | 資安分析師 |
| CISO      | 4         | 鄭文華  | wenhua.cheng@javabank.com       | 資安長（最高權限） |

> 所有帳號密碼皆為 `123456`

### 各模組建議使用帳號

| 模組     | 建議帳號                     | 理由 |
|----------|------------------------------|------|
| 信用卡   | 林家豪（CFSO）或 王淑芬（CFDM） | 消費金融部 |
| 貸款     | 劉冠宇（JCRO）或 李志明（CRO）  | 授信審查部 |
| 風控     | 李志明（CRO）或 蔡宗翰（ISSA）  | 風控 / 資安 |
| 系統日誌 | 鄭文華（CISO）                 | permLevel 4 才能停用員工 |

### API 存取控制（`@PreAuthorize`）

```
hasAnyRole('CISO', 'ISSA')  → 員工管理（查詢/新增/修改/停用）
hasAnyRole('CISO', 'ISSA')  → 停用員工（系統日誌點選停權）
已登入即可               → 系統日誌查詢（停權按鈕僅 CISO 可見）
已登入即可               → 客戶列表查詢
```

不符合角色打 API 會收到 **HTTP 403 Forbidden**。
