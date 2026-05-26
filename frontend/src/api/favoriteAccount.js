import api from './axios'

const BASE = '/api/customer/favorite-accounts'

export async function getFavoriteAccounts() {
  const res = await api.get(BASE)
  return res.data.data
}

// data: { accountNumber, bankCode, alias, bankName }
export async function addFavoriteAccount(data) {
  const res = await api.post(BASE, data)
  return res.data.data
}

// data: { alias, bankCode, bankName }
export async function updateFavoriteAccount(id, data) {
  const res = await api.put(`${BASE}/${id}`, data)
  return res.data.data
}

export async function deleteFavoriteAccount(id) {
  const res = await api.delete(`${BASE}/${id}`)
  return res.data
}
