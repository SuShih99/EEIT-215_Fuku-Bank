import axios from 'axios'
import router from '@/router'

export const BASE_URL = 'http://localhost:8080'

// 建立一個 axios 實例，統一設定後端 API 的基礎 URL
const api = axios.create({
  baseURL: BASE_URL,
  withCredentials: true, // ← 帶上 Session Cookie，Spring Security 才認得（管理端用）
})

// === 請求攔截器：自動帶入 JWT Token（客戶端用） ===
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('customer_token')
    const adminToken = localStorage.getItem('auth_token')
    const requestUrl = config.url || ''
    const isCustomerApi =
      requestUrl.startsWith('/api/customer/') ||
      requestUrl.startsWith('/user/') ||
      requestUrl.startsWith('/api/loan-applications/') ||
      requestUrl.startsWith('/api/loan-accounts/') ||
      requestUrl.startsWith('/api/loan-documents/')
    const isAdminApi =
      requestUrl.startsWith('/api/') &&
      !isCustomerApi &&
      !requestUrl.startsWith('/api/public/') &&
      !requestUrl.startsWith('/api/linepay/')

    if (token && isCustomerApi) {
      config.headers.Authorization = `Bearer ${token}`
    } else if (adminToken && isAdminApi) {
      config.headers.Authorization = `Bearer ${adminToken}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// === 回應攔截器：遇到 401 自動跳轉登入頁 ===
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const currentRoute = router.currentRoute.value

      // 判斷是客戶端還是管理端
      if (currentRoute.path.startsWith('/user')) {
        // 客戶端 401 → 清掉 token，導向客戶登入
        localStorage.removeItem('customer_user')
        localStorage.removeItem('customer_token')
        if (currentRoute.name !== 'user-login') {
          router.push({ name: 'user-login' })
        }
      } else {
        // 管理端 401 → 清掉 session，導向管理登入
        localStorage.removeItem('auth_user')
        localStorage.removeItem('auth_token')
        if (currentRoute.name !== 'admin-login') {
          router.push({ name: 'admin-login' })
        }
      }
    }
    return Promise.reject(error)
  },
)

export default api
