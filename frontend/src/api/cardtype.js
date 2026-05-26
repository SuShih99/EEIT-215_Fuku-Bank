import api from './axios'

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

const unwrap = (res) => {
  const payload = res.data

  if (payload?.success === false) {
    throw new Error(payload.message || 'API request failed')
  }

  return payload?.data ?? payload
}

const toArray = (value, label = '資料') => {
  if (Array.isArray(value)) return value
  if (Array.isArray(value?.content)) return value.content
  throw new Error(`${label}回傳格式錯誤`)
}

const getWithRetry = async (url, attempts = 2) => {
  let lastError

  for (let i = 0; i < attempts; i += 1) {
    try {
      return unwrap(await api.get(url))
    } catch (error) {
      lastError = error
      const status = error.response?.status
      const canRetry = !status || status >= 500

      if (!canRetry || i === attempts - 1) break

      await sleep(250)
    }
  }

  throw lastError
}

export const getCardTypes = async () => toArray(await getWithRetry('/api/admin/card-types'), '卡別')

export const getCardTypeById = async (id) =>
  unwrap(await api.get(`/api/admin/card-types/${id}`))

export const createCardType = async (data) =>
  unwrap(await api.post('/api/admin/card-types', data))

export const updateCardType = async (id, data) =>
  unwrap(await api.put(`/api/admin/card-types/${id}`, data))

export const deleteCardType = async (id) =>
  (await api.delete(`/api/admin/card-types/${id}`)).data

export const uploadImage = async (formData) =>
  unwrap(await api.post('/api/admin/card-types/upload', formData))

export const getUserCardTypes = async () => toArray(await getWithRetry('/api/public/card-types'), '信用卡別')

export const getUserCardTypeById = async (id) =>
  unwrap(await api.get(`/api/public/card-types/${id}`))
