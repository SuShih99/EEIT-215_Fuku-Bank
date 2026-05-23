<template>
  <div class="dashboard">
    <!-- ── 歡迎區 ── -->
    <div class="welcome-section">
      <div class="welcome-text">
        <h1>{{ greeting }}，{{ userName }}</h1>
        <p class="welcome-sub">
          角色：<a-tag class="role-tag-muted">{{ userRole }}</a-tag>
          <span v-if="lastLogin" class="last-login">上次登入：{{ lastLogin }}</span>
        </p>
      </div>
      <div class="welcome-date">
        <div class="date-display">{{ todayDate }}</div>
        <div class="time-display">{{ currentTime }}</div>
      </div>
    </div>

    <!-- ── 統計卡片 ── -->
    <div class="stats-grid">
      <div class="stat-card" v-for="stat in stats" :key="stat.label">
        <div class="stat-icon" :style="{ background: stat.bg }">
          <component :is="stat.icon" :style="{ color: stat.color, fontSize: '24px' }" />
        </div>
        <div class="stat-info">
          <div class="stat-value">
            <span v-if="stat.loading" class="loading-dot">...</span>
            <span v-else>{{ stat.value }}</span>
          </div>
          <div class="stat-label">{{ stat.label }}</div>
        </div>
      </div>
    </div>

    <!-- ── 快捷入口 ── -->
    <div class="section-title">快捷操作</div>
    <div class="shortcut-grid">
      <div
        class="shortcut-card"
        v-for="item in shortcuts"
        :key="item.route"
        @click="$router.push(item.route)"
      >
        <div class="shortcut-icon" :style="{ background: item.bg }">
          <component :is="item.icon" :style="{ color: item.color, fontSize: '20px' }" />
        </div>
        <div class="shortcut-label">{{ item.label }}</div>
        <div class="shortcut-desc">{{ item.desc }}</div>
      </div>
    </div>

    <!-- ── 最新交易（業務人員可見）── -->
    <template v-if="!isCISO">
      <div class="section-title">最新交易紀錄</div>
      <a-table
        :columns="transColumns"
        :data-source="recentTrans"
        :loading="transLoading"
        :pagination="false"
        size="small"
        row-key="transactionId"
        :locale="{ emptyText: '目前沒有交易紀錄' }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'transactionType'">
            <a-tag class="transaction-type-tag">
              {{ typeLabel(record.transactionType) }}
            </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'amount'">
            {{ formatAmount(record.amount, record.currency) }}
          </template>
          <template v-else-if="column.dataIndex === 'createdAt'">
            {{ formatDate(record.createdAt) }}
          </template>
        </template>
      </a-table>
    </template>

  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import {
  BankOutlined,
  SwapOutlined,
  FileTextOutlined,
  TeamOutlined,
  UserOutlined,
  CreditCardOutlined,
  AlertOutlined,
  DollarOutlined,
  AccountBookOutlined,
  AuditOutlined,
} from '@ant-design/icons-vue'
import { getLatestAccounts } from '@/api/account'
import { getLatestTransLogs } from '@/api/account'
import { getCustomers } from '@/api/customer'
import { getEmployeeCount } from '@/api/auth'

const authStore = useAuthStore()

// ── 角色權限（permLevel 數字判斷，統一來源）──
const permLevel = computed(() => authStore.user?.permLevel ?? 0)
const isCISO   = computed(() => permLevel.value >= 4)  // 資安長 (Lvl4+)
const isManager = computed(() => permLevel.value >= 2) // 主管及以上

// 角色 roleCode → 顯示名稱映射表
const roleDisplayName = {
  CISO: '系統管理員',
  CFDM: '主管',
  CFSO: '職員',
}

// ── 使用者資訊 ──
const userName = computed(() => roleDisplayName[authStore.user?.roleCode] || authStore.user?.empName || '使用者')
const userRole = computed(() => authStore.user?.roleCode || '未知')
const lastLogin = computed(() => {
  const d = authStore.user?.lastLoginDate
  if (!d) return ''
  return new Date(d).toLocaleString('zh-TW')
})

// ── 時間 ──
const todayDate = ref('')
const currentTime = ref('')
let timer = null

function updateTime() {
  const now = new Date()
  const weekDays = ['日', '一', '二', '三', '四', '五', '六']
  todayDate.value = `${now.getFullYear()}/${String(now.getMonth() + 1).padStart(2, '0')}/${String(now.getDate()).padStart(2, '0')} 週${weekDays[now.getDay()]}`
  currentTime.value = now.toLocaleTimeString('zh-TW', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 12) return '早安'
  if (h < 18) return '午安'
  return '晚安'
})

// ── 統計卡片 ──
const accountCount = ref(0)
const customerCount = ref(0)
const employeeCount = ref(0)
const todayTransCount = ref(0)
const statsLoading = ref(true)
const iconTones = {
  account: { bg: 'rgba(92, 107, 95, 0.11)', color: '#5C6B5F' },
  customer: { bg: 'rgba(78, 102, 92, 0.10)', color: '#4E665C' },
  employee: { bg: 'rgba(92, 91, 76, 0.10)', color: '#5C5B4C' },
  transaction: { bg: 'rgba(93, 107, 113, 0.10)', color: '#5D6B71' },
  loan: { bg: 'rgba(143, 116, 70, 0.11)', color: '#7A6744' },
  risk: { bg: 'rgba(166, 90, 77, 0.10)', color: '#9A554B' },
  card: { bg: 'rgba(106, 92, 116, 0.10)', color: '#665A70' },
  system: { bg: 'rgba(91, 96, 101, 0.10)', color: '#5B6065' },
}

// ── 統計卡片：依角色切換 ──
const stats = computed(() => {
  if (isCISO.value) {
    // 資安長視角：稽核數據
    return [
      { label: '員工總人數', value: employeeCount.value, icon: TeamOutlined,        ...iconTones.employee, loading: statsLoading.value },
      { label: '今日系統登入', value: todayTransCount.value || '—',  icon: AccountBookOutlined, ...iconTones.system, loading: false },
      { label: '系統可用率',   value: '99.9%',                       icon: AlertOutlined,       ...iconTones.account, loading: false },
      { label: '高風險操作',   value: '0',                            icon: AuditOutlined,       ...iconTones.risk, loading: false },
    ]
  }
  // 業務人員視角：業務數據
  return [
    { label: '帳戶總數', value: accountCount.value,   icon: BankOutlined,  ...iconTones.account, loading: statsLoading.value },
    { label: '客戶總數', value: customerCount.value,  icon: UserOutlined,  ...iconTones.customer, loading: statsLoading.value },
    { label: '員工人數', value: employeeCount.value,  icon: TeamOutlined,  ...iconTones.employee, loading: statsLoading.value },
    { label: '最新交易', value: todayTransCount.value, icon: SwapOutlined,  ...iconTones.transaction, loading: statsLoading.value },
  ]
})

// ── 快捷入口：依角色切換 ──
const businessShortcuts = [
  { label: '帳戶管理',   desc: '查看與管理帳戶',    route: '/admin/accounts',          icon: BankOutlined,     ...iconTones.account },
  { label: '交易紀錄',   desc: '查看所有交易紀錄',    route: '/admin/trans-logs',       icon: FileTextOutlined, ...iconTones.transaction },
  { label: '貸款管理',   desc: '審核貸款申請',       route: '/admin/loan-applications', icon: AuditOutlined,    ...iconTones.loan },
  { label: '風險事件',   desc: '監控異常風險',       route: '/admin/risk-events',       icon: AlertOutlined,    ...iconTones.risk },
  { label: '信用卡管理', desc: '卡別與申請',         route: '/admin/card-applications', icon: CreditCardOutlined, ...iconTones.card },
]
const cisoShortcuts = [
  { label: '系統日誌', desc: '查看所有操作日誌', route: '/admin/logs',      icon: FileTextOutlined, ...iconTones.system },
  { label: '員工管理', desc: '帳號與權限管理',   route: '/admin/employees', icon: TeamOutlined,     ...iconTones.employee },
]
const shortcuts = computed(() => isCISO.value ? cisoShortcuts : businessShortcuts)

// ── 最新交易表格 ──
const recentTrans = ref([])
const transLoading = ref(true)

const transColumns = [
  { title: '交易序號', dataIndex: 'referenceId', width: 260, ellipsis: true },
  { title: '帳號', dataIndex: 'accountNumber', width: 160 },
  { title: '類型', dataIndex: 'transactionType', width: 100 },
  { title: '金額', dataIndex: 'amount', width: 140 },
  { title: '時間', dataIndex: 'createdAt', width: 170 },
]

const typeMap = {
  TRANSFER: '轉帳',
  DEPOSIT: '存款',
  WITHDRAW: '提款',
  EXCHANGE: '換匯',
  INTEREST: '利息',
  LOAN_DISBURSEMENT: '貸款撥款',
  LOAN_REPAYMENT: '貸款還款',
  CARD_PAYMENT: '信用卡繳款',
  CARD_SETTLEMENT: '信用卡結算',
  CARD_REWARD: '信用卡回饋',
  REVERSAL: '沖正',
}

function typeLabel(t) { return typeMap[t] || t }

function formatAmount(amount, currency) {
  const c = currency || 'TWD'
  return `${c} ${Number(amount).toLocaleString()}`
}

function formatDate(d) {
  if (!d) return ''
  return new Date(d).toLocaleString('zh-TW')
}


// ── 載入資料 ──
async function loadStats() {
  statsLoading.value = true
  try {
    // 資安長不需要帳戶與客戶統計，只取員工數
if (isCISO.value) {
      const empRes = await getEmployeeCount().catch(() => null)
      if (empRes) employeeCount.value = empRes.data?.data ?? 0
    } else {
      const [accRes, custRes, empRes] = await Promise.allSettled([
        getLatestAccounts(0, 1),
        getCustomers(),
        getEmployeeCount(),
      ])
      if (accRes.status === 'fulfilled') accountCount.value = accRes.value.data?.data?.totalElements ?? 0
      if (custRes.status === 'fulfilled') customerCount.value = custRes.value.data?.data?.length ?? 0
      if (empRes.status === 'fulfilled') employeeCount.value = empRes.value.data?.data ?? 0
    }
  } catch {
    // silent
  } finally {
    statsLoading.value = false
  }
}

async function loadRecentTrans() {
  if (isCISO.value) return // 資安長不顯示交易資料
  transLoading.value = true
  try {
    const res = await getLatestTransLogs(0, 8)
    const data = res.data?.data
    recentTrans.value = data?.content ?? data ?? []
    todayTransCount.value = data?.totalElements ?? recentTrans.value.length
  } catch {
    recentTrans.value = []
  } finally {
    transLoading.value = false
  }
}

onMounted(() => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  loadStats()
  loadRecentTrans()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.dashboard {
  max-width: 1200px;
}

/* ── 歡迎區 ── */
.welcome-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #5C6B5F 0%, #7A8C7E 100%);
  border-radius: 24px;
  padding: 32px;
  margin-bottom: 24px;
  color: #fff;
  box-shadow: 0 10px 30px rgba(92, 107, 95, 0.2);
}

.welcome-text h1 {
  color: #fff;
  font-size: 24px;
  margin: 0 0 8px 0;
  font-weight: 600;
}

.welcome-sub {
  color: rgba(255, 255, 255, 0.85);
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.last-login {
  color: rgba(255, 255, 255, 0.65);
  font-size: 13px;
}

.role-tag-muted {
  background: rgba(255, 255, 255, 0.16) !important;
  border-color: rgba(255, 255, 255, 0.26) !important;
  color: rgba(255, 255, 255, 0.92) !important;
  font-weight: 600;
}

.welcome-date {
  text-align: right;
}

.date-display {
  font-size: 15px;
  opacity: 0.85;
}

.time-display {
  font-size: 28px;
  font-weight: 300;
  letter-spacing: 2px;
  margin-top: 4px;
}

/* ── 統計卡片 ── */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 28px;
}

.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #8c8c8c;
  margin-top: 2px;
}

.loading-dot {
  color: #bbb;
}

/* ── 區塊標題 ── */
.section-title {
  font-size: 17px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 14px;
}

/* ── 快捷入口 ── */
.shortcut-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 28px;
}

.shortcut-card {
  background: #fff;
  border-radius: 14px;
  padding: 28px 16px 24px;
  text-align: center;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 130px;
}

.shortcut-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.shortcut-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 10px;
}

.shortcut-label {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 4px;
}

.shortcut-desc {
  font-size: 12px;
  color: #8c8c8c;
}

.transaction-type-tag {
  background: rgba(92, 107, 95, 0.08) !important;
  border: 1px solid rgba(92, 107, 95, 0.18) !important;
  color: #5C6B5F !important;
  font-weight: 600;
}

/* ── RWD ── */
@media (max-width: 1100px) {
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
  .shortcut-grid { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 600px) {
  .stats-grid { grid-template-columns: 1fr; }
  .shortcut-grid { grid-template-columns: repeat(2, 1fr); }
  .welcome-section { flex-direction: column; gap: 16px; text-align: center; }
  .welcome-date { text-align: center; }
}

</style>
