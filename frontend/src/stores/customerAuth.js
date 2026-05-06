import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * Customer Auth Store — 管理客戶登入狀態
 *
 * 登入成功後把後端回傳的 LoginResponse 存在這裡，
 * 供 UserLayout header、路由守衛、權限判斷使用。
 *
 * 與 auth.js (員工 store) 完全獨立，互不干擾。
 */
export const useCustomerAuthStore = defineStore('customerAuth', () => {
  // 目前登入的客戶資料（null = 未登入）
  const customer = ref(JSON.parse(localStorage.getItem('customer_user')) || null)

  // JWT Token
  const token = ref(localStorage.getItem('customer_token') || null)

  // 是否已登入
  const isLoggedIn = ref(!!customer.value)

  /**
   * 設定登入的客戶（登入成功時呼叫）
   */
  function setCustomer(data) {
    customer.value = data
    token.value = data.token
    isLoggedIn.value = true
    localStorage.setItem('customer_user', JSON.stringify(data))
    localStorage.setItem('customer_token', data.token)
  }

  /**
   * 清除客戶（登出時呼叫）
   */
  function clearCustomer() {
    customer.value = null
    token.value = null
    isLoggedIn.value = false
    localStorage.removeItem('customer_user')
    localStorage.removeItem('customer_token')
  }

  return { customer, token, isLoggedIn, setCustomer, clearCustomer }
})
