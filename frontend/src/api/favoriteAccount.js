import api from './axios'

/**
 * 常用帳號 API — 基於 JWT 驗證
 */

const BASE = '/api/customer/favorite-accounts'

/**
 * 取得我的常用帳號列表
 */
export async function getFavoriteAccounts() {
  const res = await api.get(BASE)
  return res.data.data
}

/**
 * 新增常用帳號
 * @param {Object} data - { accountNumber, bankCode, alias, bankName }
 */
export async function addFavoriteAccount(data) {
  const res = await api.post(BASE, data)
  return res.data.data
}

/**
 * 更新常用帳號
 * @param {Number} id - 常用帳號 ID
 * @param {Object} data - { alias, bankCode, bankName }
 */
export async function updateFavoriteAccount(id, data) {
  const res = await api.put(`${BASE}/${id}`, data)
  return res.data.data
}

/**
 * 刪除常用帳號
 * @param {Number} id - 常用帳號 ID
 */
export async function deleteFavoriteAccount(id) {
  const res = await api.delete(`${BASE}/${id}`)
  return res.data
}
