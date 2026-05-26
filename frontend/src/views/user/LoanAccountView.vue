<template>
  <div class="loan-account-page">

    <!-- ── 頁首 ── -->
    <div class="page-header">
      <div class="page-header-inner">
        <div class="page-title-block">
          <span class="page-icon"><i class="fa-solid fa-file-invoice-dollar"></i></span>
          <div>
            <h1 class="page-title">我的貸款帳戶</h1>
            <p class="page-subtitle">查看貸款明細與每期還款進度</p>
          </div>
        </div>
      </div>
    </div>

    <div class="page-body">

      <!-- ── 載入中 ── -->
      <div v-if="loading" class="state-block">
        <div class="skeleton-list">
          <div v-for="i in 2" :key="i" class="skel-card">
            <div class="sk sk-tag"></div>
            <div class="sk sk-line"></div>
            <div class="sk sk-short"></div>
          </div>
        </div>
      </div>

      <!-- ── 錯誤 ── -->
      <div v-else-if="error" class="state-block state-error">
        <span class="state-icon"><i class="fa-solid fa-triangle-exclamation"></i></span>
        <div class="state-text">{{ error }}</div>
        <button class="btn-retry" @click="loadAccounts">重新載入</button>
      </div>

      <!-- ── 空狀態 ── -->
      <div v-else-if="accounts.length === 0" class="state-block state-empty">
        <span class="state-icon"><i class="fa-solid fa-inbox"></i></span>
        <div class="state-text">目前尚無貸款帳戶</div>
        <button class="btn-apply" @click="$router.push({ name: 'user-loan-apply' })">
          立即申請貸款
        </button>
      </div>

      <!-- ── 狀態篩選列 ── -->
      <div v-else class="filter-bar">
        <button class="filter-btn" :class="{ active: filterStatus === null }"       @click="filterStatus = null">全部</button>
        <button class="filter-btn" :class="{ active: filterStatus === 'OVERDUE' }"  @click="filterStatus = 'OVERDUE'">逾期</button>
        <button class="filter-btn" :class="{ active: filterStatus === 'ACTIVE' }"   @click="filterStatus = 'ACTIVE'">還款中</button>
        <button class="filter-btn" :class="{ active: filterStatus === 'PAID_OFF' }" @click="filterStatus = 'PAID_OFF'">已結清</button>
      </div>

      <!-- ── 帳戶列表 ── -->
      <div v-if="!loading && !error && accounts.length > 0" class="account-list">
        <div v-if="filteredSortedAccounts.length === 0" class="filter-empty">
          <i class="fa-solid fa-filter-circle-xmark"></i>
          <span>目前無符合的帳戶</span>
        </div>
        <div
          v-for="acc in filteredSortedAccounts"
          :key="acc.accountId"
          class="account-card"
        >
          <!-- 卡片頭：帳戶編號 + 狀態標籤 + 展開按鈕 -->
          <div class="card-top">
            <div class="card-top-left">
              <span class="account-id">{{ acc.accountId }}</span>
              <span class="type-badge">{{ LOAN_TYPE_MAP[acc.applyType] || acc.applyType }}</span>
            </div>
            <div class="card-top-right">
              <span class="status-badge" :class="statusClass(acc.accountStatus)">
                {{ statusLabel(acc.accountStatus) }}
              </span>
              <!-- 立即繳款按鈕（僅 ACTIVE 帳戶顯示） -->
              <button
                v-if="acc.accountStatus === 'ACTIVE' || acc.accountStatus === 'OVERDUE'"
                class="repay-btn"
                @click="$router.push({ name: 'user-loan-repayment', query: { accountId: acc.accountId } })"
                title="前往繳款"
              >
                立即繳款
              </button>
              <!-- 查看還款時間表按鈕 -->
              <button
                class="expand-btn"
                @click="openRepaymentModal(acc)"
                title="查看還款明細"
              >
                還款明細
              </button>
            </div>
          </div>

          <!-- ── 主體：進度 + 重點摘要 + 條件摘要 ── -->
          <div class="card-body">

            <!-- 左：圓形還款進度圖 + 期數進度 -->
            <div class="circle-section">
              <!-- 圓形圖：圈內只顯示百分比 -->
              <div class="circle-chart-wrap">
                <svg viewBox="0 0 120 120" class="circle-svg">
                  <circle cx="60" cy="60" r="48" fill="none" stroke="var(--surface-2)" stroke-width="10"/>
                  <circle
                    cx="60" cy="60" r="48"
                    fill="none"
                    :stroke="progressStroke(acc)"
                    stroke-width="10"
                    stroke-linecap="round"
                    stroke-dasharray="301.59"
                    :stroke-dashoffset="301.59 * (1 - paidRatio(acc))"
                    transform="rotate(-90 60 60)"
                    style="transition: stroke-dashoffset 0.6s ease;"
                  />
                </svg>
                <div class="circle-center-text">
                  <span class="cctext-pct" :style="{ color: progressStroke(acc) }">
                    {{ Math.round(paidRatio(acc) * 100) }}%
                  </span>
                  <span class="cctext-paid-label">已還清</span>
                </div>
              </div>
              <!-- 圓圈下方：剩餘本金 / 貸款本金 -->
              <div class="circle-principal">
                <span class="circle-principal-remaining" :style="{ color: progressStroke(acc) }">
                  $ {{ formatDecimal(acc.remainingPrincipal) }}
                </span>
                <span class="circle-principal-sep">/</span>
                <span class="circle-principal-total">$ {{ formatDecimal(acc.principalAmount) }}</span>
              </div>
              <div class="circle-label">剩餘本金 / 貸款本金</div>
              <!-- 期數進度條 -->
              <div class="period-progress">
                <div class="period-progress-header">
                  <span class="period-label">已繳期數</span>
                  <span class="period-val" :style="{ color: progressStroke(acc) }">
                    {{ acc.paidPeriods }} / {{ acc.confirmedPeriod }}
                  </span>
                </div>
                <span class="progress-bar-wrap">
                  <span
                    class="progress-bar-fill"
                    :style="{ width: progressPct(acc) + '%' }"
                    :class="progressClass(acc)"
                  ></span>
                </span>
              </div>
            </div>

            <!-- 中：付款重點 -->
            <div class="payment-focus" :class="{ overdue: isOverdue(acc) }">
              <span class="focus-label">月繳金額</span>
              <strong class="focus-amount">$ {{ formatDecimal(acc.monthlyPayment) }}</strong>
              <div class="focus-date">
                <span>下次繳款日</span>
                <strong class="mono" :class="{ 'overdue-text': isOverdue(acc) }">
                  {{ acc.nextPaymentDate ? formatDate(acc.nextPaymentDate) : '—' }}
                </strong>
              </div>
              <div v-if="isOverdue(acc)" class="overdue-note">
                已逾期，請盡快完成繳款
              </div>
            </div>

            <!-- 右：條件摘要格 -->
            <div class="info-grid">
              <div class="info-tile">
                <span class="info-row-label">貸款期數</span>
                <span class="info-row-val">{{ acc.confirmedPeriod }} 期</span>
              </div>
              <div class="info-tile rate-tile">
                <div class="rate-side">
                  <span class="info-row-label">年利率</span>
                  <span class="info-row-val">{{ formatRate(acc.rate) }}</span>
                </div>
                <div class="rate-divider"></div>
                <div class="rate-side">
                  <span class="info-row-label">月利率</span>
                  <span class="info-row-val">{{ formatMonthlyRate(acc.rate) }}</span>
                </div>
              </div>
              <div class="info-tile">
                <span class="info-row-label">撥款日</span>
                <span class="info-row-val mono">{{ formatDate(acc.startDate) }}</span>
              </div>
              <div class="info-tile">
                <span class="info-row-label">剩餘期數</span>
                <span class="info-row-val mono">{{ Math.max(Number(acc.confirmedPeriod || 0) - Number(acc.paidPeriods || 0), 0) }} 期</span>
              </div>
            </div>

          </div>

        </div>
      </div>

    </div>
  </div>

  <!-- ── 還款時間表 Modal ── -->
  <teleport to="body">
    <transition name="modal-fade">
      <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
        <div class="modal-box">
          <div class="modal-header">
            <div class="modal-title-group">
              <span class="modal-title">還款時間表</span>
              <span v-if="modalAccount" class="modal-subtitle">
                帳戶 {{ modalAccount.accountId }}
                <span class="type-badge" style="margin-left:8px;">
                  {{ LOAN_TYPE_MAP[modalAccount.applyType] || modalAccount.applyType }}
                </span>
              </span>
            </div>
            <button class="modal-close-btn" @click="closeModal">✕</button>
          </div>

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
                      :class="rowClass(rp.repaymentStatus)"
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
            <div v-else class="rep-loading">尚無還款明細資料</div>
          </div>
        </div>
      </div>
    </transition>
  </teleport>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/api/axios'
const accounts       = ref([])   // 貸款帳戶清單
const loading        = ref(false)
const error          = ref('')
const filterStatus   = ref(null) // null = 全部
const showModal      = ref(false)
const modalAccount   = ref(null)
const repayments     = ref([])   // Modal 中的帳戶還款明細
const repaymentLoading = ref(false)
const LOAN_TYPE_MAP = {
  PERSONAL: '個人信貸',
  CAR:      '汽車貸款',
  MOTOR:    '機車貸款',
  STUDENT:  '學貸',
  BUSINESS: '創業貸款',
  HOUSE:    '房屋貸款',
  LAND:     '土地貸款',
}
const ACCOUNT_STATUS = {
  ACTIVE:   { label: '還款中',   cls: 'st-active'  },
  OVERDUE:  { label: '逾期',     cls: 'st-overdue' },
  PAID_OFF: { label: '已結清',   cls: 'st-paidoff' },
}
const REPAYMENT_STATUS = {
  SCHEDULED: { label: '待繳',   cls: 'rs-scheduled' },
  PAID:      { label: '已繳',   cls: 'rs-paid'      },
  OVERDUE:   { label: '逾期',   cls: 'rs-overdue'   },
}
const filteredSortedAccounts = computed(() => {
  let list = filterStatus.value
    ? accounts.value.filter(a => a.accountStatus === filterStatus.value)
    : [...accounts.value]
  return list.sort((a, b) => {
    // 逾期優先
    if (a.accountStatus === 'OVERDUE' && b.accountStatus !== 'OVERDUE') return -1
    if (b.accountStatus === 'OVERDUE' && a.accountStatus !== 'OVERDUE') return 1
    // 已結清排最後
    if (a.accountStatus === 'PAID_OFF' && b.accountStatus !== 'PAID_OFF') return 1
    if (b.accountStatus === 'PAID_OFF' && a.accountStatus !== 'PAID_OFF') return -1
    // 依下次繳款日升冪
    const da = a.nextPaymentDate ? new Date(a.nextPaymentDate) : new Date('9999-12-31')
    const db = b.nextPaymentDate ? new Date(b.nextPaymentDate) : new Date('9999-12-31')
    return da - db
  })
})
function statusLabel(st) { return ACCOUNT_STATUS[st]?.label || st }
function statusClass(st) { return ACCOUNT_STATUS[st]?.cls   || '' }
function repStatusLabel(st) { return REPAYMENT_STATUS[st]?.label || st }
function repStatusClass(st) { return REPAYMENT_STATUS[st]?.cls   || '' }
function progressPct(acc) {
  const paid = Number(acc.paidPeriods || 0)
  const total = Number(acc.confirmedPeriod || 0)
  if (!total) return 0
  return Math.min(100, Math.max(0, Math.round((paid / total) * 100)))
}
function progressClass(acc) {
  if (acc.accountStatus === 'OVERDUE')  return 'prog-overdue'
  if (acc.accountStatus === 'PAID_OFF') return 'prog-done'
  return 'prog-active'
}
function paidRatio(acc) {
  if (!acc.principalAmount || acc.principalAmount === 0) return 0
  const paid = acc.principalAmount - (acc.remainingPrincipal ?? acc.principalAmount)
  return Math.min(1, Math.max(0, paid / acc.principalAmount))
}
function progressStroke(acc) {
  if (acc.accountStatus === 'OVERDUE')  return '#A65A4D'
  if (acc.accountStatus === 'PAID_OFF') return '#4A8C5C'
  return '#5C6B5F'
}
function isOverdue(acc) {
  return acc.accountStatus === 'OVERDUE'
}
function rowClass(st) {
  if (st === 'PAID')    return 'row-paid'
  if (st === 'OVERDUE') return 'row-overdue'
  return ''
}
function formatAmount(n) {
  return n != null ? Number(n).toLocaleString('zh-TW') : '—'
}
function formatDecimal(n) {
  return n != null ? Number(n).toLocaleString('zh-TW', { minimumFractionDigits: 0, maximumFractionDigits: 0 }) : '—'
}
function formatRate(r) {
  return r != null ? (r * 100).toFixed(2) + ' %' : '—'
}
function formatMonthlyRate(r) {
  return r != null ? ((r / 12) * 100).toFixed(4) + ' %' : '—'
}
function formatDate(d) {
  if (!d) return '—'
  // 支援 LocalDate（YYYY-MM-DD）與 LocalDateTime
  return String(d).substring(0, 10)
}
async function loadAccounts() {
  loading.value = true
  error.value = ''
  try {
    const token = localStorage.getItem('customer_token')
    const res = await api.get('/api/loan-accounts/my', {
      headers: { Authorization: `Bearer ${token}` },
    })
    accounts.value = res.data.data || []
  } catch (e) {
    error.value = e.response?.data?.message || '載入失敗，請稍後再試'
  } finally {
    loading.value = false
  }
}
async function openRepaymentModal(acc) {
  modalAccount.value = acc
  showModal.value = true
  repayments.value = []
  repaymentLoading.value = true
  try {
    const token = localStorage.getItem('customer_token')
    const res = await api.get(`/api/loan-accounts/${acc.accountId}/repayments`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    repayments.value = res.data.data || []
  } catch (e) {
    repayments.value = []
  } finally {
    repaymentLoading.value = false
  }
}

function closeModal() {
  showModal.value = false
  modalAccount.value = null
  repayments.value = []
}

onMounted(loadAccounts)
</script>

<style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/7.0.1/css/all.min.css');

/* ── CSS 變數（與 LoanStatusView 同一套設計語言）── */
.loan-account-page {
  --accent:    #A65A4D;
  --primary:   #5C6B5F;
  --pk:        #3F4A42;
  --bg:        #F5F1EA;
  --surface:   #FDFAF6;
  --surface-2: #EAE4DA;
  --border:    #D6CEC3;
  --ink:       #2B2B2B;
  --muted:     #A89A8E;
  --muted-2:   #6E6259;
  --green:     #4A8C5C;
  --red:       #A65A4D;
  --amber:     #C49A3C;

  min-height: 100vh;
  background: var(--bg);
  font-family: 'Noto Sans TC', sans-serif;
  color: var(--ink);
  border-radius: 10px;
}

/* ── 頁首 ── */
.page-header {
  background: var(--surface);
  border-radius: 10px 10px 0 0;
  border-bottom: 1px solid var(--border);
  padding: 28px 0;
}
.page-header-inner {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 24px;
}
.page-title-block {
  display: flex;
  align-items: center;
  gap: 14px;
}
.page-icon   { font-size: 28px; }
.page-title  { font-size: 22px; font-weight: 700; margin: 0 0 3px; }
.page-subtitle { font-size: 13px; color: var(--muted-2); margin: 0; }

/* ── Body ── */
.page-body {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 24px 64px;
}

/* ── States ── */
.state-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  padding: 64px 0;
  text-align: center;
}
.state-icon  { font-size: 40px; }
.state-text  { font-size: 15px; color: var(--muted-2); }
.btn-retry, .btn-apply {
  padding: 9px 22px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}
.btn-retry  { background: var(--surface-2); color: var(--muted-2); }
.btn-apply  { background: var(--primary); color: #fff; }
.btn-apply:hover { background: var(--pk); }

/* ── Skeleton ── */
.skeleton-list { width: 100%; display: flex; flex-direction: column; gap: 16px; }
.skel-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 22px 24px;
  display: flex; flex-direction: column; gap: 12px;
}
.sk { background: var(--surface-2); border-radius: 6px; animation: shimmer 1.2s ease-in-out infinite alternate; }
.sk-tag   { height: 22px; width: 80px;  border-radius: 20px; }
.sk-line  { height: 14px; width: 100%; }
.sk-short { height: 12px; width: 55%; }
@keyframes shimmer { from { opacity: 0.7; } to { opacity: 0.35; } }

/* ── 篩選列（對齊 LoanStatusView .type-filter-group / .type-pill）── */
.filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 16px;
}
.filter-btn {
  padding: 5px 13px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid var(--border);
  background: var(--surface);
  color: var(--muted-2);
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
  font-family: 'Noto Sans TC', sans-serif;
}
.filter-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}
.filter-btn.active {
  background: var(--primary);
  border-color: var(--primary);
  color: #fff;
}

/* 篩選後無結果 */
.filter-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 40px 0;
  color: var(--muted-2);
  font-size: 14px;
}
.filter-empty i { font-size: 28px; }

/* ── 帳戶卡片 ── */
.account-list { display: flex; flex-direction: column; gap: 18px; }

.account-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  overflow: hidden;
  transition: box-shadow 0.15s;
}
.account-card:hover { box-shadow: 0 4px 20px rgba(92,107,95,0.10); }

/* 卡片頭列 */
.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border);
  flex-wrap: wrap;
  gap: 10px;
}
.card-top-left  { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.card-top-right { display: flex; align-items: center; gap: 10px; }

.account-id {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 12px;
  color: var(--muted-2);
  letter-spacing: 0.04em;
}

.type-badge {
  font-size: 11px;
  padding: 3px 9px;
  border-radius: 5px;
  background: var(--surface-2);
  border: 1px solid var(--border);
  color: var(--muted-2);
}

/* 帳戶狀態標籤 */
.status-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 12px;
  border-radius: 20px;
}
.st-active  { background: rgba(74,140,92,0.12);   color: #1a7a40; }
.st-overdue { background: rgba(166,90,77,0.12);   color: var(--red); }
.st-paidoff { background: rgba(80,80,80,0.08);    color: #555; }

/* 立即繳款按鈕 */
.repay-btn {
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 8px;
  border: 1px solid var(--primary);
  background: var(--primary);
  color: #fff;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
  font-weight: 600;
}
.repay-btn:hover {
  background: var(--pk);
  border-color: var(--pk);
  transform: translateY(-1px);
}

/* 展開按鈕 */
.expand-btn {
  font-size: 12px;
  padding: 5px 12px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--surface-2);
  color: var(--muted-2);
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}
.expand-btn:hover, .expand-btn.expanded {
  border-color: var(--primary);
  color: var(--primary);
  background: rgba(92,107,95,0.08);
}

/* ── 卡片主體（進度 + 重點摘要 + 條件摘要）── */
.card-body {
  display: grid;
  grid-template-columns: 180px 170px minmax(380px, 1fr);
  align-items: center;
  gap: 24px;
  padding: 20px 28px;
  border-bottom: 1px solid var(--border);
  max-width: 1080px;
  margin: 0 auto;
  width: 100%;
}

/* 左：圓形區塊 */
.circle-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
}
.circle-chart-wrap {
  position: relative;
  width: 104px;
  height: 104px;
}
.circle-svg {
  width: 104px;
  height: 104px;
  display: block;
}
.circle-center-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
  pointer-events: none;
}
.cctext-pct {
  font-size: 20px;
  font-weight: 800;
  font-family: 'IBM Plex Mono', monospace;
  line-height: 1;
}
.cctext-paid-label {
  font-size: 11px;
  color: var(--muted-2);
  font-weight: 500;
}
/* 圓圈下方：金額行 */
.circle-principal {
  margin-top: 8px;
  display: flex;
  align-items: baseline;
  gap: 4px;
  font-family: 'IBM Plex Mono', monospace;
}
.circle-principal-remaining {
  font-size: 14px;
  font-weight: 700;
}
.circle-principal-sep {
  font-size: 12px;
  color: var(--muted-2);
}
.circle-principal-total {
  font-size: 12px;
  color: var(--muted-2);
}
.circle-label {
  margin-top: 3px;
  font-size: 10px;
  color: var(--muted);
  letter-spacing: 0.03em;
  text-align: center;
}
.period-progress {
  margin-top: 10px;
  width: 156px;
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.period-progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.period-label {
  font-size: 10px;
  color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
}
.period-val {
  font-size: 11px;
  font-weight: 700;
  font-family: 'IBM Plex Mono', monospace;
}

.payment-focus {
  min-width: 0;
  border-left: 1px solid var(--border);
  padding-left: 24px;
}
.payment-focus.overdue {
  border-left-color: rgba(166,90,77,0.35);
}
.focus-label {
  display: block;
  font-size: 12px;
  color: var(--muted-2);
  margin-bottom: 4px;
}
.focus-amount {
  display: block;
  font-family: 'IBM Plex Mono', monospace;
  font-size: 24px;
  line-height: 1.1;
  color: var(--ink);
}
.focus-date {
  display: flex;
  flex-direction: column;
  gap: 3px;
  margin-top: 14px;
  font-size: 12px;
  color: var(--muted-2);
}
.focus-date strong {
  font-size: 18px;
  color: var(--ink);
}
.overdue-note {
  display: inline-flex;
  margin-top: 10px;
  padding: 5px 9px;
  border-radius: 8px;
  background: rgba(166,90,77,0.10);
  color: var(--red);
  font-size: 12px;
  font-weight: 700;
}

/* 右：資訊摘要格 */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  min-width: 0;
}
.info-tile {
  min-height: 66px;
  padding: 12px 16px;
  border-radius: 8px;
  background: rgba(234,228,218,0.48);
  border: 1px solid rgba(214,206,195,0.68);
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 3px;
}
.rate-tile {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 1px minmax(0, 1fr);
  align-items: center;
  gap: 16px;
  padding-left: 18px;
  padding-right: 18px;
}
.rate-side {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.rate-divider {
  width: 1px;
  height: 42px;
  background: var(--border);
}
.info-row-label {
  font-size: 12px;
  color: var(--muted-2);
  flex-shrink: 0;
}
.info-row-val {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
  text-align: left;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}
.rate-side .info-row-val {
  font-size: 16px;
  line-height: 1.15;
  white-space: nowrap;
}
.info-row-val.mono { font-family: 'IBM Plex Mono', monospace; }
.info-row-sub {
  font-size: 12px;
  font-weight: 400;
  color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
}
.overdue-text { color: var(--red) !important; }

@media (max-width: 960px) {
  .card-body {
    grid-template-columns: 180px 1fr;
    gap: 20px;
  }
  .info-grid {
    grid-column: 1 / -1;
  }
}

@media (max-width: 560px) {
  .card-body {
    grid-template-columns: 1fr;
    padding: 18px;
  }
  .payment-focus {
    border-left: none;
    border-top: 1px solid var(--border);
    padding-left: 0;
    padding-top: 16px;
  }
  .info-grid {
    grid-template-columns: 1fr;
  }
}

/* 進度條 */
.progress-bar-wrap {
  display: block;
  height: 4px;
  background: var(--surface-2);
  border-radius: 2px;
  overflow: hidden;
  width: 100%;
}
.progress-bar-fill {
  display: block;
  height: 100%;
  border-radius: 2px;
  transition: width 0.4s ease;
}
.prog-active  { background: var(--primary); }
.prog-overdue { background: var(--red); }
.prog-done    { background: var(--green); }

.rep-loading {
  text-align: center;
  color: var(--muted-2);
  font-size: 13px;
  padding: 20px 0;
}

/* 還款明細表 */
.rep-table-wrap { overflow-x: auto; }

.rep-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  white-space: nowrap;
}
.rep-table th {
  text-align: right;
  padding: 8px 12px;
  font-family: 'IBM Plex Mono', monospace;
  font-size: 10px;
  letter-spacing: 0.06em;
  color: var(--muted-2);
  border-bottom: 1px solid var(--border);
  background: var(--surface-2);
  font-weight: 600;
}
.rep-table th:first-child { text-align: center; }
.rep-table th:last-child  { text-align: center; }

.rep-table td {
  text-align: right;
  padding: 9px 12px;
  border-bottom: 1px solid var(--border);
  color: var(--ink);
}
.rep-table td:first-child { text-align: center; font-weight: 600; }
.rep-table td:last-child  { text-align: center; }
.rep-table tr:last-child td { border-bottom: none; }

/* 列狀態 */
.row-paid    td { color: var(--muted); }
.row-overdue td { color: var(--red); }
.row-overdue    { background: rgba(166,90,77,0.04); }

.mono  { font-family: 'IBM Plex Mono', monospace; }
.muted { color: var(--muted-2); }

/* 還款狀態標籤 */
.rep-status {
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  display: inline-block;
}
.rs-scheduled { background: rgba(166,140,0,0.10);  color: #7a6000; }
.rs-paid      { background: rgba(74,140,92,0.10);  color: #1a7a40; }
.rs-overdue   { background: rgba(166,90,77,0.12);  color: var(--red); }

.text-right { text-align: right !important; }
.text-center { text-align: center !important; }

.rep-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.rep-title {
  color: var(--muted-2);
  font-size: 12px;
  font-weight: 700;
}
.spin {
  display: inline-block;
  animation: spin 0.9s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ── Modal 遮罩 ── */
.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

/* ── Modal 本體：對齊貸款後端帳戶明細 card ── */
.modal-box {
  background: #ffffff;
  border: 1px solid #dde1de;
  border-radius: 16px;
  width: 100%;
  max-width: 860px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 24px rgba(0,0,0,0.08);
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px 16px;
  border-bottom: 1px solid #dde1de;
  background: #f0f2f0;
  flex-shrink: 0;
}
.modal-title-group {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.modal-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--ink);
}
.modal-subtitle {
  font-size: 12px;
  color: var(--muted-2);
  display: flex;
  align-items: center;
}
.modal-close-btn {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: 1px solid #dde1de;
  background: #ffffff;
  color: var(--muted-2);
  font-size: 13px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  flex-shrink: 0;
}
.modal-close-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: rgba(92,107,95,0.08);
}

.modal-summary {
  display: flex;
  gap: 0;
  flex-shrink: 0;
  border-bottom: 1px solid #dde1de;
  background: #ffffff;
  overflow-x: auto;
}
.summary-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 12px 20px;
  border-right: 1px solid #dde1de;
  min-width: 130px;
}
.summary-item:last-child { border-right: none; }
.summary-label {
  font-size: 10px;
  font-weight: 600;
  color: var(--muted-2);
  letter-spacing: 0.06em;
}
.summary-value {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
}
.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px 24px;
}

.modal-fade-enter-active, .modal-fade-leave-active { transition: all 0.2s ease; }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; }
.modal-fade-enter-from .modal-box, .modal-fade-leave-to .modal-box { transform: scale(0.96) translateY(8px); }
.modal-fade-enter-active .modal-box, .modal-fade-leave-active .modal-box { transition: transform 0.2s ease; }

@media (max-width: 640px) {
  .modal-overlay { padding: 12px; }
  .modal-box { max-height: 90vh; }
  .modal-header { padding: 14px 16px; }
  .modal-body { padding: 16px; }
}
</style>
