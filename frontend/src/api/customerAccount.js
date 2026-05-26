import api from './axios'

// 客戶端帳戶 API，後端依 JWT 取得目前登入客戶。

export async function getMyAccounts() {
  const res = await api.get('/api/customer/accounts')
  return res.data.data
}

// params: { accountNumber, startDate, endDate, page, size }
export async function getMyTransactions(params = {}) {
  const res = await api.get('/api/customer/transactions', { params })
  return res.data.data
}

export async function getTransferBanks() {
  const res = await api.get('/api/customer/transfer-banks')
  return res.data.data
}

export async function downloadPassbookPdf(accountNumber) {
  const res = await api.get(`/api/customer/accounts/${accountNumber}/passbook/pdf`, {
    responseType: 'blob',
  })
  return res.data
}

// data: { fromAccountNumber, toBankCode, toAccountNumber, amount, note }
export async function doTransfer(data) {
  const res = await api.post('/api/customer/transfers', data)
  return res.data.data
}

export async function requestTransferOtp() {
  const res = await api.post('/api/customer/transfers/otp')
  return res.data.data
}

export async function getExchangeRates() {
  const res = await api.get('/api/public/exchange-rates')
  return res.data.data
}

// data: { fromAccountNumber, toAccountNumber, fromAmount, note }
export async function doExchange(data) {
  const res = await api.post('/api/customer/exchanges', data)
  return res.data.data
}
