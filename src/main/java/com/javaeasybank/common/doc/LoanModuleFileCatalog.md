# 貸款相關檔案總整理

本文整理專案中所有貸款相關檔案，依「用途、對象、功能、方法、分類」歸納，方便快速理解貸款模組的前後端、資料表、帳務整合、風控整合與通知流程。

## 0. 本次核對結果

本文件已依目前專案內容重新掃描並補齊下列範圍：

1. 檔名直接包含 `Loan` / `loan` 的後端、前端、SQL、Email、文件檔。
2. 檔名不含 `Loan`，但內容實際參與貸款功能的橫切檔案，例如 `SecurityConfig`、前後台 Layout、帳戶交易 Enum、通存摺顯示邏輯。
3. `AccountIntegrationService` 目前版本新增的還款防呆方法 `isFullMultiPeriodRepayment`，已補入帳務整合方法清單。
4. 本整理檔以 `src/main/resources/database/loan_init.sql` 作為資料表主參考；`loan/database/LoanTable.sql` 保留為模組內建表腳本對照。

## 1. 模組總覽

貸款功能的核心生命週期如下：

1. 客戶在前台送出貸款申請。
2. 行員在後台新增聯繫紀錄，必要時請客戶補件。
3. 行員進行二次填單，儲存核准金額、期數、利率等審核資料。
4. 行員送審後，貸款模組呼叫風控模組。
5. 風控回調核准或拒絕。
6. 核准後貸款模組呼叫帳戶模組建立貸款帳戶並撥款。
7. 帳戶模組撥款完成後回調貸款模組建立 LoanAccount 與還款期數。
8. 客戶還款時由帳戶模組扣款，貸款模組同步更新還款期數。
9. 排程每日掃描逾期與到期提醒。

### 1.1 客戶端工作流程與行為

客戶端入口集中在前台「貸款服務」選單，主要行為分為申請、狀態追蹤、補件、帳戶查詢與還款。

| 階段 | 客戶行為 | 前端畫面 | 後端 API / 行為 |
|---|---|---|---|
| 進入貸款服務 | 從側邊選單進入「快速申請貸款」、「查詢申貸狀態」或「貸款帳戶」 | `UserLayout.vue` | 路由導向對應使用者頁面 |
| 查看方案與試算 | 選擇貸款類型、金額、期數，前端即時計算預估利率與月付金 | `LoanApplyView.vue` | `GET /api/loan-applications/rate-rules` 載入利率規則 |
| 選擇撥款帳戶 | 從自己名下正常台幣活存帳戶中選擇核准後要入帳的帳號 | `LoanApplyView.vue` | 前端載入客戶帳戶資料，只允許 TWD / CHECKING / ACTIVE 帳戶 |
| 送出申請 | 填妥貸款類型、金額、期數、利率、撥款帳號後送出 | `LoanApplyView.vue` | `POST /api/loan-applications/member`，建立 `PENDING_CONTACT` 申請並寄申請成立信 |
| 追蹤申請狀態 | 查看所有申請的狀態、最新聯繫結果、申請條件與核准條件 | `LoanStatusView.vue` | `GET /api/loan-applications/my` 只回傳登入客戶自己的申請 |
| 上傳補件 | 當申請需要補件時，上傳身分證、收入證明、在職證明等文件 | `LoanStatusView.vue` | `POST /api/loan-documents/{applicationId}/upload`，最多 5 份，送出前可刪除 |
| 正式送出補件 | 確認文件齊全後送出，送出後不可自行刪改 | `LoanStatusView.vue` | `POST /api/loan-documents/{applicationId}/submit`，寫入 `documentsSubmittedAt` 並通知風控 |
| 查看貸款帳戶 | 撥款完成後查看本金、利率、月付金、已繳期數、下次繳款日 | `LoanAccountView.vue` | `GET /api/loan-accounts/my`，只查本人貸款帳戶 |
| 查看還款明細 | 開啟單一貸款帳戶的完整攤還表 | `LoanAccountView.vue` | `GET /api/loan-accounts/{accountId}/repayments`，含所有權驗證 |
| 執行還款 | 選擇貸款帳戶與扣款帳戶，填入本期或連續多期應繳金額後還款 | `LoanRepaymentView.vue` | `POST /api/customer/loan-repayments`，帳務扣款後同步貸款期數 |
| 查詢還款紀錄 | 查看貸款還款交易紀錄與參考號碼 | `LoanRepaymentView.vue` | `GET /api/customer/loan-repayments`，依客戶與貸款帳號篩選交易紀錄 |

客戶端行為限制：

1. 客戶端所有個人資料 API 都透過 JWT 解析 `customerId`，避免查到他人申請、文件或帳戶。
2. 補件文件只能由申請本人上傳、查詢與刪除；正式送出後不可再刪除。
3. 貸款帳戶與還款明細都會驗證帳戶的 `customerId` 是否等於登入者。
4. 還款扣款帳戶只能使用本人名下正常的台幣活存帳戶。
5. 還款金額需符合本期或連續多期應繳總額，最後一期可依剩餘本金清償。

### 1.2 行員端工作流程與行為

行員端入口集中在後台「消金貸款業務」選單，主要行為分為申請管理、聯繫、補件檢視、二次填單、送審、撥款補償與貸款帳戶管理。

| 階段 | 行員行為 | 前端畫面 | 後端 API / 行為 |
|---|---|---|---|
| 進入貸款後台 | 從後台選單進入「貸款申請管理」或「貸款帳戶管理」 | `AdminLayout.vue` | 路由導向後台貸款頁面 |
| 查詢申請列表 | 依申請狀態切換列表，並可用貸款類型、申請人姓名、欄位排序與分頁篩選 | `LoanApplicationView.vue` | `GET /api/admin/loan-applications?status=...`，預設查 `PENDING_CONTACT` |
| 查看最新動態 | 定期刷新列表，掌握風控或帳戶模組回調後的狀態變化 | `LoanApplicationView.vue` | 前端每 30 秒刷新；後端也提供 `GET /recent-updates` |
| 新增聯繫紀錄 | 記錄電話、Email、SMS 聯繫結果與備註 | `LoanContactLogModal.vue` | `POST /api/admin/loan-applications/{id}/contact-logs`，同步更新主表最新聯繫狀態 |
| 查詢聯繫紀錄 | 查看某筆申請所有聯繫歷程 | `LoanContactLogModal.vue` | `GET /api/admin/loan-applications/{id}/contact-logs` |
| 查看補件文件 | 開啟補件視窗查看客戶已正式送出的文件 | `LoanDocumentModal.vue` | `GET /api/admin/loan-documents/{applicationId}`，未送出時回空清單 |
| 二次填單 | 根據聯繫與審核結果填入核准金額、期數、利率、擔保備註 | `LoanReviewModal.vue` | `POST /api/admin/loan-applications/{id}/review`，儲存 `DRAFT` |
| 正式送審 | 將二次填單送往風控模組 | `LoanReviewModal.vue` | `PATCH /api/admin/loan-applications/{id}/review/submit`，狀態轉 `PENDING_REVIEW`，afterCommit 呼叫風控 |
| 重送風控 | 當狀態停在 `PENDING_REVIEW` 且風控未收到時手動補償 | `LoanApplicationView.vue` / API | `PATCH /api/admin/loan-applications/{id}/risk/retry` |
| 重試撥款 | 當風控核准後狀態停在 `APPROVED` 時手動重新觸發撥款 | `LoanApplicationView.vue` / API | `PATCH /api/admin/loan-applications/{id}/disburse/retry` |
| 查貸款帳戶 | 查看全部貸款帳戶，依帳戶狀態或貸款類型篩選 | `LoanAccountAdminView.vue` | `GET /api/admin/loan-accounts` |
| 查還款時間表 | 查看指定帳戶的所有期數、應繳日、本金、利息與狀態 | `LoanAccountAdminView.vue` | `GET /api/admin/loan-accounts/{accountId}/repayments` |
| 手動同步已繳 | 當帳務已扣款但貸款期數需補同步時，手動標記目前期數已繳 | 後台 API | `POST /api/admin/loan-accounts/{accountId}/repayments/sync-paid` |

行員端行為限制與權限重點：

1. 後台貸款頁面由管理端登入與路由權限控管。
2. 審核填單按鈕在前端依角色與 `permLevel` 控制，需主管等級以上才可操作。
3. 聯繫紀錄採 append-only 思路，新增後保留歷程並更新申請主表快照欄位。
4. 二次填單可先儲存草稿，正式送審後狀態變為 `SUBMITTED`，再推進申請到 `PENDING_REVIEW`。
5. 風控與撥款重試屬補償操作，只適合處理外部服務未收到或 afterCommit 失敗等異常情境。
6. 行員端查文件不做客戶所有權限制，但 `LoanDocumentService` 會在客戶尚未正式送出補件時回傳空清單。

### 1.3 系統自動行為

| 觸發 | 系統行為 | 涉及檔案 |
|---|---|---|
| 客戶送出申請 | 建立申請、初始狀態 `PENDING_CONTACT`、寄申請成立信 | `LoanApplicationService`, `EmailService` |
| 行員送審 | 交易提交後呼叫風控，避免 DB 回滾但外部副作用已發生 | `LoanApplicationService`, `LoanRiskClient` |
| 風控核准 | 狀態轉 `APPROVED`，afterCommit 觸發自動撥款 | `LoanApplicationService` |
| 帳戶撥款完成 | 帳戶模組 afterCommit 回調貸款模組，建立貸款帳戶與還款期數 | `AccountIntegrationService`, `LoanApplicationService`, `LoanAccountService`, `LoanRepaymentService` |
| 客戶還款成功 | 帳務扣款、貸款負債下降、交易紀錄寫入、貸款期數標記已繳 | `AccountIntegrationService`, `LoanRepaymentService` |
| 全數還清 | 貸款帳戶轉 `PAID_OFF`，申請轉 `CLOSED`，寄結清信 | `LoanRepaymentService`, `EmailService` |
| 每日 01:00 | 掃描逾期與 1-3 天內到期提醒，寄通知信 | `LoanRepaymentScheduler`, `EmailService` |

核心資料對象：

| 對象 | 對應 Entity / DTO | 說明 |
|---|---|---|
| 貸款申請 | `LoanApplication` | 客戶申請主資料，含申請類型、金額、期數、利率、狀態、撥款帳號 |
| 聯繫紀錄 | `LoanContactLog` | 行員聯繫客戶的 append-only 紀錄 |
| 二次填單 | `LoanReviewDetail` | 行員審核後確認的金額、期數、利率、擔保備註、送審狀態 |
| 貸款帳戶 | `LoanAccount` | 撥款完成後建立的貸款模組帳戶資料 |
| 還款期數 | `LoanRepayment` | 每一期應繳日、本金、利息、剩餘本金、還款狀態 |
| 補件文件 | `LoanDocument` | 客戶上傳的貸款補件檔案後設資料 |
| 帳務貸款帳戶 | `account.entity.Account` with `AccountType.LOAN` | 帳戶模組中的實際負債帳戶，用於撥款、還款與交易紀錄 |

## 2. 檔案分類總表

### 2.1 後端貸款核心

| 分類 | 檔案 | 對象 | 主要用途 |
|---|---|---|---|
| Controller | `loan/controller/LoanApplicationController.java` | 客戶 | 申請貸款、查詢自己的申請、取得利率規則 |
| Controller | `loan/controller/LoanAdminController.java` | 行員 | 查申請、聯繫紀錄、二次填單、送審、補償重送 |
| Controller | `loan/controller/LoanAccountController.java` | 客戶 | 查自己的貸款帳戶與還款明細 |
| Controller | `loan/controller/LoanAccountAdminController.java` | 行員 | 查全部貸款帳戶、查還款明細、手動同步繳款 |
| Controller | `loan/controller/LoanDocumentController.java` | 客戶 / 行員 | 補件上傳、送出、刪除、查詢、文件類型 |
| Controller | `loan/controller/LoanCallbackController.java` | 外部模組 | 接收風控與帳戶模組狀態回調 |
| Service | `loan/service/LoanApplicationService.java` | 貸款申請 | 申請生命週期核心流程 |
| Service | `loan/service/LoanAccountService.java` | 貸款帳戶 | 撥款後建帳、查詢帳戶 |
| Service | `loan/service/LoanRepaymentService.java` | 還款期數 | 建立攤還表、同步已繳、結清 |
| Service | `loan/service/LoanDocumentService.java` | 補件文件 | 文件上傳、送出、刪除、查詢 |
| Client | `loan/client/LoanRiskClient.java` | 風控 | 呼叫風控送審與補件通知 |
| Scheduler | `loan/scheduler/LoanRepaymentScheduler.java` | 還款排程 | 每日逾期掃描與到期提醒 |
| Utils | `loan/utils/AmortizationCalculator.java` | 攤還計算 | 等額本息月付金與攤還表 |

### 2.2 後端資料層

| 分類 | 檔案 | 對象 | 主要用途 |
|---|---|---|---|
| Entity | `loan/entity/LoanApplication.java` | 申請主表 | 對應 `loan_application` |
| Entity | `loan/entity/LoanContactLog.java` | 聯繫紀錄 | 對應 `loan_contact_log` |
| Entity | `loan/entity/LoanReviewDetail.java` | 二次填單 | 對應 `loan_review_detail` |
| Entity | `loan/entity/LoanAccount.java` | 貸款帳戶 | 對應 `loan_account` |
| Entity | `loan/entity/LoanRepayment.java` | 還款期數 | 對應 `loan_repayment` |
| Entity | `loan/entity/LoanDocument.java` | 補件文件 | 對應 `loan_document` |
| Repository | `loan/repository/LoanApplicationRepository.java` | 申請 | 依狀態、客戶、更新時間查詢 |
| Repository | `loan/repository/LoanContactLogRepository.java` | 聯繫紀錄 | 依申請查詢聯繫歷程 |
| Repository | `loan/repository/LoanReviewDetailRepository.java` | 二次填單 | 依申請查詢審核資料 |
| Repository | `loan/repository/LoanAccountRepository.java` | 貸款帳戶 | 依申請、帳號、客戶、狀態查詢 |
| Repository | `loan/repository/LoanRepaymentRepository.java` | 還款期數 | 依帳戶、狀態、日期查詢 |
| Repository | `loan/repository/LoanDocumentRepository.java` | 文件 | 依申請查文件、計算文件數量 |

### 2.3 DTO 與 Enum

| 分類 | 檔案 | 用途 |
|---|---|---|
| Request DTO | `LoanMemberRequestDTO.java` | 客戶送出申請資料 |
| Request DTO | `LoanContactLogRequestDTO.java` | 行員新增聯繫紀錄 |
| Request DTO | `LoanReviewDetailRequestDTO.java` | 行員二次填單 |
| Request DTO | `LoanRiskRequestDTO.java` | 貸款送風控審核資料 |
| Request DTO | `LoanStatusCallbackRequestDTO.java` | 風控 / 帳戶回調貸款狀態 |
| Request DTO | `LoanDocumentInfoDTO.java` | 補件通知風控的文件摘要 |
| Response DTO | `LoanApplicationResponseDTO.java` | 回傳申請清單與詳情 |
| Response DTO | `LoanContactLogResponseDTO.java` | 回傳聯繫紀錄 |
| Response DTO | `LoanReviewDetailResponseDTO.java` | 回傳二次填單 |
| Response DTO | `LoanAccountResponseDTO.java` | 回傳貸款帳戶 |
| Response DTO | `LoanRepaymentResponseDTO.java` | 回傳還款期數 |
| Response DTO | `LoanDocumentResponseDTO.java` | 回傳補件文件 |
| Enum | `LoanApplicationStatus.java` | 申請狀態 |
| Enum | `LoanAccountStatus.java` | 貸款帳戶狀態 |
| Enum | `LoanRepaymentStatus.java` | 還款期數狀態 |
| Enum | `LoanContactStatus.java` | 聯繫結果 |
| Enum | `LoanContactChannel.java` | 聯繫管道 |
| Enum | `LoanDocumentType.java` | 文件類型 |
| Enum | `LoanReviewStatus.java` | 二次填單狀態 |

### 2.4 帳戶整合相關

| 分類 | 檔案 | 用途 |
|---|---|---|
| Service | `account/service/AccountIntegrationService.java` | 建立貸款帳務帳戶、撥款、還款、查還款紀錄 |
| Controller | `account/controller/AccountIntegrationController.java` | 客戶端還款 API |
| Request DTO | `account/dto/request/LoanAccountCreateRequest.java` | 建立帳務貸款帳戶 |
| Request DTO | `account/dto/request/LoanDisbursementRequest.java` | 貸款撥款 |
| Request DTO | `account/dto/request/LoanInterestRequest.java` | 貸款利息入帳 |
| Request DTO | `account/dto/request/LoanRepaymentRequest.java` | 客戶還款 |
| Response DTO | `account/dto/response/LoanAccountResponse.java` | 帳務貸款帳戶回應 |
| Response DTO | `account/dto/response/LoanAccountTransactionResponse.java` | 撥款 / 還款交易結果 |
| Account Entity | `account/entity/Account.java` | 一般帳戶 12 碼；貸款 / 信用卡專用帳戶可為 14 碼 |
| Account Enum | `account/enums/AccountType.java` | 定義 `LOAN` 帳戶類型 |
| Account Enum | `account/enums/TransactionType.java` | 定義 `LOAN_DISBURSEMENT`、`LOAN_REPAYMENT` 與 `INTEREST` 交易類型 |
| Account Enum | `account/enums/AccountPurpose.java` | 定義開戶用途 `LOAN` |
| PDF Service | `account/service/PassbookPdfService.java` | 通存摺 PDF 顯示 `LOAN` 為「貸款帳戶」 |

### 2.5 前端畫面

| 分類 | 檔案 | 對象 | 主要用途 |
|---|---|---|---|
| User View | `frontend/src/views/user/LoanApplyView.vue` | 客戶 | 申請貸款、利率試算、入帳帳戶選擇 |
| User View | `frontend/src/views/user/LoanStatusView.vue` | 客戶 | 查申請狀態、上傳補件、送出補件 |
| User View | `frontend/src/views/user/LoanAccountView.vue` | 客戶 | 查貸款帳戶與攤還明細 |
| User View | `frontend/src/views/user/LoanRepaymentView.vue` | 客戶 | 還款、選扣款帳戶、查還款紀錄 |
| Admin View | `frontend/src/views/admin/LoanApplicationView.vue` | 行員 | 申請列表、篩選、排序、開啟聯繫 / 審核 / 補件 modal |
| Admin View | `frontend/src/views/admin/LoanContactLogModal.vue` | 行員 | 新增與查詢聯繫紀錄 |
| Admin View | `frontend/src/views/admin/LoanReviewModal.vue` | 行員 | 二次填單、儲存草稿、正式送審 |
| Admin View | `frontend/src/views/admin/LoanDocumentModal.vue` | 行員 | 查閱客戶已送出的補件文件 |
| Admin View | `frontend/src/views/admin/LoanAccountAdminView.vue` | 行員 | 查貸款帳戶、查還款明細 |
| Router | `frontend/src/router/index.js` | 前端路由 | 定義客戶與後台貸款路由 |
| User Layout | `frontend/src/layouts/UserLayout.vue` | 客戶 | 側邊選單「貸款服務」入口 |
| Admin Layout | `frontend/src/layouts/AdminLayout.vue` | 行員 | 後台選單「消金貸款業務」入口 |

### 2.6 資源與資料庫

| 分類 | 檔案 | 用途 |
|---|---|---|
| SQL | `src/main/resources/database/loan_init.sql` | 建立貸款資料表 |
| SQL | `src/main/resources/database/loan_mockdata.sql` | 貸款測試資料 |
| SQL | `src/main/java/com/javaeasybank/loan/database/LoanTable.sql` | 舊版 / 模組內貸款建表腳本 |
| Mail Template | `templates/mail/loan-applied.html` | 申請成立通知 |
| Mail Template | `templates/mail/loan-document-required.html` | 補件通知 |
| Mail Template | `templates/mail/loan-rejected.html` | 退件通知 |
| Mail Template | `templates/mail/loan-disbursed.html` | 核准撥款通知 |
| Mail Template | `templates/mail/loan-repayment-paid.html` | 還款成功通知 |
| Mail Template | `templates/mail/loan-paid-off.html` | 貸款結清通知 |
| Mail Template | `templates/mail/loan-overdue.html` | 逾期通知 |
| Mail Template | `templates/mail/loan-repayment-reminder.html` | 到期提醒 |
| HTTP | `loan/utils/test.http` | 手動測試 API |
| Doc | `common/doc/LoanAccountIntegrationGuide.md` | 貸款與帳戶整合說明，但目前檔案內容在終端顯示有編碼亂碼 |

### 2.7 權限與橫切設定

| 分類 | 檔案 | 用途 |
|---|---|---|
| Security | `common/config/SecurityConfig.java` | 開放 `/api/loan-applications/rate-rules`；限制 `/api/loan-callbacks/**` 僅本機 IP 可呼叫 |
| Static Upload | `common/service/FileStorageService.java` | 被 `LoanDocumentService` 用於儲存與刪除補件檔案 |
| Email | `common/service/EmailService.java` | 提供貸款申請、補件、核准撥款、還款、結清、逾期與提醒通知 |

## 3. Controller 端點與方法

### 3.1 `LoanApplicationController`

Base URL: `/api/loan-applications`

| 方法 | HTTP | Path | 對象 | 功能 |
|---|---|---|---|---|
| `applyMember` | `POST` | `/member` | 客戶 | 從 JWT 取得 `customerId`，建立貸款申請，回傳 `applicationId` |
| `getMyApplications` | `GET` | `/my` | 客戶 | 查詢目前登入客戶的所有貸款申請 |
| `getRateRules` | `GET` | `/rate-rules` | 公開 | 取得各貸款類型利率與期數規則 |
| `extractCustomerId` | private | - | 內部 | 從 Authorization Bearer token 解析 customerId |

### 3.2 `LoanAdminController`

Base URL: `/api/admin/loan-applications`

| 方法 | HTTP | Path | 對象 | 功能 |
|---|---|---|---|---|
| `getByStatus` | `GET` | `/` | 行員 | 依申請狀態查詢，預設 `PENDING_CONTACT` |
| `addContactLog` | `POST` | `/{id}/contact-logs` | 行員 | 新增聯繫紀錄並更新申請最新聯繫狀態 |
| `getContactLogs` | `GET` | `/{id}/contact-logs` | 行員 | 查詢指定申請聯繫紀錄 |
| `saveReview` | `POST` | `/{id}/review` | 行員 | 儲存二次填單草稿 |
| `submitReview` | `PATCH` | `/{id}/review/submit` | 行員 | 正式送審並觸發風控流程 |
| `getReview` | `GET` | `/{id}/review` | 行員 | 查詢二次填單 |
| `getRecentlyUpdated` | `GET` | `/recent-updates` | 行員 | 查最近被外部模組異動的申請 |
| `retryRiskSubmit` | `PATCH` | `/{id}/risk/retry` | 行員 | 重新送風控 |
| `retryDisburse` | `PATCH` | `/{id}/disburse/retry` | 行員 | 重新觸發撥款 |

### 3.3 `LoanAccountController`

Base URL: `/api/loan-accounts`

| 方法 | HTTP | Path | 對象 | 功能 |
|---|---|---|---|---|
| `getMyAccounts` | `GET` | `/my` | 客戶 | 查詢自己的貸款帳戶 |
| `getByApplicationId` | `GET` | `/application/{applicationId}` | 客戶 | 依申請查貸款帳戶，並驗證所有權 |
| `getRepayments` | `GET` | `/{accountId}/repayments` | 客戶 | 查指定貸款帳戶的還款期數，並驗證所有權 |
| `extractCustomerId` | private | - | 內部 | 從 JWT 解析 customerId |

### 3.4 `LoanAccountAdminController`

Base URL: `/api/admin/loan-accounts`

| 方法 | HTTP | Path | 對象 | 功能 |
|---|---|---|---|---|
| `getAllAccounts` | `GET` | `/` | 行員 | 查全部貸款帳戶，可用 `status` 篩選 |
| `getByApplicationId` | `GET` | `/application/{applicationId}` | 行員 | 依申請查貸款帳戶 |
| `getRepayments` | `GET` | `/{accountId}/repayments` | 行員 | 查還款期數 |
| `syncPaidRepayment` | `POST` | `/{accountId}/repayments/sync-paid` | 行員 | 手動同步已繳狀態 |

### 3.5 `LoanDocumentController`

此 Controller 沒有 class-level base path，各方法直接宣告完整 path。

| 方法 | HTTP | Path | 對象 | 功能 |
|---|---|---|---|---|
| `upload` | `POST` | `/api/loan-documents/{applicationId}/upload` | 客戶 | 上傳補件，multipart form-data |
| `submitDocs` | `POST` | `/api/loan-documents/{applicationId}/submit` | 客戶 | 正式送出補件，寫入 `documentsSubmittedAt` |
| `deleteDoc` | `DELETE` | `/api/loan-documents/{documentId}` | 客戶 | 刪除自己上傳且尚未送出的文件 |
| `getMyDocs` | `GET` | `/api/loan-documents/{applicationId}` | 客戶 | 查自己申請的文件 |
| `getAdminDocs` | `GET` | `/api/admin/loan-documents/{applicationId}` | 行員 | 查客戶已送出的文件 |
| `getDocumentTypes` | `GET` | `/api/loan-documents/types` | 共用 | 回傳文件類型中文名稱 |
| `extractCustomerId` | private | - | 內部 | 從 JWT 解析 customerId |

### 3.6 `LoanCallbackController`

Base URL: `/api/loan-callbacks`

| 方法 | HTTP | Path | 對象 | 功能 |
|---|---|---|---|---|
| `handleStatusCallback` | `POST` | `/{applicationId}/status` | 風控 / 帳戶 | 接收狀態回調，交給 `LoanApplicationService.handleStatusCallback` |

## 4. Service 方法整理

### 4.1 `LoanApplicationService`

用途：貸款申請生命週期核心，負責申請、聯繫、二次填單、風控送審、回調、撥款補償、利率規則與 DTO 轉換。

| 方法 | 類型 | 功能 |
|---|---|---|
| `getByStatus(LoanApplicationStatus status)` | public | 行員依狀態查申請 |
| `getMyApplications(String customerId)` | public | 客戶查自己的申請 |
| `insertMember(String customerId, LoanMemberRequestDTO dto)` | public | 客戶建立申請，初始狀態 `PENDING_CONTACT`，並寄申請成立信 |
| `generateId(String prefix)` | private | 產生 `LA` / `CL` / `RD` 等 ID |
| `buildBaseLoan()` | private | 建立基礎 `LoanApplication` |
| `fillLoanContent(...)` | private | 填入貸款類型、金額、期數、利率 |
| `addContactLog(String applicationId, LoanContactLogRequestDTO dto)` | public | 新增聯繫紀錄 |
| `insertContactLog(...)` | private | 用 `JdbcTemplate` 寫入聯繫紀錄 |
| `updateLoanContactState(...)` | private | 更新申請主表最新聯繫狀態與申請狀態 |
| `getContactLogs(String applicationId)` | public | 查聯繫紀錄 |
| `saveReviewDetail(String applicationId, LoanReviewDetailRequestDTO dto)` | public | 儲存或更新二次填單草稿 |
| `insertReviewDetail(...)` | private | 新增二次填單 |
| `updateReviewDetail(...)` | private | 更新二次填單 |
| `submitReview(String applicationId)` | public | 二次填單正式送審，狀態推進到 `PENDING_REVIEW` 並 afterCommit 呼叫風控 |
| `buildRiskRequest(LoanApplication loan, LoanReviewDetail detail)` | private | 組合風控送審 DTO |
| `handleStatusCallback(String applicationId, LoanStatusCallbackRequestDTO dto)` | public | 處理 RISK / ACCOUNT 回調 |
| `handleAccountDisbursedCallback(String applicationId)` | public | 帳戶撥款完成回調，不帶帳號版本 |
| `handleAccountDisbursedCallback(String applicationId, String loanAccountNumber)` | public | 帳戶撥款完成回調，將申請轉 `DISBURSED` 並建立貸款帳戶 |
| `autoDisburse(String applicationId)` | public | 核准後自動建帳與撥款，使用 `NOT_SUPPORTED` 避免外層事務鎖定 |
| `retryDisburse(String applicationId)` | public | 申請卡在 `APPROVED` 時手動重跑撥款 |
| `retryRiskSubmit(String applicationId)` | public | 申請卡在 `PENDING_REVIEW` 時手動重送風控 |
| `getReviewDetail(String applicationId)` | public | 查二次填單 |
| `getRecentlyUpdated()` | public | 查有 `updateTime` 的申請 |
| `getRateRules()` | public | 回傳貸款類型、基準利率、期數、期數加碼等規則 |
| `toResponseDTO(LoanApplication loan)` | private | 申請 Entity 轉 Response DTO |
| `toContactLogResponseDTO(LoanContactLog log)` | private | 聯繫紀錄 Entity 轉 DTO |
| `toReviewDetailResponseDTO(LoanReviewDetail detail)` | private | 二次填單 Entity 轉 DTO |

重要狀態推進：

| 觸發 | 狀態變化 |
|---|---|
| 客戶申請 | `PENDING_CONTACT` |
| 行員聯繫中 | `IN_CONTACT` |
| 行員確認聯繫並送審 | `PENDING_REVIEW` |
| 風控核准 | `APPROVED` |
| 風控拒絕 | `REJECTED` |
| 撥款完成 | `DISBURSED` |
| 全數還清 | `CLOSED` |
| 客戶婉拒 | `CANCELLED` |

### 4.2 `LoanAccountService`

用途：撥款完成後建立貸款帳戶與查詢貸款帳戶。

| 方法 | 類型 | 功能 |
|---|---|---|
| `createOnDisbursement(String applicationId)` | public | 撥款完成後建帳，不帶帳戶模組貸款帳號 |
| `createOnDisbursement(String applicationId, String loanAccountNumber)` | public | 撥款完成後建帳，含帳戶模組貸款帳號，並建立還款排程 |
| `getMyAccounts(String customerId)` | public | 客戶查自己的貸款帳戶 |
| `getAllAccounts(LoanAccountStatus status)` | public | 行員查全部貸款帳戶，可依狀態篩選 |
| `getByApplicationId(String applicationId)` | public | 依申請查貸款帳戶 |
| `getAccountById(String accountId)` | public | 依貸款帳戶 ID 查單筆 |
| `generateId(String prefix)` | private | 產生 `LAC` ID |
| `toResponseDTO(LoanAccount account)` | private | Entity 轉 DTO，並補 `cif` |

### 4.3 `LoanRepaymentService`

用途：建立還款時間表、同步已繳款、更新帳戶進度與結清。

| 方法 | 類型 | 功能 |
|---|---|---|
| `createSchedule(LoanAccount account)` | public | 依攤還表預建所有還款期數 |
| `processRepayment(String applicationId)` | public | 依申請處理單期繳款 |
| `processRepayments(String applicationId, int periodsToPay)` | public | 依申請批次處理多期繳款 |
| `processRepaymentByAccountId(String accountId)` | public | 行員手動同步指定帳戶已繳期數 |
| `validateAccountingAlreadyDeducted(LoanAccount account)` | private | 驗證帳務模組負債已扣到正確餘額 |
| `processRepayments(LoanAccount account, int periodsToPay)` | private | 核心繳款同步邏輯 |
| `getPendingRepayments(LoanAccount account)` | private | 查 `SCHEDULED` / `OVERDUE` 待繳期數 |
| `getCurrentPendingRepayment(LoanAccount account)` | private | 查目前應繳期 |
| `getCurrentPendingRepayment(LoanAccount account, List<LoanRepayment> pending)` | private | 從已查出的待繳清單找目前應繳期 |
| `getByAccountId(String accountId)` | public | 查完整還款時間表 |
| `closeApplication(LoanAccount account)` | private | 全數還清後將申請狀態改 `CLOSED` 並寄結清信 |
| `toResponseDTO(LoanRepayment rp)` | private | Entity 轉 DTO |
| `generateId(String prefix)` | private | 產生 `RP` ID |

### 4.4 `LoanDocumentService`

用途：客戶補件文件生命週期。

| 方法 | 類型 | 功能 |
|---|---|---|
| `upload(String applicationId, String customerId, String documentType, MultipartFile file)` | public | 驗證所有權、未送出、數量上限、文件類型後儲存文件 |
| `submitDocuments(String applicationId, String customerId)` | public | 客戶正式送出補件，寫 `documentsSubmittedAt`，best-effort 通知風控 |
| `delete(String documentId, String customerId)` | public | 補件送出前允許刪除自己上傳的文件 |
| `getByApplicationId(String applicationId, String customerId)` | public | 客戶查自己的文件 |
| `getByApplicationId(String applicationId)` | public | 行員查文件，客戶尚未送出時回空清單 |
| `generateId(String prefix)` | private | 產生 `DOC` ID |
| `toResponseDTO(LoanDocument doc)` | private | Entity 轉 DTO |

### 4.5 `LoanRiskClient`

用途：封裝貸款模組對風控模組的 HTTP 呼叫。

| 方法 | 類型 | 功能 |
|---|---|---|
| `LoanRiskClient(RestTemplate restTemplate)` | public constructor | 注入 RestTemplate |
| `attachDocuments(String businessId, List<LoanDocumentInfoDTO> documents)` | public | PATCH 補件資料到風控，失敗只記錄警告 |
| `submitForReview(LoanRiskRequestDTO dto)` | public | POST 送審到風控，失敗丟 `BusinessException` |
| `buildRiskUrl(String path)` | private | 組出風控 API URL |

### 4.6 `LoanRepaymentScheduler`

用途：每日 01:00 掃描還款期數。

| 方法 | 類型 | 功能 |
|---|---|---|
| `scanOverdueRepayments()` | public scheduled | 將逾期期數標記為 `OVERDUE`、更新帳戶狀態、寄逾期信，並寄 1-3 天內到期提醒信 |

### 4.7 `AmortizationCalculator`

用途：等額本息計算。

| 方法 | 類型 | 功能 |
|---|---|---|
| `AmortizationCalculator()` | private constructor | 工具類禁止實例化 |
| `calcMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int periods)` | public static | 計算每月固定應繳金額 |
| `buildSchedule(BigDecimal principal, BigDecimal annualRate, int periods, LocalDate firstPaymentDate)` | public static | 展開完整攤還表 |
| `RepaymentRow(...)` | record | 單期攤還資料，含期數、日期、總額、本金、利息、剩餘本金 |

## 5. Repository 方法整理

| Repository | 方法 | 功能 |
|---|---|---|
| `LoanApplicationRepository` | `findByApplicationStatusOrderByCreateTimeDesc` | 依申請狀態查詢 |
| `LoanApplicationRepository` | `findByCustomerIdOrderByCreateTimeDesc` | 查客戶自己的申請 |
| `LoanApplicationRepository` | `findByUpdateTimeIsNotNullOrderByUpdateTimeDesc` | 查最近更新申請 |
| `LoanContactLogRepository` | `findByApplicationIdOrderByContactTimeDesc` | 查聯繫紀錄 |
| `LoanReviewDetailRepository` | `findByApplicationId` | 查二次填單 |
| `LoanAccountRepository` | `findByApplicationId` | 依申請查貸款帳戶 |
| `LoanAccountRepository` | `findByAccountNumber` | 依帳務貸款帳號查貸款帳戶 |
| `LoanAccountRepository` | `findByCustomerIdOrderByCreateTimeDesc` | 查客戶貸款帳戶 |
| `LoanAccountRepository` | `findByAccountStatusOrderByCreateTimeDesc` | 依帳戶狀態查詢 |
| `LoanRepaymentRepository` | `findByAccountIdOrderByPeriodIndexAsc` | 查帳戶還款表 |
| `LoanRepaymentRepository` | `findByAccountIdAndRepaymentStatus` | 查指定狀態期數 |
| `LoanRepaymentRepository` | `findByAccountIdAndRepaymentStatusIn` | 查多狀態期數 |
| `LoanRepaymentRepository` | `findByAccountIdAndPeriodIndex` | 查指定期數 |
| `LoanRepaymentRepository` | `findByScheduledDateBeforeAndRepaymentStatus` | 排程查逾期 |
| `LoanRepaymentRepository` | `findByScheduledDateBetweenAndRepaymentStatus` | 排程查到期提醒 |
| `LoanDocumentRepository` | `findByApplicationIdOrderByUploadTimeAsc` | 查文件 |
| `LoanDocumentRepository` | `countByApplicationId` | 計算文件數量上限 |

## 6. Entity 與資料表欄位

### 6.1 `loan_application`

| 欄位 | 說明 |
|---|---|
| `application_id` | 申請 ID |
| `customer_id` | 客戶 ID |
| `apply_type` | 貸款類型 |
| `apply_amount` | 申請金額 |
| `apply_period` | 申請期數 |
| `rate` | 申請利率 |
| `disbursement_account` | 撥款入帳帳號 |
| `application_status` | 申請狀態 |
| `create_time` | 建立時間 |
| `latest_contact_status` | 最新聯繫結果 |
| `latest_contact_time` | 最新聯繫時間 |
| `update_time` | 外部流程異動時間 |
| `documents_submitted_at` | 客戶送出補件時間 |

### 6.2 `loan_contact_log`

| 欄位 | 說明 |
|---|---|
| `log_id` | 聯繫紀錄 ID |
| `application_id` | 所屬申請 |
| `emp_id` | 行員 ID |
| `contact_status` | 聯繫結果 |
| `contact_channel` | 聯繫管道 |
| `contact_time` | 聯繫時間 |
| `note` | 備註 |

### 6.3 `loan_review_detail`

| 欄位 | 說明 |
|---|---|
| `review_id` | 二次填單 ID |
| `application_id` | 所屬申請 |
| `confirmed_amount` | 核准金額 |
| `confirmed_period` | 核准期數 |
| `confirmed_rate` | 核准利率 |
| `collateral_note` | 擔保 / 審核補充 |
| `emp_id` | 審核行員 |
| `review_time` | 編輯時間 |
| `review_status` | `DRAFT` / `SUBMITTED` |
| `submitted_time` | 送審時間 |
| `review_note` | 審核備註 |

### 6.4 `loan_account`

| 欄位 | 說明 |
|---|---|
| `account_id` | 貸款模組帳戶 ID |
| `account_number` | 帳戶模組貸款帳號 |
| `application_id` | 所屬申請 |
| `customer_id` | 客戶 ID |
| `apply_type` | 貸款類型 |
| `principal_amount` | 本金 |
| `confirmed_period` | 核准期數 |
| `rate` | 利率 |
| `monthly_payment` | 月付金 |
| `paid_periods` | 已繳期數 |
| `remaining_principal` | 剩餘本金 |
| `start_date` | 起始日 |
| `next_payment_date` | 下次繳款日 |
| `account_status` | `ACTIVE` / `OVERDUE` / `PAID_OFF` |
| `create_time` | 建立時間 |
| `update_time` | 更新時間 |

### 6.5 `loan_repayment`

| 欄位 | 說明 |
|---|---|
| `repayment_id` | 還款期數 ID |
| `account_id` | 所屬貸款帳戶 |
| `period_index` | 第幾期 |
| `scheduled_date` | 應繳日 |
| `paid_date` | 實際繳款日 |
| `total_amount` | 本期總額 |
| `principal_portion` | 本金部分 |
| `interest_portion` | 利息部分 |
| `remaining_after` | 繳完本期後剩餘本金 |
| `repayment_status` | `SCHEDULED` / `PAID` / `OVERDUE` |
| `create_time` | 建立時間 |
| `update_time` | 更新時間 |

### 6.6 `loan_document`

| 欄位 | 說明 |
|---|---|
| `document_id` | 文件 ID |
| `application_id` | 所屬申請 |
| `document_type` | 文件類型 |
| `file_url` | 檔案 URL |
| `original_name` | 原始檔名 |
| `uploaded_by` | 上傳者 |
| `upload_time` | 上傳時間 |

## 7. Enum 分類

### 7.1 `LoanApplicationStatus`

| 值 | 說明 |
|---|---|
| `PENDING_CONTACT` | 待聯繫 |
| `IN_CONTACT` | 聯繫中 |
| `PENDING_REVIEW` | 已送風控 / 待審核 |
| `RETURNED` | 補件 / 退回 |
| `APPROVED` | 風控核准，待撥款 |
| `REJECTED` | 風控拒絕 |
| `CANCELLED` | 客戶取消或婉拒 |
| `DISBURSED` | 已撥款 |
| `CLOSED` | 已結清 |

### 7.2 其他狀態

| Enum | 值 |
|---|---|
| `LoanAccountStatus` | `ACTIVE`, `OVERDUE`, `PAID_OFF` |
| `LoanRepaymentStatus` | `SCHEDULED`, `PAID`, `OVERDUE` |
| `LoanReviewStatus` | `DRAFT`, `SUBMITTED` |
| `LoanContactStatus` | `NOT_CONTACTED`, `ATTEMPTED`, `REACHED`, `CONFIRMED`, `DECLINED` |
| `LoanContactChannel` | `PHONE`, `EMAIL`, `SMS` |
| `LoanDocumentType` | `ID_CARD`, `INCOME_CERT`, `EMPLOYMENT_CERT`, `BANK_STATEMENT`, `PROPERTY_CERT`, `TITLE_DEED`, `OTHER` |

## 8. DTO 欄位整理

### 8.1 Request DTO

| DTO | 欄位 | 用途 |
|---|---|---|
| `LoanMemberRequestDTO` | `applyType`, `applyAmount`, `applyPeriod`, `rate`, `disbursementAccount` | 客戶申請 |
| `LoanContactLogRequestDTO` | `empId`, `contactStatus`, `contactChannel`, `note` | 行員聯繫紀錄 |
| `LoanReviewDetailRequestDTO` | `confirmedAmount`, `confirmedPeriod`, `confirmedRate`, `collateralNote`, `empId` | 行員二次填單 |
| `LoanRiskRequestDTO` | `applicationId`, `customerId`, `cif`, `applyType`, `confirmedAmount`, `confirmedPeriod`, `confirmedRate`, `collateralNote`, `empId`, `submittedTime`, `callbackUrl`, `scene`, `businessId`, `amount` | 送風控 |
| `LoanStatusCallbackRequestDTO` | `newStatus`, `callerModule`, `note`, `loanAccountNumber` | 外部狀態回調 |
| `LoanDocumentInfoDTO` | `documentId`, `documentType`, `fileUrl`, `originalName` | 補件通知風控 |

### 8.2 Response DTO

| DTO | 主要欄位 |
|---|---|
| `LoanApplicationResponseDTO` | 申請 ID、客戶 ID、CIF、姓名、申請條件、核准條件、申請狀態、聯繫狀態、補件時間 |
| `LoanContactLogResponseDTO` | 紀錄 ID、申請 ID、行員 ID、聯繫結果、管道、時間、備註 |
| `LoanReviewDetailResponseDTO` | 二次填單 ID、申請 ID、核准金額、期數、利率、擔保備註、行員、狀態、送審時間 |
| `LoanAccountResponseDTO` | 帳戶 ID、帳號、申請 ID、客戶 ID、CIF、本金、期數、利率、月付金、已繳期數、剩餘本金、狀態 |
| `LoanRepaymentResponseDTO` | 期數 ID、帳戶 ID、期數、應繳日、繳款日、總額、本金、利息、剩餘本金、狀態 |
| `LoanDocumentResponseDTO` | 文件 ID、申請 ID、文件類型、URL、原始檔名、上傳者、上傳時間 |

## 9. 帳戶整合方法

### 9.1 `AccountIntegrationService` 貸款相關方法

| 方法 | 功能 |
|---|---|
| `createLoanAccount(LoanAccountCreateRequest request)` | 建立帳戶模組中的 `AccountType.LOAN` 貸款負債帳戶 |
| `disburseLoan(LoanDisbursementRequest request)` | 銀行撥款帳戶扣款、客戶入帳帳戶加款，並 afterCommit 通知貸款模組 |
| `hasDisbursementRecordByApplicationId(String applicationId)` | 檢查指定申請是否已有撥款交易紀錄 |
| `addLoanInterest(LoanInterestRequest request)` | 將貸款利息加到帳務負債 |
| `getActiveTwdCheckingAccounts(String customerId)` | 查客戶可作為撥款 / 還款來源的台幣活存帳戶 |
| `repayLoan(String customerId, LoanRepaymentRequest request)` | 客戶還款：扣款帳戶扣款、貸款負債下降、銀行收款帳戶入帳，並同步貸款期數 |
| `getLoanRepaymentRecords(...)` | 查客戶貸款還款交易紀錄 |

重要私有方法：

| 方法 | 功能 |
|---|---|
| `findCustomer` | 驗證並查客戶 |
| `generateDedicatedAccountNumber` | 依身分證編碼產生 901-999 貸款專用帳號 |
| `encodeIdentityNumber` | 身分證英文轉數字編碼 |
| `lockAccounts` / `lockSingleAccount` | 交易時鎖定帳戶避免併發問題 |
| `validateBusinessAccount` | 驗證銀行內部帳戶 |
| `validateLoanAccount` | 驗證帳務貸款帳戶 |
| `validateRepaymentSourceOrTarget` | 驗證台幣活存扣款 / 入帳帳戶 |
| `validateLoanRepaymentAmount` | 驗證還款金額必須等於本期或連續多期應繳總額 |
| `isFinalPrincipalRepayment` | 最後一期允許以剩餘本金清償 |
| `isFullMultiPeriodRepayment` | 允許一次繳清目前所有待繳期數總額 |
| `insertTransLog` | 直接寫入交易紀錄 |
| `toLoanAccountResponse` / `toLoanTransactionResponse` | 回應 DTO 轉換 |

### 9.2 `AccountIntegrationController` 貸款端點

| 方法 | HTTP | Path | 功能 |
|---|---|---|---|
| `getLoanRepaymentDebitAccounts` | `GET` | `/api/customer/loan-repayments/debit-accounts` | 查可扣款台幣活存帳戶 |
| `repayLoan` | `POST` | `/api/customer/loan-repayments` | 執行貸款還款 |
| `getLoanRepaymentRecords` | `GET` | `/api/customer/loan-repayments` | 查貸款還款紀錄 |

## 10. 前端畫面與方法

### 10.1 客戶端

| 檔案 | 主要方法 / computed | 功能 |
|---|---|---|
| `LoanApplyView.vue` | `loadRateRules` | 呼叫 `/api/loan-applications/rate-rules` |
| `LoanApplyView.vue` | `loadAccounts` | 取得可撥款入帳帳戶 |
| `LoanApplyView.vue` | `computedRate`, `estimatedMonthly`, `availablePeriods` | 利率與月付金前端試算 |
| `LoanApplyView.vue` | `validate`, `validateAll` | 申請表單驗證 |
| `LoanApplyView.vue` | `submitForm` | POST `/api/loan-applications/member` |
| `LoanApplyView.vue` | `resetForm`, `resetAll`, `goApply`, `goStatus` | 表單與頁面切換 |
| `LoanStatusView.vue` | `load` | GET `/api/loan-applications/my` |
| `LoanStatusView.vue` | `toggleDocPanel`, `loadDocs` | 展開並載入補件 |
| `LoanStatusView.vue` | `submitUpload` | POST `/api/loan-documents/{appId}/upload` |
| `LoanStatusView.vue` | `deleteDoc` | DELETE `/api/loan-documents/{documentId}` |
| `LoanStatusView.vue` | `submitDocuments` | POST `/api/loan-documents/{appId}/submit` |
| `LoanAccountView.vue` | `loadAccounts` | GET `/api/loan-accounts/my` |
| `LoanAccountView.vue` | `openRepaymentModal` | GET `/api/loan-accounts/{accountId}/repayments` |
| `LoanAccountView.vue` | `progressPct`, `paidRatio`, `isOverdue` | 帳戶進度與狀態顯示 |
| `LoanRepaymentView.vue` | `loadLoanAccounts` | GET `/api/loan-accounts/my` |
| `LoanRepaymentView.vue` | `loadDebitAccounts` | GET `/api/customer/loan-repayments/debit-accounts` |
| `LoanRepaymentView.vue` | `selectLoan`, `loadCurrentRepaymentAmount` | 選貸款帳戶並抓目前應繳期 |
| `LoanRepaymentView.vue` | `fillCurrentRepaymentAmount` | 帶入本期應繳金額 |
| `LoanRepaymentView.vue` | `submitRepayment` | POST `/api/customer/loan-repayments` |
| `LoanRepaymentView.vue` | `loadHistory`, `gotoPage` | GET `/api/customer/loan-repayments` 分頁查紀錄 |

### 10.2 行員端

| 檔案 | 主要方法 / computed | 功能 |
|---|---|---|
| `LoanApplicationView.vue` | `fetchApplications` | GET `/api/admin/loan-applications` |
| `LoanApplicationView.vue` | `setStatus` | 切換申請狀態篩選 |
| `LoanApplicationView.vue` | `filteredApplications`, `pagedApplications` | 前端篩選、排序、分頁 |
| `LoanApplicationView.vue` | `openContactModal`, `openReviewModal`, `openDocModal` | 開啟聯繫、審核、補件 Modal |
| `LoanApplicationView.vue` | `startAutoRefresh` | 每 30 秒自動刷新 |
| `LoanContactLogModal.vue` | `fetchLogs` | GET `/api/admin/loan-applications/{id}/contact-logs` |
| `LoanContactLogModal.vue` | `submitLog` | POST `/api/admin/loan-applications/{id}/contact-logs` |
| `LoanReviewModal.vue` | `fetchReview` | GET `/api/admin/loan-applications/{id}/review` |
| `LoanReviewModal.vue` | `saveDraft` | POST `/api/admin/loan-applications/{id}/review` |
| `LoanReviewModal.vue` | `submitReview` | PATCH `/api/admin/loan-applications/{id}/review/submit` |
| `LoanReviewModal.vue` | `amountDiff`, `periodDiff`, `rateDiff`, `hasDiff` | 顯示二次填單與原申請差異 |
| `LoanDocumentModal.vue` | `loadDocs` | GET `/api/admin/loan-documents/{appId}` |
| `LoanAccountAdminView.vue` | `fetchAccounts` | GET `/api/admin/loan-accounts` |
| `LoanAccountAdminView.vue` | `openRepaymentModal` | GET `/api/admin/loan-accounts/{accountId}/repayments` |
| `LoanAccountAdminView.vue` | `filteredAccounts`, `pagedAccounts` | 帳戶狀態 / 類型篩選與分頁 |
| `UserLayout.vue` | loan menu config | 顯示「快速申請貸款」、「查詢申貸狀態」、「貸款帳戶」入口 |
| `AdminLayout.vue` | loan menu group | 顯示「貸款申請管理」、「貸款帳戶管理」入口 |

### 10.3 路由

| 路由 | Name | Component |
|---|---|---|
| `/user/loan-apply` | `user-loan-apply` | `LoanApplyView` |
| `/user/loan-status` | `user-loan-status` | `LoanStatusView` |
| `/user/loan-accounts` | `user-loan-accounts` | `LoanAccountView` |
| `/user/loan-repayment` | `user-loan-repayment` | `LoanRepaymentView` |
| `/admin/loan-apply` | `loan-apply` | 重導到 `user-loan-apply` |
| `/admin/loan-applications` | `loan-applications` | `LoanApplicationView` |
| `/admin/loan-accounts` | `admin-loan-accounts` | `LoanAccountAdminView` |

## 11. Email 通知

`EmailService.java` 中貸款相關方法：

| 方法 | 模板 | 用途 |
|---|---|---|
| `sendLoanAppliedNotification` | `mail/loan-applied` | 客戶送出申請 |
| `sendLoanDocumentRequiredNotification` | `mail/loan-document-required` | 需要補件 |
| `sendLoanRejectedNotification` | `mail/loan-rejected` | 申請拒絕 |
| `sendLoanApprovedAndDisbursedNotification` | `mail/loan-disbursed` | 核准並撥款 |
| `sendLoanRepaymentPaidNotification` | `mail/loan-repayment-paid` | 還款成功 |
| `sendLoanPaidOffNotification` | `mail/loan-paid-off` | 全數結清 |
| `sendLoanOverdueNotification` | `mail/loan-overdue` | 逾期 |
| `sendLoanRepaymentReminderNotification` | `mail/loan-repayment-reminder` | 到期提醒 |
| `formatLoanType` | - | 將貸款類型代碼轉中文 |
| `formatAnnualRate` | - | 格式化年利率 |

## 12. 主要流程對照

### 12.1 客戶申請流程

| 步驟 | 前端 | API | 後端 |
|---|---|---|---|
| 載入利率規則 | `LoanApplyView.loadRateRules` | `GET /api/loan-applications/rate-rules` | `LoanApplicationService.getRateRules` |
| 選擇撥款帳戶 | `LoanApplyView.loadAccounts` | 客戶帳戶 API | `customerAccount` 相關 API |
| 送出申請 | `LoanApplyView.submitForm` | `POST /api/loan-applications/member` | `LoanApplicationService.insertMember` |
| 查申請狀態 | `LoanStatusView.load` | `GET /api/loan-applications/my` | `LoanApplicationService.getMyApplications` |

### 12.2 行員審核流程

| 步驟 | 前端 | API | 後端 |
|---|---|---|---|
| 查申請 | `LoanApplicationView.fetchApplications` | `GET /api/admin/loan-applications` | `getByStatus` |
| 新增聯繫 | `LoanContactLogModal.submitLog` | `POST /{id}/contact-logs` | `addContactLog` |
| 二次填單草稿 | `LoanReviewModal.saveDraft` | `POST /{id}/review` | `saveReviewDetail` |
| 正式送審 | `LoanReviewModal.submitReview` | `PATCH /{id}/review/submit` | `submitReview` + `LoanRiskClient.submitForReview` |
| 風控回調 | 風控系統 | `POST /api/loan-callbacks/{id}/status` | `handleStatusCallback` |
| 自動撥款 | 後端 afterCommit | AccountIntegrationService | `autoDisburse` |

### 12.3 補件流程

| 步驟 | 前端 | API | 後端 |
|---|---|---|---|
| 客戶查文件 | `LoanStatusView.loadDocs` | `GET /api/loan-documents/{appId}` | `LoanDocumentService.getByApplicationId(appId, customerId)` |
| 客戶上傳 | `LoanStatusView.submitUpload` | `POST /api/loan-documents/{appId}/upload` | `LoanDocumentService.upload` |
| 客戶送出 | `LoanStatusView.submitDocuments` | `POST /api/loan-documents/{appId}/submit` | `submitDocuments` + `LoanRiskClient.attachDocuments` |
| 行員查補件 | `LoanDocumentModal.loadDocs` | `GET /api/admin/loan-documents/{appId}` | `LoanDocumentService.getByApplicationId(appId)` |

### 12.4 還款流程

| 步驟 | 前端 | API | 後端 |
|---|---|---|---|
| 查貸款帳戶 | `LoanRepaymentView.loadLoanAccounts` | `GET /api/loan-accounts/my` | `LoanAccountService.getMyAccounts` |
| 查扣款帳戶 | `LoanRepaymentView.loadDebitAccounts` | `GET /api/customer/loan-repayments/debit-accounts` | `AccountIntegrationService.getActiveTwdCheckingAccounts` |
| 查目前應繳期 | `LoanRepaymentView.loadCurrentRepaymentAmount` | `GET /api/loan-accounts/{accountId}/repayments` | `LoanRepaymentService.getByAccountId` |
| 執行還款 | `LoanRepaymentView.submitRepayment` | `POST /api/customer/loan-repayments` | `AccountIntegrationService.repayLoan` + `LoanRepaymentService.processRepayments` |
| 查還款紀錄 | `LoanRepaymentView.loadHistory` | `GET /api/customer/loan-repayments` | `AccountIntegrationService.getLoanRepaymentRecords` |

## 13. 注意事項

1. `LoanApplicationService.autoDisburse` 使用 `NOT_SUPPORTED`，目的是避免貸款狀態更新、帳戶建帳、撥款交易互相持有外層事務鎖。
2. `LoanApplicationService` 以 `@Lazy` 注入自身 proxy，是為了讓內部呼叫 `autoDisburse` 時仍能套用 Spring AOP 交易傳播。
3. `LoanDocumentService.submitDocuments` 通知風控屬 best-effort，失敗不影響客戶補件送出。
4. `LoanRiskClient.submitForReview` 失敗會丟例外，避免狀態進入 `PENDING_REVIEW` 但風控未收到資料。
5. `AccountIntegrationService.repayLoan` 會要求還款金額等於本期或連續多期應繳總額；最後一期可用剩餘本金清償。
6. `loan_init.sql` 比 `loan/database/LoanTable.sql` 多了 `loan_account.account_number` 欄位註解與完整說明，實務上應優先參考 `src/main/resources/database/loan_init.sql`。
7. 前端貸款 API 多數直接在 `.vue` 中用 `api` / `axios` 呼叫，沒有獨立的 `frontend/src/api/loan.js` 封裝檔。

## 14. 使用到的技術方法

本節整理貸款模組在後端、前端、資料庫與跨模組整合中實際使用到的技術與實作方法。

### 14.1 後端框架與 API 技術

| 技術 / 方法 | 使用位置 | 用途 |
|---|---|---|
| Spring Boot | 全後端專案 | 建立 REST API、Service、Repository、Scheduler 等應用骨架 |
| Spring MVC `@RestController` | `loan/controller/*Controller.java` | 對外提供貸款申請、帳戶、文件、回調 API |
| `@RequestMapping` / `@GetMapping` / `@PostMapping` / `@PatchMapping` / `@DeleteMapping` | 各 Controller | 定義 RESTful 路由與 HTTP method |
| `ResponseEntity<ApiResponse<T>>` | 各 Controller | 統一 API 回應格式與 HTTP 狀態碼 |
| `@RequestBody` | 申請、聯繫、審核、回調 API | 接收 JSON request body |
| `@RequestParam` | 查詢、文件上傳、狀態篩選 | 接收 query string 或 multipart 欄位 |
| `@PathVariable` | 申請 ID、帳戶 ID、文件 ID | 從 URL path 擷取業務識別碼 |
| `MultipartFile` | `LoanDocumentController.upload` | 接收補件檔案上傳 |

### 14.2 權限與身分驗證

| 技術 / 方法 | 使用位置 | 用途 |
|---|---|---|
| Spring Security | `SecurityConfig.java` | 管理 API 權限、公開 API、回調來源限制 |
| `@EnableMethodSecurity` | `SecurityConfig.java` | 啟用方法層級權限註解 |
| `@PreAuthorize("hasRole('CUSTOMER')")` | 客戶端 Controller | 限制客戶貸款申請、帳戶、補件 API 需 CUSTOMER 角色 |
| JWT | `JwtUtil`、各客戶端 Controller | 從 token 解析 `customerId`，確保只能操作自己的資料 |
| `jwtUtil.resolveCustomerId(request)` | `AccountIntegrationController` | 還款 API 解析客戶身分 |
| Bearer Token Header | 多個 Controller 的 `extractCustomerId` | 從 `Authorization: Bearer ...` 取得登入客戶 |
| IP 限制 | `SecurityConfig.java` | `/api/loan-callbacks/**` 僅允許本機 IP，避免外部任意回調 |

### 14.3 資料存取與 ORM

| 技術 / 方法 | 使用位置 | 用途 |
|---|---|---|
| Spring Data JPA | `loan/repository/*.java` | 提供 Entity CRUD 與衍生查詢方法 |
| `JpaRepository<Entity, ID>` | 各 Repository | 基礎新增、查詢、更新、刪除 |
| JPA Derived Query | `findBy...OrderBy...` 系列方法 | 用方法命名產生查詢，例如依客戶、狀態、日期查詢 |
| JPA Entity | `loan/entity/*.java` | 對應貸款資料表 |
| `@Enumerated(EnumType.STRING)` | Entity enum 欄位 | 將狀態 enum 以字串存入 DB |
| `EntityManager.persist` | `LoanApplicationService.insertMember` | 建立貸款申請 |
| `JdbcTemplate` | `LoanApplicationService`、`AccountIntegrationService` | 直接寫入聯繫 / 審核資料與交易紀錄，避開部分 JPA 快取或配合批次 SQL |
| JPA Specification | `AccountIntegrationService.getLoanRepaymentRecords` | 動態組合還款紀錄查詢條件 |
| `findAllById` | 排程與帳戶查詢 | 批次查詢多筆貸款帳戶 |

### 14.4 交易與一致性設計

| 技術 / 方法 | 使用位置 | 用途 |
|---|---|---|
| `@Transactional` | Service 層 | 確保申請、審核、帳戶、還款更新具交易一致性 |
| `@Transactional(readOnly = true)` | 查詢方法 | 標示唯讀查詢，降低不必要交易負擔 |
| `Propagation.NOT_SUPPORTED` | `LoanApplicationService.autoDisburse` | 自動撥款時不持有外層交易，避免跨模組鎖定 |
| `Propagation.REQUIRES_NEW` | `handleAccountDisbursedCallback` 相關流程 | 帳戶回調以獨立交易更新貸款狀態 |
| `TransactionSynchronizationManager.registerSynchronization` | 送審、撥款流程 | 主交易 commit 後才呼叫外部系統或跨模組回調 |
| `afterCommit()` | 風控送審、撥款完成通知 | 避免 DB 回滾後外部副作用已發生 |
| `@Lazy` 自身 Proxy 注入 | `LoanApplicationService` | 讓內部呼叫仍能觸發 Spring AOP 交易傳播 |
| 帳戶鎖定查詢 | `AccountIntegrationService.lockAccounts` | 撥款 / 還款時鎖定帳戶，降低併發交易風險 |
| 冪等檢查 | `LoanAccountService.createOnDisbursement` | 重複撥款回調時不重複建立貸款帳戶 |

### 14.5 跨模組與外部服務整合

| 技術 / 方法 | 使用位置 | 用途 |
|---|---|---|
| `RestTemplate` | `LoanRiskClient` | 呼叫風控模組 API |
| `postForEntity` | `LoanRiskClient.submitForReview` | 將二次填單資料送往風控審核 |
| `exchange(..., HttpMethod.PATCH, ...)` | `LoanRiskClient.attachDocuments` | 補件後通知風控更新附件 |
| Callback URL | `LoanRiskRequestDTO.callbackUrl` | 風控完成後回調 `/api/loan-callbacks/{applicationId}/status` |
| `callerModule` 分流 | `LoanStatusCallbackRequestDTO` | 區分 `RISK` 與 `ACCOUNT` 回調來源 |
| 帳戶整合 Service | `AccountIntegrationService` | 建立貸款帳務帳戶、撥款、還款、查交易 |
| 交易 referenceId | `ReferenceIdGenerator` | 撥款與還款交易產生共同參考號 |

### 14.6 檔案上傳與文件管理

| 技術 / 方法 | 使用位置 | 用途 |
|---|---|---|
| `multipart/form-data` | `LoanDocumentController.upload` | 客戶上傳補件 |
| `MultipartFile` | `LoanDocumentService.upload` | 接收上傳檔案 |
| `FileStorageService.store` | `LoanDocumentService.upload` | 將補件儲存到 `uploads` 下的貸款文件目錄 |
| `FileStorageService.delete` | `LoanDocumentService.delete` | 刪除補件實體檔案 |
| 文件數量限制 | `LoanDocumentRepository.countByApplicationId` | 每筆申請最多上傳 5 份文件 |
| 送出後鎖定 | `documentsSubmittedAt` | 補件正式送出後不可再上傳或刪除 |

### 14.7 Email 與模板技術

| 技術 / 方法 | 使用位置 | 用途 |
|---|---|---|
| Spring Mail `JavaMailSender` | `EmailService` | 發送貸款相關通知信 |
| Thymeleaf TemplateEngine | `EmailService` | 將 `templates/mail/loan-*.html` 渲染成 HTML 信件 |
| Thymeleaf 變數 | `loan-*.html` | 顯示申請編號、貸款類型、金額、期數、還款資訊 |
| best-effort Email | 多個 Service | Email 失敗只記錄 log，不中斷主要業務流程 |
| `formatLoanType` | `EmailService` | 將貸款類型代碼轉中文顯示 |
| `formatAnnualRate` | `EmailService` | 將年利率格式化為百分比 |

### 14.8 排程與批次處理

| 技術 / 方法 | 使用位置 | 用途 |
|---|---|---|
| Spring Scheduling `@Scheduled` | `LoanRepaymentScheduler.scanOverdueRepayments` | 每日固定時間掃描貸款期數 |
| Cron `0 0 1 * * *` | `LoanRepaymentScheduler` | 每天 01:00 執行 |
| 批次狀態更新 | `repaymentRepo.saveAll`、`loanAccountRepo.saveAll` | 批次標記逾期與更新帳戶狀態 |
| `LocalDate` / `LocalDateTime` | 排程與還款 | 比對應繳日、建立時間、更新時間 |
| `ChronoUnit.DAYS.between` | 到期提醒 | 計算距離應繳日剩餘天數 |

### 14.9 金額、利率與攤還計算

| 技術 / 方法 | 使用位置 | 用途 |
|---|---|---|
| `BigDecimal` | 金額、利率、負債、月付金 | 避免浮點誤差 |
| `RoundingMode.CEILING` | `calcMonthlyPayment` | 月付金無條件進位，避免短收 |
| `RoundingMode.HALF_UP` | 利息與金額正規化 | 四捨五入到指定小數位 |
| 等額本息公式 | `AmortizationCalculator.calcMonthlyPayment` | 計算固定月付金 |
| 攤還表展開 | `AmortizationCalculator.buildSchedule` | 逐期計算本金、利息、剩餘本金 |
| 最後一期校正 | `buildSchedule` | 最後一期直接清零剩餘本金，消除累積誤差 |
| 還款金額驗證 | `validateLoanRepaymentAmount` | 限制還款金額需符合本期或連續多期總額 |

### 14.10 前端技術方法

| 技術 / 方法 | 使用位置 | 用途 |
|---|---|---|
| Vue 3 Composition API | `Loan*.vue` | 使用 `ref`、`reactive`、`computed`、`watch`、`onMounted` 管理狀態 |
| Vue Router | `frontend/src/router/index.js` | 定義客戶端與行員端貸款頁面路由 |
| Pinia / Store | `LoanApplyView.vue`、後台權限判斷 | 取得客戶資料、後台登入角色與權限 |
| Axios instance `api` | 多數 Vue 檔 | 呼叫後端 API，統一 baseURL 與攔截器 |
| 原生 `axios` | `LoanApplyView.vue` | 載入利率規則與提交申請 |
| `FormData` | `LoanStatusView.submitUpload` | 上傳補件檔案 |
| `computed` | 試算、篩選、分頁、差異顯示 | 即時計算利率、月付金、申請列表篩選、審核差異 |
| `watch` | Modal 開啟與表單變化 | 開啟視窗時載入資料或重設表單 |
| `onMounted` | 頁面初始載入 | 載入申請、帳戶、還款紀錄、利率規則 |
| localStorage token | 前端 API 呼叫 | 取得 `customer_token` 或 `auth_user.token` 放入 Authorization header |

### 14.11 前端互動與 UI 方法

| 技術 / 方法 | 使用位置 | 用途 |
|---|---|---|
| 狀態對照表 Map | 多個貸款 Vue 檔 | 將 enum 轉成中文標籤、CSS class、icon |
| 前端表單驗證 | `LoanApplyView.validate`、`LoanReviewModal` | 檢查金額、期數、利率、帳戶欄位 |
| 前端試算 | `LoanApplyView.computedRate`、`estimatedMonthly` | 送出前預估年利率與月付金 |
| Modal | `LoanContactLogModal`、`LoanReviewModal`、`LoanDocumentModal` | 行員在列表中開啟細節操作 |
| 分頁 | `LoanApplicationView`、`LoanAccountAdminView`、`LoanRepaymentView` | 前端分頁顯示申請、帳戶、還款紀錄 |
| 排序與篩選 | `LoanApplicationView`、`LoanAccountAdminView` | 依狀態、貸款類型、姓名、欄位排序 |
| 自動刷新 | `LoanApplicationView.startAutoRefresh` | 行員頁面每 30 秒更新申請列表 |

### 14.12 資料庫與 SQL 方法

| 技術 / 方法 | 使用位置 | 用途 |
|---|---|---|
| SQL DDL | `loan_init.sql` | 建立貸款申請、聯繫、審核、帳戶、還款、文件資料表 |
| Foreign Key | `loan_init.sql` | 維持申請、帳戶、還款、文件關聯 |
| `DECIMAL(18,2)` | 金額欄位 | 儲存申請金額、月付金、還款金額 |
| `DECIMAL(10,6)` | 利率欄位 | 儲存年利率 |
| `DATETIME2` / `DATE` | 時間欄位 | 儲存申請、聯繫、審核、繳款與更新時間 |
| Mock Data SQL | `loan_mockdata.sql` | 建立貸款測試資料 |

### 14.13 例外處理與防呆方法

| 技術 / 方法 | 使用位置 | 用途 |
|---|---|---|
| `BusinessException` | 貸款 Service | 業務規則不符時中斷流程並回傳錯誤 |
| `AccountException` | `AccountIntegrationService` | 帳戶、餘額、還款金額、帳號不符等錯誤 |
| 所有權驗證 | Controller / Service | 防止客戶存取他人申請、文件、帳戶 |
| 狀態檢查 | `submitReview`、`retryRiskSubmit`、`retryDisburse` | 限制只能在合法狀態下推進流程 |
| 金額正規化 | `normalizePositiveAmount`、`normalizeRate` | 確保金額大於 0、利率合法、scale 正確 |
| 貸款帳戶 balance 防呆 | `ensureLoanBalanceZero` | 貸款帳務帳戶不允許一般餘額變動 |
| overpayment 防呆 | `repayLoan` | 避免還款金額超過剩餘負債，特殊清償情境例外 |
