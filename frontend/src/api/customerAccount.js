import api from './axios'

/**
 * 客戶端帳戶 API — 基於 JWT 驗證，自動取得當前登入客戶的資料
 */

/**
 * 取得我的所有帳戶
 * GET /api/customer/accounts
 */
export async function getMyAccounts() {
  const res = await api.get('/api/customer/accounts')
  return res.data.data
}

/**
 * 取得我的交易紀錄（分頁 + 篩選）
 * GET /api/customer/transactions
 * @param {Object} params - { accountNumber, startDate, endDate, page, size }
 */
export async function getMyTransactions(params = {}) {
  const res = await api.get('/api/customer/transactions', { params })
  return res.data.data
}

/**
 * 取得國內轉帳銀行選單
 * GET /api/customer/transfer-banks
 */
export async function getTransferBanks() {
  const res = await api.get('/api/customer/transfer-banks')
  return res.data.data
}

/**
 * 下載電子存摺 PDF
 * GET /api/customer/accounts/{accountNumber}/passbook/pdf
 */
export async function downloadPassbookPdf(accountNumber) {
  const res = await api.get(`/api/customer/accounts/${accountNumber}/passbook/pdf`, {
    responseType: 'blob',
  })
  return res.data
}

/**
 * 客戶轉帳
 * POST /api/customer/transfers
 * @param {Object} data - { fromAccountNumber, toBankCode, toAccountNumber, amount, note }
 */
export async function doTransfer(data) {
  const res = await api.post('/api/customer/transfers', data)
  return res.data.data
}

/**
 * 取得匯率
 * GET /api/public/exchange-rates
 */
export async function getExchangeRates() {
  const res = await api.get('/api/public/exchange-rates')
  return res.data.data
}

/**
 * 客戶換匯
 * POST /api/customer/exchanges
 * @param {Object} data - { fromAccountNumber, toAccountNumber, fromAmount, note }
 */
export async function doExchange(data) {
  const res = await api.post('/api/customer/exchanges', data)
  return res.data.data
}
