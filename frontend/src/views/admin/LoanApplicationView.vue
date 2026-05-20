<template>
  <div class="loan-admin" @click="typeDropdownOpen = false">

    <!-- ── 頁首 ── -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">貸款申請管理</h1>
      </div>
      <div class="header-actions">
        <button class="btn btn-ghost btn-sm" @click.stop="fetchApplications" :disabled="loading">
          <span v-if="loading" class="spin">⟳</span>
          <span v-else>↻</span>
          重新整理
        </button>
      </div>
    </div>

    <div class="dashboard-grid">
      <button class="metric-card" type="button" @click="setStatus('PENDING_CONTACT')">
        <span class="metric-label">今日新增申請</span>
        <strong class="metric-value">{{ dashboardLoading ? '—' : dashboardStats.todayNew }}</strong>
        <span class="metric-hint">依申請建立時間統計</span>
      </button>
      <button class="metric-card" type="button" @click="setStatus('PENDING_REVIEW')">
        <span class="metric-label">待審核案件</span>
        <strong class="metric-value">{{ dashboardLoading ? '—' : dashboardStats.pendingReview }}</strong>
        <span class="metric-hint">狀態為審核中</span>
      </button>
      <button class="metric-card metric-warning" type="button" @click="setStatus('RETURNED')">
        <span class="metric-label">需補件案件</span>
        <strong class="metric-value">{{ dashboardLoading ? '—' : dashboardStats.returned }}</strong>
        <span class="metric-hint">狀態為退回補件</span>
      </button>
      <div class="metric-card">
        <span class="metric-label">本月核准金額</span>
        <strong class="metric-value">{{ dashboardLoading ? '—' : formatAmount(dashboardStats.monthApprovedAmount) }}</strong>
        <span class="metric-hint">依核准/撥款申請估算</span>
      </div>
      <div class="metric-card metric-primary">
        <span class="metric-label">本月撥款金額</span>
        <strong class="metric-value">{{ dashboardLoading ? '—' : formatAmount(dashboardStats.monthDisbursedAmount) }}</strong>
        <span class="metric-hint">依已建立貸款帳戶本金</span>
      </div>
    </div>

    <!-- ── 篩選列 ── -->
    <div class="filter-bar">
      <!-- 申請狀態 pills -->
      <div class="pills-group">
        <button
          v-for="s in STATUS_OPTIONS"
          :key="s.value"
          class="filter-pill"
          :class="{ active: currentStatus === s.value }"
          @click="setStatus(s.value)"
        >
          <span class="pill-dot" :class="s.dot"></span>
          {{ s.label }}
          <span class="pill-count" v-if="currentStatus === s.value && !loading">
            {{ applications.length }}
          </span>
        </button>
      </div>

      <!-- ① 貸款類型多選下拉 -->
      <div class="type-dropdown-wrap" @click.stop>
        <button
          class="type-dropdown-trigger"
          :class="{ open: typeDropdownOpen, active: selectedTypes.length > 0 }"
          @click="typeDropdownOpen = !typeDropdownOpen"
        >
          <span class="trigger-icon"><i class="fa-solid fa-filter"></i></span>
          <span v-if="selectedTypes.length === 0">貸款類型</span>
          <span v-else-if="selectedTypes.length === 1">{{ LOAN_TYPE_NAME[selectedTypes[0]] }}</span>
          <span v-else>已選 {{ selectedTypes.length }} 種</span>
          <span class="dtag-row" v-if="selectedTypes.length > 0">
            <span
              v-for="t in selectedTypes.slice(0, 3)"
              :key="t"
              class="dtag"
              :class="'dtag-' + t"
            >{{ LOAN_TYPE_NAME[t] }}</span>
            <span v-if="selectedTypes.length > 3" class="dtag-more">+{{
                selectedTypes.length - 3
              }}</span>
          </span>
          <span class="dropdown-caret" :class="{ rotated: typeDropdownOpen }">▾</span>
        </button>

        <transition name="drop">
          <div class="type-dropdown-menu" v-show="typeDropdownOpen">
            <div class="dropdown-header">
              <span class="dropdown-title">貸款類型篩選</span>
              <button class="clear-btn" v-if="selectedTypes.length > 0" @click.stop="clearTypes">
                清除全部
              </button>
            </div>

            <label class="dropdown-item all-item" @click.stop>
              <input type="checkbox" :checked="selectedTypes.length === 0" @change="clearTypes"/>
              <span class="check-box"></span>
              <span class="item-name">全部類型</span>
              <span class="item-count">{{ applications.length }}</span>
            </label>

            <div class="dropdown-divider"></div>

            <label
              v-for="key in LOAN_TYPE_KEYS"
              :key="key"
              class="dropdown-item"
              @click.stop
            >
              <input type="checkbox" :value="key" v-model="selectedTypes"
                     @change="currentPage = 1"/>
              <span class="check-box"></span>
              <span class="item-dot" :class="'idot-' + key"></span>
              <span class="item-name">{{ LOAN_TYPE_NAME[key] }}</span>
              <span class="item-count">{{ countByType(key) }}</span>
            </label>
          </div>
        </transition>
      </div>

      <!-- 姓名模糊搜尋 -->
      <div class="name-search-wrap">
        <span class="name-search-icon"><i class="fa-solid fa-magnifying-glass"></i></span>
        <input
          class="name-search-input"
          type="text"
          placeholder="搜尋申請人姓名…"
          v-model="nameQuery"
          @input="currentPage = 1"
        />
        <button
          v-if="nameQuery"
          class="name-search-clear"
          @click="nameQuery = ''; currentPage = 1"
          title="清除"
        ><i class="fa-solid fa-x"></i></button>
      </div>

      <!-- 結果摘要 -->
      <div class="filter-meta" v-if="!loading">
        <span class="status-dot"
              :class="STATUS_OPTIONS.find(s => s.value === currentStatus)?.dot"></span>
        共 <strong>{{ filteredApplications.length }}</strong> 筆
        <span v-if="sortKey" class="sort-hint">
          · 依 {{ sortLabel(sortKey) }} {{ sortDir === 'asc' ? '↑' : '↓' }}
        </span>
      </div>
    </div>

    <!-- ── 錯誤提示 ── -->
    <div v-if="error" class="alert-error"><span><i class="fa-solid fa-triangle-exclamation"></i></span> {{ error }}</div>

    <!-- ── Table Card ── -->
    <div class="table-card">

      <!-- ② 工具列：筆數資訊 + 每頁筆數選單 -->
      <div class="table-toolbar">
        <span class="toolbar-info" v-if="!loading && filteredApplications.length > 0">
          第 {{ pageStart }}–{{ pageEnd }} 筆，共 {{ filteredApplications.length }} 筆
        </span>
        <span class="toolbar-info" v-else-if="loading">載入中…</span>
        <span class="toolbar-info" v-else>—</span>

        <div class="page-size-wrap">
          <span class="size-label">每頁顯示</span>
          <div class="styled-select-wrap">
            <select class="styled-select" v-model.number="pageSize" @change="currentPage = 1">
              <option v-for="n in PAGE_SIZE_OPTIONS" :key="n" :value="n">{{ n }} 筆</option>
            </select>
            <span class="select-caret">▾</span>
          </div>
        </div>
      </div>

      <!-- Loading skeleton -->
      <div v-if="loading" class="skeleton-wrap">
        <div v-for="i in pageSize" :key="i" class="skeleton-row">
          <div class="sk sk-id"></div>
          <div class="sk sk-mid"></div>
          <div class="sk sk-mid"></div>
          <div class="sk sk-short"></div>
          <div class="sk sk-short"></div>
          <div class="sk sk-short"></div>
        </div>
      </div>

      <!-- Empty -->
      <div v-else-if="filteredApplications.length === 0" class="empty-state">
        <div class="empty-icon"><i class="fa-solid fa-inbox"></i></div>
        <p>此條件下目前沒有申請記錄</p>
      </div>

      <!-- Table -->
      <div v-else class="table-wrap">
        <table class="data-table">
          <thead>
          <tr>
            <!-- ① 可排序欄位 -->
            <th @click="toggleSort('applicationId')" class="sortable">
              申請編號<span class="sort-icon">{{ sortIcon('applicationId') }}</span>
            </th>
            <th>申請人</th>
            <th @click="toggleSort('applyType')" class="sortable">
              類型<span class="sort-icon">{{ sortIcon('applyType') }}</span>
            </th>
            <th @click="toggleSort('applyAmount')" class="sortable">
              金額<span class="sort-icon">{{ sortIcon('applyAmount') }}</span>
            </th>
            <th @click="toggleSort('applyPeriod')" class="sortable">
              期數<span class="sort-icon">{{ sortIcon('applyPeriod') }}</span>
            </th>
            <th @click="toggleSort('rate')" class="sortable">
              利率<span class="sort-icon">{{ sortIcon('rate') }}</span>
            </th>
            <th @click="toggleSort('applicationStatus')" class="sortable">
              申請狀態<span class="sort-icon">{{ sortIcon('applicationStatus') }}</span>
            </th>
            <th @click="toggleSort('progressTime')" class="sortable">
              {{ progressColumnLabel }}<span class="sort-icon">{{ sortIcon('progressTime') }}</span>
            </th>
            <th @click="toggleSort('createTime')" class="sortable">
              申請時間<span class="sort-icon">{{ sortIcon('createTime') }}</span>
            </th>
            <th class="th-action">操作</th>
          </tr>
          </thead>
          <tbody>
          <tr
            v-for="(app, idx) in pagedApplications"
            :key="app.applicationId"
            class="data-row"
            :style="{ animationDelay: idx * 20 + 'ms' }"
          >
            <!-- 申請編號 -->
            <td><span class="id-chip">{{ app.applicationId }}</span></td>

            <!-- 申請人 -->
            <td>
              <div v-if="app.customerId" class="applicant-member">
                <span class="member-id">{{ app.cif || app.customerId }}</span>
                <span class="member-name">{{ app.memberName || '—' }}</span>
              </div>
              <div v-else class="applicant-nonmember">
                <div class="nm-name">{{ app.applicantName || '—' }}</div>
                <div class="nm-meta">
                  <span v-if="app.applicantPhone"><i class="fa-solid fa-square-phone"></i> {{ app.applicantPhone }}</span>
                  <span v-if="app.applicantEmail" class="nm-email"><i class="fa-solid fa-envelope"></i> {{ app.applicantEmail }}</span>
                </div>
              </div>
            </td>

            <!-- 類型 -->
            <td>
                <span class="type-badge" :class="'type-' + app.applyType">
                  {{ LOAN_TYPE_NAME[app.applyType] || app.applyType }}
                </span>
            </td>

            <!-- 金額 -->
            <td>
              <div class="amount">{{ formatAmount(displayAmount(app)) }}</div>
              <div class="confirmed-hint" v-if="isConfirmedValue(app)"><i class="fa-solid fa-check"></i> 確認值</div>
            </td>

            <!-- 期數 -->
            <td><span class="meta-tag">{{ displayPeriod(app) }} 個月</span></td>

            <!-- 利率 -->
            <td><span class="meta-rate">{{ formatRate(displayRate(app)) }}</span></td>

            <!-- 申請狀態 -->
            <td>
                <span class="status-badge" :class="STATUS_CLASS[app.applicationStatus]">
                  {{ STATUS_LABEL[app.applicationStatus] || app.applicationStatus }}
                </span>
            </td>

            <!-- 最新進度 -->
            <td>
              <div v-if="showContactProgress(app)" class="contact-cell">
                  <span class="contact-status" :class="CONTACT_CLASS[app.latestContactStatus]">
                    {{ CONTACT_LABEL[app.latestContactStatus] || app.latestContactStatus }}
                  </span>
                <div class="contact-time">{{ formatDate(app.latestContactTime) }}</div>
              </div>
              <div v-else-if="progressTime(app)" class="date-cell">
                <span class="date-main">{{ formatDateShort(progressTime(app)) }}</span>
                <span class="date-time">{{ formatTime(progressTime(app)) }}</span>
              </div>
              <span v-else class="text-muted">—</span>
            </td>

            <!-- 申請時間 -->
            <td>
              <div class="date-cell">
                <span class="date-main">{{ formatDateShort(app.createTime) }}</span>
                <span class="date-time">{{ formatTime(app.createTime) }}</span>
              </div>
            </td>

              <!-- 操作 -->
              <td class="td-action">
                <div class="action-btns">
                  <button
                    class="btn btn-xs btn-outline"
                    @click="openContactModal(app)"
                    title="聯繫紀錄"
                  >
                    <i class="fa-solid fa-phone-volume"></i>
                  </button>
                  <!-- 審核填單：需 permLevel >= 2 (CFDM 主管以上) -->
                  <button
                    class="btn btn-xs"
                    :class="canApprove ? 'btn-outline' : 'btn-disabled'"
                    @click="canApprove && openReviewModal(app)"
                    :disabled="!canApprove"
                    :title="canApprove ? '審核填單' : '權限不足：需主管 (CFDM) 以上才能審核'"
                  >
                    <i class="fa-solid fa-user-check"></i>
                  </button>
                  <!-- 上傳文件 -->
                  <button
                    class="btn btn-xs btn-outline"
                    @click="openDocModal(app)"
                    title="上傳文件"
                  >
                    <i class="fa-solid fa-folder"></i>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- ③ 分頁 Footer -->
      <div class="table-footer" v-if="!loading && filteredApplications.length > 0">
        <span class="footer-count">
          第 <strong>{{ currentPage }}</strong> / {{ totalPages }} 頁
        </span>

        <div class="pagination">
          <button class="page-btn" @click="goToPage(1)" :disabled="currentPage === 1"
                  title="第一頁">«
          </button>
          <button class="page-btn nav-btn" @click="goToPage(currentPage - 1)"
                  :disabled="currentPage === 1" title="上一頁">‹ 上一頁
          </button>

          <div class="page-numbers">
            <button
              v-for="p in pageNumbers"
              :key="p"
              class="page-btn"
              :class="{ active: p === currentPage, ellipsis: p === '…' }"
              @click="p !== '…' && goToPage(p)"
              :disabled="p === '…'"
            >{{ p }}
            </button>
          </div>

          <button class="page-btn nav-btn" @click="goToPage(currentPage + 1)"
                  :disabled="currentPage === totalPages" title="下一頁">下一頁 ›
          </button>
          <button class="page-btn" @click="goToPage(totalPages)"
                  :disabled="currentPage === totalPages" title="最後一頁">»
          </button>
        </div>
      </div>

    </div>
  </div>

  <!-- ── 聯繫紀錄 Modal ── -->
  <LoanContactLogModal
    v-model="contactModalOpen"
    :app="contactModalApp"
    @log-added="onLogAdded"
  />

  <!-- ── 審核填單 Modal ── -->
  <LoanReviewModal
    v-model="reviewModalOpen"
    :app="reviewModalApp"
    @review-updated="onReviewUpdated"
  />

  <!-- ── 上傳文件 Modal ── -->
  <LoanDocumentModal
    v-model="docModalOpen"
    :app="docModalApp"
  />
</template>

<script setup>
import {ref, computed, onMounted, onUnmounted} from 'vue'
import api from '@/api/axios'
import LoanContactLogModal from './LoanContactLogModal.vue'
import LoanReviewModal from './LoanReviewModal.vue'
import LoanDocumentModal from './LoanDocumentModal.vue'
import { useAuthStore } from '@/stores/auth'

// ── 角色權限判斷 ──
const authStore = useAuthStore()
// 雙重判斷：permLevel 數字 或 roleCode 字串
const APPROVER_ROLES = ['CFDM', 'CSDM', 'CRDM', 'CRO', 'COO', 'CISO', 'ISSA']
const canApprove = computed(() => {
  const level = parseInt(authStore.user?.permLevel ?? 0)
  const code = authStore.user?.roleCode ?? ''
  return level >= 2 || APPROVER_ROLES.includes(code)
})

// ── Emits ──
defineEmits([])

// ── Constants ──
const API_URL = '/api/admin/loan-applications'

// ② 每頁筆數選項
const PAGE_SIZE_OPTIONS = [10, 20, 50, 100]

// ── Contact Modal state ──
const contactModalOpen = ref(false)
const contactModalApp = ref(null)

function openContactModal(app) {
  contactModalApp.value = app
  contactModalOpen.value = true
}

function onLogAdded() {
  fetchApplications()
}

// ── Review Modal state ──
const reviewModalOpen = ref(false)
const reviewModalApp = ref(null)

function openReviewModal(app) {
  reviewModalApp.value = app
  reviewModalOpen.value = true
}

function onReviewUpdated() {
  fetchApplications()
}

// ── Document Modal state ──
const docModalOpen = ref(false)
const docModalApp  = ref(null)

function openDocModal(app) {
  docModalApp.value  = app
  docModalOpen.value = true
}

const STATUS_OPTIONS = [
  {value: 'PENDING_CONTACT', label: '新申請', dot: 'dot-amber'},
  {value: 'IN_CONTACT', label: '聯繫中', dot: 'dot-blue'},
  {value: 'PENDING_REVIEW', label: '審核中', dot: 'dot-purple'},
  {value: 'RETURNED', label: '退回補件', dot: 'dot-red'},
  {value: 'APPROVED', label: '已核准', dot: 'dot-green'},
  {value: 'REJECTED', label: '已拒絕', dot: 'dot-red'},
  {value: 'CANCELLED', label: '已取消', dot: 'dot-orange'},
  {value: 'DISBURSED', label: '已撥款', dot: 'dot-teal'},
  {value: 'CLOSED', label: '已結案', dot: 'dot-gray'},
]

const STATUS_LABEL = {
  PENDING_CONTACT: '新申請', IN_CONTACT: '聯繫中', PENDING_REVIEW: '審核中',
  RETURNED: '退回補件',
  APPROVED: '已核准', REJECTED: '已拒絕', CANCELLED: '已取消',
  DISBURSED: '已撥款', CLOSED: '已結案',
}
const STATUS_CLASS = {
  PENDING_CONTACT: 'status-yellow', IN_CONTACT: 'status-blue', PENDING_REVIEW: 'status-purple',
  RETURNED: 'status-returned',
  APPROVED: 'status-green', REJECTED: 'status-red', CANCELLED: 'status-gray',
  DISBURSED: 'status-teal', CLOSED: 'status-gray',
}
const CONTACT_LABEL = {
  NOT_CONTACTED: '未聯繫', ATTEMPTED: '嘗試中', REACHED: '已接通',
  CONFIRMED: '已確認', DECLINED: '已放棄',
}
const CONTACT_CLASS = {
  NOT_CONTACTED: 'c-gray', ATTEMPTED: 'c-blue', REACHED: 'c-green',
  CONFIRMED: 'c-gold', DECLINED: 'c-red',
}
const LOAN_TYPE_NAME = {
  PERSONAL: '個人信貸', CAR: '汽車貸款', MOTOR: '機車貸款', STUDENT: '學貸',
  BUSINESS: '創業貸款', HOUSE: '房屋貸款', LAND: '土地貸款',
}
const LOAN_TYPE_KEYS = Object.keys(LOAN_TYPE_NAME)
const POST_REVIEW_STATUSES = new Set(['PENDING_REVIEW', 'RETURNED', 'APPROVED', 'DISBURSED', 'CLOSED'])
const STATUS_PROGRESS_STATUSES = new Set(['PENDING_REVIEW', 'RETURNED', 'APPROVED', 'REJECTED', 'DISBURSED', 'CLOSED'])

// ① 排序欄位中文對照
const SORT_LABEL = {
  applicationId: '申請編號', applyType: '類型', applyAmount: '金額',
  applyPeriod: '期數', rate: '利率', applicationStatus: '申請狀態',
  progressTime: '更新時間', createTime: '申請時間',
}

// ── State ──
const currentStatus = ref('PENDING_CONTACT')
const applications = ref([])
const allApplications = ref([])
const loanAccounts = ref([])
const loading = ref(false)
const dashboardLoading = ref(false)
const error = ref('')

// ① 排序
const sortKey = ref('')
const sortDir = ref('asc')

// ② 分頁
const currentPage = ref(1)
const pageSize = ref(10)

// ④ 類型多選
const selectedTypes = ref([])
const typeDropdownOpen = ref(false)

// ⑤ 姓名模糊搜尋
const nameQuery = ref('')

// ── Computed ──

/** ④ 類型篩選 ＋ ⑤ 姓名搜尋 → ① 排序 */
const filteredApplications = computed(() => {
  let list = applications.value

  if (selectedTypes.value.length > 0)
    list = list.filter(a => selectedTypes.value.includes(a.applyType))

  if (nameQuery.value.trim()) {
    const q = nameQuery.value.trim()
    list = list.filter(a => (a.memberName || '').includes(q))
  }

  if (sortKey.value) {
    const key = sortKey.value
    const dir = sortDir.value === 'asc' ? 1 : -1
    list = [...list].sort((a, b) => {
      const av = key === 'progressTime' ? progressTime(a) : (a[key] ?? '')
      const bv = key === 'progressTime' ? progressTime(b) : (b[key] ?? '')
      if (typeof av === 'number' && typeof bv === 'number') return (av - bv) * dir
      return String(av).localeCompare(String(bv), 'zh-TW') * dir
    })
  }
  return list
})

/** ③ 分頁計算 */
const totalPages = computed(() =>
  Math.max(1, Math.ceil(filteredApplications.value.length / pageSize.value))
)
const pageStart = computed(() =>
  filteredApplications.value.length === 0 ? 0
    : (currentPage.value - 1) * pageSize.value + 1
)
const pageEnd = computed(() =>
  Math.min(currentPage.value * pageSize.value, filteredApplications.value.length)
)
const pagedApplications = computed(() => {
  const s = (currentPage.value - 1) * pageSize.value
  return filteredApplications.value.slice(s, s + pageSize.value)
})

const progressColumnLabel = computed(() =>
  STATUS_PROGRESS_STATUSES.has(currentStatus.value) ? '更新時間' : '最新聯繫'
)

const dashboardStats = computed(() => {
  const todayStart = startOfToday()
  const tomorrowStart = new Date(todayStart)
  tomorrowStart.setDate(tomorrowStart.getDate() + 1)

  const monthStart = startOfMonth()
  const nextMonthStart = new Date(monthStart)
  nextMonthStart.setMonth(nextMonthStart.getMonth() + 1)

  const apps = allApplications.value
  const todayNew = apps.filter(app => isBetween(app.createTime, todayStart, tomorrowStart)).length
  const pendingReview = apps.filter(app => app.applicationStatus === 'PENDING_REVIEW').length
  const returned = apps.filter(app => app.applicationStatus === 'RETURNED').length
  const monthApprovedAmount = apps
    .filter(app => ['APPROVED', 'DISBURSED'].includes(app.applicationStatus))
    .filter(app => isBetween(app.updateTime || app.createTime, monthStart, nextMonthStart))
    .reduce((sum, app) => sum + Number(displayAmount(app) || 0), 0)

  const monthDisbursedAmount = loanAccounts.value
    .filter(acc => isBetween(acc.createTime || acc.startDate, monthStart, nextMonthStart))
    .reduce((sum, acc) => sum + Number(acc.principalAmount || 0), 0)

  return { todayNew, pendingReview, returned, monthApprovedAmount, monthDisbursedAmount }
})

/** 省略號分頁按鈕序列 */
const pageNumbers = computed(() => {
  const total = totalPages.value
  const cur = currentPage.value
  if (total <= 7) return Array.from({length: total}, (_, i) => i + 1)
  if (cur <= 4) return [1, 2, 3, 4, 5, '…', total]
  if (cur >= total - 3) return [1, '…', total - 4, total - 3, total - 2, total - 1, total]
  return [1, '…', cur - 1, cur, cur + 1, '…', total]
})

// ── Methods ──
async function fetchApplications() {
  loading.value = true
  error.value = ''
  try {
    const res = await api.get(API_URL, {
      params: {status: currentStatus.value},
      headers: {'Cache-Control': 'no-cache', 'Pragma': 'no-cache'},
    })
    applications.value = res.data.success ? res.data.data : []
  } catch (e) {
    error.value = e.response?.data?.message || '連線失敗，請確認後端服務是否啟動（GET /api/admin/loan-applications）'
    applications.value = []
  } finally {
    loading.value = false
  }
}

async function fetchDashboardData() {
  dashboardLoading.value = true
  try {
    const statusRequests = STATUS_OPTIONS.map(s =>
      api.get(API_URL, {
        params: {status: s.value},
        headers: {'Cache-Control': 'no-cache', 'Pragma': 'no-cache'},
      })
    )
    const [applicationResponses, accountResponse] = await Promise.all([
      Promise.all(statusRequests),
      api.get('/api/admin/loan-accounts', { params: {} }),
    ])

    const seen = new Set()
    allApplications.value = applicationResponses
      .flatMap(res => res.data.success ? res.data.data : [])
      .filter(app => {
        if (!app?.applicationId || seen.has(app.applicationId)) return false
        seen.add(app.applicationId)
        return true
      })
    loanAccounts.value = accountResponse.data.data || []
  } catch (e) {
    allApplications.value = applications.value
    loanAccounts.value = []
  } finally {
    dashboardLoading.value = false
  }
}

function setStatus(status) {
  currentStatus.value = status
  currentPage.value = 1
  fetchApplications()
}

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
}

// ① 排序切換
function toggleSort(key) {
  if (sortKey.value === key) sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  else {
    sortKey.value = key;
    sortDir.value = 'asc'
  }
  currentPage.value = 1
}

function sortIcon(key) {
  if (sortKey.value !== key) return '⇅'
  return sortDir.value === 'asc' ? '↑' : '↓'
}

// ④ 類型篩選
function clearTypes() {
  selectedTypes.value = [];
  currentPage.value = 1
}

function countByType(key) {
  return applications.value.filter(a => a.applyType === key).length
}

// ── 顯示值選擇：審核中/已核准/已撥款/已結案 使用確認值，其餘用申請值 ──
function displayAmount(app) {
  return POST_REVIEW_STATUSES.has(app.applicationStatus) && app.confirmedAmount != null
    ? app.confirmedAmount
    : app.applyAmount
}
function displayPeriod(app) {
  return POST_REVIEW_STATUSES.has(app.applicationStatus) && app.confirmedPeriod != null
    ? app.confirmedPeriod
    : app.applyPeriod
}
function displayRate(app) {
  return POST_REVIEW_STATUSES.has(app.applicationStatus) && app.confirmedRate != null
    ? app.confirmedRate
    : app.rate
}
function isConfirmedValue(app) {
  return POST_REVIEW_STATUSES.has(app.applicationStatus) && app.confirmedAmount != null
}

// ── Formatters ──
function sortLabel(key) {
  return key === 'progressTime' ? progressColumnLabel.value : (SORT_LABEL[key] || key)
}

function showContactProgress(app) {
  return !STATUS_PROGRESS_STATUSES.has(app.applicationStatus) && !!app.latestContactStatus
}

function progressTime(app) {
  if (STATUS_PROGRESS_STATUSES.has(app.applicationStatus)) {
    return app.updateTime || app.documentsSubmittedAt || app.latestContactTime || app.createTime
  }
  return app.latestContactTime
}

function formatAmount(n) {
  return n ? '$ ' + Number(n).toLocaleString('zh-TW') : '—'
}

function formatRate(r) {
  return r == null ? '—' : (r * 100).toFixed(2) + '%'
}

function formatDate(d) {
  return d ? d.replace('T', ' ').substring(0, 16) : '—'
}

function formatDateShort(d) {
  return d ? d.substring(0, 10) : '—'
}

function formatTime(d) {
  return d ? d.replace('T', ' ').substring(11, 16) : ''
}

function startOfToday() {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  return d
}

function startOfMonth() {
  const d = new Date()
  d.setDate(1)
  d.setHours(0, 0, 0, 0)
  return d
}

function isBetween(value, start, end) {
  if (!value) return false
  const d = new Date(value)
  return !Number.isNaN(d.getTime()) && d >= start && d < end
}

// ── Auto-refresh（固定 30 秒）──
let refreshTimer = null

function startAutoRefresh() {
  refreshTimer = setInterval(() => {
    fetchApplications()
    fetchDashboardData()
  }, 30_000)
}

onMounted(() => {
  fetchApplications();
  fetchDashboardData()
  startAutoRefresh()
})
onUnmounted(() => clearInterval(refreshTimer))
</script>

<style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/7.0.1/css/all.min.css');

.loan-admin {
  /* ── 配色依據：對齊 admin-theme ── */
  --accent: #5C6B5F;
  --accent-dim: rgba(92, 107, 95, 0.10);
  --accent-lt: rgba(92, 107, 95, 0.20);
  --bg: #f4f5f7;
  --surface: #ffffff;
  --surface-2: #f0f2f0;
  --border: #dde1de;
  --border-2: #c8cdc9;
  --ink: #2B2B2B;
  --ink-2: #333333;
  --muted: #8c9891;
  --muted-2: #5a6a5e;
  --primary: #5C6B5F;
  --primary-dk: #4A574D;
  --red: #C0392B;
  --green: #4A8C5C;
  --yellow: #8C7355;
  --amber: #C49A3C;
  --blue: #4E7A96;
  --teal: #3D8A78;
  --purple: #7265A0;
  --orange: #C47A3C;

  padding: 32px 28px;
  max-width: 1400px;
  margin: 0 auto;
  font-family: 'Noto Sans TC', sans-serif;
  color: var(--ink);
  background: var(--bg);
  min-height: 100vh;
}

/* ── Page Header ── */
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin-bottom: 24px;
}

.page-title {
  font-family: 'Noto Serif TC', serif;
  font-size: 26px;
  font-weight: 700;
  color: var(--ink);
  line-height: 1.2;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 18px;
}

.metric-card {
  appearance: none;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  min-height: 106px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface);
  box-shadow: 0 10px 26px rgba(63, 74, 66, 0.04);
  color: var(--ink);
  text-align: left;
  transition: transform 0.15s, border-color 0.15s, box-shadow 0.15s;
}

button.metric-card {
  cursor: pointer;
}

button.metric-card:hover {
  transform: translateY(-1px);
  border-color: var(--accent);
  box-shadow: 0 12px 28px rgba(63, 74, 66, 0.08);
}

.metric-label {
  font-size: 14px;
  font-weight: 600;
  color: var(--muted-2);
}

.metric-value {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 24px;
  line-height: 1.1;
  color: var(--ink);
}

.metric-hint {
  margin-top: auto;
  font-size: 13px;
  color: var(--muted);
}

.metric-primary {
  border-color: rgba(74, 140, 92, 0.24);
  background: rgba(74, 140, 92, 0.06);
}

.metric-warning {
  border-color: rgba(192, 57, 43, 0.22);
  background: rgba(192, 57, 43, 0.05);
}

@media (max-width: 1180px) {
  .dashboard-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

/* Auto-refresh */
.auto-refresh-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.ar-label {
  font-size: 14px;
  color: var(--muted-2);
  white-space: nowrap;
}

.ar-select {
  min-width: 80px;
}

.ar-countdown {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-family: 'IBM Plex Mono', monospace;
  font-size: 13px;
  color: var(--accent);
  background: var(--accent-dim);
  border: 1px solid var(--accent-lt);
  padding: 3px 9px;
  border-radius: 20px;
}

.ar-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--accent);
  flex-shrink: 0;
}

.ar-dot.pulsing {
  animation: pulse 1.4s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.4;
    transform: scale(0.8);
  }
}

/* ── Filter Bar ── */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.pills-group {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.filter-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 13px;
  border-radius: 20px;
  font-size: 14px;
  font-family: 'IBM Plex Mono', monospace;
  cursor: pointer;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--muted-2);
  transition: all 0.15s;
}

.filter-pill:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.filter-pill.active {
  background: rgba(92, 107, 95, 0.10);
  border-color: var(--accent);
  color: var(--accent);
}

.pill-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.8;
}

.pill-count {
  background: rgba(37, 99, 235, 0.12);
  border-radius: 10px;
  padding: 1px 6px;
  font-size: 12px;
}

/* ── Type Multi-Select Dropdown ── */
.type-dropdown-wrap {
  position: relative;
}

.type-dropdown-trigger {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 6px 12px;
  border-radius: 12px;
  font-size: 13px;
  font-family: 'Noto Sans TC', sans-serif;
  cursor: pointer;
  border: 1px solid var(--border-2);
  background: var(--surface);
  color: var(--ink-2);
  transition: all 0.15s;
  outline: none;
  white-space: nowrap;
}

.type-dropdown-trigger:hover {
  border-color: var(--accent);
}

.type-dropdown-trigger.open {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-dim);
}

.type-dropdown-trigger.active {
  border-color: var(--accent);
  background: rgba(92, 107, 95, 0.10);
  color: var(--accent);
}

.trigger-icon {
  font-size: 13px;
}

.dropdown-caret {
  font-size: 10px;
  color: var(--muted-2);
  transition: transform 0.2s;
  margin-left: 2px;
}

.dropdown-caret.rotated {
  transform: rotate(180deg);
}

.dtag-row {
  display: flex;
  gap: 4px;
}

.dtag {
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 4px;
  border: 1px solid;
  font-family: 'IBM Plex Mono', monospace;
}

.dtag-PERSONAL {
  color: #7A5C3A;
  border-color: #D6CEC3;
  background: #F5F1EA;
}

.dtag-CAR {
  color: #3F5F5A;
  border-color: #B5CECA;
  background: #EEF3F2;
}

.dtag-MOTOR {
  color: #7A4A38;
  border-color: #D4B8AE;
  background: #F5EDE9;
}

.dtag-STUDENT {
  color: #4A6B5C;
  border-color: #B5CCBF;
  background: #EEF3EF;
}

.dtag-BUSINESS {
  color: #5C5074;
  border-color: #C4BCDA;
  background: #F0EEF5;
}

.dtag-HOUSE {
  color: #3D5C58;
  border-color: #AECBC7;
  background: #EBF2F1;
}

.dtag-LAND {
  color: #6E6259;
  border-color: #D6CEC3;
  background: #EAE4DA;
}

.dtag-more {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 4px;
  background: var(--surface-2);
  border: 1px solid var(--border-2);
  color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
}

.type-dropdown-menu {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: 14px;
  box-shadow: 0 10px 26px rgba(63, 74, 66, 0.06);
  min-width: 230px;
  z-index: 300;
  overflow: hidden;
}

/* dropdown transition */
.drop-enter-active, .drop-leave-active {
  transition: opacity 0.15s, transform 0.15s;
}

.drop-enter-from, .drop-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px 8px;
}

.dropdown-title {
  font-size: 13px;
  font-family: 'IBM Plex Mono', monospace;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--muted-2);
}

.clear-btn {
  font-size: 13px;
  color: var(--accent);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  font-family: 'Noto Sans TC', sans-serif;
}

.clear-btn:hover {
  text-decoration: underline;
}

.dropdown-divider {
  border: none;
  border-top: 1px solid var(--border);
  margin: 2px 0;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 9px 14px;
  cursor: pointer;
  font-size: 14px;
  color: var(--ink-2);
  transition: background 0.1s;
}

.dropdown-item:hover {
  background: var(--surface-2);
}

.dropdown-item input[type='checkbox'] {
  display: none;
}

.check-box {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  flex-shrink: 0;
  border: 1.5px solid var(--border-2);
  background: var(--surface);
  transition: all 0.15s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dropdown-item input[type='checkbox']:checked + .check-box {
  background: var(--accent);
  border-color: var(--accent);
}

.dropdown-item input[type='checkbox']:checked + .check-box::after {
  content: '\f00c';
  font-family: 'Font Awesome 6 Free';
  font-size: 10px;
  color: #fff;
  font-weight: 900;
  line-height: 1;
}

.all-item input[type='checkbox']:checked + .check-box {
  background: var(--ink-2);
  border-color: var(--ink-2);
}

.item-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.idot-PERSONAL {
  background: #7A5C3A;
}

.idot-CAR {
  background: #3F5F5A;
}

.idot-MOTOR {
  background: #7A4A38;
}

.idot-STUDENT {
  background: #4A6B5C;
}

.idot-BUSINESS {
  background: #5C5074;
}

.idot-HOUSE {
  background: #3D5C58;
}

.idot-LAND {
  background: #6E6259;
}

.item-name {
  flex: 1;
}

.item-count {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 13px;
  color: var(--muted);
  background: var(--surface-2);
  padding: 1px 7px;
  border-radius: 10px;
  border: 1px solid var(--border);
}

/* ── Filter meta ── */
.filter-meta {
  font-size: 13px;
  color: var(--muted-2);
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-meta strong {
  color: var(--ink);
}

.sort-hint {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 13px;
  color: var(--accent);
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  display: inline-block;
  flex-shrink: 0;
}

.dot-blue {
  background: var(--blue);
}

.dot-green {
  background: var(--green);
}

.dot-teal {
  background: var(--teal);
}

.dot-amber {
  background: var(--amber);
}

.dot-blue {
  background: var(--blue);
}

.dot-purple {
  background: var(--purple);
}

.dot-green {
  background: var(--green);
}

.dot-red {
  background: var(--red);
}

.dot-orange {
  background: var(--orange);
}

.dot-teal {
  background: var(--teal);
}

.dot-gray {
  background: var(--muted);
}

/* ── Error ── */
.alert-error {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(192, 57, 43, 0.08);
  border: 1px solid rgba(192, 57, 43, 0.25);
  color: var(--red);
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 14px;
  margin-bottom: 16px;
}

/* ── Table Card ── */
.table-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 26px rgba(63, 74, 66, 0.06);
}

/* ── Toolbar ── */
.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border);
  background: rgba(234, 228, 218, 0.62);
}

.toolbar-info {
  font-size: 14px;
  color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
}

.page-size-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.size-label {
  font-size: 14px;
  color: var(--muted-2);
  white-space: nowrap;
}

.styled-select-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.styled-select {
  appearance: none;
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: 6px;
  color: var(--ink);
  font-family: 'Noto Sans TC', sans-serif;
  font-size: 14px;
  padding: 5px 28px 5px 10px;
  cursor: pointer;
  outline: none;
  transition: border-color 0.15s;
  min-width: 78px;
}

.styled-select:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-dim);
}

.select-caret {
  position: absolute;
  right: 9px;
  font-size: 10px;
  color: var(--muted-2);
  pointer-events: none;
}

/* ── Skeleton ── */
.skeleton-wrap {
  padding: 8px 0;
}

.skeleton-row {
  display: flex;
  gap: 16px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border);
  align-items: center;
}

.sk {
  background: linear-gradient(90deg, #e8eae8 25%, #d5dad6 50%, #e8eae8 75%);
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
  border-radius: 4px;
  height: 14px;
  flex-shrink: 0;
}

.sk-id {
  width: 140px;
}

.sk-mid {
  width: 110px;
}

.sk-short {
  width: 70px;
}

@keyframes shimmer {
  to {
    background-position: -200% 0;
  }
}

/* ── Empty ── */
.empty-state {
  text-align: center;
  padding: 56px 20px;
  color: var(--muted);
}

.empty-icon {
  font-size: 40px;
  margin-bottom: 12px;
  opacity: 0.4;
}

.empty-state p {
  font-size: 14px;
}

/* ── Table ── */
.table-wrap {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  white-space: nowrap;
}

.data-table th {
  text-align: left;
  padding: 11px 16px;
  font-size: 14px;
  font-family: 'IBM Plex Mono', monospace;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--muted-2);
  border-bottom: 1px solid var(--border);
  font-weight: 600;
  background: rgba(234, 228, 218, 0.62);
  user-select: none;
}

/* ① 排序欄位 */
.data-table th.sortable {
  cursor: pointer;
}

.data-table th.sortable:hover {
  color: var(--accent);
  background: var(--surface-2);
}

.sort-icon {
  font-size: 13px;
  opacity: 0.45;
  margin-left: 4px;
}

.data-table th.sortable:hover .sort-icon {
  opacity: 1;
}

.th-action {
  text-align: center;
}

.data-row {
  animation: rowIn 0.2s both;
}

@keyframes rowIn {
  from {
    opacity: 0;
    transform: translateY(4px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.data-row td {
  padding: 13px 16px;
  border-bottom: 1px solid var(--border);
  vertical-align: middle;
  font-size: 15px;
  color: var(--ink-2);
}

.data-row:last-child td {
  border-bottom: none;
}

.data-row:hover td {
  background: rgba(92, 107, 95, 0.03);
}

.td-action {
  text-align: center;
}

/* Cells */
.id-chip {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 13px;
  color: var(--accent);
  background: var(--accent-dim);
  border: 1px solid var(--accent-lt);
  padding: 3px 9px;
  border-radius: 5px;
  display: inline-block;
}

.applicant-member {
  display: flex;
  align-items: center;
  gap: 0;
}

.member-id {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 14px;
  color: var(--muted-2);
}

.member-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--ink);
  margin-left: 8px;
  padding-left: 8px;
  border-left: 1px solid var(--border);
}

/* ── 姓名搜尋欄 ── */
.name-search-wrap {
  display: flex;
  align-items: center;
  position: relative;
  margin-left: auto;
}

.name-search-icon {
  position: absolute;
  left: 9px;
  font-size: 12px;
  pointer-events: none;
  opacity: 0.55;
}

.name-search-input {
  appearance: none;
  background: var(--surface);
  border: 1px solid var(--border-2);
  border-radius: 8px;
  color: var(--ink);
  font-family: 'Noto Sans TC', sans-serif;
  font-size: 14px;
  padding: 6px 28px 6px 28px;
  outline: none;
  width: 180px;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.name-search-input:focus {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-dim);
}

.name-search-input::placeholder {
  color: var(--muted);
}

.name-search-clear {
  position: absolute;
  right: 8px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 11px;
  color: var(--muted);
  padding: 0;
  line-height: 1;
}

.name-search-clear:hover {
  color: var(--accent);
}

.applicant-nonmember {
  line-height: 1.4;
}

.nm-name {
  font-size: 15px;
  font-weight: 500;
  color: var(--ink);
}

.nm-meta {
  display: flex;
  gap: 10px;
  margin-top: 2px;
}

.nm-meta span {
  font-size: 13px;
  color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
}

.nm-email {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.type-badge {
  font-size: 13px;
  font-family: 'IBM Plex Mono', monospace;
  padding: 3px 9px;
  border-radius: 5px;
  border: 1px solid var(--border);
  background: var(--surface-2);
  color: var(--muted-2);
  display: inline-block;
}

.type-PERSONAL {
  color: #7A5C3A;
  border-color: #D6CEC3;
  background: rgba(245, 241, 234, 0.82);
}

.type-CAR {
  color: #3F5F5A;
  border-color: #B5CECA;
  background: rgba(238, 243, 242, 0.86);
}

.type-MOTOR {
  color: #7A4A38;
  border-color: #D4B8AE;
  background: rgba(245, 237, 233, 0.86);
}

.type-STUDENT {
  color: #4A6B5C;
  border-color: #B5CCBF;
  background: rgba(238, 243, 239, 0.86);
}

.type-BUSINESS {
  color: #5C5074;
  border-color: #C4BCDA;
  background: rgba(240, 238, 245, 0.86);
}

.type-HOUSE {
  color: #3D5C58;
  border-color: #AECBC7;
  background: rgba(235, 242, 241, 0.86);
}

.type-LAND {
  color: #6E6259;
  border-color: #D6CEC3;
  background: rgba(234, 228, 218, 0.86);
}

.amount {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
}

.confirmed-hint {
  font-size: 12px;
  color: var(--green);
  font-family: 'IBM Plex Mono', monospace;
  margin-top: 2px;
  opacity: 0.8;
}

.meta-tag {
  font-size: 14px;
  color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
}

.meta-rate {
  font-size: 14px;
  color: var(--green);
  font-family: 'IBM Plex Mono', monospace;
  font-weight: 500;
}

.status-badge {
  font-size: 13px;
  font-family: 'IBM Plex Mono', monospace;
  padding: 4px 10px;
  border-radius: 20px;
  border: 1px solid;
  display: inline-block;
  white-space: nowrap;
  font-weight: 500;
}

.status-yellow {
  color: #7a6000;
  border-color: rgba(196, 154, 60, 0.24);
  background: rgba(196, 154, 60, 0.10);
}

.status-blue {
  color: #4A574D;
  border-color: rgba(92, 107, 95, 0.20);
  background: rgba(92, 107, 95, 0.08);
}

.status-purple {
  color: #5A4F82;
  border-color: rgba(114, 101, 160, 0.20);
  background: rgba(114, 101, 160, 0.08);
}

.status-green {
  color: var(--primary-dk);
  border-color: rgba(74, 140, 92, 0.20);
  background: rgba(74, 140, 92, 0.08);
}

.status-red {
  color: var(--red);
  border-color: rgba(166, 90, 77, 0.28);
  background: rgba(166, 90, 77, 0.10);
}

.status-returned {
  color: #c0392b;
  border-color: #f5a8a1;
  background: #fff5f5;
  font-weight: 600;
}

.status-teal {
  color: #4A574D;
  border-color: rgba(92, 107, 95, 0.20);
  background: rgba(92, 107, 95, 0.08);
}

.status-gray {
  color: var(--muted-2);
  border-color: var(--border-2);
  background: rgba(234, 228, 218, 0.62);
}

.contact-cell {
  line-height: 1.3;
}

.contact-status {
  font-size: 13px;
  font-family: 'IBM Plex Mono', monospace;
  display: inline-block;
  font-weight: 500;
}

.c-gray {
  color: var(--muted);
}

.c-blue {
  color: #4A574D;
}

.c-green {
  color: var(--primary);
}

.c-gold {
  color: #7a6000;
}

.c-red {
  color: var(--red);
}

.contact-time {
  font-size: 13px;
  color: var(--muted);
  font-family: 'IBM Plex Mono', monospace;
  margin-top: 2px;
}

.date-cell {
  line-height: 1.3;
}

.date-main {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 14px;
  color: var(--ink-2);
  display: block;
}

.date-time {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 13px;
  color: var(--muted);
}

.action-btns {
  display: flex;
  gap: 6px;
  justify-content: center;
}

/* Buttons */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  border: none;
  border-radius: 6px;
  font-family: 'Noto Sans TC', sans-serif;
  cursor: pointer;
  transition: all 0.15s;
  font-weight: 500;
}

.btn-sm {
  padding: 7px 14px;
  font-size: 14px;
}

.btn-xs {
  padding: 5px 9px;
  font-size: 13px;
  border-radius: 6px;
}

.btn-ghost {
  background: var(--surface);
  color: var(--muted-2);
  border: 1px solid var(--border);
}

.btn-ghost:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: rgba(92, 107, 95, 0.05);
}

.btn-ghost:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.btn-outline {
  background: transparent;
  color: var(--muted-2);
  border: 1px solid var(--border);
}

.btn-outline:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: rgba(92, 107, 95, 0.05);
}
/* 權限不足時的禁用按鈕樣式 */
.btn-disabled {
  background: transparent;
  color: #ccc;
  border: 1px solid #e8e8e8;
  cursor: not-allowed;
  opacity: 0.5;
}

/* ── ③ Pagination Footer ── */
.table-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-top: 1px solid var(--border);
  background: var(--surface-2);
  flex-wrap: wrap;
  gap: 10px;
}

.footer-count {
  font-size: 14px;
  color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
}

.footer-count strong {
  color: var(--ink);
}

.pagination {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-numbers {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 5px 9px;
  border-radius: 6px;
  font-size: 14px;
  font-family: 'IBM Plex Mono', monospace;
  cursor: pointer;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--muted-2);
  transition: all 0.15s;
  min-width: 32px;
  white-space: nowrap;
}

.nav-btn {
  padding: 5px 12px;
}

.page-btn:hover:not(:disabled) {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-dim);
}

.page-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.page-btn.active {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
  font-weight: 600;
}

.page-btn.ellipsis {
  border-color: transparent;
  background: transparent;
  cursor: default;
  color: var(--muted);
}

/* Misc */
.text-muted {
  color: var(--muted);
}

.spin {
  animation: spin 0.8s linear infinite;
  display: inline-block;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
