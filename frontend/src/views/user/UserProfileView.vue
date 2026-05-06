<template>
  <div class="customer-page profile-page">
    <h2 class="page-title">會員中心</h2>

    <div class="profile-layout">
      <!-- 左欄：大頭照 + CIF -->
      <aside class="profile-left">
        <div class="avatar-section">
          <button class="avatar-wrapper" @click="triggerUpload" aria-label="更換大頭照">
            <img v-if="avatarSrc" :src="avatarSrc" class="avatar-img" alt="使用者大頭照" />
            <span v-else class="avatar-fallback" aria-hidden="true">{{ (profile.name || '會')[0] }}</span>
            <span class="avatar-overlay" aria-hidden="true">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
                <circle cx="12" cy="13" r="4"/>
              </svg>
            </span>
          </button>
          <input
            ref="fileInput"
            type="file"
            accept="image/*"
            style="display: none"
            @change="handleFileChange"
          />
          <p class="avatar-hint">點擊更換大頭照</p>
        </div>

        <div class="cif-box">
          <span class="cif-label">客戶編號 (CIF)</span>
          <span class="cif-value">{{ profile.cif || '-' }}</span>
        </div>
      </aside>

      <!-- 右欄：個人資料 -->
      <section class="profile-right jb-card">
        <form @submit.prevent="handleSave" novalidate>
          <div class="form-grid">
            <!-- 唯讀 -->
            <div class="jb-form-item">
              <label class="jb-label">姓名</label>
              <input :value="profile.name" class="jb-input" disabled aria-label="姓名" />
            </div>
            <div class="jb-form-item">
              <label class="jb-label">生日</label>
              <input :value="profile.birthday" class="jb-input" disabled aria-label="生日" />
            </div>
            <div class="jb-form-item">
              <label class="jb-label">性別</label>
              <input :value="genderMap[profile.gender] || profile.gender" class="jb-input" disabled aria-label="性別" />
            </div>
            <div class="jb-form-item">
              <label class="jb-label">使用者帳號</label>
              <input :value="profile.username" class="jb-input" disabled aria-label="帳號" />
            </div>

            <!-- 密碼 -->
            <div class="jb-form-item full-width">
              <label class="jb-label">密碼</label>
              <div class="password-row">
                <input value="••••••••" class="jb-input" disabled style="flex: 1" aria-label="密碼" />
                <button type="button" class="jb-btn jb-btn-secondary jb-btn-sm" @click="handleRequestReset">
                  變更密碼
                </button>
              </div>
            </div>

            <!-- 可編輯 -->
            <div class="jb-form-item">
              <label for="profile-phone" class="jb-label">手機號碼</label>
              <input id="profile-phone" v-model="editForm.phone" type="tel" class="jb-input" placeholder="請輸入手機號碼" />
            </div>
            <div class="jb-form-item">
              <label for="profile-email" class="jb-label">電子信箱</label>
              <input id="profile-email" v-model="editForm.email" type="email" class="jb-input" placeholder="請輸入信箱" />
            </div>
            <div class="jb-form-item full-width">
              <label for="profile-address" class="jb-label">通訊地址</label>
              <input id="profile-address" v-model="editForm.address" type="text" class="jb-input" placeholder="請輸入地址" />
            </div>
          </div>

          <button
            type="submit"
            class="jb-btn jb-btn-primary jb-btn-lg"
            :disabled="saving"
            style="margin-top: var(--space-2)"
          >
            <span v-if="saving" class="jb-spinner" style="width:18px;height:18px;"></span>
            <span>{{ saving ? '儲存中...' : '儲存變更' }}</span>
          </button>
        </form>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  customerGetProfile,
  customerUpdateProfile,
  customerUploadAvatar,
  customerRequestReset,
} from '@/api/customerAuth'
import { useCustomerAuthStore } from '@/stores/customerAuth'
import { BASE_URL } from '@/api/axios'

const customerAuthStore = useCustomerAuthStore()
const fileInput = ref(null)
const saving = ref(false)

const genderMap = { M: '男', F: '女' }

const profile = reactive({
  customerId: '',
  cif: '',
  name: '',
  birthday: '',
  gender: '',
  username: '',
  email: '',
  phone: '',
  address: '',
  avatarUrl: null,
})

const editForm = reactive({
  phone: '',
  email: '',
  address: '',
})

const avatarSrc = ref(null)

onMounted(async () => {
  try {
    const res = await customerGetProfile()
    const data = res.data.data
    Object.assign(profile, data)
    editForm.phone = data.phone
    editForm.email = data.email
    editForm.address = data.address
    updateAvatarSrc(data.avatarUrl)
  } catch {
    alert('載入個人資料失敗')
  }
})

function updateAvatarSrc(url) {
  avatarSrc.value = !url ? null : url.startsWith('http') ? url : BASE_URL + url
}

function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileChange(e) {
  const file = e.target.files[0]
  if (!file) return
  avatarSrc.value = URL.createObjectURL(file)

  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await customerUploadAvatar(formData)
    const data = res.data.data
    updateAvatarSrc(data.avatarUrl)
    if (customerAuthStore.customer) {
      customerAuthStore.customer.avatarUrl = data.avatarUrl
      customerAuthStore.setCustomer(customerAuthStore.customer)
    }
    alert('大頭照更新成功')
  } catch {
    alert('大頭照上傳失敗')
  }
}

async function handleSave() {
  saving.value = true
  try {
    const res = await customerUpdateProfile({
      phone: editForm.phone,
      email: editForm.email,
      address: editForm.address,
    })
    Object.assign(profile, res.data.data)
    alert('個人資料已更新')
  } catch (err) {
    alert(err.response?.data?.message || '更新失敗')
  } finally {
    saving.value = false
  }
}

async function handleRequestReset() {
  try {
    await customerRequestReset({ email: profile.email })
    alert('密碼重設連結已發送至您的信箱，請查收')
  } catch (err) {
    alert(err.response?.data?.message || '發送失敗')
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 900px;
}

.page-title {
  font-family: var(--font-heading);
  font-size: var(--text-h2);
  margin-bottom: var(--space-4);
  letter-spacing: 3px;
}

.profile-layout {
  display: flex;
  gap: var(--space-4);
}

/* 左欄 */
.profile-left {
  width: 200px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-section {
  text-align: center;
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
  border-radius: 50%;
  overflow: hidden;
  border: none;
  padding: 0;
  background: none;
  cursor: pointer;
}

.avatar-img {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid var(--border);
  display: block;
}

.avatar-fallback {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  color: var(--primary);
  font-family: var(--font-heading);
  font-size: 48px;
  font-weight: 600;
  border: 3px solid var(--border);
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(43, 43, 43, 0.4);
  opacity: 0;
  transition: opacity var(--duration) var(--ease);
  border-radius: 50%;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.avatar-hint {
  margin-top: var(--space-2);
  font-size: var(--text-xs);
  color: var(--text-secondary);
}

.cif-box {
  margin-top: var(--space-4);
  text-align: center;
  padding: var(--space-3);
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  width: 100%;
  border: 1px solid rgba(214, 206, 195, 0.5);
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.cif-label {
  font-size: var(--text-xs);
  color: var(--text-secondary);
}

.cif-value {
  font-size: var(--text-body);
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 1px;
}

/* 右欄 */
.profile-right {
  flex: 1;
  min-width: 0;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 var(--space-3);
}

.form-grid .full-width {
  grid-column: 1 / -1;
}

.password-row {
  display: flex;
  gap: var(--space-3);
}

@media (max-width: 700px) {
  .profile-layout { flex-direction: column; }
  .profile-left { width: 100%; }
  .form-grid { grid-template-columns: 1fr; }
}
</style>
