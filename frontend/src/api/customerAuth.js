import api from './axios'

// === 統一管理 Customer Auth 的 API 設定 ===

// 客戶註冊
export function customerRegister(data) {
  return api.post('/api/customer/auth/register', data)
}

// 客戶登入
export function customerLogin(data) {
  return api.post('/api/customer/auth/login', data)
}

// 電子郵件驗證
export function customerVerifyEmail(token) {
  return api.get('/api/customer/auth/verify-email', {
    params: { token },
  })
}

// 取得目前客戶資訊
export function customerGetProfile() {
  return api.get('/api/customer/auth/me')
}

// 修改個人資料
export function customerUpdateProfile(data) {
  return api.put('/api/customer/auth/profile', data)
}

// 上傳大頭照
export function customerUploadAvatar(formData) {
  return api.post('/api/customer/auth/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// 請求密碼重設（發送 Email）
export function customerRequestReset(data) {
  return api.post('/api/customer/auth/request-reset', data)
}

// 執行密碼重設
export function customerResetPassword(data) {
  return api.post('/api/customer/auth/reset-password', data)
}

// 一鍵帶入客戶認證測試資料
export function seedCustomerAuth() {
  return api.post('/api/customer/auth/seed')
}

// 查詢登入紀錄
export async function getCustomerLoginLogs() {
  const res = await api.get('/api/customer/security/login-logs')
  return res.data.data
}

// 查詢授權裝置
export async function getCustomerDevices() {
  const res = await api.get('/api/customer/security/devices')
  return res.data.data
}

// 移除裝置授權
export function revokeCustomerDevice(deviceId) {
  return api.delete(`/api/customer/security/devices/${deviceId}`)
}
