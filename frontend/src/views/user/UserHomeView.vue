<template>
  <div class="customer-page home-page">
    <div class="washi-overlay" aria-hidden="true"></div>

    <!-- 未開戶引導 -->
    <section v-if="!hasAccount" class="onboard-card" aria-label="開戶引導">
      <div class="onboard-icon">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="var(--primary)" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round">
          <rect x="2" y="5" width="20" height="14" rx="2"/>
          <line x1="2" y1="10" x2="22" y2="10"/>
        </svg>
      </div>
      <div class="onboard-content">
        <h2 class="onboard-title">歡迎加入 Java Easy Bank</h2>
        <p class="onboard-desc">
          您尚未擁有任何帳戶。立即申請開戶，即可享有存款、轉帳、換匯等完整金融服務。
        </p>
        <div class="onboard-actions">
          <button class="jb-btn jb-btn-primary jb-btn-lg" @click="$router.push({ name: 'user-account-application' })">
            立即申請開戶
          </button>
          <button class="jb-btn jb-btn-secondary" @click="$router.push({ name: 'user-account-application' })">
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
      <section class="profile-card" aria-label="個人資訊">
        <div class="profile-header">
          <button
            class="profile-avatar"
            aria-label="前往會員中心"
            @click="$router.push({ name: 'user-profile' })"
          >
            <img v-if="avatarSrc" :src="avatarSrc" class="profile-avatar-img" alt="大頭照" />
            <span v-else class="profile-avatar-fallback">{{ customerInitial }}</span>
          </button>
          <div class="profile-info">
            <h2 class="profile-name">{{ customerName }}</h2>
            <p class="profile-id">帳號 {{ maskedId }}</p>
          </div>
        </div>
        <div class="profile-meta">
          <div class="meta-item">
            <span class="meta-label">會員等級</span>
            <span class="meta-value">一般會員</span>
          </div>
          <div class="meta-item">
            <span class="meta-label">最近登入</span>
            <span class="meta-value">{{ todayStr }}</span>
          </div>
        </div>
        <button class="profile-link" @click="$router.push({ name: 'user-profile' })">
          我的權益
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18l6-6-6-6"/></svg>
        </button>
      </section>

      <!-- 右：資產總覽 -->
      <section class="asset-overview" aria-label="資產總覽">
        <div class="asset-header">
          <h2 class="asset-title">資產總覽</h2>
          <p class="asset-subtitle">淨資產（折合臺幣）</p>
          <p class="asset-total">$ {{ formatMoney(mockData.netAsset) }}</p>
        </div>

        <div class="asset-grid">
          <!-- 存款 -->
          <div class="asset-block">
            <div class="asset-block-header">
              <span class="asset-block-label">存款</span>
            </div>
            <p class="asset-block-amount">$ {{ formatMoney(mockData.deposit) }}</p>
            <div class="asset-block-detail">
              <div class="detail-row">
                <span>臺幣淨資產</span>
                <span>$ {{ formatMoney(mockData.twdAsset) }}</span>
              </div>
              <div class="detail-row">
                <span>外幣淨資產</span>
                <span>$ {{ formatMoney(mockData.foreignAsset) }}</span>
              </div>
            </div>
            <div class="asset-block-actions">
              <button class="block-action-btn" @click="comingSoon">臺幣轉帳</button>
              <button class="block-action-btn" @click="comingSoon">臺幣明細</button>
            </div>
          </div>

          <!-- 信用卡 -->
          <div class="asset-block">
            <div class="asset-block-header">
              <span class="asset-block-label">信用卡消費總額</span>
            </div>
            <p class="asset-block-amount">$ {{ formatMoney(mockData.creditTotal) }}</p>
            <div class="asset-block-detail">
              <div class="detail-row">
                <span>臺幣未出帳</span>
                <span>$ {{ formatMoney(mockData.creditUnbilled) }}</span>
              </div>
              <div class="detail-row">
                <span>{{ currentMonth }} 月帳單</span>
                <span class="text-safe">無需繳費</span>
              </div>
            </div>
            <div class="asset-block-actions">
              <button class="block-action-btn" @click="comingSoon">我要繳費</button>
              <button class="block-action-btn" @click="comingSoon">帳單分期</button>
            </div>
          </div>

          <!-- 貸款 -->
          <div class="asset-block">
            <div class="asset-block-header">
              <span class="asset-block-label">貸款</span>
            </div>
            <p class="asset-block-amount asset-block-amount--muted">尚無貸款</p>
            <div class="asset-block-detail">
              <div class="detail-row">
                <span>信用貸款</span>
                <span>—</span>
              </div>
              <div class="detail-row">
                <span>房屋貸款</span>
                <span>—</span>
              </div>
            </div>
            <div class="asset-block-actions">
              <button class="block-action-btn" @click="comingSoon">申請貸款</button>
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
          <button class="block-action-btn" @click="comingSoon">立即換匯</button>
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
        <p class="exchange-time">資料時間：{{ exchangeTime }} <button class="refresh-btn" @click="fetchExchangeRates"><span :class="{ 'spin-anim': isUpdatingRates }" style="display:inline-block;">↻</span> 更新</button></p>
      </section>

      <!-- 歷史水位圖 -->
      <section class="watermark-card" aria-label="歷史水位圖">
        <div class="card-title-row">
          <h3 class="card-title">臺外幣歷史水位圖</h3>
          <div class="period-tabs">
            <button
              v-for="p in periods"
              :key="p.value"
              class="period-tab"
              :class="{ active: activePeriod === p.value }"
              @click="activePeriod = p.value"
            >{{ p.label }}</button>
          </div>
        </div>
        <div class="section-rule"></div>
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

async function checkAccountStatus() {
  try {
    // 優先檢查是否已有帳戶（帳戶可能是直接建立的，不一定透過開戶申請）
    const accounts = await getMyAccounts()
    if (accounts && accounts.length > 0) {
      hasAccount.value = true
      return
    }
    // 沒有帳戶，再檢查是否有已核准的開戶申請
    const apps = await getMyAccountApplications()
    const hasApproved = apps && apps.some(a => a.status === 'APPROVED')
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
  return url.startsWith('http') ? url : BASE_URL + url
})
const maskedId = computed(() => {
  const id = customerAuthStore.customer?.username || 'user0001'
  return id.slice(0, 3) + '****'
})
const todayStr = computed(() => {
  const d = new Date()
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}`
})
const currentMonth = computed(() => String(new Date().getMonth() + 1).padStart(2, '0'))

// === 假資料 ===
const mockData = {
  netAsset: 1523680,
  deposit: 1280000,
  twdAsset: 1150000,
  foreignAsset: 130000,
  creditTotal: 43680,
  creditUnbilled: 12500,
}

function formatMoney(n) {
  return n.toLocaleString('en-US')
}

function comingSoon() {
  alert('此功能即將推出，敬請期待！')
}

// === 資產分佈 ===
const distributionData = [
  { label: '臺幣', pct: 75, color: 'var(--primary)' },
  { label: '外幣', pct: 9, color: '#8BA58E' },
  { label: '信用卡', pct: 3, color: 'var(--accent)' },
  { label: '貸款', pct: 0, color: 'var(--border)' },
]

const donutCanvas = ref(null)
let donutChart = null

function drawDonut() {
  if (!donutCanvas.value) return
  if (donutChart) donutChart.destroy()
  donutChart = new Chart(donutCanvas.value, {
    type: 'doughnut',
    data: {
      labels: distributionData.map(d => d.label),
      datasets: [{
        data: distributionData.map(d => d.pct || 0.5),
        backgroundColor: ['#5C6B5F', '#8BA58E', '#A65A4D', '#D6CEC3'],
        borderWidth: 0,
        hoverOffset: 4,
      }],
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
            label: (ctx) => ` ${ctx.label}：${distributionData[ctx.dataIndex].pct}%`,
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
  for (let i = months; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    labels.push(`${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}`)
    twdData.push(Math.round(900000 + Math.random() * 400000))
    fxData.push(Math.round(80000 + Math.random() * 80000))
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
            callback: v => v >= 1000000 ? (v / 1000000).toFixed(1) + 'M' : v >= 1000 ? (v / 1000).toFixed(0) + 'K' : v,
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
            label: ctx => ` ${ctx.dataset.label}：$${ctx.parsed.y.toLocaleString()}`,
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
    exchangeRates.value = exchangeRates.value.map(currency => {
      const rateToTwd = rates[currency.code]
      if (!rateToTwd) return currency
      
      const midRate = 1 / rateToTwd
      const sell = (midRate * 0.995).toFixed(4)
      const buy = (midRate * 1.005).toFixed(4)
      
      return { ...currency, sell, buy }
    })
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
    drawDonut()
    drawLine()
    fetchExchangeRates()
  }
})
</script>

<style scoped>
.home-page {
  max-width: 100%;
  position: relative;
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
  grid-template-columns: 340px 1fr;
  gap: var(--space-5);
  margin-bottom: var(--space-6);
}

/* === 個人資訊卡片 === */
.profile-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: var(--space-5);
  box-shadow: none;
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.profile-header {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.profile-avatar {
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  border-radius: 50%;
  flex-shrink: 0;
}

.profile-avatar-img {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border);
}

.profile-avatar-fallback {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-primary);
  color: var(--primary);
  font-family: var(--font-heading);
  font-size: 24px;
  font-weight: 600;
  border: 2px solid var(--border);
}

.profile-name {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 2px;
  margin-bottom: 2px;
}

.profile-id {
  font-size: var(--text-xs);
  color: var(--text-disabled);
  letter-spacing: 1px;
}

.profile-meta {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding-top: var(--space-2);
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.meta-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.meta-label {
  font-size: var(--text-xs);
  color: var(--text-secondary);
}

.meta-value {
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--text-primary);
}

.profile-link {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: var(--text-sm);
  font-weight: 500;
  color: var(--primary);
  font-family: var(--font-body);
  padding: 0;
  transition: color var(--duration) var(--ease);
}

.profile-link:hover {
  color: var(--primary-dark);
}

/* === 資產總覽 === */
.asset-overview {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: var(--space-5);
  box-shadow: none;
}

.asset-header {
  margin-bottom: var(--space-4);
}

.asset-title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 3px;
  margin-bottom: var(--space-1);
}

.asset-subtitle {
  font-size: var(--text-xs);
  color: var(--text-secondary);
  margin-bottom: var(--space-2);
}

.asset-total {
  font-family: 'Inter', 'Noto Sans TC', var(--font-body);
  font-size: 36px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.asset-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-3);
}

.asset-block {
  background: var(--bg-primary);
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: var(--radius-md);
  padding: var(--space-4);
}

.asset-block-header {
  margin-bottom: var(--space-2);
}

.asset-block-label {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 1px;
}

.asset-block-amount {
  font-family: 'Inter', 'Noto Sans TC', var(--font-body);
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: var(--space-3);
  letter-spacing: -0.02em;
}

.asset-block-amount--muted {
  color: var(--text-disabled);
  font-family: var(--font-body);
  font-size: var(--text-body);
}

.asset-block-detail {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  margin-bottom: var(--space-3);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.detail-row {
  display: flex;
  justify-content: space-between;
  font-size: var(--text-xs);
  color: var(--text-secondary);
}

.text-safe {
  color: var(--primary);
  font-weight: 500;
}

.asset-block-actions {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.block-action-btn {
  padding: 6px 14px;
  font-size: var(--text-xs);
  font-family: var(--font-body);
  font-weight: 500;
  color: var(--text-primary);
  background: transparent;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: border-color 0.2s var(--ease), background 0.2s var(--ease);
}

.block-action-btn:hover {
  border-color: var(--primary);
  background: var(--primary-light);
}

/* === 下方佈局 === */
.dashboard-bottom {
  display: grid;
  grid-template-columns: 280px 1fr 1fr;
  gap: var(--space-5);
}

/* === 通用卡片標題 === */
.card-title {
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 3px;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
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
}

.exchange-table {
  width: 100%;
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
  100% { transform: rotate(360deg); }
}

/* === 歷史水位圖 === */
.watermark-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: 20px;
  padding: var(--space-5);
  box-shadow: none;
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
  .distribution-card .card-title { grid-column: 1 / -1; }
  .distribution-card .section-rule { grid-column: 1 / -1; }
}

@media (max-width: 768px) {
  .asset-grid { grid-template-columns: 1fr; }
  .dashboard-bottom { grid-template-columns: 1fr; }
  .distribution-card {
    grid-template-columns: 1fr;
  }
}
</style>
