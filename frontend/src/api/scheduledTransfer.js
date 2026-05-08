import api from './axios'

/**
 * 預約轉帳 API — 基於 JWT 驗證
 */

const BASE = '/api/customer/scheduled-transfers'

/**
 * 取得我的預約轉帳列表
 */
export async function getScheduledTransfers() {
  const res = await api.get(BASE)
  return res.data.data
}

/**
 * 建立預約轉帳
 * @param {Object} data - { fromAccountNumber, toAccountNumber, amount, scheduledDate, note }
 */
export async function createScheduledTransfer(data) {
  const res = await api.post(BASE, data)
  return res.data.data
}

/**
 * 取消預約轉帳
 * @param {Number} id - 預約轉帳 ID
 */
export async function cancelScheduledTransfer(id) {
  const res = await api.patch(`${BASE}/${id}/cancel`)
  return res.data
}
