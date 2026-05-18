import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * Auth Store — 管理員工登入狀態
 *
 * 登入成功後把後端回傳的 AuthEmpResponse 存在這裡，
 * 供 Layout header、路由守衛、權限判斷使用。
 */
export const useAuthStore = defineStore('auth', () => {
  // 目前登入的員工資料（null = 未登入）
  const user = ref(JSON.parse(localStorage.getItem('auth_user')) || null)

  // 是否已登入
  const isLoggedIn = ref(!!user.value)

  /**
   * 設定登入的使用者（登入成功時呼叫）
   */
  function setUser(userData) {
    const { token, ...profile } = userData
    if (token) {
      localStorage.setItem('auth_token', token)
    }

    user.value = profile
    isLoggedIn.value = true
    localStorage.setItem('auth_user', JSON.stringify(profile))
  }

  /**
   * 清除使用者（登出時呼叫）
   */
  function clearUser() {
    user.value = null
    isLoggedIn.value = false
    localStorage.removeItem('auth_user')
    localStorage.removeItem('auth_token')
  }

  return { user, isLoggedIn, setUser, clearUser }
})
