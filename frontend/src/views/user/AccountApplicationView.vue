<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { submitAccountApplication, getMyAccountApplications } from '@/api/accountApplication'
import { getMyAccounts } from '@/api/customerAccount'
import { customerGetProfile } from '@/api/customerAuth'

const router = useRouter()

// ===== 步驟 =====
const step = ref(1)
const totalSteps = 5
const submitting = ref(false)
const submitted = ref(false)
const submitResult = ref(null)
const submitError = ref('')

// 已有申請記錄
const existingApplications = ref([])
const loadingApps = ref(true)
const existingAccounts = ref([])
const loadingAccounts = ref(true)

const UI_ACCOUNT_TYPE = {
  CHECKING_TWD: 'CHECKING_TWD',
  CHECKING_FOREIGN: 'CHECKING_FOREIGN',
  SUB_ACCOUNT: 'SUB_ACCOUNT',
  TIME_DEPOSIT: 'TIME_DEPOSIT',
}

// ===== 表單資料 =====
const form = reactive({
  // Step 1: 帳戶類型
  accountType: '',
  currency: '',

  // Step 2: 個人資料
  name: '',
  idNumber: '',
  birthday: '',
  gender: '',
  email: '',
  address: '',
  nationality: 'TW',
  phone: '',
  registeredAddress: '',
  currentAddress: '',
  sameAddress: false,

  // Step 3: 職業 & 資金
  occupation: '',
  employer: '',
  annualIncome: '',
  estimatedMonthlyTx: '',
  accountPurpose: '',
  fundSource: '',

  // Step 4: 法遵 & 證件
  taxResidency: 'TW',
  isPep: false,
  idFront: null,
  idBack: null,
  secondId: null,
})

// 預覽
const idFrontPreview = ref(null)
const idBackPreview = ref(null)
const secondIdPreview = ref(null)

// ===== 驗證 =====
const errors = reactive({})

function clearErrors() {
  Object.keys(errors).forEach(k => delete errors[k])
}

function validateStep(s) {
  clearErrors()
  if (s === 1) {
    if (!form.accountType) errors.accountType = '請選擇帳戶類型'
    if (showCurrency.value && !form.currency) errors.currency = '請選擇幣別'
  } else if (s === 2) {
    if (!form.name) errors.name = '請輸入姓名'
    if (!form.idNumber) errors.idNumber = '請輸入身分證字號'
    if (form.idNumber && !/^[A-Z]\d{9}$/.test(form.idNumber)) {
      errors.idNumber = '身分證字號格式不正確（1 個英文字加 9 個數字）'
    }
    if (!form.birthday) errors.birthday = '請選擇出生日期'
    if (!form.gender) errors.gender = '請選擇性別'
    if (!form.email) errors.email = '請輸入電子信箱'
    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      errors.email = '電子信箱格式不正確'
    }
    if (!form.phone) errors.phone = '請輸入手機號碼'
    if (form.phone && !/^09\d{8}$/.test(form.phone)) {
      errors.phone = '手機號碼格式不正確（09xxxxxxxx）'
    }
    if (!form.address) errors.address = '請輸入通訊地址'
    if (!form.registeredAddress) errors.registeredAddress = '請輸入戶籍地址'
    if (!form.currentAddress) errors.currentAddress = '請輸入現居地址'
  } else if (s === 3) {
    // 職業資訊非必填，不做強制驗證
  } else if (s === 4) {
    if (!form.idFront) errors.idFront = '請上傳身分證正面'
    if (!form.idBack) errors.idBack = '請上傳身分證反面'
    if (!form.secondId) errors.secondId = '請上傳第二證件'
  }
  return Object.keys(errors).length === 0
}

// ===== 步驟導航 =====
function nextStep() {
  if (validateStep(step.value)) {
    step.value++
  }
}

function prevStep() {
  clearErrors()
  step.value--
}

// ===== 地址同步 =====
function syncAddress() {
  if (form.sameAddress) {
    form.currentAddress = form.registeredAddress
  }
  if (!form.address) {
    form.address = form.currentAddress || form.registeredAddress
  }
}

// ===== 圖片壓縮 =====
function compressImage(file, maxWidth = 1200, quality = 0.8) {
  return new Promise((resolve) => {
    const img = new Image()
    img.onload = () => {
      const canvas = document.createElement('canvas')
      let w = img.width
      let h = img.height

      // 等比例縮小
      if (w > maxWidth) {
        h = Math.round((h * maxWidth) / w)
        w = maxWidth
      }

      canvas.width = w
      canvas.height = h
      const ctx = canvas.getContext('2d')
      ctx.drawImage(img, 0, 0, w, h)

      canvas.toBlob(
        (blob) => {
          const compressed = new File([blob], file.name, {
            type: 'image/jpeg',
            lastModified: Date.now(),
          })
          resolve(compressed)
        },
        'image/jpeg',
        quality
      )
    }
    img.src = URL.createObjectURL(file)
  })
}

// ===== 檔案處理 =====
async function handleFile(field, event) {
  const file = event.target.files[0]
  if (!file) return

  // 驗證格式
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    errors[field] = '僅支援 JPG / PNG 格式'
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    errors[field] = '檔案大小不可超過 5MB'
    return
  }

  delete errors[field]

  // 壓縮圖片（2.8MB → ~200KB）
  const compressed = await compressImage(file)
  form[field] = compressed

  // 產生預覽
  const reader = new FileReader()
  reader.onload = (e) => {
    if (field === 'idFront') idFrontPreview.value = e.target.result
    else if (field === 'idBack') idBackPreview.value = e.target.result
    else if (field === 'secondId') secondIdPreview.value = e.target.result
  }
  reader.readAsDataURL(compressed)
}

// ===== 帳戶類型選項 =====
const currencies = [
  { value: 'TWD', label: '新台幣 TWD' },
  { value: 'USD', label: '美元 USD' },
  { value: 'EUR', label: '歐元 EUR' },
  { value: 'JPY', label: '日圓 JPY' },
  { value: 'GBP', label: '英鎊 GBP' },
  { value: 'CNY', label: '人民幣 CNY' },
  { value: 'AUD', label: '澳幣 AUD' },
]

const hasActiveTwdChecking = computed(() =>
  existingAccounts.value.some(account =>
    account.status === 'ACTIVE' &&
    account.accountType === 'CHECKING' &&
    account.currency === 'TWD'
  )
)

const accountTypes = computed(() => {
  const options = []

  if (!hasActiveTwdChecking.value) {
    options.push({
      value: UI_ACCOUNT_TYPE.CHECKING_TWD,
      label: '台幣活期存款',
      desc: '日常存款與轉帳使用的主帳戶，開立後才能申請子帳戶',
    })
  }

  options.push(
    {
      value: UI_ACCOUNT_TYPE.CHECKING_FOREIGN,
      label: '外幣活期存款',
      desc: '可選擇外幣幣別，支援換匯與外幣資金收付',
    },
    {
      value: UI_ACCOUNT_TYPE.TIME_DEPOSIT,
      label: '定期存款',
      desc: '固定期限存款，可依需求選擇台幣或外幣',
    }
  )

  if (hasActiveTwdChecking.value) {
    options.push({
      value: UI_ACCOUNT_TYPE.SUB_ACCOUNT,
      label: '子帳戶',
      desc: '綁定您的台幣活存主帳戶，方便分帳與資金管理',
    })
  }

  return options
})

const purposeOptions = [
  { value: 'SALARY', label: '薪資轉帳' },
  { value: 'SAVINGS', label: '儲蓄' },
  { value: 'INVESTMENT', label: '理財投資' },
  { value: 'PAYMENT', label: '日常支付' },
  { value: 'FOREIGN_EXCHANGE', label: '外匯交易' },
  { value: 'OTHER', label: '其他' },
]

const fundSourceOptions = [
  { value: 'SALARY', label: '薪資收入' },
  { value: 'BUSINESS_INCOME', label: '營業收入' },
  { value: 'INVESTMENT', label: '投資收益' },
  { value: 'INHERITANCE', label: '繼承' },
  { value: 'RETIREMENT', label: '退休金' },
  { value: 'SAVINGS', label: '儲蓄' },
  { value: 'OTHER', label: '其他' },
]

const annualIncomeOptions = [
  { value: 50, label: '50 萬元以下' },
  { value: 100, label: '51 - 100 萬元' },
  { value: 200, label: '101 - 200 萬元' },
  { value: 500, label: '201 - 500 萬元' },
  { value: 1000, label: '501 - 1000 萬元' },
  { value: 1001, label: '1001 萬元以上' },
]

const showCurrency = computed(() =>
  form.accountType === UI_ACCOUNT_TYPE.CHECKING_FOREIGN ||
  form.accountType === UI_ACCOUNT_TYPE.TIME_DEPOSIT
)

const selectableCurrencies = computed(() => {
  if (form.accountType === UI_ACCOUNT_TYPE.CHECKING_FOREIGN) {
    return currencies.filter(currency => currency.value !== 'TWD')
  }
  return currencies
})

function resolvePayloadAccountType() {
  if (form.accountType === UI_ACCOUNT_TYPE.SUB_ACCOUNT) return 'SUB_ACCOUNT'
  if (form.accountType === UI_ACCOUNT_TYPE.TIME_DEPOSIT) return 'TIME_DEPOSIT'
  return 'CHECKING'
}

function resolvePayloadCurrency() {
  if (form.accountType === UI_ACCOUNT_TYPE.CHECKING_TWD || form.accountType === UI_ACCOUNT_TYPE.SUB_ACCOUNT) {
    return 'TWD'
  }
  return form.currency || 'TWD'
}

// ===== 提交 =====
async function handleSubmit() {
  if (!validateStep(4)) return

  submitting.value = true
  submitError.value = ''

  try {
    const formData = new FormData()

    // 文字欄位
    formData.append('accountType', resolvePayloadAccountType())
    formData.append('currency', resolvePayloadCurrency())
    formData.append('customerName', form.name)
    formData.append('idNumber', form.idNumber)
    formData.append('birthday', form.birthday)
    formData.append('gender', form.gender)
    formData.append('email', form.email)
    formData.append('address', form.address)
    formData.append('nationality', form.nationality)
    formData.append('phone', form.phone)
    formData.append('registeredAddress', form.registeredAddress)
    formData.append('currentAddress', form.currentAddress)
    if (form.occupation) formData.append('occupation', form.occupation)
    if (form.employer) formData.append('employer', form.employer)
    if (form.annualIncome) formData.append('annualIncome', form.annualIncome)
    if (form.estimatedMonthlyTx) formData.append('estimatedMonthlyTx', form.estimatedMonthlyTx)
    if (form.accountPurpose) formData.append('accountPurpose', form.accountPurpose)
    if (form.fundSource) formData.append('fundSource', form.fundSource)
    if (form.taxResidency) formData.append('taxResidency', form.taxResidency)
    formData.append('isPep', form.isPep)

    // 圖片
    formData.append('idFront', form.idFront)
    formData.append('idBack', form.idBack)
    formData.append('secondId', form.secondId)

    const result = await submitAccountApplication(formData)
    submitResult.value = result
    submitted.value = true
    step.value = totalSteps
  } catch (err) {
    submitError.value = err.response?.data?.message || '申請提交失敗，請稍後再試'
  } finally {
    submitting.value = false
  }
}

// ===== 載入既有申請 =====
async function fetchApplications() {
  loadingApps.value = true
  try {
    const list = await getMyAccountApplications()
    existingApplications.value = list || []
  } catch {
    // ignore
  } finally {
    loadingApps.value = false
  }
}

async function fetchAccounts() {
  loadingAccounts.value = true
  try {
    existingAccounts.value = await getMyAccounts() || []
  } catch {
    existingAccounts.value = []
  } finally {
    loadingAccounts.value = false
  }
}

async function fetchProfile() {
  try {
    const res = await customerGetProfile()
    const profile = res.data?.data || {}
    form.name = profile.name || form.name
    form.idNumber = profile.idNumber || form.idNumber
    form.birthday = profile.birthday || form.birthday
    form.gender = profile.gender || form.gender
    form.email = profile.email || form.email
    form.phone = profile.phone || form.phone
    form.nationality = profile.nationality || form.nationality
    form.registeredAddress = profile.registeredAddress || profile.address || form.registeredAddress
    form.currentAddress = profile.currentAddress || profile.address || form.currentAddress
    form.address = profile.address || profile.currentAddress || profile.registeredAddress || form.address
    form.occupation = profile.occupation || form.occupation
    form.employer = profile.employer || form.employer
    form.annualIncome = normalizeAnnualIncomeRange(profile.annualIncome) || form.annualIncome
    form.estimatedMonthlyTx = profile.estimatedMonthlyTx || form.estimatedMonthlyTx
    form.accountPurpose = profile.accountPurpose || form.accountPurpose
    form.fundSource = profile.fundSource || form.fundSource
    form.taxResidency = profile.taxResidency || form.taxResidency
    form.isPep = Boolean(profile.isPep)
  } catch {
    // ignore profile prefill failure
  }
}

function getStatusLabel(status) {
  const map = { PENDING: '審核中', APPROVED: '已核准', REJECTED: '已駁回', CANCELLED: '已取消' }
  return map[status] || status
}

function getStatusClass(status) {
  const map = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'muted' }
  return `status-${map[status] || 'warning'}`
}

function getAccountTypeLabel(app) {
  if (app.accountType === 'SUB_ACCOUNT') return '子帳戶'
  if (app.accountType === 'TIME_DEPOSIT') {
    return app.currency ? `${app.currency} 定期存款` : '定期存款'
  }
  if (app.accountType === 'CHECKING' && app.currency && app.currency !== 'TWD') {
    return `${app.currency} 外幣活期存款`
  }
  return '台幣活期存款'
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

// ===== 一鍵帶入測試資料 =====
async function fillMockData() {
  // Step 1
  form.accountType = accountTypes.value[0]?.value || UI_ACCOUNT_TYPE.CHECKING_TWD
  form.currency = form.accountType === UI_ACCOUNT_TYPE.CHECKING_FOREIGN ? 'USD' : 'TWD'

  // Step 2
  form.name = '王小明'
  form.idNumber = 'A123456789'
  form.birthday = '1990-06-15'
  form.gender = 'M'
  form.email = 'test.user@example.com'
  form.nationality = 'TW'
  form.phone = '0912345678'
  form.address = '台北市大安區忠孝東路四段100號5樓'
  form.registeredAddress = '台北市中正區重慶南路一段122號3樓'
  form.currentAddress = '台北市大安區忠孝東路四段100號5樓'
  form.sameAddress = false

  // Step 3
  form.occupation = '軟體工程師'
  form.employer = 'ABC 科技股份有限公司'
  form.annualIncome = 100
  form.estimatedMonthlyTx = 50
  form.accountPurpose = 'SALARY'
  form.fundSource = 'SALARY'

  // Step 4 - 法遵
  form.taxResidency = 'TW'
  form.isPep = false

  // Step 4 - 從 public/ 載入假證件圖片
  const [front, back, second] = await Promise.all([
    fetchAsFile('/id-front.png', 'id-front.png'),
    fetchAsFile('/id-back.png', 'id-back.png'),
    fetchAsFile('/second-id.png', 'second-id.png'),
  ])
  form.idFront = front
  form.idBack = back
  form.secondId = second

  idFrontPreview.value = '/id-front.png'
  idBackPreview.value = '/id-back.png'
  secondIdPreview.value = '/second-id.png'

  clearErrors()
}

watch(
  () => form.accountType,
  (nextValue) => {
    if (!showCurrency.value) {
      form.currency = ''
      return
    }
    if (nextValue === UI_ACCOUNT_TYPE.CHECKING_FOREIGN && form.currency === 'TWD') {
      form.currency = ''
    }
  }
)

watch(accountTypes, (options) => {
  const values = options.map(option => option.value)
  if (!values.includes(form.accountType)) {
    form.accountType = ''
    form.currency = ''
  }
})

/** 從 public 資料夾抓圖片，轉成 File 物件供 FormData 上傳 */
async function fetchAsFile(url, fileName) {
  const res = await fetch(url)
  const blob = await res.blob()
  return new File([blob], fileName, { type: blob.type || 'image/png' })
}

onMounted(async () => {
  await Promise.all([fetchApplications(), fetchAccounts(), fetchProfile()])
})
</script>

<template>
  <div class="acct-app-page">
    <!-- 頁面標題 -->
    <header class="page-header">
      <p class="eyebrow">ACCOUNT APPLICATION</p>
      <h1 class="page-title">開戶申請</h1>
      <div class="header-rule"></div>
      <p class="page-subtitle">請依序填寫以下資料，完成開戶申請程序</p>
      <button class="mock-btn" @click="fillMockData" v-if="!submitted">
        一鍵帶入測試資料
      </button>
    </header>

    <!-- 步驟指示器 -->
    <nav class="stepper" v-if="!submitted">
      <div
        v-for="s in 4"
        :key="s"
        class="stepper-item"
        :class="{ active: step === s, done: step > s }"
      >
        <div class="stepper-dot">
          <svg v-if="step > s" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
          <span v-else>{{ s }}</span>
        </div>
        <span class="stepper-label">{{ ['帳戶類型', '個人資料', '職業與資金', '證件上傳'][s - 1] }}</span>
      </div>
    </nav>

    <!-- Step 1: 帳戶類型 -->
    <section v-if="step === 1" class="step-card">
      <h2 class="step-title">選擇帳戶類型</h2>

      <div v-if="loadingAccounts" class="state-panel" style="margin-bottom: var(--space-4)">正在整理可申請的帳戶類型...</div>

      <div v-else class="type-grid">
        <label
          v-for="t in accountTypes"
          :key="t.value"
          class="type-option"
          :class="{ selected: form.accountType === t.value }"
        >
          <input type="radio" v-model="form.accountType" :value="t.value" class="sr-only" />
          <span class="type-name">{{ t.label }}</span>
          <span class="type-desc">{{ t.desc }}</span>
        </label>
      </div>
      <p v-if="errors.accountType" class="field-error">{{ errors.accountType }}</p>
      <p v-if="!loadingAccounts && hasActiveTwdChecking" class="helper-text">
        您已持有台幣活存主帳戶，因此本頁不再顯示台幣活存申請，並開放子帳戶申請。
      </p>

      <div v-if="showCurrency" class="jb-form-item" style="margin-top: var(--space-5)">
        <label class="jb-label">幣別</label>
        <select v-model="form.currency" class="jb-select">
          <option value="" disabled>請選擇幣別</option>
          <option v-for="c in selectableCurrencies" :key="c.value" :value="c.value">{{ c.label }}</option>
        </select>
        <p v-if="errors.currency" class="field-error">{{ errors.currency }}</p>
      </div>

      <div class="step-actions">
        <span></span>
        <button class="jb-btn jb-btn-primary" :disabled="loadingAccounts" @click="nextStep">下一步</button>
      </div>
    </section>

    <!-- Step 2: 個人資料 -->
    <section v-if="step === 2" class="step-card">
      <h2 class="step-title">填寫個人資料</h2>

      <div class="form-grid">
        <div class="jb-form-item">
          <label class="jb-label"><span class="jb-required">*</span> 姓名</label>
          <input v-model.trim="form.name" class="jb-input" placeholder="請輸入真實姓名" />
          <p v-if="errors.name" class="field-error">{{ errors.name }}</p>
        </div>

        <div class="jb-form-item">
          <label class="jb-label"><span class="jb-required">*</span> 身分證字號</label>
          <input v-model.trim="form.idNumber" class="jb-input" placeholder="A123456789" maxlength="10" />
          <p v-if="errors.idNumber" class="field-error">{{ errors.idNumber }}</p>
        </div>

        <div class="jb-form-item">
          <label class="jb-label"><span class="jb-required">*</span> 出生日期</label>
          <input v-model="form.birthday" type="date" class="jb-input" />
          <p v-if="errors.birthday" class="field-error">{{ errors.birthday }}</p>
        </div>

        <div class="jb-form-item">
          <label class="jb-label"><span class="jb-required">*</span> 性別</label>
          <select v-model="form.gender" class="jb-select">
            <option value="" disabled>請選擇性別</option>
            <option value="M">男</option>
            <option value="F">女</option>
          </select>
          <p v-if="errors.gender" class="field-error">{{ errors.gender }}</p>
        </div>

        <div class="jb-form-item">
          <label class="jb-label"><span class="jb-required">*</span> 國籍</label>
          <select v-model="form.nationality" class="jb-select">
            <option value="TW">中華民國</option>
            <option value="US">美國</option>
            <option value="JP">日本</option>
            <option value="OTHER">其他</option>
          </select>
        </div>

        <div class="jb-form-item span-2">
          <label class="jb-label"><span class="jb-required">*</span> 手機號碼</label>
          <input v-model.trim="form.phone" class="jb-input" placeholder="0912345678" maxlength="10" />
          <p v-if="errors.phone" class="field-error">{{ errors.phone }}</p>
        </div>

        <div class="jb-form-item span-2">
          <label class="jb-label"><span class="jb-required">*</span> 電子信箱</label>
          <input v-model.trim="form.email" type="email" class="jb-input" placeholder="請輸入電子信箱" />
          <p v-if="errors.email" class="field-error">{{ errors.email }}</p>
        </div>

        <div class="jb-form-item span-2">
          <label class="jb-label"><span class="jb-required">*</span> 戶籍地址</label>
          <input v-model.trim="form.registeredAddress" class="jb-input" placeholder="請輸入戶籍地址" @input="syncAddress" />
          <p v-if="errors.registeredAddress" class="field-error">{{ errors.registeredAddress }}</p>
        </div>

        <div class="jb-form-item span-2">
          <div style="display:flex; align-items:center; gap:var(--space-2); margin-bottom:var(--space-1)">
            <label class="jb-label" style="margin:0"><span class="jb-required">*</span> 現居地址</label>
            <label class="checkbox-inline">
              <input type="checkbox" v-model="form.sameAddress" @change="syncAddress" />
              <span>同戶籍地址</span>
            </label>
          </div>
          <input
            v-model.trim="form.currentAddress"
            class="jb-input"
            placeholder="請輸入現居地址"
            :disabled="form.sameAddress"
          />
          <p v-if="errors.currentAddress" class="field-error">{{ errors.currentAddress }}</p>
        </div>

        <div class="jb-form-item span-2">
          <label class="jb-label"><span class="jb-required">*</span> 通訊地址</label>
          <input v-model.trim="form.address" class="jb-input" placeholder="請輸入通訊地址" />
          <p v-if="errors.address" class="field-error">{{ errors.address }}</p>
        </div>
      </div>

      <div class="step-actions">
        <button class="jb-btn jb-btn-secondary" @click="prevStep">上一步</button>
        <button class="jb-btn jb-btn-primary" @click="nextStep">下一步</button>
      </div>
    </section>

    <!-- Step 3: 職業與資金 -->
    <section v-if="step === 3" class="step-card">
      <h2 class="step-title">職業與資金來源</h2>

      <div class="form-grid">
        <div class="jb-form-item">
          <label class="jb-label">職業</label>
          <input v-model.trim="form.occupation" class="jb-input" placeholder="如：軟體工程師" />
        </div>

        <div class="jb-form-item">
          <label class="jb-label">任職機構</label>
          <input v-model.trim="form.employer" class="jb-input" placeholder="公司名稱" />
        </div>

        <div class="jb-form-item">
          <label class="jb-label">年收入</label>
          <select v-model="form.annualIncome" class="jb-select">
            <option value="">請選擇</option>
            <option v-for="o in annualIncomeOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
        </div>

        <div class="jb-form-item">
          <label class="jb-label">預估月交易量（萬元）</label>
          <input v-model.number="form.estimatedMonthlyTx" type="number" class="jb-input" placeholder="例：50" min="0" />
        </div>

        <div class="jb-form-item">
          <label class="jb-label">開戶目的</label>
          <select v-model="form.accountPurpose" class="jb-select">
            <option value="">請選擇</option>
            <option v-for="o in purposeOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
        </div>

        <div class="jb-form-item span-2">
          <label class="jb-label">主要資金來源</label>
          <select v-model="form.fundSource" class="jb-select">
            <option value="">請選擇</option>
            <option v-for="o in fundSourceOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
          </select>
        </div>
      </div>

      <div class="step-actions">
        <button class="jb-btn jb-btn-secondary" @click="prevStep">上一步</button>
        <button class="jb-btn jb-btn-primary" @click="nextStep">下一步</button>
      </div>
    </section>

    <!-- Step 4: 法遵 & 證件上傳 -->
    <section v-if="step === 4 && !submitted" class="step-card">
      <h2 class="step-title">法遵聲明與證件上傳</h2>

      <!-- 法遵 -->
      <div class="compliance-section">
        <div class="jb-form-item">
          <label class="jb-label">稅務居住國</label>
          <select v-model="form.taxResidency" class="jb-select" style="max-width:300px">
            <option value="TW">中華民國</option>
            <option value="US">美國</option>
            <option value="JP">日本</option>
            <option value="OTHER">其他</option>
          </select>
        </div>

        <label class="pep-check">
          <input type="checkbox" v-model="form.isPep" />
          <div>
            <span class="pep-title">PEPs 聲明</span>
            <span class="pep-desc">本人或家庭成員（含配偶）目前或曾任重要政治性職務之人</span>
          </div>
        </label>
      </div>

      <!-- 證件上傳 -->
      <div class="upload-section">
        <h3 class="upload-heading">證件上傳<span class="upload-hint">（僅支援 JPG / PNG，單檔 5MB 以內）</span></h3>

        <div class="upload-grid">
          <div class="upload-item">
            <label class="upload-box" :class="{ filled: idFrontPreview }">
              <input type="file" accept="image/jpeg,image/png" class="sr-only" @change="handleFile('idFront', $event)" />
              <img v-if="idFrontPreview" :src="idFrontPreview" alt="身分證正面" />
              <div v-else class="upload-placeholder">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="m21 15-5-5L5 21"/></svg>
                <span>身分證正面</span>
              </div>
            </label>
            <p v-if="errors.idFront" class="field-error">{{ errors.idFront }}</p>
          </div>

          <div class="upload-item">
            <label class="upload-box" :class="{ filled: idBackPreview }">
              <input type="file" accept="image/jpeg,image/png" class="sr-only" @change="handleFile('idBack', $event)" />
              <img v-if="idBackPreview" :src="idBackPreview" alt="身分證反面" />
              <div v-else class="upload-placeholder">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="m21 15-5-5L5 21"/></svg>
                <span>身分證反面</span>
              </div>
            </label>
            <p v-if="errors.idBack" class="field-error">{{ errors.idBack }}</p>
          </div>

          <div class="upload-item">
            <label class="upload-box" :class="{ filled: secondIdPreview }">
              <input type="file" accept="image/jpeg,image/png" class="sr-only" @change="handleFile('secondId', $event)" />
              <img v-if="secondIdPreview" :src="secondIdPreview" alt="第二證件" />
              <div v-else class="upload-placeholder">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="8.5" cy="8.5" r="1.5"/><path d="m21 15-5-5L5 21"/></svg>
                <span>第二證件（健保卡/駕照）</span>
              </div>
            </label>
            <p v-if="errors.secondId" class="field-error">{{ errors.secondId }}</p>
          </div>
        </div>
      </div>

      <p v-if="submitError" class="submit-error">{{ submitError }}</p>

      <div class="step-actions">
        <button class="jb-btn jb-btn-secondary" @click="prevStep">上一步</button>
        <button class="jb-btn jb-btn-primary" @click="handleSubmit" :disabled="submitting">
          {{ submitting ? '提交中...' : '確認送出申請' }}
        </button>
      </div>
    </section>

    <!-- Step 5: 完成 -->
    <section v-if="step === totalSteps && submitted" class="step-card result-card">
      <div class="result-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><polyline points="16 9 10.5 14.5 8 12"/></svg>
      </div>
      <h2 class="step-title" style="text-align:center">申請已送出</h2>
      <p class="result-desc">
        您的開戶申請已成功送出，申請編號為
        <strong>#{{ submitResult?.id }}</strong>。
        我們將於 1-3 個工作天內完成審核，屆時將通知您審核結果。
      </p>
      <div class="step-actions" style="justify-content:center">
        <button class="jb-btn jb-btn-primary" @click="router.push({ name: 'user-home' })">
          回到首頁
        </button>
      </div>
    </section>

    <!-- 既有申請紀錄 -->
    <section v-if="!submitted" class="history-section">
      <h3 class="section-title">我的開戶申請紀錄</h3>

      <div v-if="loadingApps" class="state-panel">載入中...</div>
      <div v-else-if="existingApplications.length === 0" class="state-panel">尚無開戶申請紀錄</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>申請編號</th>
              <th>帳戶類型</th>
              <th>申請時間</th>
              <th>狀態</th>
              <th>帳號</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="app in existingApplications" :key="app.id">
              <td>#{{ app.id }}</td>
              <td>{{ getAccountTypeLabel(app) }}</td>
              <td>{{ app.createdAt?.substring(0, 10) || '-' }}</td>
              <td>
                <span class="status-pill" :class="getStatusClass(app.status)">
                  {{ getStatusLabel(app.status) }}
                </span>
              </td>
              <td>{{ app.createdAccountNumber || '-' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<style scoped>
.acct-app-page {
  max-width: 800px;
  margin: 0 auto;
  padding: var(--space-6) 0;
  display: grid;
  gap: var(--space-6);
}

/* === 頁面標題 === */
.page-header {
  text-align: center;
}

.eyebrow {
  font-family: var(--font-display);
  font-size: 14px;
  letter-spacing: 0.2em;
  color: var(--text-disabled);
  margin-bottom: var(--space-2);
}

.page-title {
  font-family: var(--font-heading);
  font-size: 52px;
  color: var(--text-primary);
  margin-bottom: var(--space-3);
}

.header-rule {
  width: 48px;
  height: 2px;
  background: var(--primary);
  margin: 0 auto var(--space-3);
}

.page-subtitle {
  color: var(--text-secondary);
  font-size: 16px;
}

.helper-text {
  margin-top: var(--space-5);
  padding: var(--space-3) var(--space-4);
  background: rgba(92, 107, 95, 0.06);
  border-left: 3px solid var(--primary);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: 1.6;
}

.mock-btn {
  margin-top: var(--space-3);
  padding: 8px 20px;
  font-size: 13px;
  font-family: var(--font-body);
  font-weight: 500;
  color: var(--accent);
  background: rgba(166, 90, 77, 0.06);
  border: 1px dashed var(--accent);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: background 0.2s;
}

.mock-btn:hover {
  background: rgba(166, 90, 77, 0.12);
}

/* === 步驟指示器 === */
.stepper {
  display: flex;
  justify-content: center;
  gap: var(--space-1);
}

.stepper-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  flex: 0 0 120px;
  position: relative;
}

.stepper-item:not(:last-child)::after {
  content: '';
  position: absolute;
  top: 16px;
  left: calc(50% + 20px);
  width: calc(100% - 40px);
  height: 1px;
  background: var(--border);
}

.stepper-item.done:not(:last-child)::after {
  background: var(--primary);
}

.stepper-dot {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid var(--border);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 600;
  color: var(--text-disabled);
  background: var(--bg-card);
  position: relative;
  z-index: 1;
  transition: all 0.2s;
}

.stepper-item.active .stepper-dot {
  border-color: var(--primary);
  color: #fff;
  background: var(--primary);
}

.stepper-item.done .stepper-dot {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--bg-card);
}

.stepper-label {
  font-size: 14px;
  color: var(--text-disabled);
  text-align: center;
  white-space: nowrap;
}

.stepper-item.active .stepper-label {
  color: var(--primary);
  font-weight: 600;
}

.stepper-item.done .stepper-label {
  color: var(--text-secondary);
}

/* === 步驟卡片 === */
.step-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.step-title {
  font-family: var(--font-heading);
  font-size: var(--text-h3);
  color: var(--text-primary);
  margin-bottom: var(--space-5);
}

.step-actions {
  display: flex;
  justify-content: space-between;
  margin-top: var(--space-6);
  gap: var(--space-3);
}

/* === 帳戶類型選擇 === */
.type-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: var(--space-4);
}

.type-option {
  cursor: pointer;
  border: 2px solid var(--border);
  border-radius: var(--radius-md);
  padding: var(--space-5);
  background: var(--bg-card);
  transition: all 0.2s;
  display: grid;
  gap: var(--space-2);
}

.type-option:hover {
  border-color: var(--primary-light);
}

.type-option.selected {
  border-color: var(--primary);
  background: rgba(92, 107, 95, 0.04);
}

.type-name {
  font-weight: 600;
  font-size: var(--text-base);
  color: var(--text-primary);
}

.type-desc {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

/* === 表單 grid === */
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

.span-2 {
  grid-column: span 2;
}

.field-error {
  color: var(--accent);
  font-size: var(--text-xs);
  margin-top: var(--space-1);
}

.checkbox-inline {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-sm);
  color: var(--text-secondary);
  cursor: pointer;
}

.checkbox-inline input {
  accent-color: var(--primary);
}

/* === 法遵 === */
.compliance-section {
  display: grid;
  gap: var(--space-5);
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-6);
  border-bottom: 1px solid var(--border);
}

.pep-check {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  cursor: pointer;
  padding: var(--space-4);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-card);
}

.pep-check input {
  margin-top: 3px;
  accent-color: var(--primary);
}

.pep-title {
  display: block;
  font-weight: 600;
  font-size: var(--text-sm);
  color: var(--text-primary);
  margin-bottom: var(--space-1);
}

.pep-desc {
  display: block;
  font-size: var(--text-xs);
  color: var(--text-secondary);
  line-height: 1.5;
}

/* === 上傳 === */
.upload-section {
  margin-bottom: var(--space-4);
}

.upload-heading {
  font-size: var(--text-base);
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: var(--space-4);
}

.upload-hint {
  font-weight: 400;
  font-size: var(--text-xs);
  color: var(--text-disabled);
  margin-left: var(--space-2);
}

.upload-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
}

.upload-box {
  display: flex;
  align-items: center;
  justify-content: center;
  aspect-ratio: 3 / 2;
  border: 2px dashed var(--border);
  border-radius: var(--radius-md);
  cursor: pointer;
  background: var(--bg-card);
  overflow: hidden;
  transition: border-color 0.2s;
}

.upload-box:hover {
  border-color: var(--primary);
}

.upload-box.filled {
  border-style: solid;
}

.upload-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  color: var(--text-disabled);
  font-size: var(--text-xs);
  padding: var(--space-3);
  text-align: center;
}

/* === 結果 === */
.result-card {
  text-align: center;
  padding: var(--space-8) var(--space-6);
}

.result-icon {
  margin-bottom: var(--space-4);
}

.result-desc {
  color: var(--text-secondary);
  font-size: var(--text-sm);
  line-height: 1.7;
  max-width: 400px;
  margin: 0 auto var(--space-4);
}

/* === 提交錯誤 === */
.submit-error {
  color: var(--accent);
  font-size: var(--text-sm);
  text-align: center;
  padding: var(--space-3);
  background: rgba(166, 90, 77, 0.06);
  border-radius: var(--radius-sm);
  margin-top: var(--space-4);
}

/* === 歷史紀錄 === */
.history-section {
  border-top: 1px solid var(--border);
  padding-top: var(--space-6);
}

.section-title {
  font-family: var(--font-heading);
  font-size: var(--text-h3);
  margin-bottom: var(--space-4);
}

.table-wrap {
  overflow-x: auto;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-card);
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: var(--space-3);
  border-bottom: 1px solid var(--border);
  text-align: left;
  white-space: nowrap;
}

th {
  color: var(--text-secondary);
  font-size: var(--text-xs);
  font-weight: 600;
  background: var(--bg-secondary);
}

tbody tr:last-child td {
  border-bottom: none;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 var(--space-2);
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: 600;
}

.status-success { color: #237804; background: #f6ffed; }
.status-danger  { color: #a8071a; background: #fff1f0; }
.status-warning { color: #ad6800; background: #fffbe6; }
.status-muted   { color: var(--text-disabled); background: var(--bg-secondary); }

.state-panel {
  min-height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0,0,0,0);
  border: 0;
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
  .span-2 {
    grid-column: span 1;
  }
  .upload-grid {
    grid-template-columns: 1fr;
  }
  .stepper {
    gap: 0;
  }
  .stepper-item {
    flex: 1;
    min-width: 0;
  }
  .stepper-label {
    font-size: 11px;
  }
}
</style>
