<template>
  <div class="profile-page">
    <h2 class="page-title">會員中心</h2>

    <div class="profile-layout">
      <!-- 左欄：大頭照 + CIF -->
      <div class="profile-left">
        <div class="avatar-section">
          <div class="avatar-wrapper" @click="triggerUpload">
            <a-avatar :size="120" :src="avatarSrc">
              <template #icon><UserOutlined style="font-size: 60px" /></template>
            </a-avatar>
            <div class="avatar-overlay">
              <CameraOutlined style="font-size: 24px; color: #fff" />
            </div>
          </div>
          <input
            ref="fileInput"
            type="file"
            accept="image/*"
            style="display: none"
            @change="handleFileChange"
          />
          <p class="avatar-hint">點擊更換大頭照 (僅支援 JPG/PNG)</p>
        </div>

        <div class="cif-section">
          <div class="cif-label">客戶編號 (CIF)</div>
          <div class="cif-value">{{ profile.cif || '-' }}</div>
        </div>
      </div>

      <!-- 右欄：個人資料表單 -->
      <div class="profile-right">
        <a-form layout="vertical">
          <div class="form-grid">
            <!-- 唯讀欄位 -->
            <a-form-item label="姓名">
              <a-input :value="profile.name" disabled />
            </a-form-item>

            <a-form-item label="生日">
              <a-input :value="profile.birthday" disabled />
            </a-form-item>

            <a-form-item label="性別">
              <a-input :value="genderMap[profile.gender] || profile.gender" disabled />
            </a-form-item>

            <a-form-item label="使用者帳號">
              <a-input :value="profile.username" disabled />
            </a-form-item>

            <!-- 密碼 -->
            <a-form-item label="密碼" class="full-width">
              <div class="password-row">
                <a-input value="••••••••" disabled style="flex: 1" />
                <a-button type="primary" ghost @click="handleRequestReset">
                  <MailOutlined />
                  變更密碼
                </a-button>
              </div>
            </a-form-item>

            <!-- 可編輯欄位 -->
            <a-form-item label="手機號碼">
              <a-input v-model:value="editForm.phone" placeholder="請輸入手機號碼" />
            </a-form-item>

            <a-form-item label="信箱">
              <a-input v-model:value="editForm.email" placeholder="請輸入信箱" />
            </a-form-item>

            <a-form-item label="地址" class="full-width">
              <a-input v-model:value="editForm.address" placeholder="請輸入地址" />
            </a-form-item>
          </div>

          <a-button
            type="primary"
            size="large"
            :loading="saving"
            @click="handleSave"
            style="margin-top: 8px"
          >
            儲存變更
          </a-button>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { UserOutlined, CameraOutlined, MailOutlined } from '@ant-design/icons-vue'
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

// 載入個人資料
onMounted(async () => {
  try {
    const res = await customerGetProfile()
    const data = res.data.data
    Object.assign(profile, data)
    editForm.phone = data.phone
    editForm.email = data.email
    editForm.address = data.address
    updateAvatarSrc(data.avatarUrl)
  } catch (err) {
    message.error('載入個人資料失敗')
  }
})

function updateAvatarSrc(url) {
  if (!url) {
    avatarSrc.value = null
  } else {
    avatarSrc.value = url.startsWith('http') ? url : BASE_URL + url
  }
}

// 上傳大頭照
function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileChange(e) {
  const file = e.target.files[0]
  if (!file) return

  // 根據 Reichart 規範：檢查格式
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  if (!isJpgOrPng) {
    message.error('大頭照僅支援 JPG/PNG 格式！')
    return
  }

  // 前端預覽
  avatarSrc.value = URL.createObjectURL(file)

  // 上傳到後端
  const formData = new FormData()
  formData.append('file', file)
  try {
    const res = await customerUploadAvatar(formData)
    const data = res.data.data
    updateAvatarSrc(data.avatarUrl)

    // 更新 store 的 avatarUrl
    if (customerAuthStore.customer) {
      customerAuthStore.customer.avatarUrl = data.avatarUrl
      customerAuthStore.setCustomer(customerAuthStore.customer)
    }

    message.success('大頭照更新成功')
  } catch (err) {
    message.error('大頭照上傳失敗')
  }
}

// 儲存變更
async function handleSave() {
  saving.value = true
  try {
    const res = await customerUpdateProfile({
      phone: editForm.phone,
      email: editForm.email,
      address: editForm.address,
    })
    const data = res.data.data
    Object.assign(profile, data)
    message.success('個人資料已更新')
  } catch (err) {
    message.error(err.response?.data?.message || '更新失敗')
  } finally {
    saving.value = false
  }
}

// 請求密碼重設
async function handleRequestReset() {
  try {
    await customerRequestReset({ email: profile.email })
    message.success('密碼重設連結已發送至您的信箱，請查收')
  } catch (err) {
    message.error(err.response?.data?.message || '發送失敗')
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 900px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 24px;
}

.profile-layout {
  display: flex;
  gap: 32px;
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
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
  cursor: pointer;
  display: inline-block;
  border-radius: 50%;
  overflow: hidden;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.4);
  opacity: 0;
  transition: opacity 0.2s;
  border-radius: 50%;
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.avatar-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #999;
}

.cif-section {
  margin-top: 24px;
  text-align: center;
  padding: 16px;
  background: #f5f6fa;
  border-radius: 10px;
  width: 100%;
}

.cif-label {
  font-size: 12px;
  color: #8c8c8c;
  margin-bottom: 4px;
}

.cif-value {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  letter-spacing: 1px;
}

/* 右欄 */
.profile-right {
  flex: 1;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 20px;
}

.form-grid .full-width {
  grid-column: 1 / -1;
}

.password-row {
  display: flex;
  gap: 12px;
}

@media (max-width: 700px) {
  .profile-layout { flex-direction: column; }
  .profile-left { width: 100%; }
  .form-grid { grid-template-columns: 1fr; }
}
</style>
