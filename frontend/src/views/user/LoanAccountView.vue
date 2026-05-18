<template>
  <div class="loan-account-page">

    <!-- ── 頁首 ── -->
    <div class="page-header">
      <div class="page-header-inner">
        <div class="page-title-block">
          <span class="page-icon">🏦</span>
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
        <span class="state-icon">⚠️</span>
        <div class="state-text">{{ error }}</div>
        <button class="btn-retry" @click="loadAccounts">重新載入</button>
      </div>

      <!-- ── 空狀態 ── -->
      <div v-else-if="accounts.length === 0" class="state-block state-empty">
        <span class="state-icon">📂</span>
        <div class="state-text">目前尚無貸款帳戶</div>
        <button class="btn-apply" @click="$router.push({ name: 'user-loan-apply' })">
          立即申請貸款
        </button>
      </div>

      <!-- ── 帳戶列表 ── -->
      <div v-else class="account-list">
        <div
          v-for="acc in accounts"
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
                v-if="acc.accountStatus === 'ACTIVE'"
                class="repay-btn"
                @click="$router.push({ name: 'user-loan-repayment', query: { accountId: acc.accountId } })"
                title="前往繳款"
              >
                立即繳款
              </button>
              <!-- 展開還款時間表按鈕 -->
              <button
                class="expand-btn"
                :class="{ expanded: expandedId === acc.accountId }"
                @click="toggleRepayments(acc.accountId)"
                :title="expandedId === acc.accountId ? '收合' : '查看還款明細'"
              >
                {{ expandedId === acc.accountId ? '▲ 收合' : '▼ 還款明細' }}
              </button>
            </div>
          </div>

          <!-- ── 主體：圓形圖（左）+ 資訊列（右）── -->
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

            <!-- 右：資訊列 -->
            <div class="info-rows">
              <!-- 貸款條件 -->
              <div class="info-row">
                <span class="info-row-label">貸款期數</span>
                <span class="info-row-val">{{ acc.confirmedPeriod }} 期</span>
              </div>
              <div class="info-row">
                <span class="info-row-label">年利率</span>
                <span class="info-row-val">
                  {{ formatRate(acc.rate) }}
                  <span class="info-row-sub" v-if="acc.rate != null">月利率 {{ formatMonthlyRate(acc.rate) }}</span>
                </span>
              </div>
              <div class="info-row">
                <span class="info-row-label">月繳金額</span>
                <span class="info-row-val">$ {{ formatDecimal(acc.monthlyPayment) }}</span>
              </div>
              <!-- 分隔線 -->
              <div class="info-divider"></div>
              <!-- 還款進度 -->
              <div class="info-row">
                <span class="info-row-label">撥款日</span>
                <span class="info-row-val mono">{{ formatDate(acc.startDate) }}</span>
              </div>
              <div class="info-row">
                <span class="info-row-label">下次繳款日</span>
                <span class="info-row-val mono" :class="{ 'overdue-text': isOverdue(acc) }">
                  {{ acc.nextPaymentDate ? formatDate(acc.nextPaymentDate) : '—' }}
                </span>
              </div>
            </div>

          </div>

          <!-- ── 還款時間表（展開區） ── -->
          <transition name="slide">
            <div v-if="expandedId === acc.accountId" class="repayment-section">

              <!-- 載入還款明細中 -->
              <div v-if="repaymentLoading" class="rep-loading">載入還款明細中…</div>

              <!-- 還款明細表 -->
              <div v-else-if="repayments.length > 0" class="rep-table-wrap">
                <table class="rep-table">
                  <thead>
                    <tr>
                      <th>期</th>
                      <th>應繳日</th>
                      <th>實繳日</th>
                      <th>月繳金額</th>
                      <th>本金</th>
                      <th>利息</th>
                      <th>剩餘本金</th>
                      <th>狀態</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr
                      v-for="rp in repayments"
                      :key="rp.repaymentId"
                      :class="rowClass(rp.repaymentStatus)"
                    >
                      <td class="mono">{{ rp.periodIndex }}</td>
                      <td class="mono">{{ formatDate(rp.scheduledDate) }}</td>
                      <td class="mono">{{ rp.paidDate ? formatDate(rp.paidDate) : '—' }}</td>
                      <td class="mono">{{ formatDecimal(rp.totalAmount) }}</td>
                      <td class="mono">{{ formatDecimal(rp.principalPortion) }}</td>
                      <td class="mono muted">{{ formatDecimal(rp.interestPortion) }}</td>
                      <td class="mono">{{ formatDecimal(rp.remainingAfter) }}</td>
                      <td>
                        <span class="rep-status" :class="repStatusClass(rp.repaymentStatus)">
                          {{ repStatusLabel(rp.repaymentStatus) }}
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div v-else class="rep-loading">尚無還款明細資料</div>
            </div>
          </transition>

        </div>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/api/axios'

// ── 狀態變數 ──
const accounts       = ref([])   // 貸款帳戶清單
const loading        = ref(false)
const error          = ref('')
const expandedId     = ref(null) // 目前展開的帳戶 ID
const repayments     = ref([])   // 展開中的帳戶之還款明細
const repaymentLoading = ref(false)

// ── 貸款類型對照 ──
const LOAN_TYPE_MAP = {
  PERSONAL: '個人信貸',
  CAR:      '汽車貸款',
  MOTOR:    '機車貸款',
  STUDENT:  '學貸',
  BUSINESS: '創業貸款',
  HOUSE:    '房屋貸款',
  LAND:     '土地貸款',
}

// ── 帳戶狀態對照 ──
const ACCOUNT_STATUS = {
  ACTIVE:   { label: '還款中',   cls: 'st-active'  },
  OVERDUE:  { label: '逾期',     cls: 'st-overdue' },
  PAID_OFF: { label: '已結清',   cls: 'st-paidoff' },
}

// ── 還款狀態對照 ──
const REPAYMENT_STATUS = {
  SCHEDULED: { label: '待繳',   cls: 'rs-scheduled' },
  PAID:      { label: '已繳',   cls: 'rs-paid'      },
  OVERDUE:   { label: '逾期',   cls: 'rs-overdue'   },
}

// ── helper：帳戶狀態 ──
function statusLabel(st) { return ACCOUNT_STATUS[st]?.label || st }
function statusClass(st) { return ACCOUNT_STATUS[st]?.cls   || '' }

// ── helper：還款狀態 ──
function repStatusLabel(st) { return REPAYMENT_STATUS[st]?.label || st }
function repStatusClass(st) { return REPAYMENT_STATUS[st]?.cls   || '' }

// ── helper：還款進度 ──
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

// ── helper：圓形進度圖 ──
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

// ── helper：逾期判斷（用於下次繳款日變紅）──
function isOverdue(acc) {
  return acc.accountStatus === 'OVERDUE'
}

// ── 表格列樣式（已繳列淡化）──
function rowClass(st) {
  if (st === 'PAID')    return 'row-paid'
  if (st === 'OVERDUE') return 'row-overdue'
  return ''
}

// ── 格式化工具 ──
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

// ── API：載入我的貸款帳戶 ──
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

// ── API：切換展開 / 收合還款明細 ──
async function toggleRepayments(accountId) {
  // 已展開 → 收合
  if (expandedId.value === accountId) {
    expandedId.value = null
    repayments.value = []
    return
  }

  // 展開新帳戶 → 先清空舊資料，再打 API
  expandedId.value = accountId
  repayments.value = []
  repaymentLoading.value = true
  try {
    const token = localStorage.getItem('customer_token')
    const res = await api.get(`/api/loan-accounts/${accountId}/repayments`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    repayments.value = res.data.data || []
  } catch (e) {
    repayments.value = []
  } finally {
    repaymentLoading.value = false
  }
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
}

/* ── 頁首 ── */
.page-header {
  background: var(--surface);
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

/* ── 帳戶卡片 ── */
.account-list { display: flex; flex-direction: column; gap: 20px; }

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
  padding: 16px 20px;
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
  background: var(--primary-dark);
  border-color: var(--primary-dark);
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

/* ── 卡片主體（左圓圖 + 右資訊列）── */
.card-body {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 20px 20px;
  border-bottom: 1px solid var(--border);
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
  width: 120px;
  height: 120px;
}
.circle-svg {
  width: 120px;
  height: 120px;
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
  font-size: 22px;
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
  margin-top: 10px;
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
  margin-top: 12px;
  width: 120px;
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

/* 右：資訊列 */
.info-rows {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 0;
}
.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.info-row-label {
  font-size: 12px;
  color: var(--muted-2);
  flex-shrink: 0;
}
.info-row-val {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink);
  text-align: right;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}
.info-row-val.mono { font-family: 'IBM Plex Mono', monospace; }
.info-row-sub {
  font-size: 10px;
  font-weight: 400;
  color: var(--muted-2);
  font-family: 'IBM Plex Mono', monospace;
}
.info-divider {
  border-top: 1px dashed var(--border);
  margin: 2px 0;
}
.overdue-text { color: var(--red) !important; }

@media (max-width: 520px) {
  .card-body { flex-direction: column; align-items: center; }
  .info-rows  { width: 100%; }
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

/* ── 還款時間表展開區 ── */
.repayment-section { padding: 16px 20px 20px; }

.rep-loading {
  text-align: center;
  color: var(--muted-2);
  font-size: 13px;
  padding: 20px 0;
}

/* 展開動畫 */
.slide-enter-active, .slide-leave-active { transition: all 0.25s ease; }
.slide-enter-from, .slide-leave-to       { opacity: 0; transform: translateY(-8px); }

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
</style>
