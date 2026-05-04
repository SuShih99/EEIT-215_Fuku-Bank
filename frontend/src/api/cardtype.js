
import api from './axios'

// === Card Type API ===

//查詢卡別
export const getCardTypes = async () =>
  (await api.get('/api/admin/card-types')).data.data
//查詢單一
export const getCardTypeById = async (id) =>
  (await api.get(`/api/admin/card-types/${id}`)).data.data
//新增卡別
export const createCardType = async (data) =>
  (await api.post('/api/admin/card-types', data)).data.data
//更新卡別
export const updateCardType = async (id, data) =>
  (await api.put(`/api/admin/card-types/${id}`, data)).data.data
//刪除卡別
export const deleteCardType = async (id) =>
  (await api.delete(`/api/admin/card-types/${id}`)).data
//上傳圖片
export const uploadImage = async (formData) =>
  (await api.post('/api/admin/card-types/upload', formData)).data.data

// ===