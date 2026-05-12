<template>
  <div class="customer-page register-page">
    <main class="register-container">
      <!-- Logo -->
      <div class="register-logo">
        <JbLogo size="md" clickable @navigate="$router.push('/')" />
      </div>

      <section class="register-card jb-card">
        <h1 class="register-title">開立帳戶</h1>
        <p class="register-subtitle">加入爪哇銀行，開啟您的數位金融旅程</p>

        <form @submit.prevent="handleRegister" novalidate>
          <div class="form-grid">
            <div class="jb-form-item">
              <label for="reg-name" class="jb-label">姓名 <span class="jb-required">*</span></label>
              <input id="reg-name" v-model="form.name" type="text" class="jb-input" placeholder="請輸入姓名" required />
            </div>

            <div class="jb-form-item">
              <label for="reg-birthday" class="jb-label">生日 <span class="jb-required">*</span></label>
              <input id="reg-birthday" v-model="form.birthday" type="date" class="jb-date" required />
            </div>

            <div class="jb-form-item">
              <label for="reg-gender" class="jb-label">性別 <span class="jb-required">*</span></label>
              <select id="reg-gender" v-model="form.gender" class="jb-select" required>
                <option value="" disabled>請選擇性別</option>
                <option value="M">男</option>
                <option value="F">女</option>
              </select>
            </div>

            <div class="jb-form-item">
              <label for="reg-id" class="jb-label">身分證字號 <span class="jb-required">*</span></label>
              <input id="reg-id" v-model="form.idNumber" type="text" class="jb-input" placeholder="例如：A123456789" required />
            </div>

            <div class="jb-form-item">
              <label for="reg-username" class="jb-label">使用者帳號 <span class="jb-required">*</span></label>
              <input id="reg-username" v-model="form.username" type="text" class="jb-input" placeholder="英文字母或數字" required />
            </div>

            <div class="jb-form-item">
              <label for="reg-password" class="jb-label">密碼 <span class="jb-required">*</span></label>
              <div class="jb-password-wrap">
                <input
                  id="reg-password"
                  v-model="form.password"
                  :type="showPwd ? 'text' : 'password'"
                  class="jb-input"
                  placeholder="請輸入密碼"
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

            <div class="jb-form-item full-width">
              <label for="reg-phone" class="jb-label">手機號碼 <span class="jb-required">*</span></label>
              <input id="reg-phone" v-model="form.phone" type="tel" class="jb-input" placeholder="例如：0912345678" required />
            </div>

            <div class="jb-form-item full-width">
              <label for="reg-email" class="jb-label">電子信箱 <span class="jb-required">*</span></label>
              <input id="reg-email" v-model="form.email" type="email" class="jb-input" placeholder="請輸入電子信箱" required />
            </div>

            <div class="jb-form-item full-width">
              <label for="reg-address" class="jb-label">通訊地址 <span class="jb-required">*</span></label>
              <input id="reg-address" v-model="form.address" type="text" class="jb-input" placeholder="請輸入通訊地址" required />
            </div>
          </div>

          <button
            type="submit"
            class="jb-btn jb-btn-primary jb-btn-block jb-btn-lg"
            :disabled="loading"
            style="margin-top: var(--space-2)"
          >
            <span v-if="loading" class="jb-spinner" style="width:18px;height:18px;"></span>
            <span>{{ loading ? '註冊中...' : '註冊' }}</span>
          </button>
        </form>

        <hr class="jb-divider" />

        <p class="login-link">
          已有帳號？
          <router-link to="/login" class="jb-link">立即登入</router-link>
        </p>
      </section>
    </main>

    <transition name="toast-fade">
      <div v-if="toast.visible" class="jb-toast" :class="`toast-${toast.type}`">
        {{ toast.text }}
      </div>
    </transition>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { customerRegister } from '@/api/customerAuth'
import JbLogo from '@/components/JbLogo.vue'

const router = useRouter()
const loading = ref(false)
const showPwd = ref(false)
const toast = reactive({ visible: false, text: '', type: 'error', timer: null })

const form = reactive({
  name: '',
  birthday: '',
  gender: '',
  idNumber: '',
  username: '',
  password: '',
  phone: '',
  email: '',
  address: '',
})

function showToast(text, type = 'error') {
  toast.text = text
  toast.type = type
  toast.visible = true
  if (toast.timer) clearTimeout(toast.timer)
  toast.timer = setTimeout(() => {
    toast.visible = false
  }, 3200)
}

async function handleRegister() {
  loading.value = true
  try {
    await customerRegister({
      name: form.name,
      birthday: form.birthday,
      gender: form.gender,
      idNumber: form.idNumber,
      username: form.username,
      password: form.password,
      phone: form.phone,
      email: form.email,
      address: form.address,
    })
    showToast('註冊成功，驗證信已寄出，請先至信箱完成認證', 'success')
    setTimeout(() => {
      router.push('/login')
    }, 1200)
  } catch (err) {
    showToast(err.response?.data?.message || '註冊失敗，請檢查輸入資料', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  background: var(--bg-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-6) var(--space-3);
}

.register-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 580px;
}

.register-logo {
  margin-bottom: var(--space-5);
  display: flex;
  justify-content: center;
}

.register-card {
  width: 100%;
}

.register-title {
  text-align: center;
  margin-bottom: var(--space-2);
}

.register-subtitle {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  text-align: center;
  margin-bottom: var(--space-5);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 var(--space-3);
}

.form-grid .full-width {
  grid-column: 1 / -1;
}

.login-link {
  text-align: center;
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

@media (max-width: 600px) {
  .form-grid { grid-template-columns: 1fr; }
  .register-card { padding: var(--space-4) var(--space-3); }
}

.jb-toast {
  position: fixed;
  left: 50%;
  bottom: 28px;
  transform: translateX(-50%);
  min-width: min(420px, calc(100vw - 32px));
  max-width: calc(100vw - 32px);
  padding: 14px 18px;
  border-radius: 16px;
  border: 1px solid rgba(138, 122, 98, 0.18);
  box-shadow: 0 18px 40px rgba(63, 74, 66, 0.16);
  backdrop-filter: blur(10px);
  color: var(--text-primary);
  text-align: center;
  z-index: 40;
}

.toast-success {
  background: rgba(245, 250, 245, 0.94);
  border-color: rgba(103, 125, 107, 0.28);
}

.toast-error {
  background: rgba(255, 246, 244, 0.95);
  border-color: rgba(181, 109, 88, 0.25);
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translate(-50%, 10px);
}
</style>
