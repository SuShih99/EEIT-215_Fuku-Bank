<template>
  <div class="customer-page profile-page">
    <h2 class="page-title">個人設定</h2>

    <!-- 頂部設定項目選擇 -->
    <div class="setting-selector jb-card">
      <span class="setting-label">請選擇設定項目</span>
      <div class="radio-group">
        <label class="custom-radio">
          <input type="radio" v-model="activeTab" value="email" />
          <span class="radio-mark"></span>
          帳號及電子郵件
        </label>
        <label class="custom-radio">
          <input type="radio" v-model="activeTab" value="basic" />
          <span class="radio-mark"></span>
          銀行個人基本資料
        </label>
      </div>
    </div>

    <!-- 主內容區塊 -->
    <section class="profile-content jb-card">
      <h3 class="section-title">
        {{ activeTab === 'email' ? '帳號、電子郵件與大頭照' : '銀行個人基本資料' }}
      </h3>

      <form @submit.prevent="handleSave" novalidate>
        
        <!-- 帳號及電子郵件 -->
        <div v-if="activeTab === 'email'" class="ctbc-form">
          <div class="ctbc-row avatar-tab" style="margin-bottom: 24px;">
            <div class="avatar-section">
              <button type="button" class="avatar-wrapper" @click="triggerUpload" aria-label="更換大頭照">
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
          </div>
          <div class="divider" style="margin-bottom: 16px; border-top: 1px solid var(--border);"></div>
          <div class="ctbc-row">
            <label class="ctbc-label">使用者帳號</label>
            <div class="ctbc-field">
              <input :value="profile.username" class="jb-input" disabled />
            </div>
          </div>
          <div class="ctbc-row">
            <label class="ctbc-label">姓名</label>
            <div class="ctbc-field">
              <input :value="profile.name" class="jb-input" disabled />
            </div>
          </div>
          <div class="ctbc-row">
            <label class="ctbc-label">目前電子郵件信箱</label>
            <div class="ctbc-field">
              <input :value="maskEmail(profile.email)" type="text" class="jb-input" disabled />
            </div>
          </div>
          <div class="ctbc-row">
            <label class="ctbc-label">輸入新的電子郵件信箱</label>
            <div class="ctbc-field">
              <input v-model="editForm.email" type="email" class="jb-input" placeholder="若不修改請留空" />
            </div>
          </div>
          <div class="ctbc-row">
            <label class="ctbc-label">再次輸入電子郵件信箱</label>
            <div class="ctbc-field">
              <input v-model="editForm.emailConfirm" type="email" class="jb-input" placeholder="請再次輸入以確認" />
            </div>
          </div>
          <div class="ctbc-row">
            <label class="ctbc-label">密碼</label>
            <div class="ctbc-field password-field">
              <input value="••••••••" class="jb-input" disabled style="width: 180px;" />
              <button type="button" class="jb-btn jb-btn-secondary jb-btn-sm" @click="handleRequestReset">
                變更密碼
              </button>
            </div>
          </div>
        </div>

        <!-- 銀行個人基本資料 -->
        <div v-if="activeTab === 'basic'" class="ctbc-form">
          <h4 class="sub-section-title">基本資料</h4>
          <div class="ctbc-row">
            <label class="ctbc-label">身分證字號</label>
            <div class="ctbc-field">
              <input :value="profile.idNumber" class="jb-input" disabled />
            </div>
          </div>
          <div class="ctbc-row">
            <label class="ctbc-label">生日</label>
            <div class="ctbc-field">
              <input :value="profile.birthday" class="jb-input" disabled />
            </div>
          </div>
          <div class="ctbc-row">
            <label class="ctbc-label">性別</label>
            <div class="ctbc-field">
              <input :value="genderMap[profile.gender] || profile.gender" class="jb-input" disabled />
            </div>
          </div>

          <div class="divider" style="margin: 16px 0; border-top: 1px solid var(--border);"></div>

          <h4 class="sub-section-title">聯絡資料</h4>
          <div class="ctbc-row">
            <label class="ctbc-label">手機號碼</label>
            <div class="ctbc-field">
              <input v-model="editForm.phone" type="tel" class="jb-input" />
              <span class="field-hint">範例：0912987654</span>
            </div>
          </div>
          <div class="ctbc-row">
            <label class="ctbc-label">戶籍地址</label>
            <div class="ctbc-field">
              <input :value="profile.registeredAddress" type="text" class="jb-input" disabled style="width: 100%; max-width: 400px;" />
            </div>
          </div>
          <div class="ctbc-row">
            <label class="ctbc-label">通訊地址</label>
            <div class="ctbc-field">
              <input v-model="editForm.address" type="text" class="jb-input" style="width: 100%; max-width: 400px;" />
            </div>
          </div>

          <div class="divider" style="margin: 16px 0; border-top: 1px solid var(--border);"></div>

          <h4 class="sub-section-title">職業與財務資料</h4>
          <div class="ctbc-row">
            <label class="ctbc-label">行業</label>
            <div class="ctbc-field" style="flex-wrap: wrap;">
              <select v-model="editForm.jobSelect" class="jb-input" style="width: 200px;">
                <option value="">請選擇</option>
                <option v-for="opt in jobOptions" :key="opt" :value="opt">{{ opt }}</option>
              </select>
              <input v-if="editForm.jobSelect === '其他'" v-model="editForm.jobOther" type="text" class="jb-input" placeholder="請填寫行業" style="width: 200px;" />
            </div>
          </div>
          <div class="ctbc-row">
            <label class="ctbc-label">職稱</label>
            <div class="ctbc-field" style="flex-wrap: wrap;">
              <select v-model="editForm.occupationSelect" class="jb-input" style="width: 200px;">
                <option value="">請選擇</option>
                <option v-for="opt in occupationOptions" :key="opt" :value="opt">{{ opt }}</option>
              </select>
              <input v-if="editForm.occupationSelect === '其他'" v-model="editForm.occupationOther" type="text" class="jb-input" placeholder="請填寫職稱" style="width: 200px;" />
            </div>
          </div>
          <div class="ctbc-row">
            <label class="ctbc-label">公司/學校名稱</label>
            <div class="ctbc-field">
              <input v-model="editForm.employer" type="text" class="jb-input" style="width: 100%; max-width: 400px;" />
            </div>
          </div>
          <div class="ctbc-row">
            <label class="ctbc-label">收入及資產來源</label>
            <div class="ctbc-field" style="flex-wrap: wrap;">
              <select v-model="editForm.fundSourceSelect" class="jb-input" style="width: 200px;">
                <option value="">請選擇</option>
                <option v-for="opt in fundSourceOptions" :key="opt" :value="opt">{{ opt }}</option>
              </select>
              <input v-if="editForm.fundSourceSelect === '其他'" v-model="editForm.fundSourceOther" type="text" class="jb-input" placeholder="請填寫來源" style="width: 200px;" />
            </div>
          </div>
          <div class="ctbc-row">
            <label class="ctbc-label">年收入</label>
            <div class="ctbc-field">
              <select v-model="editForm.annualIncome" class="jb-input" style="width: 200px;">
                <option value="">請選擇</option>
                <option v-for="option in annualIncomeOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </div>
          </div>
        </div>

        <!-- 底部按鈕區 -->
        <div class="form-actions">
          <button type="submit" class="jb-btn jb-btn-primary btn-action" :disabled="saving">
            <span v-if="saving" class="jb-spinner" style="width:18px;height:18px;"></span>
            <span>{{ saving ? '處理中...' : '儲存變更' }}</span>
          </button>
          <button type="button" class="jb-btn jb-btn-secondary btn-action" @click="resetForm">
            清除重填
          </button>
        </div>

      </form>
    </section>

    <!-- Custom Toast -->
    <transition name="toast-fade">
      <div v-if="toast.visible" class="jb-toast" :class="`toast-${toast.type}`">
        {{ toast.text }}
      </div>
    </transition>

    <!-- Custom Confirm Modal -->
    <transition name="modal-fade">
      <div v-if="confirmModal.visible" class="jb-modal-overlay">
        <div class="jb-modal jb-card">
          <h3 class="jb-modal-title">{{ confirmModal.title }}</h3>
          <p class="jb-modal-content">{{ confirmModal.content }}</p>
          <div class="jb-modal-actions">
            <button class="jb-btn jb-btn-secondary" @click="confirmModal.onCancel">取消</button>
            <button class="jb-btn jb-btn-primary" @click="confirmModal.onOk">確定離開</button>
          </div>
        </div>
      </div>
    </transition>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
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
const activeTab = ref('email')

const genderMap = { M: '男', F: '女' }

const profile = reactive({
  customerId: '',
  cif: '',
  name: '',
  idNumber: '',
  birthday: '',
  gender: '',
  username: '',
  email: '',
  phone: '',
  address: '',
  registeredAddress: '',
  job: '',
  occupation: '',
  employer: '',
  fundSource: '',
  annualIncome: null,
  avatarUrl: null,
})

const editForm = reactive({
  phone: '',
  email: '',
  emailConfirm: '',
  address: '',
  jobSelect: '',
  jobOther: '',
  occupationSelect: '',
  occupationOther: '',
  employer: '',
  fundSourceSelect: '',
  fundSourceOther: '',
  annualIncome: '',
})

const jobOptions = ['農林漁牧業', '礦業及土石採取業', '製造業', '水電燃氣業', '營造業', '批發及零售業', '住宿及餐飲業', '運輸及通信業', '金融及保險業', '不動產及租賃業', '專業科學及技術服務業', '教育服務業', '醫療保健及社會工作服務業', '藝術娛樂及休閒服務業', '家管/學生/退休/無業', '其他']
const occupationOptions = ['民意代表/高階主管', '專業人員', '技術員及助理專業人員', '事務支援人員', '服務及銷售工作人員', '農林漁牧業生產人員', '技藝有關工作人員', '機械設備操作及組裝人員', '基層技術工及勞力工', '軍人', '無', '其他']
const fundSourceOptions = ['薪資', '經營事業收入', '退休(職)金', '遺產繼承(含贈與)', '買賣房地產', '理財投資', '租金收入', '其他']
const annualIncomeOptions = [
  { value: 50, label: '50 萬元以下' },
  { value: 100, label: '51 - 100 萬元' },
  { value: 200, label: '101 - 200 萬元' },
  { value: 500, label: '201 - 500 萬元' },
  { value: 1000, label: '501 - 1000 萬元' },
  { value: 1001, label: '1001 萬元以上' },
]

const avatarSrc = ref(null)

function maskEmail(email) {
  if (!email) return ''
  const parts = email.split('@')
  if (parts.length !== 2) return email
  const name = parts[0]
  if (name.length <= 2) return `**@${parts[1]}`
  return `${name.slice(0, 2)}***${name.slice(-1)}@${parts[1]}`
}

const isDirty = computed(() => {
  if (editForm.email || editForm.emailConfirm) return true
  if (editForm.phone !== (profile.phone || '')) return true
  if (editForm.address !== (profile.address || '')) return true
  
  const currentJob = editForm.jobSelect === '其他' ? editForm.jobOther : editForm.jobSelect
  if (currentJob !== (profile.job || '')) return true
  
  const currentOcc = editForm.occupationSelect === '其他' ? editForm.occupationOther : editForm.occupationSelect
  if (currentOcc !== (profile.occupation || '')) return true
  
  const currentFund = editForm.fundSourceSelect === '其他' ? editForm.fundSourceOther : editForm.fundSourceSelect
  if (currentFund !== (profile.fundSource || '')) return true
  
  if (editForm.employer !== (profile.employer || '')) return true
  if (editForm.annualIncome !== normalizeAnnualIncomeRange(profile.annualIncome)) return true
  
  return false
})

function handleBeforeUnload(e) {
  if (isDirty.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}

// --- Custom Toast & Modal ---
const toast = reactive({ visible: false, text: '', type: 'success', timer: null })
function showToast(text, type = 'success') {
  toast.text = text
  toast.type = type
  toast.visible = true
  if (toast.timer) clearTimeout(toast.timer)
  toast.timer = setTimeout(() => { toast.visible = false }, 3000)
}

const confirmModal = reactive({
  visible: false,
  title: '',
  content: '',
  onOk: () => {},
  onCancel: () => {},
})

function showConfirm(title, content) {
  return new Promise((resolve) => {
    confirmModal.title = title
    confirmModal.content = content
    confirmModal.visible = true
    confirmModal.onOk = () => { confirmModal.visible = false; resolve(true) }
    confirmModal.onCancel = () => { confirmModal.visible = false; resolve(false) }
  })
}
// ----------------------------

onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  try {
    const res = await customerGetProfile()
    const data = res.data.data
    Object.assign(profile, data)
    resetForm()
    updateAvatarSrc(data.avatarUrl)
  } catch {
    showToast('載入個人資料失敗', 'error')
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
    showToast('大頭照更新成功', 'success')
  } catch {
    showToast('大頭照上傳失敗', 'error')
  }
}

async function handleSave() {
  if (activeTab.value === 'email' && editForm.email) {
    if (editForm.email !== editForm.emailConfirm) {
      showToast('兩次輸入的電子郵件不一致', 'warning')
      return
    }
  }

  saving.value = true
  
  const finalJob = editForm.jobSelect === '其他' ? editForm.jobOther : editForm.jobSelect
  const finalOcc = editForm.occupationSelect === '其他' ? editForm.occupationOther : editForm.occupationSelect
  const finalFund = editForm.fundSourceSelect === '其他' ? editForm.fundSourceOther : editForm.fundSourceSelect

  try {
    const res = await customerUpdateProfile({
      phone: editForm.phone,
      email: editForm.email || profile.email,
      address: editForm.address,
      job: finalJob,
      occupation: finalOcc,
      employer: editForm.employer,
      fundSource: finalFund,
      annualIncome: editForm.annualIncome ? Number(editForm.annualIncome) : null,
    })
    Object.assign(profile, res.data.data)
    if (customerAuthStore.customer) {
      customerAuthStore.setCustomer({
        ...customerAuthStore.customer,
        ...res.data.data,
        token: customerAuthStore.token,
      })
    }
    resetForm()
    showToast('個人資料已更新', 'success')
  } catch (err) {
    showToast(err.response?.data?.message || '更新失敗', 'error')
  } finally {
    saving.value = false
  }
}

function parseSelectOther(value, options) {
  if (!value) return { select: '', other: '' }
  if (options.includes(value)) return { select: value, other: '' }
  return { select: '其他', other: value }
}

function normalizeAnnualIncomeRange(value) {
  if (value === null || value === undefined || value === '') return ''
  const rawAmount = Number(value)
  if (Number.isNaN(rawAmount)) return ''
  const amountInTenThousands = rawAmount > 10000 ? rawAmount / 10000 : rawAmount
  if (amountInTenThousands <= 50) return 50
  if (amountInTenThousands <= 100) return 100
  if (amountInTenThousands <= 200) return 200
  if (amountInTenThousands <= 500) return 500
  if (amountInTenThousands <= 1000) return 1000
  return 1001
}

function resetForm() {
  editForm.phone = profile.phone || ''
  editForm.email = ''
  editForm.emailConfirm = ''
  editForm.address = profile.address || ''
  
  const j = parseSelectOther(profile.job, jobOptions)
  editForm.jobSelect = j.select
  editForm.jobOther = j.other
  
  const o = parseSelectOther(profile.occupation, occupationOptions)
  editForm.occupationSelect = o.select
  editForm.occupationOther = o.other
  
  const f = parseSelectOther(profile.fundSource, fundSourceOptions)
  editForm.fundSourceSelect = f.select
  editForm.fundSourceOther = f.other
  
  editForm.employer = profile.employer || ''
  editForm.annualIncome = normalizeAnnualIncomeRange(profile.annualIncome)
}

onUnmounted(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

onBeforeRouteLeave(async (to, from, next) => {
  if (isDirty.value) {
    const answer = await showConfirm('您有尚未儲存的變更', '確定要離開嗎？未儲存的資料將會遺失。')
    if (answer) {
      next()
    } else {
      next(false)
    }
  } else {
    next()
  }
})

async function handleRequestReset() {
  try {
    await customerRequestReset({
      email: profile.email,
      idNumber: profile.idNumber,
      birthday: profile.birthday
    })
    showToast('密碼重設連結已發送至您的電子信箱，請查收', 'success')
  } catch (err) {
    showToast(err.response?.data?.message || '發送失敗', 'error')
  }
}
</script>

<style scoped>
.profile-page {
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

.setting-selector {
  display: flex;
  align-items: center;
  gap: var(--space-6);
  padding: var(--space-4) var(--space-5);
  margin-bottom: var(--space-4);
  border-radius: 12px;
  background: rgba(255, 249, 239, 0.92);
  border: 1px solid rgba(214, 206, 195, 0.86);
  box-shadow: 0 12px 36px rgba(63, 74, 66, 0.08);
}

.setting-label {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 16px;
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
  transition:
    color var(--duration) var(--ease),
    opacity var(--duration) var(--ease);
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

.profile-content {
  padding: var(--space-5) var(--space-6);
  border-radius: 12px;
  background: rgba(255, 249, 239, 0.92);
  border: 1px solid rgba(214, 206, 195, 0.86);
  box-shadow: 0 12px 36px rgba(63, 74, 66, 0.08);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: var(--space-5);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid rgba(214, 206, 195, 0.8);
  color: var(--text-primary);
}

.sub-section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--space-2);
  margin-top: 0;
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
  width: 160px;
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

.field-hint {
  font-size: var(--text-xs);
  color: var(--text-secondary);
}

.divider {
  border-color: rgba(214, 206, 195, 0.8) !important;
}

.jb-input {
  min-height: 48px;
  color: var(--text-primary);
  background: rgba(250, 250, 247, 0.92);
  border: 1px solid rgba(214, 206, 195, 0.86);
  border-radius: 8px;
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
}

.jb-input:disabled {
  color: var(--text-secondary);
  background: rgba(234, 228, 218, 0.72);
  border-color: rgba(214, 206, 195, 0.86);
}

.password-field {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.avatar-tab {
  align-items: flex-start;
  gap: var(--space-5);
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
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
  width: 100px;
  height: 100px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid rgba(214, 206, 195, 0.86);
  display: block;
}

.avatar-fallback {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(234, 228, 218, 0.72);
  color: var(--primary);
  font-family: var(--font-heading);
  font-size: 40px;
  font-weight: 600;
  border: 3px solid rgba(214, 206, 195, 0.86);
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
  background: rgba(250, 250, 247, 0.84);
  border: 1px solid rgba(214, 206, 195, 0.86);
  border-radius: 8px;
  padding: var(--space-3) var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.cif-label {
  font-size: var(--text-xs);
  color: var(--text-secondary);
}

.cif-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 1px;
}

.form-actions {
  margin-top: 40px;
  display: flex;
  justify-content: center;
  gap: var(--space-4);
}

.btn-action {
  width: 180px;
}

.jb-btn {
  border-radius: 8px;
}

.jb-btn-primary {
  background: var(--primary);
  color: #fff;
  border-color: var(--primary);
}

.jb-btn-primary:hover {
  background: var(--primary-dark);
  border-color: var(--primary-dark);
  box-shadow: 0 4px 16px rgba(107, 95, 80, 0.1);
}

.jb-btn-secondary {
  background: rgba(255, 249, 239, 0.72);
  color: var(--primary-dark);
  border-color: rgba(214, 206, 195, 0.86);
}

.jb-btn-secondary:hover {
  border-color: var(--primary);
  background: var(--primary-light);
}

@media (max-width: 700px) {
  .profile-page {
    padding: 16px 0;
  }

  .setting-selector {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-3);
  }
  .ctbc-row {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-2);
  }
  .ctbc-label {
    width: 100%;
  }
  .ctbc-field {
    width: 100%;
    flex-wrap: wrap;
  }
  .form-actions {
    flex-direction: column;
  }
  .btn-action {
    width: 100%;
  }
}

.jb-toast {
  position: fixed;
  top: 40px;
  left: 50%;
  transform: translateX(-50%);
  padding: 12px 24px;
  background: rgba(255, 249, 239, 0.96);
  border: 1px solid rgba(214, 206, 195, 0.86);
  border-radius: 12px;
  box-shadow: 0 12px 36px rgba(63, 74, 66, 0.08);
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

.jb-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(43, 43, 43, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}
.jb-modal {
  width: 90%;
  max-width: 400px;
  padding: var(--space-6);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  border-radius: 12px;
  background: rgba(255, 249, 239, 0.96);
  border: 1px solid rgba(214, 206, 195, 0.86);
  box-shadow: 0 12px 36px rgba(63, 74, 66, 0.12);
}
.jb-modal-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
}
.jb-modal-content {
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.5;
}
.jb-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  margin-top: var(--space-2);
}
.modal-fade-enter-active, .modal-fade-leave-active { transition: opacity 0.2s var(--ease); }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
</style>
