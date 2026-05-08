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
 * 客戶轉帳
 * POST /api/customer/transfers
 * @param {Object} data - { fromAccountNumber, toAccountNumber, amount, note }
 */
export async function doTransfer(data) {
  const res = await api.post('/api/customer/transfers', data)
  return res.data.data
}
