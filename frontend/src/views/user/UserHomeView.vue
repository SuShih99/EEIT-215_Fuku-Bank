<template>
  <div class="customer-page home-page">
    <div class="washi-overlay" aria-hidden="true"></div>

    <!-- 未開戶引導 -->
    <section v-if="!hasAccount" class="onboard-card" aria-label="開戶引導">
      <div class="onboard-icon">
        <svg
          width="48"
          height="48"
          viewBox="0 0 24 24"
          fill="none"
          stroke="var(--primary)"
          stroke-width="1.2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <rect x="2" y="5" width="20" height="14" rx="2" />
          <line x1="2" y1="10" x2="22" y2="10" />
        </svg>
      </div>
      <div class="onboard-content">
        <h2 class="onboard-title">歡迎加入 Java Easy Bank</h2>
        <p class="onboard-desc">
          您尚未擁有任何帳戶。立即申請開戶，即可享有存款、轉帳、換匯等完整金融服務。
        </p>
        <div class="onboard-actions">
          <button
            class="jb-btn jb-btn-primary jb-btn-lg"
            @click="$router.push({ name: 'user-account-application' })"
          >
            立即申請開戶
          </button>
          <button
            class="jb-btn jb-btn-secondary"
            @click="$router.push({ name: 'user-account-application' })"
          >
            查看申請進度
          </button>
        </div>
      </div>
      <div class="onboard-features">
        <div class="onboard-feature" v-for="f in onboardFeatures" :key="f.title">
          <div class="feature-icon" v-html="f.icon"></div>
          <div>
            <p class="feature-title">{{ f.title }}</p>
            <p class="feature-desc">{{ f.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 上方：個人卡片 + 資產總覽 -->
    <div v-if="hasAccount" class="dashboard-top">
      <!-- 左：個人資訊卡片 -->
      <section class="profile-summary-card" aria-label="個人資訊">
        <div class="profile-header-wrap">
          <button
            class="profile-avatar-btn"
            aria-label="前往會員中心"
            @click="$router.push({ name: 'user-profile' })"
          >
            <img v-if="avatarSrc" :src="avatarSrc" class="profile-avatar-img" alt="大頭照" />
            <span v-else class="profile-avatar-fallback">{{ customerInitial }}</span>
          </button>
          <div class="profile-main-info">
            <h2 class="profile-name">{{ customerName }}</h2>
            <p class="profile-meta-id">帳號 {{ maskedId }}</p>
          </div>
        </div>

        <div class="profile-divider"></div>

        <div class="profile-details">
          <div class="profile-detail-row">
            <span class="profile-label">會員等級</span>
            <span class="profile-value">一般會員</span>
          </div>
          <div class="profile-detail-row">
            <span class="profile-label">最近登入</span>
            <span class="profile-value">{{ todayStr }}</span>
          </div>
        </div>

        <div class="profile-link-row" @click="$router.push({ name: 'user-profile' })">
          <span class="profile-label" style="color: var(--text-primary); font-weight: 600"
            >我的權益</span
          >
          <svg
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path d="M9 18l6-6-6-6" />
          </svg>
        </div>
      </section>

      <!-- 右：資產總覽 -->
      <section class="asset-overview-card" aria-label="資產總覽">
        <div class="asset-card-header">
          <div class="asset-title-group">
            <h2 class="asset-overview-title">資產總覽</h2>
            <p class="asset-overview-subtitle">淨資產（折合臺幣）</p>
          </div>
          <button
            class="toggle-visibility-btn"
            @click="showAmounts = !showAmounts"
            :aria-label="showAmounts ? '隱藏金額' : '顯示金額'"
          >
            <svg
              v-if="showAmounts"
              width="28"
              height="28"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.9"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
              <circle cx="12" cy="12" r="3"></circle>
            </svg>
            <svg
              v-else
              width="28"
              height="28"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="1.9"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path
                d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"
              ></path>
              <line x1="1" y1="1" x2="23" y2="23"></line>
            </svg>
          </button>
        </div>

        <div class="asset-main-amount">
          <span class="asset-currency-symbol">$</span>
          <span class="asset-total-amount">{{ formatMoney(Math.round(assetData.netAsset)) }}</span>
        </div>

        <div class="asset-grid">
          <!-- 存款 -->
          <div class="asset-subcard">
            <div class="asset-subcard-header">
              <span class="asset-subcard-title">存款</span>
            </div>
            <p class="asset-subcard-amount">$ {{ formatMoney(Math.round(assetData.deposit)) }}</p>
            <div class="asset-subcard-details">
              <div class="asset-subcard-detail">
                <span>臺幣淨資產</span>
                <span>$ {{ formatMoney(Math.round(assetData.twdAsset)) }}</span>
              </div>
              <div class="asset-subcard-detail">
                <span>外幣淨資產</span>
                <span>$ {{ formatMoney(Math.round(assetData.foreignAsset)) }}</span>
              </div>
            </div>
            <div class="asset-subcard-actions">
              <button class="subcard-primary-btn" @click="$router.push({ name: 'user-transfer' })">
                臺幣轉帳
              </button>
              <button
                class="subcard-secondary-btn"
                @click="$router.push({ name: 'user-transactions' })"
              >
                臺幣明細
              </button>
            </div>
          </div>

          <!-- 信用卡 -->
          <div class="asset-subcard">
            <div class="asset-subcard-header">
              <span class="asset-subcard-title">信用卡消費總額</span>
            </div>
            <p class="asset-subcard-amount">$ {{ formatMoney(assetData.creditTotal) }}</p>
            <div class="asset-subcard-details">
              <div class="asset-subcard-detail">
                <span>臺幣未出帳</span>
                <span>$ {{ formatMoney(assetData.creditUnbilled) }}</span>
              </div>
              <div class="asset-subcard-detail">
                <span>{{ currentMonth }} 帳單</span>
                <span :class="assetData.currentBillAmount > 0 ? 'text-overdue' : 'text-safe'">
                  {{
                    assetData.currentBillAmount > 0
                      ? '$ ' + formatMoney(assetData.currentBillAmount)
                      : '無需繳費'
                  }}
                </span>
              </div>
            </div>
            <div class="asset-subcard-actions">
              <button class="subcard-primary-btn" @click="router.push({ name: 'user-card-types' })">
                申辦卡片
              </button>
              <button
                class="subcard-secondary-btn"
                @click="router.push({ name: 'user-card-bills' })"
              >
                我要繳費
              </button>
            </div>
          </div>

          <!-- 貸款 -->
          <div class="asset-subcard">
            <div class="asset-subcard-header">
              <span class="asset-subcard-title">貸款</span>
            </div>
            <!-- 無帳戶 -->
            <p
              v-if="sortedLoanAccounts.length === 0"
              class="asset-subcard-amount asset-subcard-amount--muted"
            >
              {{ showAmounts ? '尚無貸款' : '***' }}
            </p>
            <!-- 有帳戶：顯示剩餘本金總計 -->
            <p v-else class="asset-subcard-amount">
              {{
                showAmounts
                  ? '$ ' +
                    sortedLoanAccounts
                      .reduce((s, a) => s + (a.remainingPrincipal || 0), 0)
                      .toLocaleString('zh-TW')
                  : '***'
              }}
            </p>
            <div class="asset-subcard-details">
              <!-- 依最近繳款日排序，最多顯示 2 筆 -->
              <template v-if="sortedLoanAccounts.length > 0">
                <div
                  v-for="acc in sortedLoanAccounts.slice(0, 2)"
                  :key="acc.accountId"
                  class="asset-subcard-detail"
                >
                  <span>{{ LOAN_TYPE_MAP[acc.applyType] || acc.applyType }}</span>
                  <span
                    v-if="showAmounts"
                    :class="{ 'text-overdue': acc.accountStatus === 'OVERDUE' }"
                  >
                    {{ acc.nextPaymentDate ? acc.nextPaymentDate.substring(0, 10) : '—' }}
                  </span>
                  <span v-else>***</span>
                </div>
              </template>
              <!-- 無帳戶時顯示預設項目 -->
              <template v-else>
                <div class="asset-subcard-detail">
                  <span>信用貸款</span>
                  <span>—</span>
                </div>
                <div class="asset-subcard-detail">
                  <span>房屋貸款</span>
                  <span>—</span>
                </div>
              </template>
            </div>
            <div class="asset-subcard-actions loan-subcard-actions">
              <button
                class="subcard-primary-btn"
                @click="$router.push({ name: 'user-loan-apply' })"
              >
                申請貸款
              </button>
              <button
                class="subcard-secondary-btn"
                @click="$router.push({ name: 'user-loan-status' })"
              >
                查看申請
              </button>
              <button
                class="subcard-secondary-btn"
                @click="$router.push({ name: 'user-loan-accounts' })"
              >
                查看帳戶
              </button>
              <button
                class="subcard-secondary-btn"
                @click="$router.push({ name: 'user-loan-repayment' })"
              >
                立即還款
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>

    <!-- 下方：資產分佈 + 匯率 + 歷史水位 -->
    <div v-if="hasAccount" class="dashboard-bottom">
      <!-- 資產分佈 -->
      <section class="distribution-card" aria-label="資產分佈">
        <h3 class="card-title">資產分佈</h3>
        <div class="section-rule"></div>
        <div class="donut-wrap">
          <canvas ref="donutCanvas" width="200" height="200"></canvas>
        </div>
        <ul class="donut-legend">
          <li v-for="item in distributionData" :key="item.label">
            <span class="legend-dot" :style="{ background: item.color }"></span>
            <span class="legend-label">{{ item.label }}</span>
            <span class="legend-value">{{ item.pct }}%</span>
          </li>
        </ul>
      </section>

      <!-- 匯率 -->
      <section class="exchange-card" aria-label="匯率">
        <div class="card-title-row">
          <h3 class="card-title">匯率</h3>
          <button class="block-action-btn" @click="$router.push({ name: 'user-exchange' })">
            立即換匯
          </button>
        </div>
        <div class="section-rule"></div>
        <table class="exchange-table" :class="{ 'is-updating': isUpdatingRates }">
          <thead>
            <tr>
              <th></th>
              <th>幣別</th>
              <th>我要賣</th>
              <th>我要買</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in exchangeRates" :key="r.code">
              <td class="exchange-flag">{{ r.flag }}</td>
              <td class="exchange-name">{{ r.name }} {{ r.code }}</td>
              <td class="exchange-rate">{{ r.sell }}</td>
              <td class="exchange-rate">{{ r.buy }}</td>
            </tr>
          </tbody>
        </table>
        <p class="exchange-time">
          資料時間：{{ exchangeTime }}
          <button class="refresh-btn" @click="fetchExchangeRates">
            <span :class="{ 'spin-anim': isUpdatingRates }" style="display: inline-block">↻</span>
            更新
          </button>
        </p>
      </section>

      <!-- 歷史水位圖 -->
      <section class="watermark-card" aria-label="歷史水位圖">
        <h3 class="card-title">臺外幣歷史水位圖</h3>
        <div class="section-rule"></div>
        <div class="period-tabs" style="margin-bottom: 16px">
          <button
            v-for="p in periods"
            :key="p.value"
            class="period-tab"
            :class="{ active: activePeriod === p.value }"
            @click="activePeriod = p.value"
          >
            {{ p.label }}
          </button>
        </div>
        <div class="chart-wrap">
          <canvas ref="lineCanvas" height="260"></canvas>
        </div>
        <div class="chart-legend">
          <span class="chart-legend-item">
            <span class="legend-line" style="background: var(--primary)"></span>
            臺幣
          </span>
          <span class="chart-legend-item">
            <span class="legend-line" style="background: var(--accent)"></span>
            外幣（折合臺幣）
          </span>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useCustomerAuthStore } from '@/stores/customerAuth'
import api, { BASE_URL } from '@/api/axios'
import { Chart, registerables } from 'chart.js'
import { getMyAccountApplications } from '@/api/accountApplication'
import { getMyAccounts } from '@/api/customerAccount'
import { getBills, getUnbilledBills } from '@/api/userCardBill'

Chart.register(...registerables)

const router = useRouter()
const customerAuthStore = useCustomerAuthStore()

// === 是否已開戶 ===
const hasAccount = ref(true) // 預設 true 避免閃爍

const onboardFeatures = [
  {
    title: '快速開戶',
    desc: '線上申請，最快 1 個工作天完成審核',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>',
  },
  {
    title: '安全保障',
    desc: '嚴格 KYC 實名認證，保障帳戶安全',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>',
  },
  {
    title: '多元服務',
    desc: '存款、轉帳、外幣、信用卡一站搞定',
    icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/></svg>',
  },
]

//==信用卡==
const currentBillMonth = ref('')

async function fetchCreditSummary() {
  try {
    const bills = await getBills(0, 10)
    const unbilled = await getUnbilledBills(0, 10)

    const billList = bills.content || []
    const unbilledList = unbilled.content || []

    // 依帳單月份由新到舊排序，抓最新一期
    const sortedBills = [...billList].sort((a, b) => {
      return String(b.billingMonth).localeCompare(String(a.billingMonth))
    })

    const latestBill = sortedBills[0]
    currentBillMonth.value = latestBill?.billingMonth || ''
    const billTotal = Number(latestBill?.totalAmount || 0)
    const paidAmount = Number(latestBill?.paidAmount || 0)
    const remainingBillAmount = Math.max(billTotal - paidAmount, 0)

    assetData.value.currentBillAmount = remainingBillAmount
    assetData.value.currentBillPaidAmount = paidAmount

    const unbilledTotal = unbilledList.reduce((sum, item) => {
      return sum + Number(item.txnAmount || 0)
    }, 0)

    assetData.value.creditUnbilled = unbilledTotal

    // 最新一期帳單 + 未出帳
    assetData.value.creditTotal = billTotal + unbilledTotal

    calculateAssets()
  } catch (e) {
    console.error('首頁信用卡金額取得失敗', e)
  }
}

// === 貸款帳戶 ===
const accountsList = ref([])
const loanAccounts = ref([])

const LOAN_TYPE_MAP = {
  PERSONAL: '個人信貸',
  CAR: '汽車貸款',
  MOTOR: '機車貸款',
  STUDENT: '學貸',
  BUSINESS: '創業貸款',
  HOUSE: '房屋貸款',
  LAND: '土地貸款',
}

// 依最近繳款日排序（未結清），最近到期的排最前面
const sortedLoanAccounts = computed(() => {
  return [...loanAccounts.value]
    .filter((a) => a.accountStatus !== 'PAID_OFF')
    .sort((a, b) => {
      if (!a.nextPaymentDate && !b.nextPaymentDate) return 0
      if (!a.nextPaymentDate) return 1
      if (!b.nextPaymentDate) return -1
      return new Date(a.nextPaymentDate) - new Date(b.nextPaymentDate)
    })
})

async function checkAccountStatus() {
  try {
    // 優先檢查是否已有帳戶（帳戶可能是直接建立的，不一定透過開戶申請）
    const accounts = await getMyAccounts()
    if (accounts && accounts.length > 0) {
      hasAccount.value = true
      accountsList.value = accounts
      calculateAssets()
      return
    }
    // 沒有帳戶，再檢查是否有已核准的開戶申請
    const apps = await getMyAccountApplications()
    const hasApproved = apps && apps.some((a) => a.status === 'APPROVED')
    hasAccount.value = hasApproved
  } catch {
    // API 失敗時保持顯示 dashboard（避免影響已有帳戶的用戶）
    hasAccount.value = true
  }
}

// === 個人資訊 ===
const customerName = computed(() => customerAuthStore.customer?.name || '會員')
const customerInitial = computed(() => (customerAuthStore.customer?.name || '會')[0])
const avatarSrc = computed(() => {
  const url = customerAuthStore.customer?.avatarUrl
  if (!url) return null
  if (url.startsWith('http')) return url
  if (url.startsWith('/uploads/')) return BASE_URL + url
  return url
})
const maskedId = computed(() => {
  const id = customerAuthStore.customer?.username || 'user0001'
  return id.slice(0, 3) + '****'
})
const todayStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}`
})
//計算月結帳單月份
const currentMonth = computed(() => {
  if (!currentBillMonth.value) return '--'
  return currentBillMonth.value
})

// === 資產資料 ===
const assetData = ref({
  netAsset: 0,
  deposit: 0,
  twdAsset: 0,
  foreignAsset: 0,
  creditTotal: 43680,
  creditUnbilled: 12500,
  currentBillAmount: 0,
  currentBillPaidAmount: 0,
})

function calculateAssets() {
  let twd = 0
  let fxTwd = 0

  accountsList.value.forEach((a) => {
    if (a.accountType === 'LOAN') return
    if (a.currency === 'TWD') {
      twd += Number(a.balance || 0)
    } else {
      const rateObj = exchangeRates.value.find((r) => r.code === a.currency)
      if (rateObj && rateObj.buy !== '-') {
        // 使用買入匯率折算台幣
        fxTwd += Number(a.balance || 0) * Number(rateObj.buy)
      } else {
        // 粗估匯率（如果還沒讀取到）
        const fallbacks = { USD: 32.5, CNY: 4.5, JPY: 0.2, EUR: 35, AUD: 21 }
        fxTwd += Number(a.balance || 0) * (fallbacks[a.currency] || 30)
      }
    }
  })

  assetData.value.twdAsset = twd
  assetData.value.foreignAsset = fxTwd
  assetData.value.deposit = twd + fxTwd
  assetData.value.netAsset = assetData.value.deposit - assetData.value.creditTotal

  const total = twd + fxTwd + assetData.value.creditTotal
  if (total > 0) {
    distributionData.value[0].pct = Math.round((twd / total) * 100)
    distributionData.value[1].pct = Math.round((fxTwd / total) * 100)
    distributionData.value[2].pct = Math.round((assetData.value.creditTotal / total) * 100)
  }

  // 重繪圖表
  drawDonut()
  drawLine()
}

const showAmounts = ref(true)

function formatMoney(n) {
  if (!showAmounts.value) return '***'
  return Number(n || 0).toLocaleString('en-US')
}

function comingSoon() {
  alert('此功能即將推出，敬請期待！')
}

// === 資產分佈 ===
const distributionData = ref([
  { label: '臺幣', pct: 0, color: 'var(--primary)' },
  { label: '外幣', pct: 0, color: '#8BA58E' },
  { label: '信用卡', pct: 0, color: 'var(--accent)' },
  { label: '貸款', pct: 0, color: 'var(--border)' },
])

const donutCanvas = ref(null)
let donutChart = null

function drawDonut() {
  if (!donutCanvas.value) return
  if (donutChart) donutChart.destroy()
  donutChart = new Chart(donutCanvas.value, {
    type: 'doughnut',
    data: {
      labels: distributionData.value.map((d) => d.label),
      datasets: [
        {
          data: distributionData.value.map((d) => d.pct || 0.1), // if all 0, use 0.1 to avoid empty chart
          backgroundColor: ['#5C6B5F', '#8BA58E', '#A65A4D', '#D6CEC3'],
          borderWidth: 0,
          hoverOffset: 4,
        },
      ],
    },
    options: {
      cutout: '68%',
      responsive: true,
      maintainAspectRatio: true,
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: '#2B2B2B',
          titleFont: { family: 'Noto Sans TC' },
          bodyFont: { family: 'Noto Sans TC' },
          callbacks: {
            label: (ctx) => ` ${ctx.label}：${distributionData.value[ctx.dataIndex].pct}%`,
          },
        },
      },
    },
  })
}

// === 歷史水位圖 ===
const lineCanvas = ref(null)
const activePeriod = ref('all')
const periods = [
  { label: '全部', value: 'all' },
  { label: '1 個月', value: '1m' },
  { label: '3 個月', value: '3m' },
  { label: '6 個月', value: '6m' },
]

let lineChart = null

function generateMockLine(months) {
  const labels = []
  const twdData = []
  const fxData = []
  const now = new Date()

  const currentTwd = assetData.value.twdAsset || 0
  const currentFx = assetData.value.foreignAsset || 0

  for (let i = months; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    labels.push(`${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}`)

    if (i === 0) {
      twdData.push(currentTwd)
      fxData.push(currentFx)
    } else {
      const seed = d.getMonth() + d.getFullYear()
      const rand1 = (Math.sin(seed) * 0.5 + 0.5) * 0.2 - 0.1 // -10% ~ +10%
      const rand2 = (Math.cos(seed) * 0.5 + 0.5) * 0.2 - 0.1
      twdData.push(Math.round(currentTwd * (1 + rand1)))
      fxData.push(Math.round(currentFx * (1 + rand2)))
    }
  }
  return { labels, twdData, fxData }
}

function getMonthsForPeriod(p) {
  if (p === '1m') return 1
  if (p === '3m') return 3
  if (p === '6m') return 6
  return 12
}

function drawLine() {
  if (!lineCanvas.value) return
  if (lineChart) lineChart.destroy()
  const months = getMonthsForPeriod(activePeriod.value)
  const { labels, twdData, fxData } = generateMockLine(months)
  lineChart = new Chart(lineCanvas.value, {
    type: 'line',
    data: {
      labels,
      datasets: [
        {
          label: '臺幣',
          data: twdData,
          borderColor: '#5C6B5F',
          backgroundColor: 'rgba(92,107,95,0.06)',
          fill: true,
          tension: 0.3,
          borderWidth: 2,
          pointRadius: 0,
          pointHitRadius: 10,
        },
        {
          label: '外幣（折合臺幣）',
          data: fxData,
          borderColor: '#A65A4D',
          backgroundColor: 'rgba(166,90,77,0.04)',
          fill: true,
          tension: 0.3,
          borderWidth: 2,
          pointRadius: 0,
          pointHitRadius: 10,
        },
      ],
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: { mode: 'index', intersect: false },
      scales: {
        x: {
          grid: { display: false },
          ticks: { font: { family: 'Noto Sans TC', size: 11 }, color: '#A8A199' },
          border: { color: 'rgba(0,0,0,0.05)' },
        },
        y: {
          grid: { color: 'rgba(0,0,0,0.04)' },
          ticks: {
            font: { family: 'Cormorant Garamond', size: 12 },
            color: '#A8A199',
            callback: (v) =>
              v >= 1000000
                ? (v / 1000000).toFixed(1) + 'M'
                : v >= 1000
                  ? (v / 1000).toFixed(0) + 'K'
                  : v,
          },
          border: { display: false },
        },
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: '#2B2B2B',
          titleFont: { family: 'Noto Sans TC' },
          bodyFont: { family: 'Noto Sans TC' },
          callbacks: {
            label: (ctx) => ` ${ctx.dataset.label}：$${ctx.parsed.y.toLocaleString()}`,
          },
        },
      },
    },
  })
}

const exchangeRates = ref([
  { flag: '🇺🇸', name: '美元', code: 'USD', sell: '-', buy: '-' },
  { flag: '🇨🇳', name: '人民幣', code: 'CNY', sell: '-', buy: '-' },
  { flag: '🇯🇵', name: '日幣', code: 'JPY', sell: '-', buy: '-' },
  { flag: '🇪🇺', name: '歐元', code: 'EUR', sell: '-', buy: '-' },
  { flag: '🇦🇺', name: '澳幣', code: 'AUD', sell: '-', buy: '-' },
])

const exchangeTime = ref('2026/05/08 14:30')
const isUpdatingRates = ref(false)

async function fetchExchangeRates() {
  if (isUpdatingRates.value) return
  isUpdatingRates.value = true
  try {
    const res = await api.get('/api/public/exchange-rates')
    const rates = res.data.data.rates

    // Update timestamp
    const d = new Date()
    exchangeTime.value = `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`

    // Convert base 1 TWD = X currency to 1 currency = Y TWD
    exchangeRates.value = exchangeRates.value.map((currency) => {
      const rateToTwd = rates[currency.code]
      if (!rateToTwd) return currency

      const midRate = 1 / rateToTwd
      const sell = (midRate * 0.995).toFixed(4)
      const buy = (midRate * 1.005).toFixed(4)

      return { ...currency, sell, buy }
    })

    // 匯率更新後，重新計算外幣資產
    calculateAssets()
  } catch (e) {
    console.error('Failed to fetch exchange rates', e)
  } finally {
    // 確保動畫至少顯示 500ms，讓使用者有感
    setTimeout(() => {
      isUpdatingRates.value = false
    }, 500)
  }
}

watch(activePeriod, () => drawLine())

onMounted(async () => {
  await checkAccountStatus()
  if (hasAccount.value) {
    await fetchCreditSummary()

    drawDonut()
    drawLine()
    fetchExchangeRates()
    // 抓取貸款帳戶，依最近繳款日排序顯示
    try {
      const res = await api.get('/api/loan-accounts/my')
      loanAccounts.value = res.data.data || []
    } catch {
      loanAccounts.value = []
    }
  }
})
</script>

<style scoped>
.home-page {
  max-width: 100%;
  width: 100%;
  position: relative;
  overflow-x: hidden;
}

.washi-overlay {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: url('/washi-texture.png') repeat;
  opacity: 0.04;
}

.home-page > *:not(.washi-overlay) {
  position: relative;
  z-index: 1;
}

/* === 開戶引導 === */
.onboard-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: var(--space-8) var(--space-6);
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-5);
}

.onboard-icon {
  opacity: 0.7;
}

.onboard-content {
  max-width: 480px;
}

.onboard-title {
  font-family: var(--font-heading);
  font-size: var(--text-h2);
  letter-spacing: 3px;
  margin-bottom: var(--space-3);
}

.onboard-desc {
  color: var(--text-secondary);
  font-size: var(--text-sm);
  line-height: 1.7;
  margin-bottom: var(--space-5);
}

.onboard-actions {
  display: flex;
  justify-content: center;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.onboard-features {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-5);
  width: 100%;
  max-width: 700px;
  margin-top: var(--space-4);
  padding-top: var(--space-5);
  border-top: 1px solid var(--border);
}

.onboard-feature {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
  text-align: center;
}

.feature-icon {
  color: var(--primary);
  margin-bottom: var(--space-1);
}

.feature-title {
  font-weight: 600;
  font-size: var(--text-sm);
  color: var(--text-primary);
  margin-bottom: 2px;
}

.feature-desc {
  font-size: var(--text-xs);
  color: var(--text-secondary);
  line-height: 1.5;
}

@media (max-width: 640px) {
  .onboard-features {
    grid-template-columns: 1fr;
  }
}

/* === 上方佈局 === */
.dashboard-top {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 24px;
  margin-bottom: 32px;
  width: 100%;
  min-width: 0;
}

/* === 左側：個人資訊卡片 (Profile Summary) === */
.profile-summary-card {
  background-color: rgba(255, 249, 239, 0.72);
  border: 1px solid rgba(214, 206, 195, 0.92);
  border-radius: 22px;
  padding: 24px;
  box-shadow: 0 8px 22px rgba(63, 74, 66, 0.05);
  display: flex;
  flex-direction: column;
  width: 100%;
  min-width: 0;
}

.profile-header-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 4px;
}

.profile-avatar-btn {
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  flex-shrink: 0;
}

.profile-avatar-img,
.profile-avatar-fallback {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--border);
  background-color: rgba(234, 228, 218, 0.75);
  color: var(--primary-dark);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-heading);
  font-size: 20px;
  font-weight: 700;
}

.profile-name {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 700;
  margin: 0;
}

.profile-meta-id {
  color: var(--text-secondary);
  font-size: 13px;
  margin: 2px 0 0;
}

.profile-divider {
  height: 1px;
  margin: 18px 0;
  background-color: rgba(214, 206, 195, 0.7);
}

.profile-details {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}

.profile-detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile-label {
  color: var(--text-secondary);
  font-size: 13px;
}

.profile-value {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 600;
}

.profile-link-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  cursor: pointer;
  transition: all 0.2s ease;
  border-top: 1px solid rgba(214, 206, 195, 0.4);
  margin-top: auto;
}

.profile-link-row:hover {
  color: var(--primary-dark);
  transform: translateX(2px);
}

/* === 右側：資產總覽主卡 (Asset Overview) === */
.asset-overview-card {
  background-color: rgba(255, 249, 239, 0.78);
  border: 1px solid rgba(214, 206, 195, 0.92);
  border-radius: 24px;
  padding: 28px;
  box-shadow: 0 10px 26px rgba(63, 74, 66, 0.06);
  width: 100%;
  min-width: 0;
}

.asset-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.asset-overview-title {
  color: var(--text-primary);
  font-size: 22px;
  font-weight: 700;
  margin: 0;
}

.asset-overview-subtitle {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 4px 0 0;
}

.toggle-visibility-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px;
  opacity: 0.6;
  transition: opacity 0.2s;
}

.toggle-visibility-btn:hover {
  opacity: 1;
}

.asset-main-amount {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 28px;
}

.asset-currency-symbol {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-secondary);
}

.asset-total-amount {
  color: var(--text-primary);
  font-size: clamp(42px, 4vw, 52px);
  font-weight: 800;
  letter-spacing: -0.02em;
  line-height: 1;
  min-width: 0;
  overflow-wrap: anywhere;
}

.asset-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  width: 100%;
  min-width: 0;
}

.asset-subcard {
  background-color: rgba(255, 250, 243, 0.9);
  border: 1px solid rgba(214, 206, 195, 0.82);
  border-radius: 18px;
  padding: 18px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.35);
  display: flex;
  flex-direction: column;
  width: 100%;
  min-width: 0;
}

.asset-subcard-title {
  color: var(--text-primary);
  font-size: 15px;
  font-weight: 700;
}

.asset-subcard-amount {
  color: var(--text-primary);
  font-size: 22px;
  font-weight: 700;
  margin: 8px 0 12px;
}

.asset-subcard-amount--muted {
  color: var(--text-secondary);
  font-weight: 500;
}

.asset-subcard-details {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 18px;
}

.asset-subcard-detail {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--text-secondary);
}

.text-overdue {
  color: #c0392b;
  font-weight: 600;
}

.asset-subcard-actions {
  display: flex;
  gap: 8px;
  margin-top: auto;
}

.subcard-primary-btn {
  flex: 1;
  padding: 8px 12px;
  color: var(--bg-primary);
  background-color: var(--primary);
  border: 1px solid var(--primary);
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.subcard-primary-btn:hover {
  background-color: var(--primary-dark);
  border-color: var(--primary-dark);
}

.subcard-secondary-btn {
  flex: 1;
  padding: 8px 12px;
  color: var(--primary-dark);
  background-color: rgba(255, 249, 239, 0.55);
  border: 1px solid var(--border);
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

/* 貸款子卡：2×2 按鈕排列 */
.loan-subcard-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

/* === 下方佈局 === */
.dashboard-bottom {
  display: grid;
  grid-template-columns: 280px 1fr 1fr;
  gap: var(--space-5);
  width: 100%;
  min-width: 0;
}

/* === 通用卡片標題 === */
.card-title {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 3px;
  white-space: nowrap;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.block-action-btn {
  min-width: 112px;
  height: 40px;
  padding: 0 18px;
  color: var(--bg-primary);
  background-color: var(--primary);
  border: 1px solid var(--primary);
  border-radius: 10px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s var(--ease);
  white-space: nowrap;
}

.block-action-btn:hover {
  background-color: var(--primary-dark);
  border-color: var(--primary-dark);
  transform: translateY(-1px);
}

.block-action-btn:active {
  transform: translateY(0);
}

.section-rule {
  width: 100%;
  height: 1px;
  background: rgba(0, 0, 0, 0.05);
  margin: var(--space-3) 0;
}

/* === 資產分佈 === */
.distribution-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: var(--space-5);
  box-shadow: none;
  width: 100%;
  min-width: 0;
}

.donut-wrap {
  display: flex;
  justify-content: center;
  padding: var(--space-3) 0;
  max-width: 180px;
  margin: 0 auto;
}

.donut-legend {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.donut-legend li {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-xs);
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}

.legend-label {
  flex: 1;
  color: var(--text-secondary);
}

.legend-value {
  font-weight: 700;
  color: var(--text-primary);
  font-family: 'Inter', 'Noto Sans TC', var(--font-body);
}

/* === 匯率 === */
.exchange-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: var(--space-5);
  box-shadow: none;
  width: 100%;
  min-width: 0;
}

.exchange-table {
  width: 100%;
  min-width: 0;
  border-collapse: collapse;
  font-size: var(--text-body); /* 放大字體從 13px 到 15px */
  transition: opacity 0.3s var(--ease);
}

.exchange-table.is-updating {
  opacity: 0.4;
}

.exchange-table th {
  text-align: left;
  font-weight: 500;
  color: var(--text-disabled);
  font-size: var(--text-xs);
  padding: var(--space-1) 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.exchange-table td {
  padding: var(--space-2) 0;
  border-bottom: 1px solid rgba(0, 0, 0, 0.03);
}

.exchange-flag {
  width: 28px;
  font-size: 18px;
}

.exchange-name {
  color: var(--text-primary);
  font-weight: 600;
  font-size: 15px; /* 放大幣別名稱 */
}

.exchange-rate {
  font-family: 'Inter', 'Noto Sans TC', var(--font-body);
  font-size: 16px; /* 放大匯率數字 */
  font-weight: 700;
  color: var(--text-primary);
  text-align: right;
  letter-spacing: -0.02em;
}

.exchange-time {
  margin-top: var(--space-3);
  font-size: var(--text-xs);
  color: var(--text-disabled);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.refresh-btn {
  background: none;
  border: none;
  color: var(--primary);
  cursor: pointer;
  font-size: var(--text-xs);
  font-family: var(--font-body);
  padding: 0;
}

.refresh-btn:hover {
  color: var(--primary-dark);
}

.spin-anim {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  100% {
    transform: rotate(360deg);
  }
}

/* === 歷史水位圖 === */
.watermark-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: var(--space-5);
  box-shadow: none;
  width: 100%;
  min-width: 0;
}

.period-tabs {
  display: flex;
  gap: 2px;
}

.period-tab {
  padding: 4px 12px;
  font-size: var(--text-xs);
  font-family: var(--font-body);
  font-weight: 500;
  color: var(--text-secondary);
  background: transparent;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all 0.2s var(--ease);
  white-space: nowrap;
}

.period-tab.active {
  color: var(--primary);
  border-color: var(--primary);
  background: var(--primary-light);
}

.period-tab:hover:not(.active) {
  border-color: var(--text-disabled);
}

.chart-wrap {
  height: 240px;
  position: relative;
}

.chart-legend {
  display: flex;
  justify-content: center;
  gap: var(--space-5);
  margin-top: var(--space-3);
  font-size: var(--text-xs);
  color: var(--text-secondary);
}

.chart-legend-item {
  display: flex;
  align-items: center;
  gap: var(--space-1);
}

.legend-line {
  width: 18px;
  height: 3px;
  border-radius: 2px;
}

/* === RWD === */
@media (max-width: 1100px) {
  .dashboard-top {
    grid-template-columns: 1fr;
  }
  .dashboard-bottom {
    grid-template-columns: 1fr 1fr;
  }
  .distribution-card {
    grid-column: 1 / -1;
    display: grid;
    grid-template-columns: auto 1fr;
    gap: var(--space-3) var(--space-5);
    align-items: start;
  }
  .distribution-card .card-title {
    grid-column: 1 / -1;
  }
  .distribution-card .section-rule {
    grid-column: 1 / -1;
  }
}

@media (max-width: 768px) {
  .home-page {
    width: 100%;
    max-width: 100%;
    overflow-x: hidden;
  }

  .dashboard-top,
  .dashboard-bottom,
  .asset-grid {
    grid-template-columns: 1fr;
    gap: 20px;
    width: 100%;
    max-width: 100%;
  }

  .profile-summary-card,
  .asset-overview-card,
  .distribution-card,
  .exchange-card,
  .watermark-card,
  .asset-subcard {
    width: 100%;
    max-width: 100%;
  }

  .asset-overview-card {
    padding: 24px 20px;
    border-radius: 20px;
  }

  .asset-card-header {
    gap: 12px;
  }

  .asset-total-amount {
    font-size: 36px;
    line-height: 1.15;
    word-break: break-word;
  }

  .asset-main-amount {
    margin-bottom: 20px;
  }

  .asset-subcard-actions {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 10px;
  }

  .asset-subcard-actions button {
    width: 100%;
    min-width: 0;
  }

  .exchange-card,
  .watermark-card,
  .distribution-card {
    padding: 24px 20px;
  }

  .exchange-card {
    overflow-x: hidden;
  }

  .exchange-table {
    table-layout: fixed;
    font-size: 13px;
  }

  .exchange-flag {
    width: 24px;
    font-size: 15px;
  }

  .exchange-name {
    font-size: 13px;
  }

  .exchange-rate {
    font-size: 13px;
    word-break: break-word;
  }

  .exchange-time {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .card-title-row {
    align-items: center;
  }

  .block-action-btn {
    min-width: 96px;
    height: 38px;
    padding: 0 14px;
    font-size: 13px;
  }

  .distribution-card {
    grid-template-columns: 1fr;
  }

  .chart-legend {
    flex-direction: column;
    gap: var(--space-2);
    align-items: center;
  }
}
</style>
