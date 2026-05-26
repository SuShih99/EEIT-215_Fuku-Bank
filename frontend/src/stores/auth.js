import { ref } from 'vue'
import { defineStore } from 'pinia'

// 管理員工登入狀態，供 layout、路由守衛與權限判斷使用。
export const useAuthStore = defineStore('auth', () => {
  const user = ref(JSON.parse(localStorage.getItem('auth_user')) || null)
  const isLoggedIn = ref(!!user.value)

  function setUser(userData) {
    const { token, ...profile } = userData
    if (token) {
      localStorage.setItem('auth_token', token)
    }

    user.value = profile
    isLoggedIn.value = true
    localStorage.setItem('auth_user', JSON.stringify(profile))
  }

  function clearUser() {
    user.value = null
    isLoggedIn.value = false
    localStorage.removeItem('auth_user')
    localStorage.removeItem('auth_token')
  }

  return { user, isLoggedIn, setUser, clearUser }
})
