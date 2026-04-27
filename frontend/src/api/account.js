import api from './axios'

// === 統一管理 Account 的 API 設定 ===

// 建立帳戶
export function createAccount(data) {
  return api.post('/api/accounts', data)
}

// 查詢單一帳戶
export function getAccount(accountNumber) {
  return api.get(`/api/accounts/${accountNumber}`)
}

// 依客戶查詢帳戶（分頁）
export function getAccountsByCustomerId(customerId, page = 0, size = 10) {
  return api.get('/api/accounts', {
    params: { customerId, page, size },
  })
}

// 依狀態篩選（分頁）
export function getAccountsByStatus(status, page = 0, size = 10) {
  return api.get(`/api/accounts/status/${status}`, {
    params: { page, size },
  })
}

// 依型別+幣別篩選（分頁）
export function getAccountsByTypeAndCurrency(type, currency, page = 0, size = 10) {
  return api.get('/api/accounts/filter', {
    params: { type, currency, page, size },
  })
}

// 查詢最新帳戶（分頁）
export function getLatestAccounts(page = 0, size = 10) {
  return api.get('/api/accounts/latest', {
    params: { page, size },
  })
}

// 轉帳
export function transfer(data) {
  return api.post('/api/transfers', data)
}

// 依 referenceId 查詢交易紀錄
export function getTransLogsByReferenceId(referenceId) {
  return api.get(`/api/trans-logs/reference/${referenceId}`)
}

// 依帳號查詢交易紀錄（分頁）
export function getTransLogsByAccountNumber(accountNumber, page = 0, size = 10) {
  return api.get(`/api/trans-logs/account/${accountNumber}`, {
    params: { page, size },
  })
}

// 依客戶 ID 查詢交易紀錄（分頁）
export function getTransLogsByCustomerId(customerId, page = 0, size = 10) {
  return api.get(`/api/trans-logs/customer/${customerId}`, {
    params: { page, size },
  })
}

// 查詢最新交易紀錄（分頁）
export function getLatestTransLogs(page = 0, size = 10) {
  return api.get('/api/trans-logs/latest', {
    params: { page, size },
  })
}

// 依客戶 ID + 日期區間查詢交易紀錄（分頁）
export function getTransLogsByCustomerIdAndDateRange(customerId, startDate, endDate, page = 0, size = 10) {
  return api.get(`/api/trans-logs/customer/${customerId}/range`, {
    params: { startDate, endDate, page, size },
  })
}
