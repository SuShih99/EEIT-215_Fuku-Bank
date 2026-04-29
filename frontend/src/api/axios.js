import axios from 'axios'

export const BASE_URL = 'http://localhost:8080'

// 建立一個 axios 實例，統一設定後端 API 的基礎 URL
const api = axios.create({
  baseURL: BASE_URL,
})

export default api