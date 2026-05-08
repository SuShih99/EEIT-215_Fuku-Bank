import api from './axios'

// === 統一管理 Customer 的 API 設定 ===

// 查詢客戶（支援 keyword 模糊搜尋）
// GET /api/customers           → 全部
// GET /api/customers?keyword=王 → 姓名含「王」的
export function getCustomers(keyword) {
  return api.get('/api/customers', {
    params: keyword ? { keyword } : {},
  })
}

// 新增客戶
export function createCustomer(data) {
  return api.post('/api/customers', data)
}

// 修改客戶聯絡資訊
export function updateCustomer(customerId, data) {
  return api.put(`/api/customers/${customerId}`, data)
}

// 註銷客戶（軟刪除）
export function deactivateCustomer(customerId) {
  return api.put(`/api/customers/${customerId}/deactivate`)
}

// 重新啟用客戶
export function activateCustomer(customerId) {
  return api.put(`/api/customers/${customerId}/activate`)
}

// 一鍵帶入測試資料
export function seedCustomers() {
  return api.post('/api/customers/seed')
}