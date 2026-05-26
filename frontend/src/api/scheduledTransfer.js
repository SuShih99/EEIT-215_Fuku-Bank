import api from './axios'

const BASE = '/api/customer/scheduled-transfers'

export async function getScheduledTransfers() {
  const res = await api.get(BASE)
  return res.data.data
}

// data: { fromAccountNumber, toBankCode, toAccountNumber, amount, scheduledDate, note }
export async function createScheduledTransfer(data) {
  const res = await api.post(BASE, data)
  return res.data.data
}

export async function requestScheduledTransferOtp() {
  const res = await api.post(`${BASE}/otp`)
  return res.data.data
}

export async function cancelScheduledTransfer(id) {
  const res = await api.patch(`${BASE}/${id}/cancel`)
  return res.data
}
