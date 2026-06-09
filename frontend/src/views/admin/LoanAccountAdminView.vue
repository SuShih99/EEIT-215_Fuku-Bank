<template>
  <div class="loan-account-admin">

    <!-- ── 頁首 ── -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">貸款帳戶管理</h1>
      </div>
      <div class="header-actions">
        <button class="btn btn-ghost btn-sm" @click="fetchAccounts" :disabled="loading">
          <span v-if="loading" class="spin">⟳</span>
          <span v-else>↻</span>
          重新整理
        </button>
      </div>
    </div>

    <!-- ── 篩選列 ── -->
    <div class="filter-bar">
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
            {{ filteredAccounts.length }}
          </span>
        </button>
      </div>

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
          <span class="dropdown-caret" :class="{ rotated: typeDropdownOpen }">▾</span>
        </button>
        <transition name="drop">
          <div class="type-dropdown-menu" v-show="typeDropdownOpen" @click.stop>
            <div class="dropdown-header">
              <span class="dropdown-title">貸款類型篩選</span>
              <button class="clear-btn" v-if="selectedTypes.length > 0" @click.stop="selectedTypes = []; currentPage = 1">
                清除全部
              </button>
            </div>
            <label class="dropdown-item all-item" @click.stop>
              <input type="checkbox" :checked="selectedTypes.length === 0" @change="selectedTypes = []; currentPage = 1"/>
              <span class="check-box"></span>
              <span class="item-name">全部類型</span>
            </label>
            <div class="dropdown-divider"></div>
            <label v-for="key in LOAN_TYPE_KEYS" :key="key" class="dropdown-item" @click.stop>
              <input type="checkbox" :value="key" v-model="selectedTypes" @change="currentPage = 1"/>
              <span class="check-box"></span>
              <span class="item-dot" :class="'idot-' + key"></span>
              <span class="item-name">{{ LOAN_TYPE_NAME[key] }}</span>
            </label>
          </div>
        </transition>
      </div>

      <div class="name-search-wrap">
        <span class="name-search-icon">⌕</span>
        <input
          class="name-search-input"
          type="text"
          placeholder="搜尋申請人姓名…"
          v-model="nameKeyword"
          @input="currentPage = 1"
        />
        <button
          v-if="nameKeyword"
          class="name-search-clear"
          @click="nameKeyword = ''; currentPage = 1"
          title="清除"
        >×</button>
      </div>

      <!-- 案件編號搜尋 -->
      <div class="id-search-wrap">
        <span class="id-search-icon"><i class="fa-solid fa-magnifying-glass"></i></span>
        <input
          class="id-search-input"
          type="text"
          placeholder="案件編號…"
          v-model="applicationIdQuery"
          @input="currentPage = 1"
        />
        <button v-if="applicationIdQuery" class="id-search-clear" @click="applicationIdQuery = ''; currentPage = 1">
          <i class="fa-solid fa-x"></i>
        </button>
      </div>

      <!-- 貸款帳戶編號搜尋 -->
      <div class="id-search-wrap">
        <span class="id-search-icon"><i class="fa-solid fa-magnifying-glass"></i></span>
        <input
          class="id-search-input"
          type="text"
          placeholder="貸款帳戶編號…"
          v-model="accountNumberQuery"
          @input="currentPage = 1"
        />
        <button v-if="accountNumberQuery" class="id-search-clear" @click="accountNumberQuery = ''; currentPage = 1">
          <i class="fa-solid fa-x"></i>
        </button>
      </div>

      <!-- 結果摘要 -->
      <div class="filter-meta" v-if="!loading">
        <span class="status-dot" :class="STATUS_OPTIONS.find(s => s.value === currentStatus)?.dot"></span>
        共 <strong>{{ filteredAccounts.length }}</strong> 筆
      </div>
    </div>

    <div class="table-card" @click="typeDropdownOpen = false">

      <!-- 載入中 -->
      <div v-if="loading" class="state-block">
        <div class="spin-lg">⟳</div>
        <div class="state-text">載入中…</div>
      </div>

      <!-- 錯誤 -->
      <div v-else-if="error" class="state-block state-error">
        <span class="state-icon"><i class="fa-solid fa-triangle-exclamation"></i></span>
        <div class="state-text">{{ error }}</div>
        <button class="btn btn-ghost btn-sm" @click="fetchAccounts">重新載入</button>
      </div>

      <!-- 空狀態 -->
      <div v-else-if="filteredAccounts.length === 0" class="state-block">
        <span class="state-icon"><i class="fa-solid fa-inbox"></i></span>
        <div class="state-text">目前沒有符合條件的帳戶</div>
      </div>

      <!-- 帳戶表格 -->
      <template v-else>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th class="sortable" @click="setSort('applicationId')">
                  案件編號<span class="sort-icon">{{ sortIcon('applicationId') }}</span>
                </th>
                <th class="sortable" @click="setSort('accountNumber')">
                  貸款帳務帳號<span class="sort-icon">{{ sortIcon('accountNumber') }}</span>
                </th>
                <th class="sortable" @click="setSort('memberName')">
                  客戶<span class="sort-icon">{{ sortIcon('memberName') }}</span>
                </th>
                <th class="sortable" @click="setSort('applyType')">
                  類型<span class="sort-icon">{{ sortIcon('applyType') }}</span>
                </th>
                <th class="sortable text-right" @click="setSort('principalAmount')">
                  本金<span class="sort-icon">{{ sortIcon('principalAmount') }}</span>
                </th>
                <th class="sortable text-right" @click="setSort('monthlyPayment')">
                  月繳<span class="sort-icon">{{ sortIcon('monthlyPayment') }}</span>
                </th>
                <th class="sortable text-right" @click="setSort('rate')">
                  年利率<span class="sort-icon">{{ sortIcon('rate') }}</span>
                </th>
                <th class="sortable text-center" @click="setSort('paidPeriods')">
                  期數進度<span class="sort-icon">{{ sortIcon('paidPeriods') }}</span>
                </th>
                <th class="sortable text-right" @click="setSort('remainingPrincipal')">
                  剩餘本金<span class="sort-icon">{{ sortIcon('remainingPrincipal') }}</span>
                </th>
                <th class="sortable" @click="setSort('nextPaymentDate')">
                  下次繳款日<span class="sort-icon">{{ sortIcon('nextPaymentDate') }}</span>
                </th>
                <th class="sortable" @click="setSort('accountStatus')">
                  狀態<span class="sort-icon">{{ sortIcon('accountStatus') }}</span>
                </th>
                <th class="sortable" @click="setSort('startDate')">
                  撥款日<span class="sort-icon">{{ sortIcon('startDate') }}</span>
                </th>
                <th class="text-center">還款時間表</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="acc in pagedAccounts"
                :key="acc.accountId"
                class="data-row"
                :class="{ 'row-overdue': acc.accountStatus === 'OVERDUE' }"
              >
                <td>
                  <span class="mono text-sm">{{ acc.applicationId || '—' }}</span>
                </td>
                <td>
                  <span class="mono text-sm">{{ acc.accountNumber || '—' }}</span>
                </td>
                <td>
                  <div class="applicant-cell">
                    <span class="mono cif-tag">{{ acc.cif || '—' }}</span>
                    <span class="member-name" v-if="acc.memberName">{{ acc.memberName }}</span>
                  </div>
                </td>
                <td>
                  <span class="type-badge" :class="'tb-' + acc.applyType">
                    {{ LOAN_TYPE_NAME[acc.applyType] || acc.applyType }}
                  </span>
                </td>
                <td class="text-right mono">
                  {{ formatAmount(acc.principalAmount) }}
                </td>
                <td class="text-right mono">
                  {{ formatDecimal(acc.monthlyPayment) }}
                </td>
                <td class="text-right mono">
                  {{ formatRate(acc.rate) }}
                </td>
                <td class="text-center">
                  <div class="period-cell">
                    <span class="period-text">{{ acc.paidPeriods }} / {{ acc.confirmedPeriod }}</span>
                    <div class="mini-bar-wrap">
                      <div
                        class="mini-bar-fill"
                        :class="progressClass(acc)"
                        :style="{ width: progressPct(acc) + '%' }"
                      ></div>
                    </div>
                  </div>
                </td>
                <td class="text-right mono">
                  {{ formatDecimal(acc.remainingPrincipal) }}
                </td>
                <td>
                  <span
                    class="mono text-sm"
                    :class="{ 'overdue-text': acc.accountStatus === 'OVERDUE' }"
                  >
                    {{ acc.nextPaymentDate ? formatDate(acc.nextPaymentDate) : '—' }}
                  </span>
                </td>
                <td>
                  <span class="status-badge" :class="statusClass(acc.accountStatus)">
                    {{ statusLabel(acc.accountStatus) }}
                  </span>
                </td>
                <td>
                  <span class="mono text-sm muted">{{ formatDate(acc.startDate) }}</span>
                </td>
                <td class="text-center">
                  <button class="schedule-btn" @click.stop="openRepaymentModal(acc)">
                    ⌕ 查看
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- ── 分頁 Footer ── -->
        <div class="table-footer">
          <div class="footer-count">
            顯示 <strong>{{ (currentPage - 1) * pageSize + 1 }}</strong> –
            <strong>{{ Math.min(currentPage * pageSize, filteredAccounts.length) }}</strong>
            共 <strong>{{ filteredAccounts.length }}</strong> 筆
          </div>
          <div class="pagination">
            <button class="page-btn nav-btn" :disabled="currentPage === 1" @click="currentPage--">
              ‹ 上一頁
            </button>
            <div class="page-numbers">
              <template v-for="p in pageList" :key="p">
                <button
                  v-if="p !== '...'"
                  class="page-btn"
                  :class="{ active: p === currentPage }"
                  @click="currentPage = p"
                >{{ p }}</button>
                <span v-else class="page-btn ellipsis">…</span>
              </template>
            </div>
            <button class="page-btn nav-btn" :disabled="currentPage === totalPages" @click="currentPage++">
              下一頁 ›
            </button>
          </div>
        </div>
      </template>
    </div>

  </div>

  <!-- ── 還款時間表 Modal ── -->
  <teleport to="body">
    <transition name="modal-fade">
      <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
        <div class="modal-box">

          <!-- Modal Header -->
          <div class="modal-header">
            <div class="modal-title-group">
              <span class="modal-title">還款時間表</span>
              <span v-if="modalAccount" class="modal-subtitle">
                案件 {{ modalAccount.applicationId || '—' }} ｜ 貸款帳務帳號 {{ modalAccount.accountNumber || '—' }}
                <span class="type-badge" :class="'tb-' + modalAccount.applyType" style="margin-left:8px;">
                  {{ LOAN_TYPE_NAME[modalAccount.applyType] || modalAccount.applyType }}
                </span>
              </span>
            </div>
            <button class="modal-close-btn" @click="closeModal">✕</button>
          </div>

          <!-- Modal 摘要列 -->
          <div v-if="modalAccount" class="modal-summary">
            <div class="summary-item">
              <span class="summary-label">本金</span>
              <span class="summary-value mono">{{ formatAmount(modalAccount.principalAmount) }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">月繳</span>
              <span class="summary-value mono">{{ formatDecimal(modalAccount.monthlyPayment) }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">年利率</span>
              <span class="summary-value mono">{{ formatRate(modalAccount.rate) }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">期數進度</span>
              <span class="summary-value mono">{{ modalAccount.paidPeriods }} / {{ modalAccount.confirmedPeriod }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">剩餘本金</span>
              <span class="summary-value mono">{{ formatDecimal(modalAccount.remainingPrincipal) }}</span>
            </div>
          </div>

          <!-- Modal Body -->
          <div class="modal-body">
            <div v-if="repaymentLoading" class="rep-loading">
              <span class="spin">⟳</span> 載入還款明細中…
            </div>
            <template v-else-if="repayments.length > 0">
              <div class="rep-header">
                <span class="rep-title">共 {{ repayments.length }} 期</span>
              </div>
              <div class="rep-table-wrap">
                <table class="rep-table">
                  <thead>
                    <tr>
                      <th>期</th>
                      <th>應繳日</th>
                      <th>實繳日</th>
                      <th class="text-right">月繳總額</th>
                      <th class="text-right">本金</th>
                      <th class="text-right">利息</th>
                      <th class="text-right">剩餘本金</th>
                      <th>狀態</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="rp in repayments"
                      :key="rp.repaymentId"
                      :class="repRowClass(rp.repaymentStatus)"
                    >
                      <td class="mono text-center">{{ rp.periodIndex }}</td>
                      <td class="mono">{{ formatDate(rp.scheduledDate) }}</td>
                      <td class="mono">{{ rp.paidDate ? formatDate(rp.paidDate) : '—' }}</td>
                      <td class="mono text-right">{{ formatDecimal(rp.totalAmount) }}</td>
                      <td class="mono text-right">{{ formatDecimal(rp.principalPortion) }}</td>
                      <td class="mono text-right muted">{{ formatDecimal(rp.interestPortion) }}</td>
                      <td class="mono text-right">{{ formatDecimal(rp.remainingAfter) }}</td>
                      <td>
                        <span class="rep-status" :class="repStatusClass(rp.repaymentStatus)">
                          {{ repStatusLabel(rp.repaymentStatus) }}
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </template>
            <div v-else class="rep-loading">尚無還款明細</div>
          </div>

        </div>
      </div>
    </transition>
  </teleport>

</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import api from '@/api/axios'
const LOAN_TYPE_NAME = {
  PERSONAL: '個人信貸',
  CAR:      '汽車貸款',
  MOTOR:    '機車貸款',
  STUDENT:  '學貸',
  BUSINESS: '創業貸款',
  HOUSE:    '房屋貸款',
  LAND:     '土地貸款',
}
const LOAN_TYPE_KEYS = Object.keys(LOAN_TYPE_NAME)

const STATUS_OPTIONS = [
  { value: null,      label: '全部',   dot: 'dot-all'     },
  { value: 'ACTIVE',  label: '還款中', dot: 'dot-active'  },
  { value: 'OVERDUE', label: '逾期',   dot: 'dot-overdue' },
  { value: 'PAID_OFF',label: '已結清', dot: 'dot-paidoff' },
]

const ACCOUNT_STATUS = {
  ACTIVE:   { label: '還款中', cls: 'st-active'  },
  OVERDUE:  { label: '逾期',   cls: 'st-overdue' },
  PAID_OFF: { label: '已結清', cls: 'st-paidoff' },
}

const REPAYMENT_STATUS = {
  SCHEDULED: { label: '待繳', cls: 'rs-scheduled' },
  PAID:      { label: '已繳', cls: 'rs-paid'      },
  OVERDUE:   { label: '逾期', cls: 'rs-overdue'   },
}
const accounts        = ref([])
const loading         = ref(false)
const error           = ref('')
const nameKeyword     = ref('')
const currentStatus   = ref(null)
const selectedTypes   = ref([])
const typeDropdownOpen = ref(false)
const repayments      = ref([])
const repaymentLoading = ref(false)
const currentPage     = ref(1)
const pageSize        = 15

// ── 排序 ──
const sortKey = ref('startDate')
const sortOrder = ref('desc')
const applicationIdQuery  = ref('')
const accountNumberQuery  = ref('')

function sortIcon(key) {
  if (sortKey.value !== key) return '⇅'
  return sortOrder.value === 'asc' ? '↑' : '↓'
}

// Modal 狀態
const showModal    = ref(false)
const modalAccount = ref(null)

// ── 篩選 + 排序 ──
const filteredAccounts = computed(() => {
  let list = accounts.value
  if (currentStatus.value) {
    list = list.filter(a => a.accountStatus === currentStatus.value)
  }
  if (selectedTypes.value.length > 0) {
    list = list.filter(a => selectedTypes.value.includes(a.applyType))
  }
  const keyword = nameKeyword.value.trim().toLowerCase()
  if (keyword) {
    list = list.filter(a => String(a.memberName || '').toLowerCase().includes(keyword))
  }
  if (applicationIdQuery.value.trim()) {
    const q = applicationIdQuery.value.trim().toLowerCase()
    list = list.filter(a => (a.applicationId || '').toLowerCase().includes(q))
  }
  if (accountNumberQuery.value.trim()) {
    const q = accountNumberQuery.value.trim().toLowerCase()
    list = list.filter(a => (a.accountNumber || '').toLowerCase().includes(q))
  }
  return list
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredAccounts.value.length / pageSize)))

const sortedAccounts = computed(() => {
  let list = [...filteredAccounts.value]
  if (!sortKey.value) return list

  list.sort((a, b) => {
    let valA = a[sortKey.value]
    let valB = b[sortKey.value]

    // 將 null/undefined 放到底部
    if (valA == null && valB != null) return 1
    if (valA != null && valB == null) return -1
    if (valA == null && valB == null) return 0

    if (typeof valA === 'string' && typeof valB === 'string') {
      const res = valA.localeCompare(valB)
      return sortOrder.value === 'asc' ? res : -res
    }

    if (valA < valB) return sortOrder.value === 'asc' ? -1 : 1
    if (valA > valB) return sortOrder.value === 'asc' ? 1 : -1
    return 0
  })
  return list
})

const pagedAccounts = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return sortedAccounts.value.slice(start, start + pageSize)
})

const pageList = computed(() => {
  const total = totalPages.value
  const cur   = currentPage.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i + 1)
  const pages = []
  pages.push(1)
  if (cur > 3)       pages.push('...')
  for (let p = Math.max(2, cur - 1); p <= Math.min(total - 1, cur + 1); p++) pages.push(p)
  if (cur < total - 2) pages.push('...')
  pages.push(total)
  return pages
})
function setStatus(val) {
  currentStatus.value = val
  currentPage.value   = 1
  repayments.value    = []
}

watch(nameKeyword, () => {
  currentPage.value = 1
})

function setSort(key) {
  if (sortKey.value === key) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortKey.value = key
    sortOrder.value = 'asc'
  }
  currentPage.value = 1
}

async function fetchAccounts() {
  loading.value  = true
  error.value    = ''
  repayments.value = []
  try {
    const res = await api.get('/api/admin/loan-accounts', { params: {} })
    accounts.value = res.data.data || []
  } catch (e) {
    error.value = e.response?.data?.message || '載入失敗，請稍後再試'
  } finally {
    loading.value = false
  }
}

async function openRepaymentModal(acc) {
  modalAccount.value    = acc
  repayments.value      = []
  repaymentLoading.value = true
  showModal.value       = true
  document.body.style.overflow = 'hidden'
  try {
    const res = await api.get(`/api/admin/loan-accounts/${acc.accountId}/repayments`)
    repayments.value = res.data.data || []
  } catch {
    repayments.value = []
  } finally {
    repaymentLoading.value = false
  }
}

function closeModal() {
  showModal.value    = false
  modalAccount.value = null
  repayments.value   = []
  document.body.style.overflow = ''
}
function statusLabel(st) { return ACCOUNT_STATUS[st]?.label || st }
function statusClass(st)  { return ACCOUNT_STATUS[st]?.cls   || '' }
function repStatusLabel(st) { return REPAYMENT_STATUS[st]?.label || st }
function repStatusClass(st) { return REPAYMENT_STATUS[st]?.cls   || '' }

function progressPct(acc) {
  if (!acc.confirmedPeriod) return 0
  return Math.round((acc.paidPeriods / acc.confirmedPeriod) * 100)
}
function progressClass(acc) {
  if (acc.accountStatus === 'OVERDUE')  return 'prog-overdue'
  if (acc.accountStatus === 'PAID_OFF') return 'prog-done'
  return 'prog-active'
}
function repRowClass(st) {
  if (st === 'PAID')    return 'rrow-paid'
  if (st === 'OVERDUE') return 'rrow-overdue'
  return ''
}

function formatAmount(n)  { return n != null ? Number(n).toLocaleString('zh-TW') : '—' }
function formatDecimal(n) {
  return n != null
    ? Number(n).toLocaleString('zh-TW', { minimumFractionDigits: 0, maximumFractionDigits: 0 })
    : '—'
}
function formatRate(r)    { return r != null ? (r * 100).toFixed(2) + ' %' : '—' }
function formatDate(d)    { return d ? String(d).substring(0, 10) : '—' }

onMounted(fetchAccounts)
</script>

<style scoped>
/* ── CSS 變數（對齊 admin-theme）── */
.loan-account-admin {
  --accent:      #5C6B5F;
  --accent-dim:  rgba(92, 107, 95, 0.10);
  --primary:     #5C6B5F;
  --pk:          #4A574D;
  --bg:          #f4f5f7;
  --surface:     #ffffff;
  --surface-2:   #f0f2f0;
  --border:      #dde1de;
  --ink:         #2B2B2B;
  --ink-2:       #333333;
  --muted:       #8c9891;
  --muted-2:     #5a6a5e;
  --green:       #4A8C5C;
  --red:         #C0392B;
  --amber:       #C49A3C;
  --yellow:      #C49A3C;

  min-height: 100vh;
  background: var(--bg);
  font-family: 'Noto Sans TC', sans-serif;
  color: var(--ink);
}

/* ── 頁首 ── */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--ink);
  margin: 0;
}

/* ── 篩選列 ── */
.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.name-search-wrap {
  position: relative;
  display: flex;
  align-items: center;
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

.filter-meta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--muted-2);
  white-space: nowrap;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.filter-meta strong {
  color: var(--ink);
}

.pills-group { display: flex; gap: 6px; flex-wrap: wrap; }

.filter-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 20px;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--muted-2);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}
.filter-pill:hover  { border-color: var(--accent); color: var(--accent); }
.filter-pill.active { background: rgba(92, 107, 95, 0.10); border-color: var(--accent); color: var(--accent); }

.pill-dot {
  width: 7px; height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}
.dot-all     { background: var(--muted-2); }
.dot-active  { background: var(--green); }
.dot-overdue { background: var(--red); }
.dot-paidoff { background: var(--muted); }

.pill-count {
  background: rgba(255,255,255,0.25);
  padding: 0 6px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 700;
  min-width: 18px;
  text-align: center;
}
.filter-pill:not(.active) .pill-count { background: var(--surface-2); color: var(--muted-2); }

/* ── 貸款類型下拉 ── */
.type-dropdown-wrap { position: relative; }

.type-dropdown-trigger {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--muted-2);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}
.type-dropdown-trigger:hover, .type-dropdown-trigger.open {
  border-color: var(--accent);
  color: var(--accent);
}
.type-dropdown-trigger.active { border-color: var(--accent); color: var(--accent); background: rgba(92, 107, 95, 0.10); }

.dropdown-caret { transition: transform 0.2s; font-size: 11px; }
.dropdown-caret.rotated { transform: rotate(180deg); }

.type-dropdown-menu {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  z-index: 200;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  box-shadow: 0 10px 26px rgba(63, 74, 66, 0.06);
  min-width: 200px;
  padding: 8px 0;
}
.dropdown-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 14px 10px;
}
.dropdown-title { font-size: 13px; font-weight: 600; color: var(--muted-2); text-transform: uppercase; letter-spacing: 0.06em; }
.clear-btn { font-size: 13px; color: var(--accent); background: none; border: none; cursor: pointer; padding: 0; }
.dropdown-divider { height: 1px; background: var(--border); margin: 4px 0; }

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  cursor: pointer;
  font-size: 14px;
  color: var(--ink);
  transition: background 0.1s;
}
.dropdown-item:hover { background: var(--surface-2); }
.dropdown-item input[type="checkbox"] { display: none; }
.check-box {
  width: 15px; height: 15px;
  border: 1.5px solid var(--border);
  border-radius: 4px;
  flex-shrink: 0;
  transition: all 0.15s;
}
.dropdown-item input:checked + .check-box {
  background: var(--accent);
  border-color: var(--accent);
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 10 8' fill='none' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1 4l3 3 5-6' stroke='white' stroke-width='1.5' stroke-linecap='round' stroke-linejoin='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: center;
  background-size: 10px;
}
.item-name { flex: 1; }
.item-dot {
  width: 8px; height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}
.idot-PERSONAL { background: #7B9EA6; }
.idot-CAR      { background: #8B7355; }
.idot-MOTOR    { background: #B8864E; }
.idot-STUDENT  { background: #6B8E6B; }
.idot-BUSINESS { background: #A67B5B; }
.idot-HOUSE    { background: #7B6B8E; }
.idot-LAND     { background: #8E7B6B; }

/* ── ID 搜尋欄位 ── */
.id-search-wrap {
  position: relative;
  display: inline-flex;
  align-items: center;
}
.id-search-icon {
  position: absolute;
  left: 9px;
  font-size: 12px;
  color: var(--muted);
  pointer-events: none;
  opacity: 0.6;
}
.id-search-input {
  width: 160px;
  padding: 6px 28px 6px 28px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: var(--surface);
  color: var(--ink);
  font-size: 14px;
  font-family: 'IBM Plex Mono', monospace;
  outline: none;
  transition: border-color 0.15s;
}
.id-search-input::placeholder { color: var(--muted); opacity: 1; }
.id-search-input:focus { border-color: var(--accent); }
.id-search-clear {
  position: absolute;
  right: 7px;
  background: none;
  border: none;
  color: var(--muted);
  cursor: pointer;
  padding: 2px 4px;
  font-size: 10px;
  line-height: 1;
  border-radius: 4px;
  display: flex;
  align-items: center;
  transition: color 0.12s;
}
.id-search-clear:hover { color: var(--ink); }

.filter-meta { font-size: 14px; color: var(--muted-2); margin-left: auto; }
.filter-meta strong { color: var(--ink); }

/* ── Table Card ── */
.table-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 26px rgba(63, 74, 66, 0.06);
}

/* ── States ── */
.state-block {
  display: flex; flex-direction: column; align-items: center;
  gap: 12px; padding: 60px 0; text-align: center;
}
.state-icon  { font-size: 36px; }
.state-icon .fa-inbox {
  font-size: 40px;
  opacity: 0.4;
}
.state-text  { font-size: 15px; color: var(--muted-2); }
.spin-lg { font-size: 28px; color: var(--muted); animation: spin 0.8s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* ── 主表格 ── */
.table-wrap { overflow-x: auto; }

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 15px;
  white-space: nowrap;
}

.data-table thead tr {
  background: var(--surface-2);
  border-bottom: 1px solid var(--border);
}
.data-table th {
  padding: 11px 14px;
  font-size: 14px;
  font-weight: 600;
  color: var(--muted-2);
  text-align: left;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  font-family: 'IBM Plex Mono', monospace;
}
.data-table th.text-right  { text-align: right; }
.data-table th.text-center { text-align: center; }
.data-table th.sortable {
  cursor: pointer;
  user-select: none;
}
.data-table th.sortable:hover { color: var(--accent); }
.sort-icon {
  font-size: 11px;
  opacity: 0.5;
  margin-left: 4px;
}
.data-table th.sortable:hover .sort-icon { opacity: 1; }

.sortable { cursor: pointer; user-select: none; }
.sortable:hover { color: var(--accent); }
.sortable span { display: inline-block; width: 14px; text-align: center; }

.data-row {
  border-bottom: 1px solid var(--border);
  transition: background 0.1s;
}
.data-row:hover      { background: var(--surface-2); }
.data-row:hover      { background: rgba(92,107,95,0.03); }
.data-row.row-overdue  { background: rgba(192,57,43,0.04); }

.data-table td {
  padding: 11px 14px;
  color: var(--ink);
  vertical-align: middle;
  font-size: 15px;
}
.data-table td.text-right  { text-align: right; }
.data-table td.text-center { text-align: center; }

/* Mono / text helpers */
.mono  { font-family: 'IBM Plex Mono', monospace; }
.text-sm { font-size: 14px; }
.muted   { color: var(--muted-2); }
.overdue-text { color: var(--red); font-weight: 600; }

/* 客戶欄 */
.applicant-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.member-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink);
}

/* CIF tag */
.cif-tag {
  font-size: 12px;
  background: rgba(234, 228, 218, 0.62);
  border: 1px solid var(--border);
  padding: 2px 7px;
  border-radius: 8px;
  color: var(--muted-2);
  align-self: flex-start;
}

/* 類型標籤 */
.type-badge {
  font-size: 13px;
  padding: 3px 8px;
  border-radius: 8px;
  font-weight: 600;
  white-space: nowrap;
}
.tb-PERSONAL { background: rgba(245,241,234,0.82); color: #7a5c3a; }
.tb-CAR      { background: rgba(238,243,242,0.86);  color: #4A574D; }
.tb-MOTOR    { background: rgba(245,237,233,0.86);  color: #7a4a38; }
.tb-STUDENT  { background: rgba(238,243,239,0.86); color: #4A6B5C; }
.tb-BUSINESS { background: rgba(240,238,245,0.86);  color: #5A4F82; }
.tb-HOUSE    { background: rgba(235,242,241,0.86); color: #3D5C58; }
.tb-LAND     { background: rgba(234,228,218,0.86); color: #6E6259; }

/* 帳戶狀態 */
.status-badge {
  font-size: 13px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 20px;
  white-space: nowrap;
}
.st-active  { background: rgba(74,140,92,0.12);   color: #1a7a40; }
.st-overdue { background: rgba(166,90,77,0.10);   color: var(--red); }
.st-paidoff { background: rgba(80,80,80,0.08);    color: #555; }

/* 期數進度格 */
.period-cell {
  display: flex; flex-direction: column; gap: 4px;
  align-items: center; min-width: 80px;
}
.period-text { font-size: 14px; font-family: 'IBM Plex Mono', monospace; color: var(--ink-2); }
.mini-bar-wrap {
  height: 3px; width: 70px;
  background: var(--surface-2);
  border-radius: 2px; overflow: hidden;
}
.mini-bar-fill {
  height: 100%; border-radius: 2px;
  transition: width 0.3s ease;
}
.prog-active  { background: var(--primary); }
.prog-overdue { background: var(--red); }
.prog-done    { background: var(--green); }

.rep-header {
  display: flex; align-items: baseline; gap: 10px;
  margin-bottom: 12px;
}
.rep-title    { font-size: 15px; font-weight: 700; color: var(--primary); }
.rep-subtitle { font-size: 13px; color: var(--muted-2); }

.rep-loading {
  text-align: center; color: var(--muted-2);
  font-size: 14px; padding: 16px 0;
  display: flex; align-items: center; justify-content: center; gap: 8px;
}
.rep-loading .spin { animation: spin 0.8s linear infinite; }

.rep-table-wrap { overflow-x: auto; border-radius: 8px; border: 1px solid var(--border); }

.rep-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
  white-space: nowrap;
}
.rep-table th {
  text-align: left;
  padding: 8px 12px;
  background: var(--surface-2);
  border-bottom: 1px solid var(--border);
  font-family: 'IBM Plex Mono', monospace;
  font-size: 13px;
  letter-spacing: 0.06em;
  color: var(--muted-2);
  font-weight: 600;
}
.rep-table th.text-right { text-align: right; }
.rep-table th.text-center { text-align: center; }

.rep-table td {
  padding: 8px 12px;
  border-bottom: 1px solid var(--border);
  color: var(--ink);
  vertical-align: middle;
}
.rep-table td.text-right  { text-align: right; }
.rep-table td.text-center { text-align: center; }
.rep-table tr:last-child td { border-bottom: none; }

.rrow-paid    td { color: var(--muted); }
.rrow-overdue    { background: rgba(192,57,43,0.04); }
.rrow-overdue td { color: var(--red); }

.rep-status {
  font-size: 12px; font-weight: 600;
  padding: 2px 8px; border-radius: 10px; display: inline-block;
}
.rs-scheduled { background: rgba(196,154,60,0.12);  color: #7a6000; }
.rs-paid      { background: rgba(74,140,92,0.10);   color: #1a7a40; }
.rs-overdue   { background: rgba(166,90,77,0.12);   color: var(--red); }

/* ── 分頁 ── */
.table-footer {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-top: 1px solid var(--border);
  background: rgba(234, 228, 218, 0.62); flex-wrap: wrap; gap: 10px;
}
.footer-count { font-size: 14px; color: var(--muted-2); font-family: 'IBM Plex Mono', monospace; }
.footer-count strong { color: var(--ink); }

.pagination { display: flex; align-items: center; gap: 4px; }
.page-numbers { display: flex; align-items: center; gap: 4px; }

.page-btn {
  display: inline-flex; align-items: center; justify-content: center;
  padding: 5px 9px; border-radius: 6px;
  font-size: 14px; font-family: 'IBM Plex Mono', monospace;
  cursor: pointer; border: 1px solid var(--border);
  background: var(--surface); color: var(--muted-2);
  transition: all 0.15s; min-width: 32px; white-space: nowrap;
}
.nav-btn { padding: 5px 12px; }
.page-btn:hover:not(:disabled) { border-color: var(--accent); color: var(--accent); background: var(--accent-dim); }
.page-btn:hover:not(:disabled) { border-color: var(--accent); color: var(--accent); background: rgba(92,107,95,0.05); }
.page-btn:disabled { opacity: 0.35; cursor: not-allowed; }
.page-btn.active { background: var(--accent); border-color: var(--accent); color: #fff; font-weight: 600; }
.page-btn.ellipsis { border-color: transparent; background: transparent; cursor: default; color: var(--muted); }

/* ── Buttons ── */
.btn {
  display: inline-flex; align-items: center; gap: 5px;
  border: none; border-radius: 6px;
  font-family: 'Noto Sans TC', sans-serif;
  cursor: pointer; transition: all 0.15s; font-weight: 500;
}
.btn-sm { padding: 7px 14px; font-size: 14px; }
.btn-ghost {
  background: var(--surface); color: var(--muted-2);
  border: 1px solid var(--border);
}
.btn-ghost:hover { border-color: var(--accent); color: var(--accent); background: rgba(92,107,95,0.05); }
.btn-ghost:disabled { opacity: 0.4; cursor: not-allowed; }

.spin { animation: spin 0.8s linear infinite; display: inline-block; }

/* ── 下拉動畫 ── */
.drop-enter-active, .drop-leave-active { transition: all 0.15s ease; }
.drop-enter-from, .drop-leave-to       { opacity: 0; transform: translateY(-6px); }

/* ── 查看按鈕 ── */
.schedule-btn {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 5px 12px; border-radius: 6px;
  font-size: 14px; font-weight: 500;
  border: 1px solid var(--border);
  background: var(--surface); color: var(--primary);
  cursor: pointer; transition: all 0.15s;
  white-space: nowrap;
}
.schedule-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: rgba(92,107,95,0.05);
}

/* ── Modal 遮罩 ── */
.modal-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: transparent;
  display: flex; align-items: center; justify-content: center;
  padding: 24px;
}

/* ── Modal 本體 ── */
.modal-box {
  background: #ffffff;
  border: 1px solid rgba(214, 206, 195, 0.78);
  border-radius: 16px;
  width: 100%; max-width: 860px;
  max-height: 85vh;
  display: flex; flex-direction: column;
  box-shadow: 0 10px 26px rgba(63,74,66,0.06);
  overflow: hidden;
}

/* Modal Header */
.modal-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 24px 16px;
  border-bottom: 1px solid #dde1de;
  background: rgba(234, 228, 218, 0.62);
  flex-shrink: 0;
}
.modal-title-group { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.modal-title { font-size: 18px; font-weight: 700; color: #2B2B2B; }
.modal-subtitle { font-size: 14px; color: #5a6a5e; display: flex; align-items: center; }
.modal-close-btn {
  width: 30px; height: 30px; border-radius: 8px;
  border: 1px solid #dde1de; background: #ffffff;
  color: #5a6a5e; font-size: 13px;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.15s; flex-shrink: 0;
}
.modal-close-btn:hover { border-color: #4A574D; color: #4A574D; background: rgba(92,107,95,0.08); }

/* Modal 摘要 */
.modal-summary {
  display: flex; gap: 0; flex-shrink: 0;
  border-bottom: 1px solid #dde1de;
  background: #ffffff;
}
.summary-item {
  display: flex; flex-direction: column; gap: 3px;
  padding: 12px 20px;
  border-right: 1px solid #dde1de;
}
.summary-item:last-child { border-right: none; }
.summary-label { font-size: 12px; font-weight: 600; color: #5a6a5e; text-transform: uppercase; letter-spacing: 0.06em; }
.summary-value { font-size: 15px; font-weight: 600; color: #2B2B2B; }

/* Modal Body */
.modal-body {
  flex: 1; overflow-y: auto; padding: 20px 24px 24px;
}

/* Modal 動畫 */
.modal-fade-enter-active, .modal-fade-leave-active { transition: all 0.2s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.modal-fade-enter-from .modal-box, .modal-fade-leave-to .modal-box { transform: scale(0.96) translateY(8px); }
.modal-fade-enter-active .modal-box, .modal-fade-leave-active .modal-box { transition: transform 0.2s ease; }
</style>
