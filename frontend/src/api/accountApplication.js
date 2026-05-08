import api from './axios'

/**
 * 客戶端：提交開戶申請（multipart/form-data）
 * @param {FormData} formData - 包含文字欄位 + 三張證件圖片
 */
export function submitAccountApplication(formData) {
  return api.post('/api/customer/account-applications', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }).then(res => res.data.data)
}

/**
 * 客戶端：查詢我的開戶申請
 */
export function getMyAccountApplications() {
  return api.get('/api/customer/account-applications')
    .then(res => res.data.data)
}

// =========================================================
// 管理端 API
// =========================================================

/**
 * 管理端：查詢開戶申請列表（分頁 + 狀態篩選）
 */
export function getAccountApplications(params = {}) {
  return api.get('/api/admin/account-applications', { params })
    .then(res => res.data.data)
}

/**
 * 管理端：查詢單筆申請詳情
 */
export function getAccountApplicationById(id) {
  return api.get(`/api/admin/account-applications/${id}`)
    .then(res => res.data.data)
}

/**
 * 管理端：核准開戶申請
 */
export function approveAccountApplication(id) {
  return api.patch(`/api/admin/account-applications/${id}/approve`)
    .then(res => res.data.data)
}

/**
 * 管理端：駁回開戶申請
 */
export function rejectAccountApplication(id, reason) {
  return api.patch(`/api/admin/account-applications/${id}/reject`, { reason })
    .then(res => res.data.data)
}
