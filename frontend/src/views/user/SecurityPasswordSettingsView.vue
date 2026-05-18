<script setup>
import { ref, reactive, onMounted } from 'vue'
import { customerRequestReset, customerGetProfile } from '@/api/customerAuth'

const form = reactive({
  idNumber: '',
  email: '',
  type: 'PASSWORD' // PASSWORD or USERNAME
})

const submitting = ref(false)
const toast = reactive({ visible: false, text: '', type: 'success', timer: null })

function showToast(text, type = 'success') {
  toast.text = text
  toast.type = type
  toast.visible = true
  if (toast.timer) clearTimeout(toast.timer)
  toast.timer = setTimeout(() => { toast.visible = false }, 3000)
}

async function fillMockData() {
  try {
    const res = await customerGetProfile()
    const profile = res.data.data
    form.idNumber = profile.idNumber || 'A123456789'
    form.email = profile.email || 'demo@example.com'
    showToast('已帶入驗證資料', 'success')
  } catch (err) {
    form.idNumber = 'A123456789'
    form.email = 'demo@example.com'
    showToast('已帶入測試驗證資料', 'success')
  }
}

async function handleSubmit() {
  if (!form.idNumber || !form.email) {
    showToast('請填寫身分證字號與註冊信箱', 'warning')
    return
  }

  submitting.value = true
  try {
    // Both password and username change request will send the verification email in this demo.
    await customerRequestReset({
      email: form.email,
      idNumber: form.idNumber
    })
    
    if (form.type === 'USERNAME') {
      showToast('使用者帳號變更驗證信已發送至您的電子信箱，請查收', 'success')
    } else {
      showToast('密碼重設驗證信已發送至您的電子信箱，請查收', 'success')
    }
    
    // Clear form
    form.idNumber = ''
    form.email = ''
  } catch (err) {
    showToast(err.response?.data?.message || '發送失敗，請確認資料是否正確', 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="customer-page security-password-page">
    <h2 class="page-title">密碼與帳號修改</h2>

    <section class="security-content jb-card">
      <h3 class="section-title">變更登入密碼 / 使用者帳號</h3>
      
      <p class="section-desc">
        為保障您的帳戶安全，變更密碼或使用者帳號前，請先進行身分驗證。系統將發送驗證信至您的電子信箱。
      </p>

      <form @submit.prevent="handleSubmit" novalidate class="ctbc-form">
        <div class="ctbc-row">
          <label class="ctbc-label">身分證字號</label>
          <div class="ctbc-field">
            <input v-model.trim="form.idNumber" type="text" class="jb-input" placeholder="請輸入身分證字號" />
          </div>
        </div>

        <div class="ctbc-row">
          <label class="ctbc-label">註冊信箱</label>
          <div class="ctbc-field">
            <input v-model.trim="form.email" type="email" class="jb-input" placeholder="請輸入註冊時的電子信箱" />
          </div>
        </div>

        <div class="ctbc-row">
          <label class="ctbc-label">申請變更項目</label>
          <div class="ctbc-field radio-group">
            <label class="custom-radio">
              <input type="radio" v-model="form.type" value="PASSWORD" />
              <span class="radio-mark"></span>
              登入密碼
            </label>
            <label class="custom-radio">
              <input type="radio" v-model="form.type" value="USERNAME" />
              <span class="radio-mark"></span>
              使用者帳號
            </label>
          </div>
        </div>

        <div class="form-actions" style="margin-top: 32px; display: flex; gap: 16px;">
          <button type="submit" class="jb-btn jb-btn-primary" :disabled="submitting">
            <span v-if="submitting" class="jb-spinner" style="width:18px;height:18px;"></span>
            發送驗證信
          </button>
          <button type="button" class="jb-btn jb-btn-secondary" @click="fillMockData">
            一鍵帶入 (Demo)
          </button>
        </div>
      </form>
    </section>

    <!-- 規則備註 -->
    <section class="rules-section">
      <h4 class="rules-title">帳號與密碼設定規則提醒：</h4>
      <ul class="rules-list">
        <li><strong>使用者帳號規則：</strong> 必須為 6~20 個字元，且包含英文與數字，不可含有特殊符號。</li>
        <li><strong>登入密碼規則：</strong> 必須為 8~16 個字元，須包含大寫英文、小寫英文與數字，不可與帳號相同。</li>
        <li>為確保安全，建議每三個月定期更新您的登入密碼，並避免使用生日、電話等容易被猜測的資訊。</li>
      </ul>
    </section>

    <!-- Custom Toast -->
    <transition name="toast-fade">
      <div v-if="toast.visible" class="jb-toast" :class="`toast-${toast.type}`">
        {{ toast.text }}
      </div>
    </transition>
  </div>
</template>

<style scoped>
.security-password-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;
}

.page-title {
  font-family: var(--font-heading);
  font-size: var(--text-h2);
  font-weight: 600;
  line-height: var(--leading-heading);
  margin-bottom: 20px;
  color: var(--text-primary);
  letter-spacing: 0;
}

.security-content {
  padding: var(--space-5) var(--space-6);
  border-radius: 12px;
  background: rgba(255, 249, 239, 0.92);
  border: 1px solid rgba(214, 206, 195, 0.86);
  box-shadow: 0 12px 36px rgba(63, 74, 66, 0.08);
  margin-bottom: 24px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: var(--space-3);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid rgba(214, 206, 195, 0.8);
  color: var(--text-primary);
}

.section-desc {
  font-size: 15px;
  color: var(--text-secondary);
  margin-bottom: 24px;
  line-height: 1.6;
}

.ctbc-form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.ctbc-row {
  display: flex;
  align-items: center;
}

.ctbc-label {
  width: 120px;
  font-weight: 500;
  color: var(--text-secondary);
  flex-shrink: 0;
  font-size: 15px;
}

.ctbc-field {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex: 1;
}

.jb-input {
  min-height: 48px;
  width: 100%;
  max-width: 320px;
  color: var(--text-primary);
  background: rgba(250, 250, 247, 0.92);
  border: 1px solid rgba(214, 206, 195, 0.86);
  border-radius: 8px;
  padding: 0 16px;
  transition:
    border-color var(--duration) var(--ease),
    box-shadow var(--duration) var(--ease),
    background var(--duration) var(--ease);
}

.jb-input:hover:not(:disabled):not(:focus) {
  border-color: var(--text-disabled);
}

.jb-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
  outline: none;
}

.radio-group {
  display: flex;
  gap: var(--space-5);
  flex-wrap: wrap;
}

.custom-radio {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  cursor: pointer;
  font-size: 15px;
  color: var(--text-secondary);
  transition: color var(--duration) var(--ease);
}

.custom-radio:hover {
  color: var(--primary-dark);
}

.custom-radio input {
  display: none;
}

.radio-mark {
  width: 18px;
  height: 18px;
  border-radius: 50%;
  border: 1.5px solid rgba(214, 206, 195, 0.92);
  position: relative;
  background: rgba(250, 250, 247, 0.84);
  transition: all var(--duration) var(--ease);
}

.custom-radio input:checked + .radio-mark {
  border-color: var(--primary);
}

.custom-radio input:checked + .radio-mark::after {
  content: '';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--primary);
}

.rules-section {
  padding: 20px 24px;
  background: rgba(92, 107, 95, 0.04);
  border-left: 4px solid var(--primary);
  border-radius: 0 8px 8px 0;
}

.rules-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--primary-dark);
  margin-top: 0;
  margin-bottom: 12px;
}

.rules-list {
  margin: 0;
  padding-left: 20px;
  color: var(--text-secondary);
  font-size: 14px;
  line-height: 1.7;
}
</style>
