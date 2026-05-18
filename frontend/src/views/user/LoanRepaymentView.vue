<template>
  <div class="repayment-page">

    <!-- ── 頁首 ── -->
    <div class="page-header">
      <div class="page-header-inner">
        <div class="page-title-block">
          <span class="page-icon">💳</span>
          <div>
            <h1 class="page-title">貸款還款</h1>
            <p class="page-subtitle">選擇貸款帳戶與扣款帳戶進行繳款</p>
          </div>
        </div>
      </div>
    </div>

    <div class="page-body">

      <!-- ── 步驟一：選貸款帳戶 ── -->
      <section class="section-card">
        <h2 class="section-title">① 選擇貸款帳戶</h2>

        <div v-if="loanLoading" class="state-inline">載入中…</div>
        <div v-else-if="loanAccounts.length === 0" class="state-inline muted">
          目前無貸款帳戶，<a class="link" @click="$router.push({ name: 'user-loan-apply' })">立即申請</a>
        </div>
        <div v-else class="loan-list">
          <div
            v-for="acc in loanAccounts"
            :key="acc.accountId"
            class="loan-option"
            :class="{ selected: selectedLoan?.accountId === acc.accountId,
                       disabled: acc.accountStatus === 'PAID_OFF' }"
            @click="acc.accountStatus !== 'PAID_OFF' && selectLoan(acc)"
          >
            <div class="lo-left">
              <span class="lo-id">{{ acc.accountId }}</span>
              <span class="lo-type">{{ LOAN_TYPE_MAP[acc.applyType] || acc.applyType }}</span>
              <span class="lo-status" :class="statusClass(acc.accountStatus)">
                {{ statusLabel(acc.accountStatus) }}
              </span>
            </div>
            <div class="lo-right">
              <div class="lo-meta">
                <span class="lo-label">剩餘本金</span>
                <span class="lo-val accent">$ {{ fmt(acc.remainingPrincipal) }}</span>
              </div>
              <div class="lo-meta">
                <span class="lo-label">月繳金額</span>
                <span class="lo-val">$ {{ fmt(acc.monthlyPayment) }}</span>
              </div>
              <div class="lo-meta">
                <span class="lo-label">下次繳款日</span>
                <span class="lo-val mono" :class="{ 'overdue': isOverdue(acc) }">
                  {{ acc.nextPaymentDate ? fmtDate(acc.nextPaymentDate) : '—' }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- ── 步驟二：還款表單（選定貸款後展開）── -->
      <transition name="slide">
        <section v-if="selectedLoan" class="section-card">
          <h2 class="section-title">② 填寫還款資訊</h2>

          <!-- 選扣款帳戶 -->
          <div class="form-group">
            <label class="form-label">扣款帳戶 <span class="req">*</span></label>
            <div v-if="debitLoading" class="state-inline">載入可用帳戶…</div>
            <div v-else-if="debitAccounts.length === 0" class="state-inline muted">
              無可用台幣活存帳戶
            </div>
            <div v-else class="debit-list">
              <label
                v-for="acc in debitAccounts"
                :key="acc.accountNumber"
                class="debit-option"
                :class="{ selected: form.fromAccountNumber === acc.accountNumber }"
              >
                <input
                  type="radio"
                  :value="acc.accountNumber"
                  v-model="form.fromAccountNumber"
                  class="debit-radio"
                />
                <div class="debit-body">
                  <span class="debit-num mono">{{ acc.accountNumber }}</span>
                  <span class="debit-bal">餘額 $ {{ fmt(acc.balance) }}</span>
                </div>
              </label>
            </div>
          </div>

          <!-- 還款金額 -->
          <div class="form-group">
            <label class="form-label">還款金額 <span class="req">*</span></label>
            <div class="amount-row">
              <input
                type="number"
                v-model.number="form.amount"
                class="form-input"
                min="1"
                :max="selectedLoan.remainingPrincipal"
                readonly
                placeholder="輸入金額"
              />
              <button class="btn-preset" @click="fillCurrentRepaymentAmount">
                填入月繳金額
              </button>
            </div>
            <p class="form-hint">
              月繳金額 <strong>$ {{ fmt(selectedLoan.monthlyPayment) }}</strong>；
              剩餘本金 <strong>$ {{ fmt(selectedLoan.remainingPrincipal) }}</strong>
            </p>
          </div>

          <!-- 備註 -->
          <div class="form-group">
            <label class="form-label">備註（選填）</label>
            <input
              type="text"
              v-model="form.note"
              class="form-input"
              placeholder="例：2025年5月還款"
              maxlength="100"
            />
          </div>

          <!-- 送出 -->
          <div class="form-actions">
            <button
              class="btn-submit"
              :disabled="!canSubmit || submitting"
              @click="submitRepayment"
            >
              {{ submitting ? '處理中…' : '確認還款' }}
            </button>
            <button class="btn-cancel" @click="reset">取消</button>
          </div>

          <!-- 錯誤訊息 -->
          <div v-if="submitError" class="alert alert-error">{{ submitError }}</div>
        </section>
      </transition>

      <!-- ── 成功回饋 ── -->
      <transition name="slide">
        <section v-if="lastResult" class="section-card result-card">
          <div class="result-icon">✅</div>
          <div class="result-body">
            <div class="result-title">還款成功</div>
            <div class="result-meta">
              <span>參考號碼</span>
              <span class="mono">{{ lastResult.referenceId }}</span>
            </div>
            <div class="result-meta">
              <span>還款金額</span>
              <span class="accent">$ {{ fmt(lastResult.amount) }}</span>
            </div>
            <div class="result-meta">
              <span>扣款帳戶餘額</span>
              <span>$ {{ fmt(lastResult.sourceAccountBalance) }}</span>
            </div>
          </div>
          <button class="btn-reload" @click="afterSuccess">繼續還款 / 查看明細</button>
        </section>
      </transition>

      <!-- ── 還款紀錄 ── -->
      <section class="section-card" v-if="selectedLoan">
        <div class="section-title-row">
          <h2 class="section-title">還款紀錄</h2>
          <button class="btn-refresh" @click="loadHistory" :disabled="historyLoading">
            {{ historyLoading ? '載入中…' : '↻ 重新整理' }}
          </button>
        </div>

        <div v-if="historyLoading" class="state-inline">載入中…</div>

        <div v-else-if="history.length === 0" class="state-inline muted">尚無還款紀錄</div>

        <div v-else class="history-table-wrap">
          <table class="history-table">
            <thead>
              <tr>
                <th>日期時間</th>
                <th>參考號碼</th>
                <th>金額</th>
                <th>扣款帳戶</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="rec in history" :key="rec.transactionId">
                <td class="mono">{{ fmtDateTime(rec.createdAt) }}</td>
                <td class="mono small">{{ rec.referenceId }}</td>
                <td class="accent mono">$ {{ fmt(rec.amount) }}</td>
                <td class="mono">{{ rec.counterpartAccount }}</td>
              </tr>
            </tbody>
          </table>
          <!-- 分頁 -->
          <div class="pagination" v-if="totalPages > 1">
            <button :disabled="currentPage === 0" @click="gotoPage(currentPage - 1)">‹</button>
            <span>{{ currentPage + 1 }} / {{ totalPages }}</span>
            <button :disabled="currentPage >= totalPages - 1" @click="gotoPage(currentPage + 1)">›</button>
          </div>
        </div>
      </section>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/api/axios'

// ── 對照表 ──
const LOAN_TYPE_MAP = {
  PERSONAL: '個人信貸', CAR: '汽車貸款', MOTOR: '機車貸款',
  STUDENT: '學貸',     BUSINESS: '創業貸款', HOUSE: '房屋貸款', LAND: '土地貸款',
}
const STATUS_MAP = {
  ACTIVE:   { label: '還款中', cls: 'st-active'  },
  OVERDUE:  { label: '逾期',   cls: 'st-overdue' },
  PAID_OFF: { label: '已結清', cls: 'st-paidoff' },
}

// ── 狀態 ──
const loanAccounts  = ref([])
const loanLoading   = ref(false)
const selectedLoan  = ref(null)
const currentRepayment = ref(null)

const debitAccounts = ref([])
const debitLoading  = ref(false)

const form = ref({ fromAccountNumber: '', amount: null, note: '' })
const submitting  = ref(false)
const submitError = ref('')
const lastResult  = ref(null)

const history        = ref([])
const historyLoading = ref(false)
const currentPage    = ref(0)
const totalPages     = ref(0)

// ── helpers ──
const token = () => localStorage.getItem('customer_token')
const auth  = () => ({ headers: { Authorization: `Bearer ${token()}` } })

function statusLabel(st) { return STATUS_MAP[st]?.label || st }
function statusClass(st) { return STATUS_MAP[st]?.cls   || '' }
function isOverdue(acc)  { return acc.accountStatus === 'OVERDUE' }

function fmt(n) {
  return n != null
    ? Number(n).toLocaleString('zh-TW', { minimumFractionDigits: 0, maximumFractionDigits: 0 })
    : '—'
}
function fmtDate(d)     { return d ? String(d).substring(0, 10) : '—' }
function fmtDateTime(d) {
  if (!d) return '—'
  return new Date(d).toLocaleString('zh-TW', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit',
  })
}

// ── 可送出條件 ──
const canSubmit = computed(() =>
  form.value.fromAccountNumber &&
  selectedLoan.value?.accountNumber &&
  form.value.amount > 0 &&
  selectedLoan.value &&
  !submitting.value
)

// ── API：貸款帳戶 ──
async function loadLoanAccounts() {
  loanLoading.value = true
  try {
    const res = await api.get('/api/loan-accounts/my', auth())
    loanAccounts.value = res.data.data || []
  } catch { loanAccounts.value = [] }
  finally { loanLoading.value = false }
}

// ── API：可扣款帳戶 ──
async function loadDebitAccounts() {
  debitLoading.value = true
  debitAccounts.value = []
  form.value.fromAccountNumber = ''
  try {
    const res = await api.get('/api/customer/loan-repayments/debit-accounts', auth())
    debitAccounts.value = res.data.data || []
    // 預選第一筆
    if (debitAccounts.value.length > 0) {
      form.value.fromAccountNumber = debitAccounts.value[0].accountNumber
    }
  } catch { debitAccounts.value = [] }
  finally { debitLoading.value = false }
}

// ── 選取貸款 ──
async function selectLoan(acc) {
  selectedLoan.value = acc
  // 預填月繳金額
  form.value.amount = acc.monthlyPayment ? Math.round(Number(acc.monthlyPayment)) : null
  form.value.note   = ''
  submitError.value = ''
  lastResult.value  = null
  currentRepayment.value = null
  await loadCurrentRepaymentAmount()
  loadDebitAccounts()
  loadHistory()
}

async function loadCurrentRepaymentAmount() {
  if (!selectedLoan.value) return
  try {
    const res = await api.get(`/api/loan-accounts/${selectedLoan.value.accountId}/repayments`, auth())
    const repayments = res.data.data || []
    currentRepayment.value = repayments
      .filter(rp => rp.repaymentStatus === 'SCHEDULED' || rp.repaymentStatus === 'OVERDUE')
      .sort((a, b) => Number(a.periodIndex) - Number(b.periodIndex))[0] || null
    if (currentRepayment.value?.totalAmount != null) {
      form.value.amount = Math.round(Number(currentRepayment.value.totalAmount))
    }
  } catch {
    currentRepayment.value = null
  }
}

function fillCurrentRepaymentAmount() {
  if (currentRepayment.value?.totalAmount != null) {
    form.value.amount = Math.round(Number(currentRepayment.value.totalAmount))
  }
}

// ── API：送出還款 ──
async function submitRepayment() {
  submitError.value = ''
  submitting.value  = true
  try {
    const body = {
      applicationId:     selectedLoan.value.applicationId,   // 讓 Loan 模組同步更新進度
      loanAccountNumber: selectedLoan.value.accountNumber,
      fromAccountNumber: form.value.fromAccountNumber,
      amount:            form.value.amount,
      note:              form.value.note || null,
    }
    const res = await api.post('/api/customer/loan-repayments', body, auth())
    lastResult.value = res.data.data
    // 更新貸款帳戶資料（剩餘本金）
    await loadLoanAccounts()
    const updated = loanAccounts.value.find(a => a.accountId === selectedLoan.value.accountId)
    if (updated) selectedLoan.value = updated
    await loadHistory()
  } catch (e) {
    submitError.value = e.response?.data?.message || '還款失敗，請確認輸入資料或稍後重試'
  } finally {
    submitting.value = false
  }
}

// ── 還款成功後重置表單 ──
function afterSuccess() {
  lastResult.value  = null
  loadCurrentRepaymentAmount()
  form.value.note   = ''
}

// ── 取消 / 重置 ──
function reset() {
  selectedLoan.value = null
  currentRepayment.value = null
  debitAccounts.value = []
  form.value = { fromAccountNumber: '', amount: null, note: '' }
  submitError.value = ''
  lastResult.value  = null
  history.value     = []
}

// ── API：還款紀錄 ──
async function loadHistory(page = 0) {
  if (!selectedLoan.value) return
  historyLoading.value = true
  try {
    const params = {
      loanAccountNumber: selectedLoan.value.accountNumber,
      page,
      size: 10,
    }
    const res = await api.get('/api/customer/loan-repayments', { ...auth(), params })
    const data = res.data.data
    history.value    = data?.content || []
    currentPage.value = data?.page ?? 0
    totalPages.value  = Math.ceil((data?.total ?? 0) / 10)
  } catch { history.value = [] }
  finally { historyLoading.value = false }
}

function gotoPage(page) {
  currentPage.value = page
  loadHistory(page)
}

const route = useRoute()

onMounted(async () => {
  await loadLoanAccounts()
  // 若從 LoanAccountView 帶入 accountId query，自動預選該帳戶
  const preselect = route.query.accountId
  if (preselect) {
    const target = loanAccounts.value.find(a => a.accountId === preselect)
    if (target) selectLoan(target)
  }
})
</script>

<style scoped>
@import url('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/7.0.1/css/all.min.css');

/* ── CSS 變數 ── */
.repayment-page {
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
  max-width: 800px;
  margin: 0 auto;
  padding: 0 24px;
}
.page-title-block { display: flex; align-items: center; gap: 14px; }
.page-icon   { font-size: 28px; }
.page-title  { font-size: 22px; font-weight: 700; margin: 0 0 3px; }
.page-subtitle { font-size: 13px; color: var(--muted-2); margin: 0; }

/* ── Body ── */
.page-body {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 24px 64px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ── 區塊卡片 ── */
.section-card {
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 24px;
}
.section-title {
  font-size: 14px;
  font-weight: 700;
  color: var(--primary);
  margin: 0 0 18px;
  letter-spacing: 0.02em;
}
.section-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.section-title-row .section-title { margin: 0; }

/* ── 狀態文字 ── */
.state-inline {
  font-size: 13px;
  color: var(--muted-2);
  padding: 12px 0;
}
.muted { color: var(--muted); }
.link  { color: var(--primary); cursor: pointer; text-decoration: underline; }

/* ── 貸款選項列表 ── */
.loan-list { display: flex; flex-direction: column; gap: 10px; }

.loan-option {
  border: 1.5px solid var(--border);
  border-radius: 10px;
  padding: 14px 18px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
  transition: all 0.15s;
  flex-wrap: wrap;
  gap: 12px;
}
.loan-option:hover { border-color: var(--primary); background: rgba(92,107,95,0.04); }
.loan-option.selected {
  border-color: var(--primary);
  background: rgba(92,107,95,0.07);
  box-shadow: 0 0 0 3px rgba(92,107,95,0.12);
}
.loan-option.disabled { opacity: 0.5; cursor: not-allowed; }

.lo-left  { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.lo-right { display: flex; gap: 20px; flex-wrap: wrap; }
.lo-id    { font-family: 'IBM Plex Mono', monospace; font-size: 12px; color: var(--muted-2); }
.lo-type  {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 5px;
  background: var(--surface-2);
  border: 1px solid var(--border);
  color: var(--muted-2);
}
.lo-status { font-size: 11px; font-weight: 600; padding: 2px 10px; border-radius: 20px; }
.st-active  { background: rgba(74,140,92,0.12);  color: #1a7a40; }
.st-overdue { background: rgba(166,90,77,0.12);  color: var(--red); }
.st-paidoff { background: rgba(80,80,80,0.08);   color: #555; }

.lo-meta { display: flex; flex-direction: column; align-items: flex-end; gap: 2px; }
.lo-label { font-size: 10px; color: var(--muted-2); font-family: 'IBM Plex Mono', monospace; }
.lo-val   { font-size: 13px; font-weight: 600; }
.lo-val.accent { color: var(--primary); font-family: 'IBM Plex Mono', monospace; }
.overdue  { color: var(--red); }

/* ── 表單 ── */
.form-group { display: flex; flex-direction: column; gap: 8px; margin-bottom: 18px; }
.form-label { font-size: 13px; font-weight: 600; color: var(--ink); }
.req { color: var(--red); margin-left: 2px; }
.form-input {
  padding: 10px 14px;
  border: 1.5px solid var(--border);
  border-radius: 9px;
  font-size: 14px;
  background: var(--surface);
  color: var(--ink);
  width: 100%;
  box-sizing: border-box;
  transition: border-color 0.15s;
}
.form-input:focus { outline: none; border-color: var(--primary); }
.form-hint { font-size: 12px; color: var(--muted-2); margin: 0; }

.amount-row { display: flex; gap: 10px; align-items: stretch; }
.amount-row .form-input { flex: 1; }
.btn-preset {
  padding: 0 14px;
  border: 1.5px solid var(--border);
  border-radius: 9px;
  background: var(--surface-2);
  color: var(--muted-2);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s;
}
.btn-preset:hover { border-color: var(--primary); color: var(--primary); }

/* ── 扣款帳戶 ── */
.debit-list { display: flex; flex-direction: column; gap: 8px; }
.debit-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1.5px solid var(--border);
  border-radius: 9px;
  cursor: pointer;
  transition: all 0.15s;
}
.debit-option:hover { border-color: var(--primary); }
.debit-option.selected { border-color: var(--primary); background: rgba(92,107,95,0.06); }
.debit-radio { accent-color: var(--primary); width: 16px; height: 16px; flex-shrink: 0; }
.debit-body  { display: flex; flex-direction: column; gap: 2px; }
.debit-num   { font-family: 'IBM Plex Mono', monospace; font-size: 13px; font-weight: 600; }
.debit-bal   { font-size: 12px; color: var(--muted-2); }

/* ── 表單操作 ── */
.form-actions { display: flex; gap: 12px; margin-top: 4px; }
.btn-submit {
  padding: 11px 28px;
  background: var(--primary);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s;
}
.btn-submit:hover:not(:disabled) { background: var(--pk); }
.btn-submit:disabled { opacity: 0.5; cursor: not-allowed; }
.btn-cancel {
  padding: 11px 20px;
  background: transparent;
  border: 1.5px solid var(--border);
  border-radius: 10px;
  font-size: 14px;
  color: var(--muted-2);
  cursor: pointer;
}
.btn-cancel:hover { border-color: var(--muted); }

/* ── 錯誤提示 ── */
.alert { padding: 12px 16px; border-radius: 8px; font-size: 13px; margin-top: 14px; }
.alert-error { background: rgba(166,90,77,0.10); color: var(--red); border: 1px solid rgba(166,90,77,0.25); }

/* ── 成功回饋 ── */
.result-card {
  display: flex;
  align-items: center;
  gap: 20px;
  border-color: rgba(74,140,92,0.35);
  background: rgba(74,140,92,0.05);
  flex-wrap: wrap;
}
.result-icon  { font-size: 32px; flex-shrink: 0; }
.result-body  { flex: 1; display: flex; flex-direction: column; gap: 6px; }
.result-title { font-size: 15px; font-weight: 700; color: var(--green); }
.result-meta  {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  border-bottom: 1px solid var(--border);
  padding-bottom: 4px;
}
.result-meta .accent { color: var(--primary); font-family: 'IBM Plex Mono', monospace; font-weight: 600; }
.btn-reload {
  padding: 9px 18px;
  background: var(--green);
  color: #fff;
  border: none;
  border-radius: 9px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}
.btn-reload:hover { opacity: 0.85; }

/* ── 還款紀錄 ── */
.btn-refresh {
  padding: 5px 14px;
  font-size: 12px;
  border: 1px solid var(--border);
  border-radius: 7px;
  background: var(--surface-2);
  color: var(--muted-2);
  cursor: pointer;
}
.btn-refresh:hover { border-color: var(--primary); color: var(--primary); }

.history-table-wrap { overflow-x: auto; }
.history-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  white-space: nowrap;
}
.history-table th {
  text-align: left;
  padding: 8px 12px;
  font-size: 11px;
  letter-spacing: 0.05em;
  color: var(--muted-2);
  border-bottom: 1px solid var(--border);
  background: var(--surface-2);
  font-weight: 600;
}
.history-table td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--border);
}
.history-table tr:last-child td { border-bottom: none; }
.history-table .accent { color: var(--primary); font-weight: 600; }
.history-table .small  { font-size: 11px; }

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 14px 0 0;
  font-size: 13px;
  color: var(--muted-2);
}
.pagination button {
  padding: 4px 12px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--surface);
  cursor: pointer;
  font-size: 14px;
}
.pagination button:disabled { opacity: 0.4; cursor: not-allowed; }

/* ── 通用 ── */
.mono { font-family: 'IBM Plex Mono', monospace; }
.accent { color: var(--primary); }

/* ── 動畫 ── */
.slide-enter-active, .slide-leave-active { transition: all 0.25s ease; }
.slide-enter-from, .slide-leave-to       { opacity: 0; transform: translateY(-10px); }
</style>
