# 貸款模組檔案快速整理

這份是 `LoanModuleFileCatalog.md` 的簡易版，只保留「哪些檔案負責什麼」的快速對照，方便先建立整體地圖。

## 1. 模組在做什麼

貸款模組的主線大致分成五段：

1. 客戶送出貸款申請。
2. 行員查看申請、聯繫客戶、補資料、填審核內容。
3. 系統把審核資料送去風控，等待 callback。
4. 核准後串帳戶模組建立貸款帳戶並撥款。
5. 後續由還款、通知、排程與帳戶同步共同維護貸款生命週期。

## 2. 後端核心檔案

### 2.1 Controller

| 檔案 | 負責什麼 |
|---|---|
| `loan/controller/LoanApplicationController.java` | 客戶端貸款申請入口，負責申請、查自己的申請、查利率規則 |
| `loan/controller/LoanAdminController.java` | 行員端申請管理入口，負責列表、聯繫、審核、送審、重送補償 |
| `loan/controller/LoanAccountController.java` | 客戶端貸款帳戶查詢入口，負責查帳戶與還款明細 |
| `loan/controller/LoanAccountAdminController.java` | 行員端貸款帳戶管理入口，負責查全部貸款帳戶與還款狀況 |
| `loan/controller/LoanDocumentController.java` | 補件入口，負責上傳、查詢、刪除、正式送出文件 |
| `loan/controller/LoanCallbackController.java` | 接收外部模組回傳結果，主要是風控與帳戶整合 callback |

### 2.2 Service

| 檔案 | 負責什麼 |
|---|---|
| `loan/service/LoanApplicationService.java` | 貸款申請主流程核心，從建單、審核、送風控到撥款狀態推進幾乎都在這裡 |
| `loan/service/LoanAccountService.java` | 負責把核准後的貸款轉成貸款帳戶，並提供帳戶查詢 |
| `loan/service/LoanRepaymentService.java` | 管理每一期還款資料，負責排程、已繳同步、結清與還款狀態更新 |
| `loan/service/LoanDocumentService.java` | 管理補件文件，負責收檔、存檔、刪檔與補件送出 |
| `loan/service/LoanContractPdfService.java` | 產生貸款契約 PDF |
| `loan/client/LoanRiskClient.java` | 對外呼叫風控模組 |
| `loan/scheduler/LoanRepaymentScheduler.java` | 每日檢查逾期與到期提醒 |
| `loan/utils/AmortizationCalculator.java` | 計算月付金與攤還表 |

### 2.3 資料層

| 檔案群 | 負責什麼 |
|---|---|
| `loan/entity/*.java` | 定義貸款資料表在程式中的結構 |
| `loan/repository/*.java` | 提供貸款資料的查詢與存取 |
| `loan/dto/requests/*.java` | 接收前端、風控或其他模組送進來的資料 |
| `loan/dto/response/*.java` | 整理要回給前端的資料格式 |
| `loan/enums/*.java` | 集中定義貸款流程中的狀態、文件類型與聯繫類型 |

## 3. 貸款資料各自代表什麼

| 檔案 | 代表什麼 |
|---|---|
| `LoanApplication.java` | 一筆貸款申請主資料 |
| `LoanContactLog.java` | 行員聯繫客戶的紀錄 |
| `LoanReviewDetail.java` | 行員二次填單後的審核內容 |
| `LoanAccount.java` | 核准並撥款後建立的貸款帳戶資料 |
| `LoanRepayment.java` | 每一期應繳與已繳的還款資料 |
| `LoanDocument.java` | 客戶上傳的補件資料 |

## 4. 帳戶整合相關檔案

| 檔案 | 負責什麼 |
|---|---|
| `account/service/AccountIntegrationService.java` | 貸款與帳戶模組之間最重要的橋樑，負責建貸款帳戶、撥款、還款、交易與同步 |
| `account/controller/AccountIntegrationController.java` | 客戶端貸款還款 API 入口 |
| `account/dto/request/Loan*.java` | 貸款建立帳戶、撥款、還款時要傳給帳戶模組的資料格式 |
| `account/dto/response/Loan*.java` | 帳戶模組回傳貸款帳戶與交易結果的格式 |
| `account/entity/Account.java` | 帳戶模組中的實際帳戶資料，貸款帳戶也會落在這裡 |

## 5. 前端檔案

### 5.1 客戶端

| 檔案 | 負責什麼 |
|---|---|
| `frontend/src/views/user/LoanApplyView.vue` | 貸款申請頁，負責填表、試算、選撥款帳戶 |
| `frontend/src/views/user/LoanStatusView.vue` | 申請進度頁，負責查狀態、補件、送出補件 |
| `frontend/src/views/user/LoanAccountView.vue` | 貸款帳戶頁，負責看貸款帳戶與攤還表 |
| `frontend/src/views/user/LoanRepaymentView.vue` | 還款頁，負責選扣款帳戶、送出還款、看還款紀錄 |

### 5.2 行員端

| 檔案 | 負責什麼 |
|---|---|
| `frontend/src/views/admin/LoanApplicationView.vue` | 行員主頁，負責看申請清單與操作審核流程 |
| `frontend/src/views/admin/LoanContactLogModal.vue` | 聯繫紀錄視窗 |
| `frontend/src/views/admin/LoanReviewModal.vue` | 審核填單與送審視窗 |
| `frontend/src/views/admin/LoanDocumentModal.vue` | 補件文件查看視窗 |
| `frontend/src/views/admin/LoanAccountAdminView.vue` | 行員看貸款帳戶與還款進度的頁面 |

### 5.3 路由與共用

| 檔案 | 負責什麼 |
|---|---|
| `frontend/src/router/index.js` | 定義貸款相關前端路由 |
| `frontend/src/layouts/UserLayout.vue` | 客戶端貸款入口位置 |
| `frontend/src/layouts/AdminLayout.vue` | 行員端貸款入口位置 |
| `frontend/src/api/axios.js` | 前端共用 API 呼叫層，處理 token、base URL 與攔截器 |

## 6. 資源與設定檔案

| 檔案 | 負責什麼 |
|---|---|
| `src/main/resources/database/loan_init.sql` | 貸款模組正式資料表初始化腳本 |
| `src/main/resources/database/loan_mockdata.sql` | 貸款測試資料 |
| `src/main/resources/database/loan_document_batch_migration.sql` | 補件批次欄位的調整腳本 |
| `src/main/java/com/javaeasybank/loan/database/LoanTable.sql` | 舊版或模組內的貸款建表腳本參考 |
| `src/main/resources/templates/mail/loan-*.html` | 貸款各種通知信模板 |
| `src/main/resources/fonts/SourceHanSansTC-Normal.otf` | 契約 PDF 用的中文字型 |
| `loan/utils/test.http` | 手動測試貸款 API 的請求範本 |

## 7. 橫切支援檔案

| 檔案 | 負責什麼 |
|---|---|
| `common/config/SecurityConfig.java` | 管理貸款 API 的權限、白名單與 callback 存取限制 |
| `common/config/JwtAuthenticationFilter.java` | 把 JWT 解析成登入者資訊，供貸款 API 使用 |
| `common/config/RestTemplateConfig.java` | 提供呼叫風控 API 用的 HTTP client 設定 |
| `common/service/FileStorageService.java` | 補件上傳時實際存檔與刪檔 |
| `common/service/EmailService.java` | 寄送申請成立、補件、核准、還款、逾期、結清等通知 |

## 8. 建議怎麼看

如果只是想快速知道結構，建議這樣看：

1. 先看這份簡易版，建立整體地圖。
2. 再看 `LoanApplicationController -> LoanApplicationService -> LoanApplication / LoanReviewDetail`，理解申請主流程。
3. 接著看 `LoanDocumentService` 與 `LoanRiskClient`，理解補件與風控串接。
4. 最後看 `AccountIntegrationService -> LoanAccountService -> LoanRepaymentService`，理解撥款與還款。

如果要看完整流程、欄位、狀態與技術細節，再回到 `LoanModuleFileCatalog.md`。
