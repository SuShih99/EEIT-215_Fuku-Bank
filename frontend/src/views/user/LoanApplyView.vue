<template>
  <div class="apply-page">

    <!-- ══════════════════════════════════════════
         首頁：三個服務按鈕
    ══════════════════════════════════════════ -->
    <div v-if="step === 'entry'" class="home-wrap">
      <div class="home-left">
        <h1 class="home-title">快速申請<br>專屬貸款方案</h1>
        <p class="home-desc">提供多元貸款方案，快速填寫申請資料，<br>專屬行員將於審核後主動與您聯繫。</p>

        <div class="home-stats">
          <div class="stat-item">
            <span class="stat-num">7+</span>
            <span class="stat-label">貸款類型</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-num">3天</span>
            <span class="stat-label">審核速度</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-num">1.5%</span>
            <span class="stat-label">最低年利率</span>
          </div>
        </div>

        <div class="home-actions">
          <button class="btn btn-primary btn-lg" @click="goApply">
            申請貸款
          </button>
          <button class="btn btn-action" @click="goStatus">
            查詢申貸狀態
          </button>
          <button class="btn btn-action" @click="$router.push({ name: 'user-loan-accounts' })">
            貸款帳戶
          </button>
        </div>
      </div>

      <div class="home-right">
        <div class="loan-showcase">
          <div
            v-for="t in LOAN_TYPE_LIST"
            :key="t.key"
            class="showcase-card"
            :class="{ active: showcaseIndex === LOAN_TYPE_LIST.indexOf(t) }"
          >
            <div class="sc-icon"><i :class="t.icon"></i></div>
            <div class="sc-name">{{ t.name }}</div>
            <div class="sc-rate" v-if="rateRules?.types?.[t.key]">
              年利率低至 <strong>{{ (rateRules.types[t.key].baseRate * 100).toFixed(1) }}%</strong>
            </div>
            <div class="sc-desc">{{ t.desc }}</div>
          </div>
          <div class="showcase-dots">
            <span
              v-for="(t, i) in LOAN_TYPE_LIST"
              :key="i"
              class="dot"
              :class="{ active: showcaseIndex === i }"
              @click="showcaseIndex = i"
            ></span>
          </div>
        </div>
      </div>
    </div>

    <!-- ══════════════════════════════════════════
         STEP 2：填寫表單
    ══════════════════════════════════════════ -->
    <div v-else-if="step === 'form'" class="step-wrap">
      <div class="step-header">
        <button class="back-btn" @click="step = 'entry'">← 返回</button>
        <div class="step-indicator">
          <span class="si active">1 填寫資料</span>
          <span class="si-sep">›</span>
          <span class="si">2 同意書確認</span>
          <span class="si-sep">›</span>
          <span class="si">3 送出完成</span>
        </div>
      </div>

      <div class="form-layout">

        <!-- ── 左：表單主體 ── -->
        <div class="form-main">

          <!-- 錯誤提示 -->
          <transition name="fade">
            <div v-if="submitError" class="form-error-banner">
              <i class="fa-solid fa-circle-xmark"></i> {{ submitError }}
            </div>
          </transition>

          <!-- ── 申請人識別 ── -->
          <div class="form-section">
            <div class="section-label">申請人識別</div>
            <div class="applicant-info-row">
              <div class="ai-item">
                <span class="ai-label">顧客識別碼（CIF）</span>
                <span class="ai-value">{{ customerCif || '—' }}</span>
              </div>
              <div class="ai-item">
                <span class="ai-label">使用者名稱</span>
                <span class="ai-value">{{ customerName || '—' }}</span>
              </div>
            </div>
          </div>

          <!-- ── 入帳帳戶 ── -->
          <div class="form-section">
            <div class="section-label">入帳帳戶</div>

            <!-- Loading -->
            <div v-if="accountsLoading" class="acct-loading">
              <span class="spin">⟳</span> 載入帳戶中…
            </div>

            <!-- 無符合帳戶 -->
            <div v-else-if="twdCheckingAccounts.length === 0 && !accountsError" class="acct-empty">
              <span class="acct-empty-icon"><i class="fa-solid fa-triangle-exclamation"></i></span>
              <div>
                <div class="acct-empty-title">您名下目前沒有正常的台幣活存帳戶</div>
                <div class="acct-empty-sub">請開立新台幣活存帳戶後再申請貸款</div>
              </div>
            </div>

            <!-- 載入錯誤 -->
            <div v-else-if="accountsError" class="acct-error">
              <span><i class="fa-solid fa-triangle-exclamation"></i></span> {{ accountsError }}
            </div>

            <!-- 下拉選單 -->
            <div v-else class="field" :class="{ 'field-error': errors.disbursementAccount }">
              <label>選擇入帳帳戶<span class="req">*</span></label>
              <div class="acct-select-wrap">
                <select
                  class="acct-select"
                  v-model="form.disbursementAccount"
                  @change="errors.disbursementAccount = ''"
                >
                  <option value="">— 請選擇入帳帳戶 —</option>
                  <option
                    v-for="acct in twdCheckingAccounts"
                    :key="acct.accountNumber"
                    :value="acct.accountNumber"
                  >
                    {{ acct.accountNumber }} ｜ 餘額 $ {{ formatAcctBalance(acct.balance) }}
                  </option>
                </select>
                <span class="acct-select-caret">▾</span>
              </div>
              <span class="err-msg" v-if="errors.disbursementAccount">{{ errors.disbursementAccount }}</span>

              <!-- 選中帳戶資訊卡 -->
              <transition name="fade">
                <div v-if="selectedAccountInfo" class="acct-info-card">
                  <div class="aic-row">
                    <span class="aic-label">帳號</span>
                    <span class="aic-val mono">{{ selectedAccountInfo.accountNumber }}</span>
                  </div>
                  <div class="aic-row">
                    <span class="aic-label">幣別</span>
                    <span class="aic-val">TWD ｜ 台幣活存</span>
                  </div>
                  <div class="aic-row">
                    <span class="aic-label">目前餘額</span>
                    <span class="aic-val green">$ {{ formatAcctBalance(selectedAccountInfo.balance) }}</span>
                  </div>
                </div>
              </transition>
            </div>

            <div class="id-notice" style="margin-top: 8px;">
              <span class="notice-icon">ℹ️</span>
              清單僅顯示您名下正常的台幣活存帳戶供選擇。
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
                  <span class="tc-icon"><i :class="t.icon"></i></span>
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

          <!-- Submit → 前往同意書 -->
          <button
            class="btn btn-primary btn-submit"
            @click="goToConsent"
            :disabled="submitting"
          >
            下一步：閱讀同意書 ›
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
         STEP 2：同意書確認
    ══════════════════════════════════════════ -->
    <div v-else-if="step === 'consent'" class="step-wrap">
      <div class="step-header">
        <button class="back-btn" @click="step = 'form'">← 返回</button>
        <div class="step-indicator">
          <span class="si done">1 填寫資料</span>
          <span class="si-sep">›</span>
          <span class="si active">2 同意書確認</span>
          <span class="si-sep">›</span>
          <span class="si">3 送出完成</span>
        </div>
      </div>

      <div class="consent-outer">
        <div class="consent-card">

          <div class="consent-heading">
            <span class="consent-icon"><i class="fa-solid fa-file-shield"></i></span>
            <div>
              <div class="consent-title">蒐集個人資料告知事項暨個人資料提供同意書</div>
              <div class="consent-subtitle">請完整閱讀以下內容後方可勾選同意</div>
            </div>
          </div>

          <!-- 全文內容 -->
          <div class="consent-body" ref="consentBox" @scroll="onConsentScroll">
            <p class="cb-section-title">蒐集個人資料告知事項：</p>
            <p>教育部（以下簡稱本部）為遵守個人資料保護法規定，在您提供個人資料予本部前，依法告知下列事項：</p>

            <p><strong>一、</strong>本部因辦理國立臺東專科學校校長遴選事務之特定目的而獲取之個人資料類別：</p>
            <p class="cb-indent">（1）辨識個人者：如姓名、職業、聯絡方式（包括但不限於電話號碼、E-MAIL、居住或工作地址）等</p>
            <p class="cb-indent">（2）辨識政府資料者：如國民身分證統一編號、護照號碼等</p>
            <p class="cb-indent">（3）個人描述：如年齡、性別、出生年月日等</p>
            <p class="cb-indent">（4）教育、考選、技術或其他專業：如學歷資格、專業技術、特別執照等</p>
            <p class="cb-indent">（5）或其他得以直接或間接識別個人之資料，詳如「國立臺東專科學校校長候選人資料表」。</p>

            <p><strong>二、</strong>本部將依個人資料保護法及相關法令之規定下，蒐集、處理及利用個人資料。</p>

            <p><strong>三、</strong>本部將於蒐集目的（即國立臺東專科學校校長遴選事務）之存續期間合理利用個人資料。</p>

            <p><strong>四、</strong>除蒐集之目的涉及國際業務或活動外，本部僅於中華民國領域內利用個人資料。</p>

            <p><strong>五、</strong>本部將於原蒐集之特定目的（即國立臺東專科學校校長遴選事務）之目的範圍內，合理利用個人資料。</p>

            <p><strong>六、</strong>依個人資料保護法第3條規定，就您之個人資料向本部行使下列權利：</p>
            <p class="cb-indent">（一）查詢或請求閱覽。</p>
            <p class="cb-indent">（二）請求製給複製本。</p>
            <p class="cb-indent">（三）請求補充或更正。</p>
            <p class="cb-indent">（四）請求停止蒐集、處理及利用。</p>
            <p class="cb-indent">（五）請求刪除。</p>
            <p>您因行使上述權利而導致權益產生減損時，本部不負相關賠償責任。</p>

            <p><strong>七、</strong>若未提供正確之個人資料，本部將無法提供特定目的之相關業務。</p>

            <p><strong>八、</strong>您瞭解此一同意書符合個人資料保護法及相關法規之要求，且同意本部留存此同意書，供日後取出查驗。</p>
          </div>

          <!-- 捲動提示 -->
          <transition name="fade">
            <div v-if="!consentScrolled" class="consent-scroll-hint">
              <i class="fa-solid fa-angles-down"></i> 請向下捲動至底部閱讀完整內容
            </div>
          </transition>

          <!-- 勾選區 -->
          <div class="consent-checks" :class="{ locked: !consentScrolled }">
            <label class="check-item" :class="{ disabled: !consentScrolled }">
              <input
                type="checkbox"
                v-model="consentCheck1"
                :disabled="!consentScrolled"
                class="check-input"
              />
              <span class="check-box-ui"></span>
              <span class="check-text">一、本人已充分知悉貴部上述告知事項。</span>
            </label>
            <label class="check-item" :class="{ disabled: !consentScrolled }">
              <input
                type="checkbox"
                v-model="consentCheck2"
                :disabled="!consentScrolled"
                class="check-input"
              />
              <span class="check-box-ui"></span>
              <span class="check-text">二、本人同意貴部蒐集、處理、利用本人之個人資料。</span>
            </label>
          </div>

          <!-- 送出 -->
          <transition name="fade">
            <div v-if="submitError" class="form-error-banner" style="margin-bottom:12px">
              <i class="fa-solid fa-circle-xmark"></i> {{ submitError }}
            </div>
          </transition>

          <button
            class="btn btn-primary btn-submit"
            :disabled="!consentCheck1 || !consentCheck2 || submitting"
            @click="submitForm"
          >
            <span v-if="submitting" class="spin">⟳</span>
            <span v-else>確認同意並送出申請</span>
          </button>

        </div>
      </div>
    </div>

    <!-- ══════════════════════════════════════════
         STEP 3：送出完成
    ══════════════════════════════════════════ -->
    <div v-else-if="step === 'done'" class="step-wrap">
      <div class="done-card">
        <div class="done-icon"><i class="fa-solid fa-circle-check"></i></div>
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
            <span class="ds-rate">{{ computedRate ? (computedRate * 100).toFixed(2) + '%' : '—' }}</span>
          </div>
          <div class="ds-row">
            <span>顧客識別碼（CIF）</span>
            <span class="ds-mono">{{ customerCif || '—' }}</span>
          </div>
        </div>

        <button class="btn btn-outline btn-lg" @click="resetAll">回到貸款首頁</button>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import axios from 'axios'
import { useCustomerAuthStore } from '@/stores/customerAuth'
import { getMyAccounts } from '@/api/customerAccount'
import { useRouter } from 'vue-router'
const BASE_URL = import.meta.env.VITE_API_BASE_URL || ''
const router = useRouter()

const LOAN_TYPE_LIST = [
  { key: 'PERSONAL', name: '個人信貸', icon: 'fa-solid fa-address-card',  desc: '靈活周轉，快速撥款到帳'      },
  { key: 'CAR',      name: '汽車貸款', icon: 'fa-solid fa-car',           desc: '購車首選，利率優惠方案'      },
  { key: 'MOTOR',    name: '機車貸款', icon: 'fa-solid fa-motorcycle',    desc: '輕鬆入手，分期無壓力'        },
  { key: 'STUDENT',  name: '學貸',     icon: 'fa-solid fa-user-graduate', desc: '安心就學，低利固定利率'      },
  { key: 'BUSINESS', name: '創業貸款', icon: 'fa-solid fa-briefcase',     desc: '創業圓夢，專業諮詢服務'      },
  { key: 'HOUSE',    name: '房屋貸款', icon: 'fa-solid fa-house-chimney', desc: '長期低利，安心置產的最佳選擇' },
  { key: 'LAND',     name: '土地貸款', icon: 'fa-solid fa-earth-asia',    desc: '土地投資，靈活運用資金'      },
]
const LOAN_TYPE_MAP = Object.fromEntries(LOAN_TYPE_LIST.map(t => [t.key, t.name]))
const showcaseIndex = ref(0)
let showcaseTimer = null
const customerAuthStore = useCustomerAuthStore()
const customerCif  = computed(() => customerAuthStore.customer?.cif  || '')
const customerName = computed(() => customerAuthStore.customer?.name || '')
const accounts        = ref([])
const accountsLoading = ref(false)
const accountsError   = ref('')
const accountsLoaded  = ref(false)

const twdCheckingAccounts = computed(() =>
  accounts.value.filter(
    a => a.currency === 'TWD' &&
         a.accountType === 'CHECKING' &&
         a.status === 'ACTIVE'
  )
)
const step        = ref('entry')
const rateRules   = ref(null)
const submitting  = ref(false)
const submitError = ref('')
const resultId    = ref('')
const consentBox      = ref(null)   // template ref
const consentScrolled = ref(false)
const consentCheck1   = ref(false)
const consentCheck2   = ref(false)

const form = reactive({
  applyType:           '',
  applyAmount:         null,
  applyPeriod:         null,
  disbursementAccount: '',
})

const errors = reactive({
  applyType:           '',
  applyAmount:         '',
  applyPeriod:         '',
  disbursementAccount: '',
})
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
  const r  = computedRate.value / 12
  const n  = form.applyPeriod
  const pv = form.applyAmount
  if (r === 0) return formatAmount(Math.round(pv / n))
  const m = pv * r * Math.pow(1 + r, n) / (Math.pow(1 + r, n) - 1)
  return '$ ' + Math.round(m).toLocaleString('zh-TW')
})
watch(() => form.applyType, () => {
  form.applyPeriod = null
})
async function loadRateRules() {
  try {
    const r = await axios.get(`${BASE_URL}/api/loan-applications/rate-rules`)
    if (r.data.success) rateRules.value = r.data.data
  } catch {
    // fallback 內建規則
    rateRules.value = {
      types: {
        PERSONAL: { baseRate: 0.04,  periods: [12,24,36,48,60] },
        CAR:      { baseRate: 0.025, periods: [12,24,36,48,60] },
        MOTOR:    { baseRate: 0.045, periods: [12,24,36] },
        STUDENT:  { baseRate: 0.015, periods: [60,84,120], fixedRate: true },
        BUSINESS: { baseRate: 0.02,  periods: [36,60,84] },
        HOUSE:    { baseRate: 0.018, periods: [120,240,360,480] },
        LAND:     { baseRate: 0.028, periods: [120,180,240] },
      },
      termRates: { '12':0,'24':0.002,'36':0.005,'48':0.008,'60':0.01,'84':0.015,'120':0,'180':0.002,'240':0.004,'360':0.006,'480':0.008 },
    }
  }
}

function goStatus() {
  router.push({ name: 'user-loan-status' })
}

function goApply() {
  resetForm()
  step.value = 'form'
}

// 驗證表單後前往同意書步驟
function goToConsent() {
  submitError.value = ''
  if (!validateAll()) return
  // 重置同意書狀態
  consentScrolled.value = false
  consentCheck1.value   = false
  consentCheck2.value   = false
  step.value = 'consent'
  // 等 DOM 渲染後嘗試偵測是否內容已短到不需捲動
  nextTick(() => {
    checkConsentScrollable()
  })
}

function checkConsentScrollable() {
  const el = consentBox.value
  if (!el) return
  // 如果內容短到不需捲動，直接解鎖
  if (el.scrollHeight <= el.clientHeight + 4) {
    consentScrolled.value = true
  }
}

function onConsentScroll() {
  if (consentScrolled.value) return
  const el = consentBox.value
  if (!el) return
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - 8) {
    consentScrolled.value = true
  }
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
    case 'applyType':
      errors.applyType = !form.applyType ? '請選擇貸款類型' : ''
      break
    case 'applyAmount':
      errors.applyAmount = !form.applyAmount
        ? '請填寫申請金額'
        : form.applyAmount <= 0 ? '金額必須大於 0' : ''
      break
    case 'applyPeriod':
      errors.applyPeriod = !form.applyPeriod ? '請選擇申請期數' : ''
      break
    case 'disbursementAccount':
      errors.disbursementAccount = !form.disbursementAccount ? '請選擇入帳帳戶' : ''
      break
  }
}

// 全表驗證
function validateAll() {
  const fields = ['applyType', 'applyAmount', 'applyPeriod', 'disbursementAccount']
  fields.forEach(validate)
  return fields.every(f => !errors[f])
}

async function submitForm() {
  submitError.value = ''
  if (!validateAll()) return

  submitting.value = true
  try {
    // customerId 由後端從 JWT Token 自動解析，前端無需傳遞
    const token = localStorage.getItem('customer_token')
    const res = await axios.post(
      `${BASE_URL}/api/loan-applications/member`,
      {
        applyType:           form.applyType,
        applyAmount:         form.applyAmount,
        applyPeriod:         form.applyPeriod,
        rate:                computedRate.value,
        disbursementAccount: form.disbursementAccount,
      },
      { headers: { Authorization: `Bearer ${token}` } }
    )
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

function resetForm() {
  Object.assign(form, { applyType: '', applyAmount: null, applyPeriod: null, disbursementAccount: '' })
  Object.keys(errors).forEach(k => errors[k] = '')
  submitError.value = ''
}

function resetAll() {
  resetForm()
  resultId.value = ''
  consentScrolled.value = false
  consentCheck1.value   = false
  consentCheck2.value   = false
  step.value = 'entry'
}

function formatAmount(n) {
  return n ? '$ ' + Number(n).toLocaleString('zh-TW') : '—'
}

function formatAcctBalance(n) {
  return n != null
    ? Number(n).toLocaleString('zh-TW', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    : '—'
}

const selectedAccountInfo = computed(() =>
  twdCheckingAccounts.value.find(a => a.accountNumber === form.disbursementAccount) || null
)

async function loadAccounts() {
  if (accountsLoaded.value) return
  accountsLoading.value = true
  accountsError.value = ''
  try {
    accounts.value = await getMyAccounts()
    accountsLoaded.value = true
  } catch (e) {
    accountsError.value = '帳戶載入失敗，請刷新頁面後再試'
  } finally {
    accountsLoading.value = false
  }
}
onMounted(() => {
  loadRateRules()
  loadAccounts()
  showcaseTimer = setInterval(() => {
    showcaseIndex.value = (showcaseIndex.value + 1) % LOAN_TYPE_LIST.length
  }, 2800)
})
onUnmounted(() => clearInterval(showcaseTimer))
</script>

<style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/7.0.1/css/all.min.css');

/* ── Variables ── */
.apply-page {
  --accent:      #A65A4D;
  --accent-dim:  rgba(166, 90, 77, 0.10);
  --accent-lt:   rgba(166, 90, 77, 0.20);
  --bg:          #F5F1EA;
  --surface:     rgba(255, 249, 239, 0.78);
  --surface-2:   rgba(234, 228, 218, 0.80);
  --border:      rgba(214, 206, 195, 0.92);
  --border-2:    rgba(196, 187, 176, 0.96);
  --ink:         #2B2B2B;
  --ink-2:       #3D3530;
  --muted:       #A89A8E;
  --muted-2:     #6E6259;
  --primary:     #5C6B5F;
  --primary-dk:  #3F4A42;
  --red:         #A65A4D;
  --green:       #5C6B5F;

  min-height: 100vh;
  background: var(--bg);
  font-family: 'Noto Sans TC', sans-serif;
  color: var(--ink);
  padding: 48px 24px 80px;
  border-radius: 10px;
}

/* ── Home ── */
.home-wrap {
  display: grid;
  grid-template-columns: minmax(320px, 1fr) 380px;
  gap: 48px;
  align-items: center;
  min-height: 80vh;
  max-width: 960px;
  margin: 0 auto;
  padding: 0 8px;
}
.home-left { display: flex; flex-direction: column; gap: 0; min-width: 0; }
.home-brand {
  display: inline-flex; align-items: center; gap: 8px;
  font-size: 13px; color: var(--muted-2);
  background: var(--surface); border: 1px solid var(--border);
  padding: 5px 14px; border-radius: 20px;
  margin-bottom: 24px; width: fit-content;
  white-space: nowrap;
}
.home-title {
  font-family: 'Noto Serif TC', serif;
  font-size: clamp(28px, 3.5vw, 48px);
  font-weight: 700; color: var(--ink);
  line-height: 1.25; margin-bottom: 16px;
  white-space: nowrap;
}
.home-desc { font-size: 14px; color: var(--muted-2); line-height: 1.8; margin-bottom: 28px; }

.home-stats {
  display: flex; align-items: center; gap: 0;
  background: var(--surface); border: 1px solid var(--border);
  border-radius: 18px; padding: 16px 0;
  margin-bottom: 32px;
  box-shadow: 0 10px 26px rgba(63,74,66,0.06);
}
.stat-item { flex: 1; text-align: center; }
.stat-num {
  display: block;
  font-family: 'IBM Plex Mono', monospace;
  font-size: 22px; font-weight: 700; color: var(--primary);
}
.stat-label { font-size: 12px; color: var(--muted-2); margin-top: 2px; display: block; white-space: nowrap; }
.stat-divider { width: 1px; height: 36px; background: var(--border); flex-shrink: 0; }

.home-actions { display: flex; flex-direction: column; gap: 10px; }

.btn-action {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 20px; border-radius: 10px;
  font-size: 14px; font-family: 'Noto Sans TC', sans-serif; font-weight: 500;
  background: var(--surface); color: var(--ink-2);
  border: 1.5px solid var(--border); cursor: pointer; transition: all 0.15s;
  white-space: nowrap;
}
.btn-action:not(:disabled):hover { border-color: var(--primary); color: var(--primary); }
.btn-action:disabled { opacity: 0.5; cursor: not-allowed; }
.coming-tag {
  font-size: 10px; color: var(--muted);
  background: var(--surface-2); border: 1px solid var(--border);
  padding: 2px 8px; border-radius: 10px;
  font-family: 'IBM Plex Mono', monospace;
}

/* ── Showcase ── */
.home-right { display: flex; align-items: center; justify-content: center; }
.loan-showcase {
  width: 100%; max-width: 360px;
  position: relative; min-height: 220px;
}
.showcase-card {
  background: var(--surface); border: 1.5px solid var(--border);
  border-radius: 22px; padding: 32px 28px;
  text-align: center;
  position: absolute; inset: 0;
  opacity: 0; transform: translateY(10px) scale(0.97);
  transition: opacity 0.5s, transform 0.5s;
  pointer-events: none;
  box-shadow: 0 10px 26px rgba(63,74,66,0.06);
}
.showcase-card.active {
  opacity: 1; transform: translateY(0) scale(1);
  pointer-events: auto;
  border-color: var(--accent);
  box-shadow: 0 0 0 4px var(--accent-dim), 0 10px 28px rgba(166,90,77,0.08);
}
.sc-icon  { font-size: 52px; margin-bottom: 12px; }
.sc-name  { font-family: 'Noto Serif TC', serif; font-size: 20px; font-weight: 700; color: var(--ink); margin-bottom: 6px; }
.sc-rate  { font-size: 14px; color: var(--muted-2); margin-bottom: 8px; }
.sc-rate strong { font-size: 20px; color: var(--accent); font-family: 'IBM Plex Mono', monospace; }
.sc-desc  { font-size: 13px; color: var(--muted-2); background: var(--surface-2); border-radius: 8px; padding: 8px 12px; }

.showcase-dots {
  position: absolute; bottom: -28px; left: 50%; transform: translateX(-50%);
  display: flex; gap: 6px;
}
.dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--border-2); cursor: pointer; transition: all 0.2s;
}
.dot.active { background: var(--accent); width: 20px; border-radius: 4px; }

/* ── Step Layout ── */
.step-wrap { width: 100%; max-width: 960px; margin: 0 auto; }

.step-header {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 32px;
}
.back-btn {
  background: none; border: none; cursor: pointer;
  font-size: 13px; color: var(--muted-2);
  font-family: 'Noto Sans TC', sans-serif;
  padding: 6px 0; transition: color 0.15s;
}
.back-btn:hover { color: var(--primary); }
.step-indicator {
  display: flex; align-items: center; gap: 8px;
  font-size: 12px; font-family: 'IBM Plex Mono', monospace;
}
.si { color: var(--muted); }
.si.active { color: var(--primary); font-weight: 600; }
.si.done   { color: var(--primary); }
.si-sep    { color: var(--border-2); }

.step-title {
  font-family: 'Noto Serif TC', serif;
  font-size: 24px; font-weight: 700; color: var(--ink); margin-bottom: 6px;
}
.step-desc { font-size: 14px; color: var(--muted-2); margin-bottom: 28px; }

/* ── Form Layout ── */
.form-layout {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 24px;
  align-items: start;
  width: 100%;
  min-width: 660px;   /* form-main(340) + gap(24) + sidebar(280) + 餘裕 */
  max-width: 860px;
  margin: 0 auto;
}
.form-main {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 22px;
  padding: 28px;
  display: flex; flex-direction: column; gap: 0;
  min-width: 340px;   /* 欄位最小可讀寬度 */
  box-shadow: 0 10px 26px rgba(63,74,66,0.06);
}

/* 申請人資訊只讀顯示 */
.applicant-info-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
}
.ai-item {
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.ai-label {
  font-size: 11px;
  color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
  letter-spacing: 0.04em;
}
.ai-value {
  font-size: 14px;
  color: var(--ink);
  font-family: 'IBM Plex Mono', monospace;
  font-weight: 600;
  word-break: break-all;
}

.applicant-tag {
  display: inline-flex; align-items: center;
  padding: 4px 12px; border-radius: 20px;
  font-size: 12px; font-weight: 600;
  margin-bottom: 20px;
  font-family: 'IBM Plex Mono', monospace;
}
.tag-member    { background: var(--accent-dim); color: var(--accent); border: 1px solid var(--accent-lt); }

.form-error-banner {
  background: rgba(166,90,77,0.08); border: 1px solid rgba(166,90,77,0.25);
  color: var(--red); border-radius: 8px;
  padding: 10px 14px; font-size: 13px;
  margin-bottom: 16px;
}

.form-section { margin-bottom: 24px; }
.section-label {
  font-size: 11px; letter-spacing: 0.12em; text-transform: uppercase;
  font-family: 'IBM Plex Mono', monospace; font-weight: 600;
  color: var(--muted-2); margin-bottom: 14px;
  padding-bottom: 8px; border-bottom: 1px solid var(--border);
}

/* Fields */
.field {
  display: flex; flex-direction: column; gap: 5px;
  margin-bottom: 14px;
}
.field:last-child { margin-bottom: 0; }
.field label {
  font-size: 12px; color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
}
.req { color: var(--red); margin-left: 2px; }
.field input {
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: 8px; color: var(--ink);
  font-family: 'Noto Sans TC', sans-serif;
  font-size: 14px; padding: 10px 14px;
  outline: none; transition: border-color 0.15s, box-shadow 0.15s;
}
.field input:focus {
  border-color: var(--primary); box-shadow: 0 0 0 3px rgba(92,107,95,0.12);
}
.field-error input { border-color: var(--red); }
.err-msg { font-size: 11px; color: var(--red); }
.field-hint { font-size: 11px; color: var(--muted); font-family: 'IBM Plex Mono', monospace; }
.field-textarea {
  width: 100%; background: var(--surface);
  border: 1px solid var(--border-2); border-radius: 8px;
  color: var(--ink); font-family: 'Noto Sans TC', sans-serif;
  font-size: 14px; padding: 10px 14px; outline: none;
  resize: vertical; min-height: 80px;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.field-textarea:focus {
  border-color: var(--primary); box-shadow: 0 0 0 3px rgba(92,107,95,0.12);
}
.id-notice {
  display: flex; align-items: flex-start; gap: 7px;
  margin-top: 10px; padding: 10px 12px;
  background: var(--accent-dim);
  border: 1px solid var(--accent-lt);
  border-radius: 8px;
  font-size: 12px; color: var(--muted-2); line-height: 1.5;
}
.notice-icon { flex-shrink: 0; font-size: 13px; }

.field-grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }
.field-grid-3 { display: flex; flex-direction: column; gap: 0; }

/* Input with prefix */
.input-prefix-wrap { position: relative; }
.input-prefix {
  position: absolute; left: 12px; top: 50%; transform: translateY(-50%);
  font-size: 14px; color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
  pointer-events: none;
}
.field input.has-prefix { padding-left: 28px; }

/* Type card grid */
.type-card-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
  margin-bottom: 4px;
}
.type-card {
  display: flex; flex-direction: column; align-items: center;
  gap: 4px; padding: 12px 8px; border-radius: 10px;
  border: 1.5px solid var(--border);
  background: var(--surface); cursor: pointer;
  transition: all 0.15s; text-align: center;
}
.type-card:hover { border-color: var(--primary); background: rgba(92,107,95,0.08); }
.type-card.selected {
  border-color: var(--primary); background: rgba(92,107,95,0.08);
  box-shadow: 0 0 0 2px rgba(92,107,95,0.18);
}
.tc-icon { font-size: 20px; }
.tc-name { font-size: 11px; font-weight: 600; color: var(--ink-2); }
.tc-rate { font-size: 10px; color: var(--primary); font-family: 'IBM Plex Mono', monospace; }

/* Period buttons */
.period-btns {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
  gap: 8px;
}
.period-btn {
  padding: 7px 14px; border-radius: 8px; font-size: 13px;
  font-family: 'IBM Plex Mono', monospace;
  cursor: pointer; border: 1.5px solid var(--border);
  background: var(--surface); color: var(--muted-2);
  transition: all 0.15s;
}
.period-btn:hover  { border-color: var(--primary); color: var(--primary); }
.period-btn.active { border-color: var(--primary); background: rgba(92,107,95,0.08); color: var(--primary); font-weight: 600; }
.period-placeholder { font-size: 13px; color: var(--muted); font-style: italic; padding: 8px 0; }

/* ── Rate Sidebar ── */
.rate-sidebar { position: sticky; top: 80px; }
.rate-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 22px;
  padding: 24px;
  box-shadow: 0 10px 26px rgba(63,74,66,0.06);
}
.rate-card-label {
  font-size: 11px; color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
  letter-spacing: 0.1em; text-transform: uppercase;
  margin-bottom: 8px;
}
.rate-big {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 44px; font-weight: 700;
  color: var(--accent); line-height: 1;
  margin-bottom: 16px;
}
.rate-unit { font-size: 22px; }
.rate-empty {
  font-size: 36px; color: var(--muted); margin-bottom: 16px;
  font-family: 'IBM Plex Mono', monospace;
}

.rate-breakdown {
  background: var(--surface-2); border-radius: 8px;
  padding: 12px 14px; margin-bottom: 12px;
}
.rb-row {
  display: flex; justify-content: space-between;
  font-size: 12px; color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
  padding: 3px 0;
}
.rb-plus  { color: var(--red); }
.rb-zero  { color: var(--muted); }
.rb-divider { border-top: 1px solid var(--border); margin: 6px 0; }
.rb-total { color: var(--ink); font-weight: 600; font-size: 13px; }

.rate-hint { margin-bottom: 12px; }
.rh-row {
  display: flex; justify-content: space-between; align-items: baseline;
  font-size: 12px; color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
}
.rh-val { font-size: 15px; font-weight: 600; color: var(--ink); }
.rh-note { font-size: 10px; color: var(--muted); text-align: right; margin-top: 2px; }

/* 計算公式 */
.rate-formula {
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 12px;
}
.formula-title {
  font-size: 11px; font-weight: 600; color: var(--primary);
  margin-bottom: 8px;
}
.formula-row {
  display: flex; flex-direction: column; gap: 2px;
  margin-bottom: 6px;
}
.formula-label {
  font-size: 10px; color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
  letter-spacing: 0.05em;
}
.formula-val {
  font-size: 12px; color: var(--ink);
  font-family: 'IBM Plex Mono', monospace;
}
.formula-math { letter-spacing: 0.03em; color: var(--primary); font-weight: 500; }
.fixed-note   { color: var(--primary); font-size: 11px; }
.formula-vars {
  font-size: 10px; color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
  border-top: 1px solid var(--border);
  padding-top: 6px; margin-top: 4px;
  line-height: 1.6;
}

.rate-tips {
  border-top: 1px solid var(--border); padding-top: 14px;
}
.tip-title { font-size: 11px; color: var(--muted-2); font-weight: 600; margin-bottom: 8px; }
.rate-tips ul { list-style: none; display: flex; flex-direction: column; gap: 5px; }
.rate-tips li { font-size: 11px; color: var(--muted); line-height: 1.5; padding-left: 10px; position: relative; }
.rate-tips li::before { content: '·'; position: absolute; left: 0; }

/* ── Buttons ── */
.btn {
  display: inline-flex; align-items: center; justify-content: center; gap: 8px;
  border: none; border-radius: 10px;
  font-family: 'Noto Sans TC', sans-serif;
  cursor: pointer; transition: all 0.15s; font-weight: 600;
}
.btn-primary { background: var(--primary); color: #fff; }
.btn-primary:hover:not(:disabled) { background: var(--primary-dk); }
.btn-primary:disabled { opacity: 0.45; cursor: not-allowed; }
.btn-outline {
  background: var(--surface); color: var(--ink-2);
  border: 1.5px solid var(--border-2);
}
.btn-outline:hover { border-color: var(--primary); color: var(--primary); background: rgba(92,107,95,0.05); }
.btn-lg     { padding: 13px 32px; font-size: 15px; border-radius: 12px; }
.btn-submit { width: 100%; padding: 13px; font-size: 15px; margin-top: 8px; }

/* ── Consent ── */
.consent-outer {
  max-width: 720px;
  margin: 0 auto;
}
.consent-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 22px;
  padding: 32px;
  box-shadow: 0 10px 26px rgba(63,74,66,0.06);
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.consent-heading {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}
.consent-icon {
  font-size: 28px;
  flex-shrink: 0;
  margin-top: 2px;
}
.consent-title {
  font-family: 'Noto Serif TC', serif;
  font-size: 16px;
  font-weight: 700;
  color: var(--ink);
  line-height: 1.5;
}
.consent-subtitle {
  font-size: 12px;
  color: var(--muted-2);
  margin-top: 4px;
}

/* 全文捲動區 */
.consent-body {
  background: var(--surface-2);
  border: 1px solid var(--border);
  border-radius: 10px;
  padding: 20px 22px;
  height: 320px;
  overflow-y: scroll;
  font-size: 13.5px;
  line-height: 1.9;
  color: var(--ink-2);
  scroll-behavior: smooth;
}
.consent-body p {
  margin: 0 0 10px;
}
.consent-body p:last-child { margin-bottom: 0; }
.cb-section-title {
  font-weight: 700;
  font-size: 14px;
  color: var(--ink);
  margin-bottom: 12px !important;
}
.cb-indent {
  padding-left: 22px;
}

/* 捲動提示 */
.consent-scroll-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  font-size: 12px;
  color: var(--muted-2);
  background: var(--accent-dim);
  border: 1px solid var(--accent-lt);
  border-radius: 8px;
  padding: 9px 14px;
  animation: hint-pulse 2s ease-in-out infinite;
}
@keyframes hint-pulse {
  0%, 100% { opacity: 1; }
  50%       { opacity: 0.6; }
}

/* 勾選區 */
.consent-checks {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 18px;
  background: var(--surface-2);
  border: 1.5px solid var(--border);
  border-radius: 10px;
  transition: opacity 0.3s;
}
.consent-checks.locked {
  opacity: 0.4;
  pointer-events: none;
  filter: grayscale(0.4);
}

.check-item {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  cursor: pointer;
  user-select: none;
}
.check-item.disabled { cursor: not-allowed; }

.check-input { display: none; }

.check-box-ui {
  width: 18px;
  height: 18px;
  border-radius: 5px;
  border: 1.5px solid var(--border-2);
  background: var(--surface);
  flex-shrink: 0;
  margin-top: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}
.check-input:checked + .check-box-ui {
  background: var(--primary);
  border-color: var(--primary);
}
.check-input:checked + .check-box-ui::after {
  content: '\f00c';
  font-family: 'Font Awesome 6 Free';
  font-weight: 900;
  font-size: 11px;
  color: #fff;
}

.check-text {
  font-size: 14px;
  color: var(--ink);
  line-height: 1.6;
}

/* ── Done ── */
.done-card {
  background: var(--surface); border: 1px solid var(--border);
  border-radius: 20px; padding: 48px 40px;
  max-width: 540px; margin: 0 auto; text-align: center;
  box-shadow: 0 10px 26px rgba(63,74,66,0.06);
}
.done-icon   { font-size: 56px; margin-bottom: 16px; }
.done-title  { font-family: 'Noto Serif TC', serif; font-size: 26px; font-weight: 700; margin-bottom: 8px; }
.done-desc   { font-size: 14px; color: var(--muted-2); margin-bottom: 24px; }
.done-id-block {
  background: var(--surface-2); border: 1px solid var(--border);
  border-radius: 10px; padding: 14px 20px;
  margin-bottom: 20px; display: flex; flex-direction: column; gap: 4px;
}
.done-id-label { font-size: 11px; color: var(--muted-2); font-family: 'IBM Plex Mono', monospace; letter-spacing: 0.1em; text-transform: uppercase; }
.done-id {
  font-family: 'IBM Plex Mono', monospace; font-size: 18px;
  font-weight: 700; color: var(--accent); letter-spacing: 0.05em;
}

.done-summary {
  background: var(--surface-2); border-radius: 10px;
  padding: 14px 18px; margin-bottom: 24px;
  display: flex; flex-direction: column; gap: 8px; text-align: left;
}
.ds-row {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 13px; color: var(--muted-2);
}
.ds-row span:last-child { color: var(--ink); font-weight: 500; }
.ds-rate { color: var(--primary) !important; font-family: 'IBM Plex Mono', monospace; }
.ds-mono { font-family: 'IBM Plex Mono', monospace; font-size: 12px; }
.ds-note { align-items: flex-start; }
.ds-note-val { font-size: 12px; color: var(--muted-2); text-align: right; max-width: 200px; white-space: pre-wrap; word-break: break-word; }

/* Misc */
.spin { animation: spin 0.8s linear infinite; display: inline-block; }
@keyframes spin { to { transform: rotate(360deg); } }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ── 入帳帳戶選單 ── */
.acct-loading {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: var(--muted-2); padding: 12px 0;
}
.acct-empty {
  display: flex; align-items: flex-start; gap: 12px;
  background: rgba(166,90,77,0.06); border: 1px solid rgba(166,90,77,0.2);
  border-radius: 10px; padding: 14px 16px;
}
.acct-empty-icon  { font-size: 20px; flex-shrink: 0; margin-top: 1px; }
.acct-empty-title { font-size: 13px; font-weight: 600; color: var(--ink); margin-bottom: 3px; }
.acct-empty-sub   { font-size: 12px; color: var(--muted-2); }
.acct-error {
  display: flex; align-items: center; gap: 8px;
  font-size: 13px; color: var(--red);
  padding: 10px 14px;
  background: rgba(166,90,77,0.06); border: 1px solid rgba(166,90,77,0.2);
  border-radius: 8px;
}

.acct-select-wrap { position: relative; }
.acct-select {
  width: 100%; appearance: none;
  background: var(--surface); border: 1px solid var(--border-2);
  border-radius: 8px; color: var(--ink);
  font-family: 'IBM Plex Mono', monospace; font-size: 13px;
  padding: 10px 36px 10px 14px;
  outline: none; cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.acct-select:focus {
  border-color: var(--primary); box-shadow: 0 0 0 3px rgba(92,107,95,0.12);
}
.field-error .acct-select { border-color: var(--red); }
.acct-select-caret {
  position: absolute; right: 12px; top: 50%; transform: translateY(-50%);
  font-size: 11px; color: var(--muted-2); pointer-events: none;
}

.acct-info-card {
  margin-top: 10px;
  background: var(--surface-2); border: 1px solid var(--border);
  border-left: 3px solid var(--primary);
  border-radius: 8px; padding: 12px 14px;
  display: flex; flex-direction: column; gap: 7px;
}
.aic-row {
  display: flex; justify-content: space-between; align-items: center;
  font-size: 12px;
}
.aic-label { color: var(--muted-2); font-family: 'IBM Plex Mono', monospace; }
.aic-val   { color: var(--ink); font-weight: 500; }
.aic-val.mono  { font-family: 'IBM Plex Mono', monospace; letter-spacing: 0.04em; }
.aic-val.green {
  color: var(--primary); font-family: 'IBM Plex Mono', monospace; font-weight: 600;
}

/* ── 表單步驟字體 +2px ── */
.step-header .back-btn      { font-size: 15px; }
.step-header .step-indicator { font-size: 14px; }

.form-main .ai-label         { font-size: 13px; }
.form-main .ai-value         { font-size: 16px; }
.form-main .form-error-banner { font-size: 15px; }
.form-main .section-label    { font-size: 13px; }
.form-main .field label      { font-size: 14px; }
.form-main .err-msg          { font-size: 13px; }
.form-main .field-hint       { font-size: 13px; }
.form-main .field input      { font-size: 16px; }
.form-main .field-textarea   { font-size: 16px; }
.form-main .id-notice        { font-size: 14px; }
.form-main .notice-icon      { font-size: 15px; }
.form-main .input-prefix     { font-size: 16px; }
.form-main .tc-name          { font-size: 13px; }
.form-main .tc-rate          { font-size: 12px; }
.form-main .period-btn       { font-size: 15px; }
.form-main .period-placeholder { font-size: 15px; }
.form-main .acct-loading     { font-size: 15px; }
.form-main .acct-empty-title { font-size: 15px; }
.form-main .acct-empty-sub   { font-size: 14px; }
.form-main .acct-error       { font-size: 15px; }
.form-main .acct-select      { font-size: 15px; }
.form-main .acct-select-caret { font-size: 13px; }
.form-main .aic-row          { font-size: 14px; }
.form-main .btn-submit       { font-size: 17px; }

.rate-sidebar .rate-card-label { font-size: 13px; }
.rate-sidebar .rate-big        { font-size: 46px; }
.rate-sidebar .rate-unit       { font-size: 24px; }
.rate-sidebar .rate-empty      { font-size: 38px; }
.rate-sidebar .rb-row          { font-size: 14px; }
.rate-sidebar .rb-total        { font-size: 15px; }
.rate-sidebar .rh-row          { font-size: 14px; }
.rate-sidebar .rh-val          { font-size: 17px; }
.rate-sidebar .rh-note         { font-size: 12px; }
.rate-sidebar .tip-title       { font-size: 13px; }
.rate-sidebar .rate-tips li    { font-size: 13px; }

@media (max-width: 900px) {
  .apply-page {
    padding: 24px 16px 56px;
  }

  .home-wrap {
    grid-template-columns: 1fr;
    gap: 28px;
    min-height: auto;
    max-width: 100%;
  }

  .home-left,
  .home-right {
    max-width: 100%;
  }

  .home-title {
    white-space: normal;
    font-size: clamp(26px, 7vw, 38px);
  }

  .home-desc {
    white-space: normal;
  }

  .home-stats {
    flex-wrap: wrap;
    gap: 0;
  }

  .stat-item {
    min-width: 140px;
    padding: 6px 0;
  }

  .stat-divider {
    display: none;
  }

  .home-actions {
    flex-direction: column;
  }

  .home-actions .btn,
  .home-actions .btn-action {
    width: 100%;
  }

  .loan-showcase {
    max-width: 100%;
  }

  .step-wrap {
    max-width: 100%;
  }

  .step-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .form-layout {
    min-width: 0;
    grid-template-columns: 1fr;
    max-width: 100%;
  }

  .form-main {
    min-width: 0;
    padding: 20px;
  }

  .rate-sidebar {
    position: static;
  }

  .type-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .period-btns {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .step-actions {
    flex-direction: column;
  }

  .step-actions .jb-btn,
  .btn-submit {
    width: 100%;
  }

  .done-card {
    padding: 36px 20px;
  }
}

@media (max-width: 600px) {
  .type-card-grid {
    grid-template-columns: 1fr;
  }

  .period-btns {
    grid-template-columns: 1fr;
  }

  .applicant-info-row,
  .field-grid-2 {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: span 1;
  }
}
</style>
