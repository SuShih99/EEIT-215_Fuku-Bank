<template>
  <div class="login-wrapper">
    <div class="login-card">
      <!-- Logo 區 -->
      <div class="login-header">
        <div class="logo-icon">
          <BankOutlined style="font-size: 36px; color: #1677ff" />
        </div>
        <h1 class="login-title">登入</h1>
        <p class="login-subtitle">歡迎回到爪哇銀行</p>
      </div>

      <!-- 登入表單 -->
      <a-form :model="form" layout="vertical" @finish="handleLogin" autocomplete="off">
        <a-form-item
          label="使用者帳號"
          name="username"
          :rules="[{ required: true, message: '請輸入使用者帳號' }]"
        >
          <a-input
            v-model:value="form.username"
            size="large"
            placeholder="請輸入帳號"
            @press-enter="handleLogin"
          >
            <template #prefix>
              <UserOutlined style="color: rgba(0, 0, 0, 0.25)" />
            </template>
          </a-input>
        </a-form-item>

        <a-form-item
          label="密碼"
          name="password"
          :rules="[{ required: true, message: '請輸入密碼' }]"
        >
          <a-input-password
            v-model:value="form.password"
            size="large"
            placeholder="請輸入密碼"
            @press-enter="handleLogin"
          >
            <template #prefix>
              <LockOutlined style="color: rgba(0, 0, 0, 0.25)" />
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <div class="btn-group">
            <a-button
              type="primary"
              html-type="submit"
              size="large"
              :loading="loading"
              class="login-btn"
            >
              登入
            </a-button>
            <a-button
              size="large"
              @click="fillTestAccount"
              class="fill-btn"
            >
              <ThunderboltOutlined />
              一鍵帶入
            </a-button>
          </div>
        </a-form-item>
      </a-form>

      <div class="register-link">
        還沒有帳號？<router-link to="/register">立即註冊</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { BankOutlined, UserOutlined, LockOutlined, ThunderboltOutlined } from '@ant-design/icons-vue'
import { customerLogin } from '@/api/customerAuth'
import { useCustomerAuthStore } from '@/stores/customerAuth'

const router = useRouter()
const customerAuthStore = useCustomerAuthStore()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

// 一鍵帶入測試帳密
function fillTestAccount() {
  form.username = 'mingwang85'
  form.password = '123456'
}

async function handleLogin() {
  if (!form.username || !form.password) return

  loading.value = true
  try {
    const res = await customerLogin({
      username: form.username,
      password: form.password,
    })

    const data = res.data.data
    customerAuthStore.setCustomer(data)

    message.success(`歡迎回來，${data.name}！`)
    router.push({ name: 'user-home' })
  } catch (err) {
    message.error(err.response?.data?.message || '登入失敗，請檢查帳號密碼')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
}

.login-card {
  width: 420px;
  padding: 40px 32px 28px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo-icon {
  width: 72px;
  height: 72px;
  margin: 0 auto 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e6f4ff;
  border-radius: 50%;
}

.login-title {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
}

.login-subtitle {
  margin: 4px 0 0;
  font-size: 14px;
  color: #8c8c8c;
}

.btn-group {
  display: flex;
  gap: 12px;
}

.login-btn {
  flex: 1;
}

.fill-btn {
  display: flex;
  align-items: center;
  gap: 4px;
}

.register-link {
  text-align: center;
  margin-top: 16px;
  font-size: 14px;
  color: #8c8c8c;
}

.register-link a {
  color: #1677ff;
  text-decoration: none;
  font-weight: 500;
}

.register-link a:hover {
  text-decoration: underline;
}
</style>