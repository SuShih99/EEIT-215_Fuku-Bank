<template>
  <div class="customer-page login-page">
    <main class="login-container">
      <!-- 左側裝飾 -->
      <aside class="login-deco" aria-hidden="true">
        <div class="deco-ink-wash"></div>
        <div class="deco-circle"></div>
        <div class="deco-brand">
          <JbLogo size="lg" />
        </div>
      </aside>

      <!-- 右側表單 -->
      <section class="login-form-section">
        <div class="login-card">
          <h1>歡迎回來</h1>
          <p class="login-subtitle">登入您的帳戶</p>

          <form @submit.prevent="handleLogin" novalidate>
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
                  <svg v-if="!showPwd" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12z"/><circle cx="12" cy="12" r="3"/></svg>
                  <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
                </button>
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
            @click="fillTestAccount"
          >
            一鍵帶入測試帳號
          </button>

          <hr class="jb-divider" />

          <p class="login-register">
            還沒有帳戶？
            <router-link to="/register" class="jb-link">立即註冊</router-link>
          </p>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { customerLogin } from '@/api/customerAuth'
import { useCustomerAuthStore } from '@/stores/customerAuth'
import JbLogo from '@/components/JbLogo.vue'

const router = useRouter()
const customerAuthStore = useCustomerAuthStore()
const loading = ref(false)
const showPwd = ref(false)

const form = reactive({
  username: '',
  password: '',
})

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
    router.push({ name: 'user-home' })
  } catch (err) {
    alert(err.response?.data?.message || '登入失敗，請檢查帳號密碼')
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
}

.login-container {
  display: flex;
  max-width: 880px;
  width: 100%;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-lg);
}

/* 左裝飾 */
.login-deco {
  width: 340px;
  flex-shrink: 0;
  background: var(--bg-secondary);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.deco-ink-wash {
  position: absolute;
  inset: -20%;
  background: radial-gradient(
    ellipse at 30% 50%,
    rgba(92, 107, 95, 0.12) 0%,
    rgba(92, 107, 95, 0.04) 50%,
    transparent 70%
  );
}

.deco-circle {
  position: absolute;
  top: 12%;
  right: -20%;
  width: 240px;
  height: 240px;
  border: 2px solid rgba(92, 107, 95, 0.10);
  border-radius: 50%;
  clip-path: polygon(0 0, 90% 0, 100% 90%, 10% 100%);
}

.deco-brand {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 右表單 */
.login-form-section {
  flex: 1;
  background: var(--bg-card);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-7) var(--space-6);
}

.login-card {
  width: 100%;
  max-width: 380px;
}

.login-card h1 {
  margin-bottom: var(--space-2);
}

.login-subtitle {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  margin-bottom: var(--space-5);
}

.login-forgot {
  text-align: right;
  margin-top: calc(-1 * var(--space-2));
  margin-bottom: var(--space-4);
  font-size: var(--text-sm);
}

.login-register {
  text-align: center;
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

/* RWD */
@media (max-width: 700px) {
  .login-deco { display: none; }
  .login-container {
    max-width: 440px;
  }
  .login-form-section {
    padding: var(--space-5) var(--space-4);
  }
}
</style>
