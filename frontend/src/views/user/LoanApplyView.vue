<template>
  <!-- 貸款申請首頁 -->
  <div class="apply-page">
    <div v-if="step === 'entry'" class="entry-wrap">
      <div class="entry-card">
        <div class="entry-deco">🏦</div>
        <h2 class="entry-title">貸款申請</h2>
        <p class="entry-desc">
          提供多元貸款方案，快速填寫申請資料，<br />
          專屬行員將於審核後主動與您聯繫。
        </p>

        <!-- 貸款類型預覽 -->
        <div class="loan-types-preview">
          <div v-for="t in LOAN_TYPE_LIST" :key="t.key" class="lt-chip" :class="'lt-' + t.key">
            <span class="lt-icon">{{ t.icon }}</span>
            <span class="lt-name">{{ t.name }}</span>
          </div>
        </div>

        <button class="btn btn-primary btn-lg" @click="step = 'select'">立即申請 →</button>
      </div>
    </div>

    <!-- 選擇身份 -->
    <div v-else-if="step === 'select'" class="step-wrap">
      <div class="step-header">
        <button class="back-btn" @click="step = 'entry'">← 返回</button>
        <div class="step-indicator">
          <span class="si active">1 選擇身份</span>
          <span class="si-sep">›</span>
          <span class="si">2 填寫資料</span>
          <span class="si-sep">›</span>
          <span class="si">3 送出完成</span>
        </div>
      </div>

      <h2 class="step-title">請選擇申請身份</h2>
      <p class="step-desc">依照您的會員狀態選擇對應的申請方式</p>

      <div class="identity-cards">
        <!-- 會員申請 -->
        <div class="id-card" @click="selectType('member')">
          <div class="id-card-icon">👤</div>
          <div class="id-card-body">
            <div class="id-card-title">用戶申請</div>
            <div class="id-card-desc">已擁有本行帳號的會員，使用顧客 ID 快速完成申請</div>
            <ul class="id-card-features">
              <li>✓ 資料自動帶入</li>
              <li>✓ 流程更簡便</li>
            </ul>
          </div>
          <div class="id-card-arrow">→</div>
        </div>

        <!-- 非會員申請 -->
        <div class="id-card" @click="selectType('nonmember')">
          <div class="id-card-icon">🌐</div>
          <div class="id-card-body">
            <div class="id-card-title">非用戶申請</div>
            <div class="id-card-desc">尚未加入會員，填寫基本個人資料後提交申請</div>
            <ul class="id-card-features">
              <li>✓ 免會員資格</li>
              <li>✓ 快速填寫送出</li>
            </ul>
          </div>
          <div class="id-card-arrow">→</div>
        </div>
      </div>
    </div>

    <!-- ══════════════════════════════════════════
         STEP 2：填寫表單
    ══════════════════════════════════════════ -->
    <div v-else-if="step === 'form'" class="step-wrap">
      <div class="step-header">
        <button class="back-btn" @click="step = 'select'">← 返回</button>
        <div class="step-indicator">
          <span class="si done">1 選擇身份</span>
          <span class="si-sep">›</span>
          <span class="si active">2 填寫資料</span>
          <span class="si-sep">›</span>
          <span class="si">3 送出完成</span>
        </div>
      </div>

      <div class="form-layout">
        <!-- ── 左：表單主體 ── -->
        <div class="form-main">
          <!-- 申請身份 badge -->
          <div
            class="applicant-tag"
            :class="applyType === 'member' ? 'tag-member' : 'tag-nonmember'"
          >
            {{ applyType === 'member' ? '👤 用戶申請' : '🌐 非用戶申請' }}
          </div>
          <button class="btn-demo" type="button" @click="fillDemo" title="假資料">一鍵帶入
          </button>

          <!-- 錯誤提示 -->
          <transition name="fade">
            <div v-if="submitError" class="form-error-banner">❌ {{ submitError }}</div>
          </transition>

          <!-- ── 非會員：個人資料 ── -->
          <div v-if="applyType === 'nonmember'" class="form-section">
            <div class="section-label">個人資料</div>
            <div class="field-grid-3">
              <div class="field" :class="{ 'field-error': errors.applicantName }">
                <label>姓名<span class="req">*</span></label>
                <input
                  v-model="form.applicantName"
                  placeholder="王小明"
                  @blur="validate('applicantName')"
                />
                <span class="err-msg" v-if="errors.applicantName">{{ errors.applicantName }}</span>
              </div>
              <div class="field" :class="{ 'field-error': errors.applicantPhone }">
                <label>電話<span class="req">*</span></label>
                <input
                  v-model="form.applicantPhone"
                  placeholder="09xxxxxxxx"
                  @blur="validate('applicantPhone')"
                />
                <span class="err-msg" v-if="errors.applicantPhone">{{
                  errors.applicantPhone
                }}</span>
              </div>
              <div class="field">
                <label>Email</label>
                <input v-model="form.applicantEmail" placeholder="example@mail.com" type="email" />
              </div>
            </div>
          </div>

          <!-- ── 會員：顧客 ID ── -->
          <div v-else class="form-section">
            <div class="section-label">會員資訊</div>
            <div class="field" :class="{ 'field-error': errors.customerId }">
              <label>顧客 ID<span class="req">*</span></label>
              <input
                v-model="form.customerId"
                type="text"
                placeholder="e.g. 0001"
                @blur="validate('customerId')"
              />
              <span class="err-msg" v-if="errors.customerId">{{ errors.customerId }}</span>
            </div>
          </div>

          <!-- ── 貸款內容 ── -->
          <div class="form-section">
            <div class="section-label">貸款內容</div>

            <!-- 類型選擇卡片 -->
            <div class="field" :class="{ 'field-error': errors.applyType }">
              <label>貸款類型<span class="req">*</span></label>
              <div class="type-card-grid">
                <div
                  v-for="t in LOAN_TYPE_LIST"
                  :key="t.key"
                  class="type-card"
                  :class="{ selected: form.applyType === t.key, ['tc-' + t.key]: true }"
                  @click="onTypeSelect(t.key)"
                >
                  <span class="tc-icon">{{ t.icon }}</span>
                  <span class="tc-name">{{ t.name }}</span>
                  <span class="tc-rate" v-if="rateRules?.types?.[t.key]">
                    {{ (rateRules.types[t.key].baseRate * 100).toFixed(1) }}% 起
                  </span>
                </div>
              </div>
              <span class="err-msg" v-if="errors.applyType">{{ errors.applyType }}</span>
            </div>

            <!-- 申請金額（獨立全寬） -->
            <div class="field" :class="{ 'field-error': errors.applyAmount }">
              <label>申請金額（TWD）<span class="req">*</span></label>
              <div class="input-prefix-wrap">
                <span class="input-prefix">$</span>
                <input
                  v-model.number="form.applyAmount"
                  type="number"
                  placeholder="500,000"
                  class="has-prefix"
                  @blur="validate('applyAmount')"
                />
              </div>
              <span class="field-hint" v-if="form.applyAmount > 0">
                {{ formatAmount(form.applyAmount) }}
              </span>
              <span class="err-msg" v-if="errors.applyAmount">{{ errors.applyAmount }}</span>
            </div>

            <!-- 申請期數（獨立全寬，按鈕 auto-fill 自動排列） -->
            <div class="field" :class="{ 'field-error': errors.applyPeriod }">
              <label>申請期數<span class="req">*</span></label>
              <div class="period-btns" v-if="availablePeriods.length > 0">
                <button
                  v-for="p in availablePeriods"
                  :key="p"
                  class="period-btn"
                  :class="{ active: form.applyPeriod === p }"
                  @click="onPeriodSelect(p)"
                  type="button"
                >
                  {{ p }} 月
                </button>
              </div>
              <div class="period-placeholder" v-else>請先選擇貸款類型</div>
              <span class="err-msg" v-if="errors.applyPeriod">{{ errors.applyPeriod }}</span>
            </div>
          </div>

          <!-- Submit -->
          <button class="btn btn-primary btn-submit" @click="submitForm" :disabled="submitting">
            <span v-if="submitting" class="spin">⟳</span>
            <span v-else>送出申請</span>
          </button>
        </div>

        <!-- ── 右：利率摘要卡 ── -->
        <div class="rate-sidebar">
          <div class="rate-card">
            <div class="rate-card-label">預估年利率</div>
            <div class="rate-big" v-if="computedRate !== null">
              {{ (computedRate * 100).toFixed(2) }}<span class="rate-unit">%</span>
            </div>
            <div class="rate-empty" v-else>—</div>

            <div class="rate-breakdown" v-if="computedRate !== null && form.applyType">
              <div class="rb-row">
                <span>基礎利率</span>
                <span>{{ (rateRules.types[form.applyType].baseRate * 100).toFixed(2) }}%</span>
              </div>
              <div class="rb-row" v-if="termPremium > 0">
                <span>期數加碼</span>
                <span class="rb-plus">+{{ (termPremium * 100).toFixed(2) }}%</span>
              </div>
              <div class="rb-row" v-else>
                <span>期數加碼</span>
                <span class="rb-zero">+0.00%</span>
              </div>
              <div class="rb-divider"></div>
              <div class="rb-row rb-total">
                <span>年利率合計</span>
                <span>{{ (computedRate * 100).toFixed(2) }}%</span>
              </div>
            </div>

            <!-- 每月還款 -->
            <div class="rate-hint" v-if="form.applyAmount && computedRate && form.applyPeriod">
              <div class="rh-row">
                <span>每月概估還款</span>
                <span class="rh-val">{{ estimatedMonthly }}</span>
              </div>
              <div class="rh-note">等額本息計算</div>
            </div>

            <div class="rate-tips">
              <div class="tip-title">申請說明</div>
              <ul>
                <li>送出後專員將於 3 個工作日內聯繫</li>
                <li>實際利率依信用評估結果為準</li>
                <li>申請不代表核貸，最終以書面通知為準</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ══════════════════════════════════════════
         STEP 3：送出完成
    ══════════════════════════════════════════ -->
    <div v-else-if="step === 'done'" class="step-wrap">
      <div class="done-card">
        <div class="done-icon">✅</div>
        <h2 class="done-title">申請已送出！</h2>
        <p class="done-desc">感謝您的申請，專員將於 3 個工作日內主動與您聯繫。</p>

        <div class="done-id-block">
          <span class="done-id-label">您的申請編號</span>
          <span class="done-id">{{ resultId }}</span>
        </div>

        <div class="done-summary">
          <div class="ds-row">
            <span>貸款類型</span>
            <span>{{ LOAN_TYPE_MAP[form.applyType] }}</span>
          </div>
          <div class="ds-row">
            <span>申請金額</span>
            <span>{{ formatAmount(form.applyAmount) }}</span>
          </div>
          <div class="ds-row">
            <span>申請期數</span>
            <span>{{ form.applyPeriod }} 個月</span>
          </div>
          <div class="ds-row">
            <span>預估年利率</span>
            <span class="ds-rate">{{
              computedRate ? (computedRate * 100).toFixed(2) + '%' : '—'
            }}</span>
          </div>
        </div>

        <button class="btn btn-outline btn-lg" @click="resetAll">返回</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import api from '@/api/axios'
import { BASE_URL } from '@/api/axios'

// ── Constants ──

const LOAN_TYPE_LIST = [
  { key: 'PERSONAL', name: '個人信貸', icon: '💳' },
  { key: 'CAR', name: '汽車貸款', icon: '🚗' },
  { key: 'MOTOR', name: '機車貸款', icon: '🛵' },
  { key: 'STUDENT', name: '學貸', icon: '🎓' },
  { key: 'BUSINESS', name: '創業貸款', icon: '🏢' },
  { key: 'HOUSE', name: '房屋貸款', icon: '🏠' },
  { key: 'LAND', name: '土地貸款', icon: '🌍' },
]
const LOAN_TYPE_MAP = Object.fromEntries(LOAN_TYPE_LIST.map((t) => [t.key, t.name]))

// ── State ──
const step = ref('entry') // entry | select | form | done
const applyType = ref('') // member | nonmember
const rateRules = ref(null)
const submitting = ref(false)
const submitError = ref('')
const resultId = ref('')

const form = reactive({
  // 共用
  applyType: '',
  applyAmount: null,
  applyPeriod: null,
  // 會員
  customerId: '',
  // 非會員
  applicantName: '',
  applicantPhone: '',
  applicantEmail: '',
})

const errors = reactive({
  customerId: '',
  applicantName: '',
  applicantPhone: '',
  applyType: '',
  applyAmount: '',
  applyPeriod: '',
})

// ── Computed ──
const availablePeriods = computed(() => {
  if (!rateRules.value || !form.applyType) return []
  return rateRules.value.types[form.applyType]?.periods || []
})

const termPremium = computed(() => {
  if (!rateRules.value || !form.applyPeriod) return 0
  return parseFloat(rateRules.value.termRates?.[String(form.applyPeriod)] ?? 0)
})

const computedRate = computed(() => {
  if (!rateRules.value || !form.applyType || !form.applyPeriod) return null
  const t = rateRules.value.types[form.applyType]
  if (!t) return null
  const base = parseFloat(t.baseRate)
  if (t.fixedRate) return base
  return parseFloat((base + termPremium.value).toFixed(5))
})

// 每月概估（等額本息簡算）
const estimatedMonthly = computed(() => {
  if (!computedRate.value || !form.applyAmount || !form.applyPeriod) return '—'
  const r = computedRate.value / 12
  const n = form.applyPeriod
  const pv = form.applyAmount
  if (r === 0) return formatAmount(Math.round(pv / n))
  const m = (pv * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1)
  return '$ ' + Math.round(m).toLocaleString('zh-TW')
})

// ── Watch ──
watch(
  () => form.applyType,
  () => {
    form.applyPeriod = null
  },
)

// ── Methods ──
async function loadRateRules() {
  try {
    const r = await api.get(`${BASE_URL}/api/loan-applications/rate-rules`)
    if (r.data.success) rateRules.value = r.data.data
  } catch {
    // fallback 內建規則
    rateRules.value = {
      types: {
        PERSONAL: { baseRate: 0.04, periods: [12, 24, 36, 48, 60] },
        CAR: { baseRate: 0.025, periods: [12, 24, 36, 48, 60] },
        MOTOR: { baseRate: 0.045, periods: [12, 24, 36] },
        STUDENT: { baseRate: 0.015, periods: [60, 84, 120], fixedRate: true },
        BUSINESS: { baseRate: 0.02, periods: [36, 60, 84] },
        HOUSE: { baseRate: 0.018, periods: [120, 240, 360, 480] },
        LAND: { baseRate: 0.028, periods: [120, 180, 240] },
      },
      termRates: {
        12: 0,
        24: 0.002,
        36: 0.005,
        48: 0.008,
        60: 0.01,
        84: 0.015,
        120: 0,
        180: 0.002,
        240: 0.004,
        360: 0.006,
        480: 0.008,
      },
    }
  }
}

function selectType(type) {
  applyType.value = type
  resetForm()
  step.value = 'form'
}

function onTypeSelect(key) {
  form.applyType = key
  errors.applyType = ''
}

function onPeriodSelect(p) {
  form.applyPeriod = p
  errors.applyPeriod = ''
}

// 單欄驗證
function validate(field) {
  switch (field) {
    case 'customerId':
      errors.customerId = !form.customerId ? '請填寫顧客 ID' : ''
      break
    case 'applicantName':
      errors.applicantName = !form.applicantName.trim() ? '請填寫姓名' : ''
      break
    case 'applicantPhone':
      errors.applicantPhone = !form.applicantPhone.trim()
        ? '請填寫電話'
        : !/^09\d{8}$/.test(form.applicantPhone)
          ? '請輸入有效的手機號碼'
          : ''
      break
    case 'applyType':
      errors.applyType = !form.applyType ? '請選擇貸款類型' : ''
      break
    case 'applyAmount':
      errors.applyAmount = !form.applyAmount
        ? '請填寫申請金額'
        : form.applyAmount <= 0
          ? '金額必須大於 0'
          : ''
      break
    case 'applyPeriod':
      errors.applyPeriod = !form.applyPeriod ? '請選擇申請期數' : ''
      break
  }
}

// 全表驗證
function validateAll() {
  const fields =
    applyType.value === 'member'
      ? ['customerId', 'applyType', 'applyAmount', 'applyPeriod']
      : ['applicantName', 'applicantPhone', 'applyType', 'applyAmount', 'applyPeriod']
  fields.forEach(validate)
  return fields.every((f) => !errors[f])
}

async function submitForm() {
  submitError.value = ''
  if (!validateAll()) return

  submitting.value = true
  try {
    const payload = {
      applyType: form.applyType,
      applyAmount: form.applyAmount,
      applyPeriod: form.applyPeriod,
      rate: computedRate.value,
    }

    let res
    if (applyType.value === 'member') {
      res = await api.post(`${BASE_URL}/api/loan-applications/member`, {
        ...payload,
        customerId: form.customerId, // String，直接送出
      })
    } else {
      res = await api.post(`${BASE_URL}/api/loan-applications/non-member`, {
        ...payload,
        applicantName: form.applicantName,
        applicantPhone: form.applicantPhone,
        applicantEmail: form.applicantEmail,
      })
    }

    if (res.data.success) {
      resultId.value = res.data.data
      step.value = 'done'
    } else {
      submitError.value = res.data.message || '送出失敗，請稍後再試'
    }
  } catch (e) {
    submitError.value = e.response?.data?.message || '連線失敗，請確認網路後再試'
  } finally {
    submitting.value = false
  }
}

function fillDemo() {
  if (applyType.value === 'member') {
    Object.assign(form, {
      customerId: 'C10001',
      applyType: 'PERSONAL',
      applyAmount: 500000,
      applyPeriod: 36,
    })
  } else {
    Object.assign(form, {
      applicantName: '林美玲',
      applicantPhone: '0912345678',
      applicantEmail: 'meilinlin@gmail.com',
      applyType: 'CAR',
      applyAmount: 800000,
      applyPeriod: 60,
    })
  }
  Object.keys(errors).forEach((k) => (errors[k] = ''))
  submitError.value = ''
}

function resetForm() {
  Object.assign(form, {
    applyType: '',
    applyAmount: null,
    applyPeriod: null,
    customerId: '',
    applicantName: '',
    applicantPhone: '',
    applicantEmail: '',
  })
  Object.keys(errors).forEach((k) => (errors[k] = ''))
  submitError.value = ''
}

function resetAll() {
  resetForm()
  resultId.value = ''
  step.value = 'entry'
}

function formatAmount(n) {
  return n ? '$ ' + Number(n).toLocaleString('zh-TW') : '—'
}

// ── Lifecycle ──
onMounted(loadRateRules)
</script>

<style scoped>
/* ── Variables ── */
.apply-page {
  --accent: #2563eb;
  --accent-dim: rgba(37, 99, 235, 0.08);
  --accent-lt: rgba(37, 99, 235, 0.15);
  --bg: #f8fafc;
  --surface: #ffffff;
  --surface-2: #f1f5f9;
  --border: #e2e8f0;
  --border-2: #cbd5e1;
  --ink: #0f172a;
  --ink-2: #1e293b;
  --muted: #94a3b8;
  --muted-2: #64748b;
  --red: #dc2626;
  --green: #16a34a;

  min-height: 100vh;
  background: var(--bg);
  font-family: 'Noto Sans TC', sans-serif;
  color: var(--ink);
  padding: 48px 24px 80px;
}

/* ── Entry ── */
.entry-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 70vh;
}
.entry-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: 48px 40px;
  max-width: 540px;
  width: 100%;
  text-align: center;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.07);
}
.entry-deco {
  font-size: 52px;
  margin-bottom: 16px;
}
.entry-title {
  font-family: 'Noto Serif TC', serif;
  font-size: 26px;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 10px;
}
.entry-desc {
  font-size: 14px;
  color: var(--muted-2);
  line-height: 1.8;
  margin-bottom: 24px;
}

.loan-types-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-bottom: 28px;
}
.lt-chip {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 12px;
  border: 1px solid var(--border);
  background: var(--surface-2);
  color: var(--muted-2);
}
.lt-chip .lt-icon {
  font-size: 14px;
}

/* ── Step Layout ── */
.step-wrap {
  width: 100%;
  max-width: 960px;
  margin: 0 auto;
}

.step-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
}
.back-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 13px;
  color: var(--muted-2);
  font-family: 'Noto Sans TC', sans-serif;
  padding: 6px 0;
  transition: color 0.15s;
}
.back-btn:hover {
  color: var(--accent);
}
.step-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-family: 'IBM Plex Mono', monospace;
}
.si {
  color: var(--muted);
}
.si.active {
  color: var(--accent);
  font-weight: 600;
}
.si.done {
  color: var(--green);
}
.si-sep {
  color: var(--border-2);
}

.step-title {
  font-family: 'Noto Serif TC', serif;
  font-size: 24px;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 6px;
}
.step-desc {
  font-size: 14px;
  color: var(--muted-2);
  margin-bottom: 28px;
}

/* ── Identity Cards ── */
.identity-cards {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  max-width: 860px;
  margin: 0 auto;
}
.id-card {
  background: var(--surface);
  border: 1.5px solid var(--border);
  border-radius: 14px;
  padding: 24px;
  cursor: pointer;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  transition: all 0.2s;
  position: relative;
  overflow: hidden;
}
.id-card:hover {
  border-color: var(--accent);
  box-shadow: 0 6px 24px rgba(37, 99, 235, 0.1);
  transform: translateY(-2px);
}
.id-card-icon {
  font-size: 32px;
  flex-shrink: 0;
}
.id-card-body {
  flex: 1;
}
.id-card-title {
  font-family: 'Noto Serif TC', serif;
  font-size: 17px;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 6px;
}
.id-card-desc {
  font-size: 13px;
  color: var(--muted-2);
  line-height: 1.6;
  margin-bottom: 10px;
}
.id-card-features {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.id-card-features li {
  font-size: 12px;
  color: var(--green);
}
.id-card-arrow {
  font-size: 20px;
  color: var(--muted);
  align-self: center;
  transition: all 0.2s;
}
.id-card:hover .id-card-arrow {
  color: var(--accent);
  transform: translateX(4px);
}

/* ── Form Layout ── */
.form-layout {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 24px;
  align-items: start;
  width: 100%; /* 強制撐滿父容器，不讓 member 短內容縮排版 */
  max-width: 860px;
  margin: 0 auto;
}
.form-main {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 0;
  min-width: 0; /* 防止 grid 子元素溢出 */
}

.applicant-tag {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 20px;
  font-family: 'IBM Plex Mono', monospace;
}
.tag-member {
  background: rgba(37, 99, 235, 0.08);
  color: var(--accent);
  border: 1px solid rgba(37, 99, 235, 0.2);
}
.tag-nonmember {
  background: rgba(22, 163, 74, 0.08);
  color: var(--green);
  border: 1px solid rgba(22, 163, 74, 0.2);
}

.form-error-banner {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: var(--red);
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 13px;
  margin-bottom: 16px;
}

.form-section {
  margin-bottom: 24px;
}
.section-label {
  font-size: 11px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-family: 'IBM Plex Mono', monospace;
  font-weight: 600;
  color: var(--muted-2);
  margin-bottom: 14px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border);
}

/* Fields */
.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin-bottom: 14px;
}
.field:last-child {
  margin-bottom: 0;
}
.field label {
  font-size: 12px;
  color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
}
.req {
  color: var(--red);
  margin-left: 2px;
}
.field input {
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: 8px;
  color: var(--ink);
  font-family: 'Noto Sans TC', sans-serif;
  font-size: 14px;
  padding: 10px 14px;
  outline: none;
  transition:
    border-color 0.15s,
    box-shadow 0.15s;
}
.field input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-dim);
}
.field-error input {
  border-color: var(--red);
}
.err-msg {
  font-size: 11px;
  color: var(--red);
}
.field-hint {
  font-size: 11px;
  color: var(--muted);
  font-family: 'IBM Plex Mono', monospace;
}

.field-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.field-grid-3 {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* Input with prefix */
.input-prefix-wrap {
  position: relative;
}
.input-prefix {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 14px;
  color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
  pointer-events: none;
}
.field input.has-prefix {
  padding-left: 28px;
}

/* Type card grid */
.type-card-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 4px;
}
.type-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 8px;
  border-radius: 10px;
  border: 1.5px solid var(--border);
  background: var(--surface);
  cursor: pointer;
  transition: all 0.15s;
  text-align: center;
}
.type-card:hover {
  border-color: var(--accent);
  background: var(--accent-dim);
}
.type-card.selected {
  border-color: var(--accent);
  background: var(--accent-dim);
  box-shadow: 0 0 0 2px var(--accent-lt);
}
.tc-icon {
  font-size: 20px;
}
.tc-name {
  font-size: 11px;
  font-weight: 600;
  color: var(--ink-2);
}
.tc-rate {
  font-size: 10px;
  color: var(--green);
  font-family: 'IBM Plex Mono', monospace;
}

/* Period buttons */
.period-btns {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
  gap: 8px;
}
.period-btn {
  padding: 7px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-family: 'IBM Plex Mono', monospace;
  cursor: pointer;
  border: 1.5px solid var(--border);
  background: var(--surface);
  color: var(--muted-2);
  transition: all 0.15s;
}
.period-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.period-btn.active {
  border-color: var(--accent);
  background: var(--accent-dim);
  color: var(--accent);
  font-weight: 600;
}
.period-placeholder {
  font-size: 13px;
  color: var(--muted);
  font-style: italic;
  padding: 8px 0;
}

/* ── Rate Sidebar ── */
.rate-sidebar {
  position: sticky;
  top: 80px;
}
.rate-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}
.rate-card-label {
  font-size: 11px;
  color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  margin-bottom: 8px;
}
.rate-big {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 44px;
  font-weight: 700;
  color: var(--accent);
  line-height: 1;
  margin-bottom: 16px;
}
.rate-unit {
  font-size: 22px;
}
.rate-empty {
  font-size: 36px;
  color: var(--muted);
  margin-bottom: 16px;
  font-family: 'IBM Plex Mono', monospace;
}

.rate-breakdown {
  background: var(--surface-2);
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 12px;
}
.rb-row {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
  padding: 3px 0;
}
.rb-plus {
  color: var(--red);
}
.rb-zero {
  color: var(--muted);
}
.rb-divider {
  border-top: 1px solid var(--border);
  margin: 6px 0;
}
.rb-total {
  color: var(--ink);
  font-weight: 600;
  font-size: 13px;
}

.rate-hint {
  margin-bottom: 12px;
}
.rh-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  font-size: 12px;
  color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
}
.rh-val {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
}
.rh-note {
  font-size: 10px;
  color: var(--muted);
  text-align: right;
  margin-top: 2px;
}

/* 計算公式 */
.rate-formula {
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 12px;
}
.formula-title {
  font-size: 11px;
  font-weight: 600;
  color: #0369a1;
  margin-bottom: 8px;
}
.formula-row {
  display: flex;
  flex-direction: column;
  gap: 2px;
  margin-bottom: 6px;
}
.formula-label {
  font-size: 10px;
  color: #0284c7;
  font-family: 'IBM Plex Mono', monospace;
  letter-spacing: 0.05em;
}
.formula-val {
  font-size: 12px;
  color: #0f172a;
  font-family: 'IBM Plex Mono', monospace;
}
.formula-math {
  letter-spacing: 0.03em;
  color: #0369a1;
  font-weight: 500;
}
.fixed-note {
  color: var(--green);
  font-size: 11px;
}
.formula-vars {
  font-size: 10px;
  color: #0284c7;
  font-family: 'IBM Plex Mono', monospace;
  border-top: 1px solid #bae6fd;
  padding-top: 6px;
  margin-top: 4px;
  line-height: 1.6;
}

.rate-tips {
  border-top: 1px solid var(--border);
  padding-top: 14px;
}
.tip-title {
  font-size: 11px;
  color: var(--muted-2);
  font-weight: 600;
  margin-bottom: 8px;
}
.rate-tips ul {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.rate-tips li {
  font-size: 11px;
  color: var(--muted);
  line-height: 1.5;
  padding-left: 10px;
  position: relative;
}
.rate-tips li::before {
  content: '·';
  position: absolute;
  left: 0;
}

/* ── Buttons ── */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: none;
  border-radius: 10px;
  font-family: 'Noto Sans TC', sans-serif;
  cursor: pointer;
  transition: all 0.15s;
  font-weight: 600;
}
.btn-primary {
  background: var(--accent);
  color: #fff;
}
.btn-primary:hover:not(:disabled) {
  background: #1d4ed8;
}
.btn-primary:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.btn-outline {
  background: var(--surface);
  color: var(--ink-2);
  border: 1.5px solid var(--border-2);
}
.btn-outline:hover {
  border-color: var(--accent);
  color: var(--accent);
}
.btn-lg {
  padding: 13px 32px;
  font-size: 15px;
  border-radius: 12px;
}
.btn-submit {
  width: 100%;
  padding: 13px;
  font-size: 15px;
  margin-top: 8px;
}

/* ── Done ── */
.done-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: 48px 40px;
  max-width: 540px;
  margin: 0 auto;
  text-align: center;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.07);
}
.done-icon {
  font-size: 56px;
  margin-bottom: 16px;
}
.done-title {
  font-family: 'Noto Serif TC', serif;
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 8px;
}
.done-desc {
  font-size: 14px;
  color: var(--muted-2);
  margin-bottom: 24px;
}
.done-id-block {
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 14px 20px;
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.done-id-label {
  font-size: 11px;
  color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}
.done-id {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 18px;
  font-weight: 700;
  color: var(--accent);
  letter-spacing: 0.05em;
}

.done-summary {
  background: var(--surface-2);
  border-radius: 10px;
  padding: 14px 18px;
  margin-bottom: 24px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  text-align: left;
}
.ds-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: var(--muted-2);
}
.ds-row span:last-child {
  color: var(--ink);
  font-weight: 500;
}
.ds-rate {
  color: var(--green) !important;
  font-family: 'IBM Plex Mono', monospace;
}

/* Misc */
.spin {
  animation: spin 0.8s linear infinite;
  display: inline-block;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
