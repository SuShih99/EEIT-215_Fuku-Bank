# 貸款模組解析 Summary

這份是 `貸款模組解析.md` 的簡易版，只保留「貸款模組在做什麼、哪些檔案負責什麼、閱讀時先看哪裡」。若要看完整流程、欄位、狀態與技術細節，請回到 `貸款模組解析.md`。

## 1. 模組主線

貸款模組負責完整貸款生命週期：

1. 客戶送出貸款申請。
2. 行員查看申請、聯繫客戶、填寫審核資料。
3. 系統把審核資料送到風控。
4. 風控 callback 回傳核准、拒絕或補件。
5. 核准後串帳戶模組建立貸款帳務帳戶並撥款。
6. 客戶後續還款，系統同步還款期數與貸款帳戶狀態。
7. 排程負責逾期掃描與到期提醒。

## 2. 主要關聯模組

| 模組 | 貸款模組如何使用 |
|---|---|
| Customer | 取得客戶姓名、CIF、電話、Email |
| Account | 建立貸款帳務帳戶、撥款、還款、查交易紀錄 |
| Risk | 送風控審核、補件通知、接收 callback |
| Common | 使用 Security、JWT、Email、檔案儲存、RestTemplate、例外處理與 PDF 中文字型載入 |
| Mail / Notification | 寄送申請、補件、拒絕、核准撥款、契約附件、還款、逾期、結清通知 |
| Frontend | 提供客戶端貸款頁與行員端管理頁 |

## 3. 客戶端功能

| 功能 | 前端檔案 | 後端入口 | 說明 |
|---|---|---|---|
| 申請貸款 | `LoanApplyView.vue` | `LoanApplicationController` | 填表、試算、選撥款帳戶、送出申請 |
| 查申請狀態 | `LoanStatusView.vue` | `LoanApplicationController` | 查看進度、聯繫結果、核准條件 |
| 上傳補件 | `LoanStatusView.vue` | `LoanDocumentController` | 上傳與送出補件文件 |
| 查貸款帳戶 | `LoanAccountView.vue` | `LoanAccountController` | 查看貸款帳戶、餘額、利率、期數進度 |
| 還款 | `LoanRepaymentView.vue` | `AccountIntegrationController` | 選扣款帳戶、送出還款、查還款紀錄 |

## 4. 行員端功能

| 功能 | 前端檔案 | 後端入口 | 說明 |
|---|---|---|---|
| 申請列表 | `LoanApplicationView.vue` | `LoanAdminController` | 查申請、篩選狀態、排序、分頁 |
| 聯繫紀錄 | `LoanContactLogModal.vue` | `LoanAdminController` | 顯示電話 / Email，新增聯繫紀錄 |
| 二次填單 | `LoanReviewModal.vue` | `LoanAdminController` | 填核准金額、期數、利率、擔保備註 |
| 補件查看 | `LoanDocumentModal.vue` | `LoanDocumentController` | 查看客戶送出的補件 |
| 貸款帳戶管理 | `LoanAccountAdminView.vue` | `LoanAccountAdminController` | 查帳戶、查還款表、用案件編號或貸款帳務帳號搜尋 |

## 5. 後端檔案職責

### 5.1 Controller

| 檔案 | 負責什麼 |
|---|---|
| `LoanApplicationController.java` | 客戶申請貸款與查自己的申請 |
| `LoanAdminController.java` | 行員查申請、聯繫、二次填單、送審與補償操作 |
| `LoanDocumentController.java` | 補件上傳、送出、刪除、查詢 |
| `LoanAccountController.java` | 客戶查自己的貸款帳戶與還款明細 |
| `LoanAccountAdminController.java` | 行員查全部貸款帳戶、還款表與手動同步 |
| `LoanCallbackController.java` | 接收風控與帳戶模組 callback |

### 5.2 Service / Client / Scheduler

| 檔案 | 負責什麼 |
|---|---|
| `LoanApplicationService.java` | 貸款申請主流程，包含建單、審核、送風控、callback、撥款補償 |
| `LoanDocumentService.java` | 補件文件管理，包含收檔、存檔、刪檔、送出 |
| `LoanAccountService.java` | 撥款後建立貸款帳戶，提供帳戶查詢 |
| `LoanRepaymentService.java` | 建立攤還表、查還款期數、同步已繳、結清 |
| `LoanContractPdfService.java` | 產生貸款契約 PDF，並使用共用中文字型工具 |
| `LoanRiskClient.java` | 呼叫風控模組 |
| `LoanRepaymentScheduler.java` | 每日逾期掃描與到期提醒 |
| `AmortizationCalculator.java` | 計算月付金與攤還表 |

## 6. 資料模型

| Entity | 代表什麼 |
|---|---|
| `LoanApplication` | 貸款申請主資料 |
| `LoanContactLog` | 行員聯繫紀錄 |
| `LoanReviewDetail` | 行員二次填單與審核資料 |
| `LoanDocument` | 補件文件資料 |
| `LoanAccount` | 撥款後的貸款帳戶 |
| `LoanRepayment` | 每一期還款資料 |

| DTO / Enum 類型 | 用途 |
|---|---|
| `dto/requests/*` | 前端、風控或其他模組送進來的請求資料 |
| `dto/response/*` | 回傳給前端的資料 |
| `enums/*` | 申請狀態、帳戶狀態、還款狀態、文件類型、聯繫類型 |

## 7. 跨模組整合

| 整合對象 | 核心檔案 | 做什麼 |
|---|---|---|
| 帳戶模組 | `AccountIntegrationService` | 建貸款帳務帳戶、撥款、還款、交易紀錄 |
| 風控模組 | `LoanRiskClient`, `LoanCallbackController` | 送審、補件通知、接收核准 / 拒絕 / 補件結果 |
| 檔案儲存 | `FileStorageService`, `LoanDocumentService` | 儲存與刪除補件檔案 |
| 通知信 | `EmailService` | 申請成立、補件、核准撥款、契約附件、還款、逾期、結清通知 |
| PDF | `LoanContractPdfService`, `PdfCjkFontLoader` | 產生貸款契約書並處理中文顯示 |

## 8. 資料庫與資源

| 檔案 | 用途 |
|---|---|
| `src/main/resources/database/loan_init.sql` | 貸款資料表 |
| `src/main/resources/database/loan_mockdata.sql` | 貸款測試資料，包含貸款本金與帳務負債對齊情境 |
| `src/main/resources/database/loan_document_batch_migration.sql` | 補件批次欄位調整 |
| `src/main/resources/templates/mail/loan-*.html` | 貸款通知信模板 |
| `src/main/resources/templates/mail/loan-contract.html` | 貸款契約附件通知模板 |
| `src/main/resources/fonts/NotoSansTC-Regular.ttf` | PDF 中文字型 |
| `src/main/java/com/javaeasybank/loan/utils/test.http` | 手動測試 API |

## 9. 建議閱讀順序

快速理解貸款主流程：

1. `LoanApplyView.vue`
2. `LoanApplicationController.java`
3. `LoanApplicationService.java`
4. `LoanApplication.java`
5. `LoanReviewDetail.java`
6. `LoanRiskClient.java`
7. `LoanCallbackController.java`

理解撥款與還款：

1. `AccountIntegrationService.java`
2. `LoanAccountService.java`
3. `LoanAccount.java`
4. `LoanRepaymentService.java`
5. `LoanRepayment.java`
6. `LoanRepaymentView.vue`
7. `LoanAccountAdminView.vue`

理解補件與通知：

1. `LoanStatusView.vue`
2. `LoanDocumentController.java`
3. `LoanDocumentService.java`
4. `FileStorageService.java`
5. `EmailService.java`
6. `LoanContractPdfService.java`
7. `PdfCjkFontLoader.java`
