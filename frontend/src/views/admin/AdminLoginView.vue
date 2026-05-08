<template>
  <div class="login-wrapper">
    <div class="login-card">
      <div class="login-header">
        <img src="/logo.png" alt="JavaBank" class="login-logo" />
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

// 快速登入帳號（直接使用完整 email）
const testAccounts = [
  { name: '鄭文華', role: 'CISO', email: 'wenhua.cheng@javabank.com' },
  { name: '林家豪', role: 'CFSO', email: 'chiahao.lin@javabank.com' },
]

function fillAccount(acc) {
  form.email = acc.email
  form.password = '123456'
}

async function handleLogin() {
  if (!form.email || !form.password) return
  loading.value = true
  try {
    const res = await login({ email: form.email, password: form.password })
    const userData = res.data.data
    authStore.setUser(userData)
    message.success(`歡迎回來，${userData.empName}！`)
    router.push({ name: 'admin-home' })
  } catch (err) {
    message.error(err.response?.data?.message || '登入失敗，請檢查 Email 與密碼')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* =========================================
   核心佈局與變數
========================================= */
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

/* =========================================
   解決痛點 1：徹底根除雙層黑框
========================================= */
/* 設定外層容器的樣式 */
.custom-input {
  border-radius: 12px !important;
  border: 1px solid #d9d9d9 !important;
  padding: 6px 14px !important;
  height: 48px !important; /* 強制加高輸入框 */
  display: flex;
  align-items: center;
  background: #fff !important;
}

/* 暴力擊破：強制拔除內部所有原生 input 的邊框與背景 */
.custom-input :deep(input),
.custom-input :deep(.ant-input) {
  border: none !important;
  outline: none !important;
  box-shadow: none !important;
  background: transparent !important;
  height: 100% !important;
}

/* Hover 與 Focus 時外框變綠色 */
.custom-input:hover,
.custom-input:focus-within,
.custom-input:deep(.ant-input-affix-wrapper-focused) {
  border-color: var(--primary-color) !important;
  box-shadow: 0 0 0 2px rgba(92, 107, 95, 0.1) !important;
}

/* =========================================
   解決痛點 2：登入按鈕與快速登入變高變寬
========================================= */
.btn-submit-rounded {
  height: 52px !important; /* 主要按鈕加高 */
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
  grid-template-columns: repeat(2, 1fr);
  gap: 16px; /* 增加按鈕之間的空隙 */
}

.quick-btn {
  height: 48px !important; /* 強制加高快速登入按鈕！ */
  padding: 0 16px !important; /* 左右撐開 */
  border-radius: 12px !important;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  border: 1px solid #e0e0e0 !important; /* 淡淡的實線邊框 */
  background: transparent !important;
  transition: all 0.3s ease;
}

/* Hover 時變墨綠色 */
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
  min-width: 44px; /* 讓前面的英文職稱對齊 */
  text-align: left;
}

.quick-name {
  font-size: 14px;
  color: #555;
  font-weight: 500;
  transition: color 0.3s ease;
}
</style>