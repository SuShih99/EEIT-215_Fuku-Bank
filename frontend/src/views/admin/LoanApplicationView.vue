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
          <span class="trigger-icon">🏷</span>
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
            <span v-if="selectedTypes.length > 3" class="dtag-more">+{{ selectedTypes.length - 3 }}</span>
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
              <input type="checkbox" :checked="selectedTypes.length === 0" @change="clearTypes" />
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
              <input type="checkbox" :value="key" v-model="selectedTypes" @change="currentPage = 1" />
              <span class="check-box"></span>
              <span class="item-dot" :class="'idot-' + key"></span>
              <span class="item-name">{{ LOAN_TYPE_NAME[key] }}</span>
              <span class="item-count">{{ countByType(key) }}</span>
            </label>
          </div>
        </transition>
      </div>

      <!-- 結果摘要 -->
      <div class="filter-meta" v-if="!loading">
        <span class="status-dot" :class="STATUS_OPTIONS.find(s => s.value === currentStatus)?.dot"></span>
        共 <strong>{{ filteredApplications.length }}</strong> 筆
        <span v-if="sortKey" class="sort-hint">
          · 依 {{ SORT_LABEL[sortKey] }} {{ sortDir === 'asc' ? '↑' : '↓' }}
        </span>
      </div>
    </div>

    <!-- ── 錯誤提示 ── -->
    <div v-if="error" class="alert-error"><span>⚠️</span> {{ error }}</div>

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
        <div class="empty-icon">📭</div>
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
            <th @click="toggleSort('latestContactStatus')" class="sortable">
              最新聯繫<span class="sort-icon">{{ sortIcon('latestContactStatus') }}</span>
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
                <span class="member-badge">會員</span>
                <span class="member-id">{{ app.cif || app.customerId }}</span>
              </div>
              <div v-else class="applicant-nonmember">
                <div class="nm-name">{{ app.applicantName || '—' }}</div>
                <div class="nm-meta">
                  <span v-if="app.applicantPhone">📞 {{ app.applicantPhone }}</span>
                  <span v-if="app.applicantEmail" class="nm-email">✉ {{ app.applicantEmail }}</span>
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
            <td><div class="amount">{{ formatAmount(app.applyAmount) }}</div></td>

            <!-- 期數 -->
            <td><span class="meta-tag">{{ app.applyPeriod }} 個月</span></td>

            <!-- 利率 -->
            <td><span class="meta-rate">{{ formatRate(app.rate) }}</span></td>

            <!-- 申請狀態 -->
            <td>
                <span class="status-badge" :class="STATUS_CLASS[app.applicationStatus]">
                  {{ STATUS_LABEL[app.applicationStatus] || app.applicationStatus }}
                </span>
            </td>

            <!-- 最新聯繫 -->
            <td>
              <div v-if="app.latestContactStatus" class="contact-cell">
                  <span class="contact-status" :class="CONTACT_CLASS[app.latestContactStatus]">
                    {{ CONTACT_LABEL[app.latestContactStatus] || app.latestContactStatus }}
                  </span>
                <div class="contact-time">{{ formatDate(app.latestContactTime) }}</div>
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
                <button class="btn btn-xs btn-outline" @click="openContactModal(app)" title="聯繫紀錄">📞</button>
                <button class="btn btn-xs btn-outline" @click="openReviewModal(app)"  title="審核填單">🗂</button>
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
          <button class="page-btn" @click="goToPage(1)"               :disabled="currentPage === 1"          title="第一頁">«</button>
          <button class="page-btn nav-btn" @click="goToPage(currentPage - 1)" :disabled="currentPage === 1"          title="上一頁">‹ 上一頁</button>

          <div class="page-numbers">
            <button
              v-for="p in pageNumbers"
              :key="p"
              class="page-btn"
              :class="{ active: p === currentPage, ellipsis: p === '…' }"
              @click="p !== '…' && goToPage(p)"
              :disabled="p === '…'"
            >{{ p }}</button>
          </div>

          <button class="page-btn nav-btn" @click="goToPage(currentPage + 1)" :disabled="currentPage === totalPages" title="下一頁">下一頁 ›</button>
          <button class="page-btn" @click="goToPage(totalPages)"        :disabled="currentPage === totalPages" title="最後一頁">»</button>
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
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import api from '@/api/axios'
import LoanContactLogModal from './LoanContactLogModal.vue'
import LoanReviewModal from './LoanReviewModal.vue'

// ── Emits ──
defineEmits([])

// ── Constants ──
const API_URL = '/api/admin/loan-applications'

// ② 每頁筆數選項
const PAGE_SIZE_OPTIONS = [10, 20, 50, 100]

// ── Contact Modal state ──
const contactModalOpen = ref(false)
const contactModalApp  = ref(null)

function openContactModal(app) {
  contactModalApp.value = app
  contactModalOpen.value = true
}
function onLogAdded() {
  fetchApplications()
}

// ── Review Modal state ──
const reviewModalOpen = ref(false)
const reviewModalApp  = ref(null)

function openReviewModal(app) {
  reviewModalApp.value = app
  reviewModalOpen.value = true
}
function onReviewUpdated() {
  fetchApplications()
}

const STATUS_OPTIONS = [
  { value: 'PENDING_CONTACT', label: '新申請', dot: 'dot-yellow' },
  { value: 'IN_CONTACT',      label: '聯繫中', dot: 'dot-blue'   },
  { value: 'PENDING_REVIEW',  label: '審核中', dot: 'dot-purple' },
  { value: 'APPROVED',        label: '已核准', dot: 'dot-green'  },
  { value: 'REJECTED',        label: '已拒絕', dot: 'dot-red'    },
  { value: 'DISBURSED',       label: '已撥款', dot: 'dot-teal'   },
  { value: 'CLOSED',          label: '已結案', dot: 'dot-gray'   },
]

const STATUS_LABEL = {
  PENDING_CONTACT: '新申請', IN_CONTACT: '聯繫中', PENDING_REVIEW: '審核中',
  APPROVED: '已核准', REJECTED: '已拒絕', CANCELLED: '已取消',
  DISBURSED: '已撥款', CLOSED: '已結案',
}
const STATUS_CLASS = {
  PENDING_CONTACT: 'status-yellow', IN_CONTACT: 'status-blue', PENDING_REVIEW: 'status-purple',
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

// ① 排序欄位中文對照
const SORT_LABEL = {
  applicationId: '申請編號', applyType: '類型', applyAmount: '金額',
  applyPeriod: '期數', rate: '利率', applicationStatus: '申請狀態',
  latestContactStatus: '最新聯繫', createTime: '申請時間',
}

// ── State ──
const currentStatus    = ref('PENDING_CONTACT')
const applications     = ref([])
const loading          = ref(false)
const error            = ref('')

// ① 排序
const sortKey  = ref('')
const sortDir  = ref('asc')

// ② 分頁
const currentPage = ref(1)
const pageSize    = ref(10)

// ④ 類型多選
const selectedTypes    = ref([])
const typeDropdownOpen = ref(false)

// ── Computed ──

/** ④ 類型篩選 → ① 排序 */
const filteredApplications = computed(() => {
  let list = applications.value

  if (selectedTypes.value.length > 0)
    list = list.filter(a => selectedTypes.value.includes(a.applyType))

  if (sortKey.value) {
    const key = sortKey.value
    const dir = sortDir.value === 'asc' ? 1 : -1
    list = [...list].sort((a, b) => {
      const av = a[key] ?? ''
      const bv = b[key] ?? ''
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

/** 省略號分頁按鈕序列 */
const pageNumbers = computed(() => {
  const total = totalPages.value
  const cur   = currentPage.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  if (cur <= 4)          return [1, 2, 3, 4, 5, '…', total]
  if (cur >= total - 3)  return [1, '…', total - 4, total - 3, total - 2, total - 1, total]
  return [1, '…', cur - 1, cur, cur + 1, '…', total]
})

// ── Methods ──
async function fetchApplications() {
  loading.value = true
  error.value   = ''
  try {
    const res = await api.get(API_URL, {
      params: { status: currentStatus.value },
      headers: { 'Cache-Control': 'no-cache', 'Pragma': 'no-cache' },
    })
    applications.value = res.data.success ? res.data.data : []
  } catch (e) {
    error.value = e.response?.data?.message || '連線失敗，請確認後端服務是否啟動（GET /api/admin/loan-applications）'
    applications.value = []
  } finally {
    loading.value = false
  }
}

function setStatus(status) {
  currentStatus.value = status
  currentPage.value   = 1
  fetchApplications()
}

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
}

// ① 排序切換
function toggleSort(key) {
  if (sortKey.value === key) sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  else { sortKey.value = key; sortDir.value = 'asc' }
  currentPage.value = 1
}
function sortIcon(key) {
  if (sortKey.value !== key) return '⇅'
  return sortDir.value === 'asc' ? '↑' : '↓'
}

// ④ 類型篩選
function clearTypes() { selectedTypes.value = []; currentPage.value = 1 }
function countByType(key) { return applications.value.filter(a => a.applyType === key).length }

// ── Formatters ──
function formatAmount(n)    { return n ? '$ ' + Number(n).toLocaleString('zh-TW') : '—' }
function formatRate(r)      { return r == null ? '—' : (r * 100).toFixed(2) + '%' }
function formatDate(d)      { return d ? d.replace('T', ' ').substring(0, 16) : '—' }
function formatDateShort(d) { return d ? d.substring(0, 10) : '—' }
function formatTime(d)      { return d ? d.replace('T', ' ').substring(11, 16) : '' }

// ── Auto-refresh（固定 30 秒）──
let refreshTimer = null

function startAutoRefresh() {
  refreshTimer = setInterval(fetchApplications, 30_000)
}

onMounted(() => { fetchApplications(); startAutoRefresh() })
onUnmounted(() => clearInterval(refreshTimer))
</script>

<style scoped>
.loan-admin {
  /* ── 配色依據 ── */
  --accent:     #A65A4D;
  --accent-dim: rgba(166, 90, 77, 0.10);
  --accent-lt:  rgba(166, 90, 77, 0.20);
  --bg:         #F5F1EA;
  --surface:    #FDFAF6;
  --surface-2:  #EAE4DA;
  --border:     #D6CEC3;
  --border-2:   #C4BBB0;
  --ink:        #2B2B2B;
  --ink-2:      #3D3530;
  --muted:      #A89A8E;
  --muted-2:    #6E6259;
  --primary:    #5C6B5F;
  --primary-dk: #3F4A42;
  --red:        #A65A4D;
  --green:      #5C6B5F;
  --yellow:     #8C7355;
  --teal:       #4A6B5C;
  --purple:     #6B5C7A;

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
  display: flex; align-items: flex-end; justify-content: space-between;
  margin-bottom: 24px;
}
.page-title {
  font-family: 'Noto Serif TC', serif;
  font-size: 26px; font-weight: 700; color: var(--ink); line-height: 1.2;
}
.header-actions { display: flex; align-items: center; gap: 10px; }

/* Auto-refresh */
.auto-refresh-wrap { display: flex; align-items: center; gap: 8px; }
.ar-label { font-size: 12px; color: var(--muted-2); white-space: nowrap; }
.ar-select { min-width: 80px; }
.ar-countdown {
  display: inline-flex; align-items: center; gap: 5px;
  font-family: 'IBM Plex Mono', monospace;
  font-size: 12px; color: var(--accent);
  background: var(--accent-dim); border: 1px solid var(--accent-lt);
  padding: 3px 9px; border-radius: 20px;
}
.ar-dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: var(--accent); flex-shrink: 0;
}
.ar-dot.pulsing {
  animation: pulse 1.4s ease-in-out infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50%       { opacity: 0.4; transform: scale(0.8); }
}

/* ── Filter Bar ── */
.filter-bar {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 16px; flex-wrap: wrap;
}
.pills-group { display: flex; gap: 6px; flex-wrap: wrap; }
.filter-pill {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 6px 13px; border-radius: 20px; font-size: 12px;
  font-family: 'IBM Plex Mono', monospace;
  cursor: pointer; border: 1px solid var(--border);
  background: var(--surface); color: var(--muted-2); transition: all 0.15s;
}
.filter-pill:hover  { border-color: var(--accent); color: var(--accent); }
.filter-pill.active { background: var(--accent-dim); border-color: var(--accent); color: var(--accent); }
.pill-dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; opacity: 0.8; }
.pill-count {
  background: rgba(37,99,235,0.12); border-radius: 10px;
  padding: 1px 6px; font-size: 10px;
}

/* ── Type Multi-Select Dropdown ── */
.type-dropdown-wrap { position: relative; }
.type-dropdown-trigger {
  display: inline-flex; align-items: center; gap: 7px;
  padding: 6px 12px; border-radius: 8px; font-size: 13px;
  font-family: 'Noto Sans TC', sans-serif;
  cursor: pointer; border: 1px solid var(--border-2);
  background: var(--surface); color: var(--ink-2);
  transition: all 0.15s; outline: none; white-space: nowrap;
}
.type-dropdown-trigger:hover   { border-color: var(--accent); }
.type-dropdown-trigger.open    { border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-dim); }
.type-dropdown-trigger.active  { border-color: var(--accent); background: var(--accent-dim); color: var(--accent); }
.trigger-icon { font-size: 13px; }
.dropdown-caret { font-size: 10px; color: var(--muted-2); transition: transform 0.2s; margin-left: 2px; }
.dropdown-caret.rotated { transform: rotate(180deg); }
.dtag-row { display: flex; gap: 4px; }
.dtag {
  font-size: 10px; padding: 1px 5px; border-radius: 4px;
  border: 1px solid; font-family: 'IBM Plex Mono', monospace;
}
.dtag-PERSONAL { color: #7A5C3A; border-color: #D6CEC3; background: #F5F1EA; }
.dtag-CAR      { color: #3F5F5A; border-color: #B5CECA; background: #EEF3F2; }
.dtag-MOTOR    { color: #7A4A38; border-color: #D4B8AE; background: #F5EDE9; }
.dtag-STUDENT  { color: #4A6B5C; border-color: #B5CCBF; background: #EEF3EF; }
.dtag-BUSINESS { color: #5C5074; border-color: #C4BCDA; background: #F0EEF5; }
.dtag-HOUSE    { color: #3D5C58; border-color: #AECBC7; background: #EBF2F1; }
.dtag-LAND     { color: #6E6259; border-color: #D6CEC3; background: #EAE4DA; }
.dtag-more {
  font-size: 10px; padding: 1px 6px; border-radius: 4px;
  background: var(--surface-2); border: 1px solid var(--border-2); color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
}

.type-dropdown-menu {
  position: absolute; top: calc(100% + 6px); left: 0;
  background: var(--surface); border: 1px solid var(--border-2);
  border-radius: 10px; box-shadow: 0 8px 24px rgba(0,0,0,0.12);
  min-width: 230px; z-index: 300; overflow: hidden;
}
/* dropdown transition */
.drop-enter-active, .drop-leave-active { transition: opacity 0.15s, transform 0.15s; }
.drop-enter-from, .drop-leave-to { opacity: 0; transform: translateY(-6px); }

.dropdown-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 14px 8px;
}
.dropdown-title {
  font-size: 11px; font-family: 'IBM Plex Mono', monospace;
  letter-spacing: 0.1em; text-transform: uppercase; color: var(--muted-2);
}
.clear-btn {
  font-size: 11px; color: var(--accent); background: none;
  border: none; cursor: pointer; padding: 0;
  font-family: 'Noto Sans TC', sans-serif;
}
.clear-btn:hover { text-decoration: underline; }
.dropdown-divider { border: none; border-top: 1px solid var(--border); margin: 2px 0; }
.dropdown-item {
  display: flex; align-items: center; gap: 9px;
  padding: 9px 14px; cursor: pointer;
  font-size: 13px; color: var(--ink-2); transition: background 0.1s;
}
.dropdown-item:hover { background: var(--surface-2); }
.dropdown-item input[type='checkbox'] { display: none; }
.check-box {
  width: 16px; height: 16px; border-radius: 4px; flex-shrink: 0;
  border: 1.5px solid var(--border-2); background: var(--surface);
  transition: all 0.15s; display: flex; align-items: center; justify-content: center;
}
.dropdown-item input[type='checkbox']:checked + .check-box {
  background: var(--accent); border-color: var(--accent);
}
.dropdown-item input[type='checkbox']:checked + .check-box::after {
  content: '✓'; font-size: 10px; color: #fff; font-weight: 700; line-height: 1;
}
.all-item input[type='checkbox']:checked + .check-box { background: var(--ink-2); border-color: var(--ink-2); }
.item-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.idot-PERSONAL { background: #7A5C3A; } .idot-CAR      { background: #3F5F5A; }
.idot-MOTOR    { background: #7A4A38; } .idot-STUDENT  { background: #4A6B5C; }
.idot-BUSINESS { background: #5C5074; } .idot-HOUSE    { background: #3D5C58; }
.idot-LAND     { background: #6E6259; }
.item-name { flex: 1; }
.item-count {
  font-family: 'IBM Plex Mono', monospace; font-size: 11px; color: var(--muted);
  background: var(--surface-2); padding: 1px 7px;
  border-radius: 10px; border: 1px solid var(--border);
}

/* ── Filter meta ── */
.filter-meta { font-size: 13px; color: var(--muted-2); display: flex; align-items: center; gap: 6px; }
.filter-meta strong { color: var(--ink); }
.sort-hint { font-family: 'IBM Plex Mono', monospace; font-size: 11px; color: var(--accent); }
.status-dot { width: 7px; height: 7px; border-radius: 50%; display: inline-block; flex-shrink: 0; }
.dot-yellow { background: var(--yellow); } .dot-blue   { background: var(--blue);   }
.dot-purple { background: var(--purple); } .dot-green  { background: var(--green);  }
.dot-red    { background: var(--red);    } .dot-teal   { background: var(--teal);   }
.dot-gray   { background: var(--muted);  }

/* ── Error ── */
.alert-error {
  display: flex; align-items: center; gap: 8px;
  background: rgba(166,90,77,0.08); border: 1px solid rgba(166,90,77,0.25); color: var(--red);
  padding: 10px 16px; border-radius: 8px; font-size: 13px; margin-bottom: 16px;
}

/* ── Table Card ── */
.table-card {
  background: var(--surface); border: 1px solid var(--border);
  border-radius: 12px; overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04);
}

/* ── Toolbar ── */
.table-toolbar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 16px; border-bottom: 1px solid var(--border);
  background: var(--surface-2);
}
.toolbar-info { font-size: 12px; color: var(--muted-2); font-family: 'IBM Plex Mono', monospace; }
.page-size-wrap { display: flex; align-items: center; gap: 8px; }
.size-label { font-size: 12px; color: var(--muted-2); white-space: nowrap; }
.styled-select-wrap { position: relative; display: inline-flex; align-items: center; }
.styled-select {
  appearance: none;
  background: var(--surface); border: 1px solid var(--border-2);
  border-radius: 6px; color: var(--ink);
  font-family: 'Noto Sans TC', sans-serif; font-size: 12px;
  padding: 5px 28px 5px 10px; cursor: pointer; outline: none;
  transition: border-color 0.15s; min-width: 78px;
}
.styled-select:focus { border-color: var(--accent); box-shadow: 0 0 0 3px var(--accent-dim); }
.select-caret { position: absolute; right: 9px; font-size: 10px; color: var(--muted-2); pointer-events: none; }

/* ── Skeleton ── */
.skeleton-wrap { padding: 8px 0; }
.skeleton-row {
  display: flex; gap: 16px; padding: 14px 20px;
  border-bottom: 1px solid var(--border); align-items: center;
}
.sk {
  background: linear-gradient(90deg, #EAE4DA 25%, #D6CEC3 50%, #EAE4DA 75%);
  background-size: 200% 100%; animation: shimmer 1.4s infinite;
  border-radius: 4px; height: 14px; flex-shrink: 0;
}
.sk-id { width: 140px; } .sk-mid { width: 110px; } .sk-short { width: 70px; }
@keyframes shimmer { to { background-position: -200% 0; } }

/* ── Empty ── */
.empty-state { text-align: center; padding: 56px 20px; color: var(--muted); }
.empty-icon  { font-size: 40px; margin-bottom: 12px; opacity: 0.4; }
.empty-state p { font-size: 14px; }

/* ── Table ── */
.table-wrap { overflow-x: auto; }
.data-table { width: 100%; border-collapse: collapse; white-space: nowrap; }
.data-table th {
  text-align: left; padding: 11px 16px;
  font-size: 11px; font-family: 'IBM Plex Mono', monospace;
  letter-spacing: 0.08em; text-transform: uppercase;
  color: var(--muted-2); border-bottom: 1px solid var(--border);
  font-weight: 600; background: var(--surface-2);
  user-select: none;
}
/* ① 排序欄位 */
.data-table th.sortable { cursor: pointer; }
.data-table th.sortable:hover { color: var(--accent); background: var(--surface-2); }
.sort-icon { font-size: 11px; opacity: 0.45; margin-left: 4px; }
.data-table th.sortable:hover .sort-icon { opacity: 1; }
.th-action { text-align: center; }

.data-row { animation: rowIn 0.2s both; }
@keyframes rowIn {
  from { opacity: 0; transform: translateY(4px); }
  to   { opacity: 1; transform: translateY(0); }
}
.data-row td {
  padding: 13px 16px; border-bottom: 1px solid var(--border);
  vertical-align: middle; font-size: 13px; color: var(--ink-2);
}
.data-row:last-child td { border-bottom: none; }
.data-row:hover td { background: rgba(166,90,77,0.04); }
.td-action { text-align: center; }

/* Cells */
.id-chip {
  font-family: 'IBM Plex Mono', monospace; font-size: 11px;
  color: var(--accent); background: var(--accent-dim);
  border: 1px solid var(--accent-lt); padding: 3px 9px;
  border-radius: 5px; display: inline-block;
}

.applicant-member { display: flex; align-items: center; gap: 8px; }
.member-badge {
  font-size: 10px; font-family: 'IBM Plex Mono', monospace;
  color: var(--blue); background: rgba(37,99,235,0.08);
  border: 1px solid rgba(37,99,235,0.2); padding: 2px 7px; border-radius: 4px;
}
.member-id { font-family: 'IBM Plex Mono', monospace; font-size: 13px; color: var(--muted-2); }
.applicant-nonmember { line-height: 1.4; }
.nm-name { font-size: 13px; font-weight: 500; color: var(--ink); }
.nm-meta { display: flex; gap: 10px; margin-top: 2px; }
.nm-meta span { font-size: 11px; color: var(--muted-2); font-family: 'IBM Plex Mono', monospace; }
.nm-email { max-width: 160px; overflow: hidden; text-overflow: ellipsis; }

.type-badge {
  font-size: 11px; font-family: 'IBM Plex Mono', monospace;
  padding: 3px 9px; border-radius: 5px;
  border: 1px solid var(--border); background: var(--surface-2);
  color: var(--muted-2); display: inline-block;
}
.type-PERSONAL { color: #7A5C3A; border-color: #D6CEC3; background: #F5F1EA; }
.type-CAR      { color: #3F5F5A; border-color: #B5CECA; background: #EEF3F2; }
.type-MOTOR    { color: #7A4A38; border-color: #D4B8AE; background: #F5EDE9; }
.type-STUDENT  { color: #4A6B5C; border-color: #B5CCBF; background: #EEF3EF; }
.type-BUSINESS { color: #5C5074; border-color: #C4BCDA; background: #F0EEF5; }
.type-HOUSE    { color: #3D5C58; border-color: #AECBC7; background: #EBF2F1; }
.type-LAND     { color: #6E6259; border-color: #D6CEC3; background: #EAE4DA; }

.amount { font-family: 'IBM Plex Mono', monospace; font-size: 14px; font-weight: 600; color: var(--ink); }
.meta-tag  { font-size: 12px; color: var(--muted-2); font-family: 'IBM Plex Mono', monospace; }
.meta-rate { font-size: 12px; color: var(--green);   font-family: 'IBM Plex Mono', monospace; font-weight: 500; }

.status-badge {
  font-size: 11px; font-family: 'IBM Plex Mono', monospace;
  padding: 4px 10px; border-radius: 20px; border: 1px solid;
  display: inline-block; white-space: nowrap; font-weight: 500;
}
.status-yellow { color: #7A5C30; border-color: #C4B090; background: #F5EFE6; }
.status-blue   { color: #3F5F5A; border-color: #9BBAB6; background: #EEF3F2; }
.status-purple { color: #5C5074; border-color: #C4BCDA; background: #F0EEF5; }
.status-green  { color: var(--primary-dk); border-color: #A5B8A9; background: #ECF0EC; }
.status-red    { color: var(--accent); border-color: rgba(166,90,77,0.4); background: rgba(166,90,77,0.08); }
.status-teal   { color: #3D5C58; border-color: #AECBC7; background: #EBF2F1; }
.status-gray   { color: var(--muted-2); border-color: var(--border-2); background: var(--surface-2); }

.contact-cell { line-height: 1.3; }
.contact-status { font-size: 11px; font-family: 'IBM Plex Mono', monospace; display: inline-block; font-weight: 500; }
.c-gray { color: var(--muted); } .c-blue  { color: var(--blue);  }
.c-green { color: var(--primary); } .c-gold  { color: var(--yellow); }
.c-red  { color: var(--red);   }
.contact-time { font-size: 11px; color: var(--muted); font-family: 'IBM Plex Mono', monospace; margin-top: 2px; }

.date-cell { line-height: 1.3; }
.date-main { font-family: 'IBM Plex Mono', monospace; font-size: 12px; color: var(--ink-2); display: block; }
.date-time { font-family: 'IBM Plex Mono', monospace; font-size: 11px; color: var(--muted); }

.action-btns { display: flex; gap: 6px; justify-content: center; }

/* Buttons */
.btn {
  display: inline-flex; align-items: center; gap: 5px;
  border: none; border-radius: 6px;
  font-family: 'Noto Sans TC', sans-serif;
  cursor: pointer; transition: all 0.15s; font-weight: 500;
}
.btn-sm  { padding: 7px 14px; font-size: 12px; }
.btn-xs  { padding: 5px 9px;  font-size: 13px; border-radius: 6px; }
.btn-ghost {
  background: var(--surface); color: var(--muted-2); border: 1px solid var(--border);
}
.btn-ghost:hover    { border-color: var(--accent); color: var(--accent); background: var(--accent-dim); }
.btn-ghost:disabled { opacity: 0.4; cursor: not-allowed; }
.btn-outline { background: transparent; color: var(--muted-2); border: 1px solid var(--border); }
.btn-outline:hover { border-color: var(--accent); color: var(--accent); background: var(--accent-dim); }

/* ── ③ Pagination Footer ── */
.table-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-top: 1px solid var(--border);
  background: var(--surface-2); flex-wrap: wrap; gap: 10px;
}
.footer-count { font-size: 12px; color: var(--muted-2); font-family: 'IBM Plex Mono', monospace; }
.footer-count strong { color: var(--ink); }

.pagination { display: flex; align-items: center; gap: 4px; }
.page-numbers { display: flex; align-items: center; gap: 4px; }
.page-btn {
  display: inline-flex; align-items: center; justify-content: center;
  padding: 5px 9px; border-radius: 6px;
  font-size: 12px; font-family: 'IBM Plex Mono', monospace;
  cursor: pointer; border: 1px solid var(--border);
  background: var(--surface); color: var(--muted-2);
  transition: all 0.15s; min-width: 32px; white-space: nowrap;
}
.nav-btn { padding: 5px 12px; }
.page-btn:hover:not(:disabled) { border-color: var(--accent); color: var(--accent); background: var(--accent-dim); }
.page-btn:disabled  { opacity: 0.35; cursor: not-allowed; }
.page-btn.active    { background: var(--accent); border-color: var(--accent); color: #fff; font-weight: 600; }
.page-btn.ellipsis  { border-color: transparent; background: transparent; cursor: default; color: var(--muted); }

/* Misc */
.text-muted { color: var(--muted); }
.spin { animation: spin 0.8s linear infinite; display: inline-block; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
