<template>
  <div class="customer-page reset-page">
    <main class="reset-container">
      <!-- Logo -->
      <a class="reset-logo" href="/" @click.prevent="$router.push('/')">
        <div class="jb-stamp">福</div>
        <span class="logo-label">JAVA_BANK</span>
      </a>

      <section class="reset-card jb-card">
        <!-- 重設表單 -->
        <template v-if="!success">
          <div class="reset-icon-wrap" aria-hidden="true">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="color: var(--primary)">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/><path d="M7 11V7a5 5 0 0110 0v4"/>
            </svg>
          </div>
          <h1 class="reset-title">重設密碼</h1>
          <p class="reset-subtitle">請輸入您的新密碼</p>

          <form @submit.prevent="handleReset" novalidate>
            <div class="jb-form-item">
              <label for="reset-pwd" class="jb-label">新密碼</label>
              <div class="jb-password-wrap">
                <input
                  id="reset-pwd"
                  v-model="form.newPassword"
                  :type="showPwd1 ? 'text' : 'password'"
                  class="jb-input"
                  placeholder="請輸入新密碼（至少 6 個字元）"
                  minlength="6"
                  required
                />
                <button
                  type="button"
                  class="jb-password-toggle"
                  :aria-label="showPwd1 ? '隱藏密碼' : '顯示密碼'"
                  @click="showPwd1 = !showPwd1"
                >
                  <svg v-if="!showPwd1" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12z"/><circle cx="12" cy="12" r="3"/></svg>
                  <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                </button>
              </div>
            </div>

            <div class="jb-form-item">
              <label for="reset-confirm" class="jb-label">確認新密碼</label>
              <div class="jb-password-wrap">
                <input
                  id="reset-confirm"
                  v-model="form.confirmPassword"
                  :type="showPwd2 ? 'text' : 'password'"
                  class="jb-input"
                  placeholder="請再次輸入新密碼"
                  required
                />
                <button
                  type="button"
                  class="jb-password-toggle"
                  :aria-label="showPwd2 ? '隱藏密碼' : '顯示密碼'"
                  @click="showPwd2 = !showPwd2"
                >
                  <svg v-if="!showPwd2" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12z"/><circle cx="12" cy="12" r="3"/></svg>
                  <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                </button>
              </div>
              <p v-if="mismatch" class="jb-field-error">兩次輸入的密碼不一致</p>
            </div>

            <button
              type="submit"
              class="jb-btn jb-btn-primary jb-btn-block jb-btn-lg"
              :disabled="loading || mismatch"
              style="margin-top: var(--space-2)"
            >
              <span v-if="loading" class="jb-spinner" style="width:18px;height:18px;"></span>
              <span>{{ loading ? '處理中...' : '確認重設' }}</span>
            </button>
          </form>
        </template>

        <!-- 成功畫面 -->
        <template v-else>
          <div class="success-section">
            <div class="success-icon" aria-hidden="true">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color: var(--primary)">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
            </div>
            <h2 class="success-title">密碼已成功重設</h2>
            <p class="success-sub">請使用新密碼登入您的帳戶</p>
            <button class="jb-btn jb-btn-primary jb-btn-lg" @click="$router.push('/login')">
              前往登入
            </button>
          </div>
        </template>
      </section>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { customerResetPassword } from '@/api/customerAuth'

const route = useRoute()
const loading = ref(false)
const success = ref(false)
const showPwd1 = ref(false)
const showPwd2 = ref(false)

const form = reactive({
  newPassword: '',
  confirmPassword: '',
})

const mismatch = computed(() =>
  form.confirmPassword.length > 0 && form.confirmPassword !== form.newPassword
)

async function handleReset() {
  if (mismatch.value) return

  const token = route.query.token
  if (!token) {
    alert('無效的重設連結')
    return
  }

  loading.value = true
  try {
    await customerResetPassword({
      token,
      newPassword: form.newPassword,
    })
    success.value = true
  } catch (err) {
    alert(err.response?.data?.message || '密碼重設失敗')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.reset-page {
  min-height: 100vh;
  background: var(--bg-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-6) var(--space-3);
}

.reset-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 440px;
}

.reset-logo {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
  text-decoration: none;
  color: inherit;
}

.logo-label {
  font-family: var(--font-heading);
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 2px;
}

.reset-card {
  width: 100%;
  text-align: center;
}

.reset-icon-wrap {
  margin-bottom: var(--space-3);
}

.reset-title {
  margin-bottom: var(--space-2);
}

.reset-subtitle {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin-bottom: var(--space-5);
}

/* 表單靠左 */
.reset-card form {
  text-align: left;
}

/* 成功畫面 */
.success-section {
  padding: var(--space-4) 0;
}

.success-icon {
  margin-bottom: var(--space-4);
}

.success-title {
  margin-bottom: var(--space-2);
}

.success-sub {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin-bottom: var(--space-5);
}
</style>
