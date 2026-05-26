import { ref } from 'vue'
import { defineStore } from 'pinia'

// 管理客戶登入狀態，與員工 auth store 分開保存。
export const useCustomerAuthStore = defineStore('customerAuth', () => {
  const customer = ref(JSON.parse(localStorage.getItem('customer_user')) || null)
  const token = ref(localStorage.getItem('customer_token') || null)
  const isLoggedIn = ref(!!customer.value)

  function setCustomer(data) {
    customer.value = data
    token.value = data.token
    isLoggedIn.value = true
    localStorage.setItem('customer_user', JSON.stringify(data))
    localStorage.setItem('customer_token', data.token)
  }

  function clearCustomer() {
    customer.value = null
    token.value = null
    isLoggedIn.value = false
    localStorage.removeItem('customer_user')
    localStorage.removeItem('customer_token')
  }

  return { customer, token, isLoggedIn, setCustomer, clearCustomer }
})
