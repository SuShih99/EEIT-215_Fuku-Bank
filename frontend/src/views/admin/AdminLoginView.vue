<template>
  <div class="login-wrapper">
    <div class="login-card">
      <div class="login-header">
        <img src="/logo.webp" alt="JavaBank" class="login-logo" />
        <p class="login-subtitle">後台管理系統</p>
      </div>

      <a-form
        :model="form"
        layout="vertical"
        @finish="handleLogin"
        autocomplete="off"
      >
        <a-form-item
          label="Email"
          name="email"
          :rules="[
            { required: true, message: '請輸入 Email' },
            { type: 'email', message: '請輸入正確的 Email 格式' }
          ]"
        >
          <a-input
            v-model:value="form.email"
            placeholder="請輸入 Email（如 name@javabank.com）"
            class="custom-input"
            @press-enter="handleLogin"
          >
            <template #prefix>
              <MailOutlined style="color: rgba(92, 107, 95, 0.5)" />
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
            placeholder="請輸入密碼"
            class="custom-input"
            @press-enter="handleLogin"
          >
            <template #prefix>
              <LockOutlined style="color: rgba(92, 107, 95, 0.5)" />
            </template>
          </a-input-password>
        </a-form-item>

        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            block
            class="btn-submit-rounded"
            :loading="loading"
          >
            登入
          </a-button>
        </a-form-item>
      </a-form>

      <div class="quick-login">
        <a-divider style="margin: 16px 0 24px">
          <span style="color: #999; font-size: 13px">快速登入</span>
        </a-divider>
        <div class="quick-btn-grid">
          <a-button
            v-for="acc in testAccounts"
            :key="acc.email"
            @click="fillAccount(acc)"
            class="quick-btn"
          >
            <span class="quick-role">{{ acc.role }}</span>
            <span class="quick-name">{{ acc.name }}</span>
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { MailOutlined, LockOutlined } from '@ant-design/icons-vue'
import { login } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)

const form = reactive({
  email: '',
  password: '',
})

// CISO=資安長(Lvl4) / CFDM=中階主管(Lvl2) / CFSO=一般職員(Lvl0)
const testAccounts = [
  { name: '系統管理員', role: 'CISO', email: 'wenhua.cheng@javabank.com' },
  { name: '主管', role: 'CFDM', email: 'shufen.wang@javabank.com' },
  { name: '職員', role: 'CFSO', email: 'chiahao.lin@javabank.com' },
]

function fillAccount(acc) {
  form.email = acc.email
  form.password = '123456'
}

// 角色 roleCode → 顯示名稱映射表
const roleDisplayName = {
  CISO: '系統管理員',
  CFDM: '主管',
  CFSO: '職員',
}

async function handleLogin() {
  if (!form.email || !form.password) return
  loading.value = true
  try {
    const res = await login({ email: form.email, password: form.password })
    const userData = res.data.data
    authStore.setUser(userData)
    const displayName = roleDisplayName[userData.roleCode] || userData.empName
    message.success(`歡迎回來，${displayName}！`)
    router.push({ name: 'admin-home' })
  } catch (err) {
    const errMsg = err.response?.data?.message || ''
    if (errMsg.includes('停權') || errMsg.includes('停用') || errMsg.includes('SUSPENDED')) {
      message.error('此帳號已被停權，請洽管理員')
    } else {
      message.error('帳號密碼錯誤')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  --primary-color: #5C6B5F;
  --primary-hover: #4A574D;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f6fa;
}

.login-card {
  width: 440px;
  padding: 48px 40px;
  background: #fff;
  border-radius: 32px;
  box-shadow: 0 24px 64px rgba(92, 107, 95, 0.06);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}

.login-logo {
  width: 160px;
  height: 160px;
  margin: 0 auto 20px;
  object-fit: contain;
}

.login-subtitle {
  margin: 0;
  font-size: 22px;
  color: #1a1a2e;
  font-weight: 700;
  letter-spacing: 2px;
}

.custom-input {
  border-radius: 12px !important;
  border: 1px solid #d9d9d9 !important;
  padding: 6px 14px !important;
  height: 48px !important;
  display: flex;
  align-items: center;
  background: #fff !important;
}

.custom-input :deep(input),
.custom-input :deep(.ant-input) {
  border: none !important;
  outline: none !important;
  box-shadow: none !important;
  background: transparent !important;
  height: 100% !important;
}

.custom-input:hover,
.custom-input:focus-within,
.custom-input:deep(.ant-input-affix-wrapper-focused) {
  border-color: var(--primary-color) !important;
  box-shadow: 0 0 0 2px rgba(92, 107, 95, 0.1) !important;
}

.btn-submit-rounded {
  height: 52px !important;
  background-color: var(--primary-color) !important;
  border-color: var(--primary-color) !important;
  border-radius: 26px !important;
  font-weight: 600;
  font-size: 16px;
  margin-top: 12px;
}

.btn-submit-rounded:hover {
  background-color: var(--primary-hover) !important;
}

.quick-btn-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.quick-btn {
  height: 48px !important;
  padding: 0 16px !important;
  border-radius: 12px !important;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  border: 1px solid #e0e0e0 !important;
  background: transparent !important;
  transition: all 0.3s ease;
}

.quick-btn:hover {
  border-color: var(--primary-color) !important;
  background-color: rgba(92, 107, 95, 0.04) !important;
}

.quick-btn:hover .quick-name {
  color: var(--primary-color) !important;
}

.quick-role {
  font-size: 12px;
  color: var(--primary-color);
  font-weight: 700;
  min-width: 44px;
  text-align: left;
}

.quick-name {
  font-size: 14px;
  color: #555;
  font-weight: 500;
  transition: color 0.3s ease;
}

@media (max-width: 560px) {
  .login-wrapper {
    align-items: flex-start;
    padding: 24px 16px;
  }

  .login-card {
    width: 100%;
    max-width: 420px;
    padding: 28px 20px;
    border-radius: 20px;
  }

  .login-header {
    margin-bottom: 24px;
  }

  .login-logo {
    width: 112px;
    height: 112px;
    margin-bottom: 14px;
  }

  .login-subtitle {
    font-size: 19px;
    letter-spacing: 1px;
  }

  .quick-btn-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .quick-btn {
    justify-content: center;
  }
}
</style>
