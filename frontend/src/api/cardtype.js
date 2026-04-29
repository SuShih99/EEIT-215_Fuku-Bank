import api from './axios'

// === Card Type API ===

// 查詢所有卡別
export function getCardTypes() {
  return api.get('/api/admin/card-types')
}

// 查詢單一卡別
export function getCardTypeById(id) {
  return api.get(`/api/admin/card-types/${id}`)
}

// 新增卡別
export function createCardType(data) {
  return api.post('/api/admin/card-types', data)
}

// 更新卡別
export function updateCardType(id, data) {
  return api.put(`/api/admin/card-types/${id}`, data)
}

// 刪除卡別
export function deleteCardType(id) {
  return api.delete(`/api/admin/card-types/${id}`)
}
//新增圖片
export function uploadImage(formData) {
  return api.post('/api/admin/card-types/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
  }

// ===