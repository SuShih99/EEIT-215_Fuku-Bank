import api from './axios'

// POST /api/customer/account-applications，formData 含文字欄位與證件圖片。
export function submitAccountApplication(formData) {
  return api.post('/api/customer/account-applications', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }).then(res => res.data.data)
}

export function getMyAccountApplications() {
  return api.get('/api/customer/account-applications')
    .then(res => res.data.data)
}

// 管理端開戶申請 API
export function getAccountApplications(params = {}) {
  return api.get('/api/admin/account-applications', { params })
    .then(res => res.data.data)
}

export function getAccountApplicationById(id) {
  return api.get(`/api/admin/account-applications/${id}`)
    .then(res => res.data.data)
}

export function approveAccountApplication(id) {
  return api.patch(`/api/admin/account-applications/${id}/approve`)
    .then(res => res.data.data)
}

export function supplementAccountApplication(id, reason) {
  return api.patch(`/api/admin/account-applications/${id}/supplement`, { reason })
    .then(res => res.data.data)
}

export function rejectAccountApplication(id, reason) {
  return api.patch(`/api/admin/account-applications/${id}/reject`, { reason })
    .then(res => res.data.data)
}

export function getAccountApplicationsStats() {
  return api.get('/api/admin/account-applications/stats')
    .then(res => res.data.data)
}
