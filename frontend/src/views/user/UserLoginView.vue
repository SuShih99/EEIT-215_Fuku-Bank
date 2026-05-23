<template>
  <div class="customer-page login-page">
    <!-- 和紙紋理 -->
    <div class="washi-overlay" aria-hidden="true"></div>

    <main class="login-container">
      <!-- 左側裝飾 -->
      <aside class="login-deco" aria-hidden="true">
        <div class="deco-ink"></div>
        <div class="deco-brand" @click="router.push('/')" style="cursor: pointer;">
          <JbLogo size="lg" />
        </div>
        <p class="deco-tagline">靜心理財<br/>安穩致遠</p>
      </aside>

      <!-- 右側表單 -->
      <section class="login-form-section">
        <div class="login-card">
          <p class="login-eyebrow">Welcome Back</p>
          <h1>歡迎回來</h1>
          <div class="login-rule"></div>
          <p class="login-subtitle">登入您的帳戶</p>

          <form @submit.prevent="handleLogin(false)" novalidate>
            <div class="jb-form-item">
              <label for="login-idnumber" class="jb-label">身分證字號</label>
              <input
                id="login-idnumber"
                v-model="form.idNumber"
                type="text"
                class="jb-input"
                placeholder="輸入身分證字號"
                required
              />
            </div>

            <div class="jb-form-item">
              <label for="login-username" class="jb-label">使用者名稱</label>
              <input
                id="login-username"
                v-model="form.username"
                type="text"
                class="jb-input"
                placeholder="輸入使用者名稱"
                autocomplete="username"
                required
              />
            </div>

            <div class="jb-form-item">
              <label for="login-password" class="jb-label">密碼</label>
              <div class="jb-password-wrap">
                <input
                  id="login-password"
                  v-model="form.password"
                  :type="showPwd ? 'text' : 'password'"
                  class="jb-input"
                  placeholder="輸入密碼"
                  autocomplete="current-password"
                  required
                />
                <button
                  type="button"
                  class="jb-password-toggle"
                  :aria-label="showPwd ? '隱藏密碼' : '顯示密碼'"
                  @click="showPwd = !showPwd"
                >
                  <svg v-if="!showPwd" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12z"/><circle cx="12" cy="12" r="3"/></svg>
                  <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2"><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                </button>
              </div>
            </div>

            <div class="jb-form-item">
              <label for="login-captcha" class="jb-label">圖形驗證碼</label>
              <div class="captcha-row">
                <input
                  id="login-captcha"
                  v-model="form.captcha"
                  type="text"
                  class="jb-input"
                  placeholder="請輸入右方驗證碼"
                  required
                />
                <div class="captcha-display" @click="refreshCaptcha" title="點擊重新產生">
                  {{ generatedCaptcha }}
                </div>
              </div>
            </div>

            <div class="login-forgot">
              <router-link to="/reset-password" class="jb-link">忘記密碼？</router-link>
            </div>

            <button
              type="submit"
              class="jb-btn jb-btn-primary jb-btn-block jb-btn-lg"
              :disabled="loading"
            >
              <span v-if="loading" class="jb-spinner" style="width:18px;height:18px;"></span>
              <span>{{ loading ? '登入中...' : '登入' }}</span>
            </button>
          </form>

          <button
            class="jb-btn jb-btn-secondary jb-btn-block"
            style="margin-top: var(--space-3)"
            :disabled="loading"
            @click="loginAsWangDaming"
          >
            登入示範帳號
          </button>

          <button
            class="jb-btn jb-btn-secondary jb-btn-block"
            style="margin-top: var(--space-2)"
            :disabled="loading"
            @click="fillTestAccount"
          >
          一鍵帶入新註冊帳號
          </button>

          <button
            class="jb-btn jb-btn-secondary jb-btn-block"
            style="margin-top: var(--space-2)"
            :disabled="loading"
            @click="fillTestAccountModified"
          >
          一鍵帶入新註冊（更改密碼）
          </button>

          <div class="login-divider">
            <span>或</span>
          </div>

          <p class="login-register">
            還沒有帳戶？
            <router-link to="/register" class="jb-link">立即註冊</router-link>
          </p>
        </div>
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
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { customerLogin } from '@/api/customerAuth'
import { useCustomerAuthStore } from '@/stores/customerAuth'
import JbLogo from '@/components/JbLogo.vue'
import { WANG_XIAOMING_DEMO_ACCOUNT } from '@/data/customerDemoAccounts'

const router = useRouter()
const customerAuthStore = useCustomerAuthStore()
const loading = ref(false)
const showPwd = ref(false)
const generatedCaptcha = ref('')

const toast = reactive({ visible: false, text: '', type: 'error', timer: null })
function showToast(text, type = 'error') {
  toast.text = text
  toast.type = type
  toast.visible = true
  if (toast.timer) clearTimeout(toast.timer)
  toast.timer = setTimeout(() => { toast.visible = false }, 3000)
}

function refreshCaptcha() {
  const chars = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ'
  let code = ''
  for (let i = 0; i < 4; i++) {
    code += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  generatedCaptcha.value = code
}

onMounted(() => {
  refreshCaptcha()
})

const form = reactive({
  idNumber: '',
  username: '',
  password: '',
  captcha: '',
})

const WANG_DAMING_ACCOUNT = {
  idNumber: 'A100260501',
  username: 'cust0001',
  password: '123456',
}

async function loginAsWangDaming() {
  fillLoginForm(WANG_DAMING_ACCOUNT)
  await handleLogin(true)
}

async function fillTestAccount() {
  fillLoginForm(WANG_XIAOMING_DEMO_ACCOUNT)
  // 只填入表單，不自動登入
}

async function fillTestAccountModified() {
  fillLoginForm({
    ...WANG_XIAOMING_DEMO_ACCOUNT,
    password: '09880988'
  })
  // 只填入表單，不自動登入
}

function fillLoginForm(account) {
  form.idNumber = account.idNumber
  form.username = account.username
  form.password = account.password
  form.captcha = generatedCaptcha.value // auto correct
}

async function handleLogin(bypass = false) {
  if (!bypass) {
    if (!form.idNumber || !form.username || !form.password || !form.captcha) {
      showToast('請填寫完整資訊', 'warning')
      return
    }
    if (form.captcha.toUpperCase() !== generatedCaptcha.value) {
      showToast('驗證碼錯誤', 'error')
      refreshCaptcha()
      form.captcha = ''
      return
    }
  }

  loading.value = true
  try {
    const res = await customerLogin({
      idNumber: form.idNumber,
      username: form.username,
      password: form.password,
    })
    const data = res.data.data
    customerAuthStore.setCustomer(data)
    router.push({ name: 'user-home' })
  } catch (err) {
    showToast(err.response?.data?.message || '登入失敗，請檢查帳號密碼', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  background: var(--bg-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-6) var(--space-3);
  position: relative;
}

/* === Washi === */
.washi-overlay {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: url('/washi-texture.png') repeat;
  opacity: 0.04;
}

.login-container {
  position: relative;
  z-index: 1;
  display: flex;
  max-width: 960px;
  width: 100%;
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid var(--border);
  box-shadow: none;
}

/* 左裝飾 */
.login-deco {
  width: 380px;
  flex-shrink: 0;
  background: var(--bg-secondary);
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  gap: var(--space-5);
  padding: var(--space-7);
}

.deco-ink {
  position: absolute;
  inset: -30%;
  background: radial-gradient(
    ellipse at 40% 50%,
    rgba(92, 107, 95, 0.06) 0%,
    transparent 60%
  );
}

.deco-brand {
  position: relative;
  z-index: 1;
}

.deco-tagline {
  position: relative;
  z-index: 1;
  font-family: var(--font-heading);
  font-size: 20px;
  font-weight: 600;
  color: var(--text-secondary);
  text-align: center;
  letter-spacing: 6px;
  line-height: 1.8;
}

/* 右表單 */
.login-form-section {
  flex: 1;
  background: var(--bg-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-8) var(--space-7);
}

.login-card {
  width: 100%;
  max-width: 380px;
}

.login-eyebrow {
  font-family: var(--font-display);
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 4px;
  color: var(--text-disabled);
  text-transform: uppercase;
  margin-bottom: var(--space-2);
}

.login-card h1 {
  margin-bottom: var(--space-3);
  letter-spacing: 3px;
}

.login-rule {
  width: 32px;
  height: 1px;
  background: var(--border);
  margin-bottom: var(--space-3);
}

.login-subtitle {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin-bottom: var(--space-6);
}

.login-forgot {
  text-align: right;
  margin-top: calc(-1 * var(--space-2));
  margin-bottom: var(--space-4);
  font-size: var(--text-sm);
}

.login-divider {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin: var(--space-5) 0;
  color: var(--text-disabled);
  font-size: var(--text-xs);
}

.login-divider::before,
.login-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border);
}

.login-register {
  text-align: center;
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

/* RWD */
@media (max-width: 800px) {
  .login-deco { display: none; }
  .login-container {
    max-width: 480px;
    border-radius: 20px;
  }
  .login-form-section {
    padding: var(--space-7) var(--space-5);
  }
}

.captcha-row {
  display: flex;
  gap: var(--space-3);
}

.captcha-display {
  width: 120px;
  height: 44px;
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: monospace;
  font-size: 18px;
  font-weight: bold;
  letter-spacing: 4px;
  color: var(--primary);
  cursor: pointer;
  user-select: none;
  flex-shrink: 0;
  transition: opacity 0.2s;
}

.captcha-display:hover {
  opacity: 0.8;
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
