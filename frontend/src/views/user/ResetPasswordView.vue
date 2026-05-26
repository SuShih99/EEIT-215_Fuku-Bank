<template>
  <div class="customer-page reset-page">
    <main class="reset-container">
      <!-- Logo -->
      <div class="reset-logo">
        <JbLogo size="md" clickable @navigate="$router.push('/')" />
      </div>

      <section class="reset-card jb-card">
        <!-- 步驟 1：身分驗證 (無 token 且未成功發送) -->
        <template v-if="!token && !requestSuccess">
          <div class="reset-icon-wrap" aria-hidden="true">
            <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" style="color: var(--primary)">
              <circle cx="12" cy="12" r="10"/><path d="M12 16v-4"/><path d="M12 8h.01"/>
            </svg>
          </div>
          <h1 class="reset-title">驗證身分</h1>
          <p class="reset-subtitle">請輸入您的個人資料以進行驗證</p>

          <form @submit.prevent="handleRequestReset" novalidate>
            <div class="jb-form-item">
              <label for="req-idnumber" class="jb-label">身分證字號</label>
              <input
                id="req-idnumber"
                v-model="reqForm.idNumber"
                type="text"
                class="jb-input"
                placeholder="請輸入身分證字號"
                required
              />
            </div>
            <div class="jb-form-item">
              <label for="req-birthday" class="jb-label">出生年月日</label>
              <input
                id="req-birthday"
                v-model="reqForm.birthday"
                type="date"
                class="jb-input"
                required
              />
            </div>
            <div class="jb-form-item">
              <label for="req-email" class="jb-label">電子郵件</label>
              <input
                id="req-email"
                v-model="reqForm.email"
                type="email"
                class="jb-input"
                placeholder="請輸入註冊的電子郵件"
                required
              />
            </div>

            <button
              type="submit"
              class="jb-btn jb-btn-primary jb-btn-block jb-btn-lg"
              :disabled="loading"
              style="margin-top: var(--space-2)"
            >
              <span v-if="loading" class="jb-spinner" style="width:18px;height:18px;"></span>
              <span>{{ loading ? '處理中...' : '發送重設信件' }}</span>
            </button>
            <button
              type="button"
              class="jb-btn jb-btn-secondary jb-btn-block jb-btn-lg"
              style="margin-top: var(--space-3)"
              @click="$router.push('/login')"
            >
              返回登入
            </button>
            <button
              type="button"
              class="jb-btn jb-btn-secondary jb-btn-block jb-btn-lg"
              style="margin-top: var(--space-3)"
              @click="fillTestAccount"
            >
              一鍵帶入測試帳號
            </button>
          </form>
        </template>

        <!-- 步驟 1 成功：已發送信件 -->
        <template v-else-if="!token && requestSuccess">
          <div class="success-section">
            <div class="success-icon" aria-hidden="true">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="color: var(--primary)">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
            </div>
            <h2 class="success-title">重設信已發送</h2>
            <p class="success-sub">請前往您的電子信箱點擊密碼重設連結。（※ 連結有效期限為 30 分鐘）</p>
            <button class="jb-btn jb-btn-primary jb-btn-lg" @click="$router.push('/login')">
              返回登入
            </button>
          </div>
        </template>

        <!-- 步驟 2：設定新密碼 (有 token 且未重設成功) -->
        <template v-else-if="token && !success">
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

        <!-- 步驟 2 成功：密碼重設完成 -->
        <template v-else-if="token && success">
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

    <!-- Custom Toast -->
    <transition name="toast-fade">
      <div v-if="toast.visible" class="jb-toast" :class="`toast-${toast.type}`">
        {{ toast.text }}
      </div>
    </transition>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { customerResetPassword, customerRequestReset } from '@/api/customerAuth'
import JbLogo from '@/components/JbLogo.vue'

const route = useRoute()
const loading = ref(false)
const success = ref(false)
const requestSuccess = ref(false)
const showPwd1 = ref(false)
const showPwd2 = ref(false)
const token = ref('')

const toast = reactive({ visible: false, text: '', type: 'error', timer: null })
function showToast(text, type = 'error') {
  toast.text = text
  toast.type = type
  toast.visible = true
  if (toast.timer) clearTimeout(toast.timer)
  toast.timer = setTimeout(() => { toast.visible = false }, 3000)
}

onMounted(() => {
  token.value = route.query.token || ''
})

const reqForm = reactive({
  idNumber: '',
  birthday: '',
  email: '',
})

function fillTestAccount() {
  reqForm.idNumber = 'A123456789'
  reqForm.birthday = '1985-05-15'
  reqForm.email = 'ming.wang@email.com'
}

const form = reactive({
  newPassword: '',
  confirmPassword: '',
})

const mismatch = computed(() =>
  form.confirmPassword.length > 0 && form.confirmPassword !== form.newPassword
)

async function handleRequestReset() {
  if (!reqForm.idNumber || !reqForm.birthday || !reqForm.email) {
    showToast('請填寫完整驗證資料', 'warning')
    return
  }

  loading.value = true
  try {
    await customerRequestReset({
      idNumber: reqForm.idNumber,
      birthday: reqForm.birthday,
      email: reqForm.email,
    })
    requestSuccess.value = true
  } catch (err) {
    showToast(err.response?.data?.message || '驗證失敗，請確認資料正確', 'error')
  } finally {
    loading.value = false
  }
}

async function handleReset() {
  if (mismatch.value) return

  if (!token.value) {
    showToast('無效的重設連結', 'error')
    return
  }

  loading.value = true
  try {
    await customerResetPassword({
      token: token.value,
      newPassword: form.newPassword,
    })
    success.value = true
  } catch (err) {
    showToast(err.response?.data?.message || '密碼重設失敗', 'error')
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
  margin-bottom: var(--space-5);
  display: flex;
  justify-content: center;
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

/* Custom Toast */
.jb-toast {
  position: fixed;
  top: 40px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 24px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  z-index: 9999;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-primary);
}
.toast-success { border-left: 4px solid var(--primary); }
.toast-error { border-left: 4px solid var(--accent); }
.toast-warning { border-left: 4px solid #C4A47C; }

.toast-fade-enter-active, .toast-fade-leave-active { transition: all 0.3s var(--ease); }
.toast-fade-enter-from, .toast-fade-leave-to { opacity: 0; transform: translate(-50%, -10px); }
</style>
