<template>
  <div class="user-transactions">
    <h2>交易紀錄查詢</h2>

    <div class="filter-bar">
      <label class="field-shell select-shell">
        <span class="field-label">帳戶</span>
        <select v-model="selectedAccount" class="filter-control" @change="fetchTransactions">
          <option value="">全部帳戶</option>
          <option v-for="account in accounts" :key="account.accountNumber" :value="account.accountNumber">
            {{ account.accountNumber }}（{{ account.currency }}）
          </option>
        </select>
      </label>

      <div class="field-shell date-shell">
        <label>
          <span class="field-label">開始日期</span>
          <input v-model="startDate" class="filter-control" type="date" @change="fetchTransactions" />
        </label>
        <span class="date-separator">→</span>
        <label>
          <span class="field-label">結束日期</span>
          <input v-model="endDate" class="filter-control" type="date" @change="fetchTransactions" />
        </label>
      </div>

      <label class="field-shell select-shell type-shell">
        <span class="field-label">交易類型</span>
        <select v-model="txType" class="filter-control" @change="fetchTransactions">
          <option value="">全部類型</option>
          <option value="TRANSFER">轉帳</option>
          <option value="DEPOSIT">存款</option>
          <option value="WITHDRAW">提款</option>
          <option value="EXCHANGE">換匯</option>
          <option value="REVERSAL">沖正</option>
          <option value="INTEREST">利息</option>
          <option value="CARD_PAYMENT">信用卡繳款</option>
          <option value="CARD_SETTLEMENT">信用卡結算</option>
          <option value="CARD_REWARD">信用卡回饋</option>
          <option value="LOAN_DISBURSEMENT">貸款撥款</option>
          <option value="LOAN_REPAYMENT">貸款還款</option>
        </select>
      </label>
    </div>

    <section class="transactions-panel" aria-label="交易紀錄列表">
      <div class="transactions-table-wrap">
        <table class="transactions-table">
          <thead>
            <tr>
              <th>日期</th>
              <th>交易編號</th>
              <th>帳號</th>
              <th>類型</th>
              <th>方向</th>
              <th class="align-right">金額</th>
              <th class="align-right">交易後餘額</th>
              <th>幣別</th>
              <th>備註</th>
            </tr>
          </thead>
          <tbody v-if="pagedTransactions.length">
            <tr v-for="(record, index) in pagedTransactions" :key="transactionKey(record, index)">
              <td>{{ formatDate(record.createdAt) }}</td>
              <td class="mono-cell">{{ record.referenceId || '-' }}</td>
              <td class="mono-cell">{{ record.accountNumber || '-' }}</td>
              <td>{{ txTypeLabel(record.transactionType) }}</td>
              <td>
                <span class="entry-pill" :class="record.entryType === 'CREDIT' ? 'credit' : 'debit'">
                  {{ record.entryType === 'CREDIT' ? '入帳' : '扣款' }}
                </span>
              </td>
              <td class="amount-cell" :class="record.entryType === 'CREDIT' ? 'credit' : 'debit'">
                {{ record.entryType === 'CREDIT' ? '+' : '-' }}{{ formatNum(record.amount) }}
              </td>
              <td class="align-right">{{ formatNum(record.balanceAfter) }}</td>
              <td>{{ record.currency || '-' }}</td>
              <td class="note-cell">{{ displayNote(record) }}</td>
            </tr>
          </tbody>
        </table>

        <div v-if="loading" class="transactions-loading-state">
          <span class="loading-brush" aria-hidden="true"></span>
          <strong>交易紀錄整理中</strong>
        </div>

        <div v-else-if="transactions.length === 0" class="transactions-empty-state">
          <div class="transactions-empty-mark" aria-hidden="true">
            <svg viewBox="0 0 96 96" role="img" focusable="false">
              <circle cx="48" cy="48" r="36" fill="rgba(232, 226, 216, 0.76)" />
              <path
                d="M25 37c11-7 25-7 42 0 6 3 12 3 17 0"
                fill="none"
                stroke="rgba(61, 70, 63, 0.72)"
                stroke-width="3"
                stroke-linecap="round"
              />
              <rect
                x="28"
                y="35"
                width="40"
                height="48"
                rx="7"
                fill="rgba(255, 249, 239, 0.95)"
                stroke="rgba(198, 188, 174, 0.98)"
                stroke-width="2"
              />
              <path
                d="M39 50h18M39 60h18M39 70h12"
                fill="none"
                stroke="rgba(92, 107, 95, 0.58)"
                stroke-width="3"
                stroke-linecap="round"
              />
            </svg>
          </div>
          <strong>目前沒有交易紀錄</strong>
          <span>完成交易後，明細會依時間整理在這裡。</span>
        </div>
      </div>

      <div v-if="transactions.length > 0" class="transactions-pagination">
        <span>共 {{ transactions.length }} 筆</span>
        <label>
          每頁
          <select v-model.number="pageSize" class="page-size-select">
            <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }}</option>
          </select>
          筆
        </label>
        <button type="button" :disabled="currentPage === 1" @click="currentPage--">上一頁</button>
        <span>{{ currentPage }} / {{ totalPages }}</span>
        <button type="button" :disabled="currentPage === totalPages" @click="currentPage++">下一頁</button>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getMyAccounts, getMyTransactions } from '@/api/customerAccount'

const route = useRoute()
const loading = ref(false)
const accounts = ref([])
const transactions = ref([])
const selectedAccount = ref('')
const startDate = ref('')
const endDate = ref('')
const txType = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const pageSizeOptions = [10, 20, 50]

const totalPages = computed(() => Math.max(1, Math.ceil(transactions.value.length / pageSize.value)))
const pagedTransactions = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return transactions.value.slice(start, start + pageSize.value)
})

watch([transactions, pageSize], () => {
  currentPage.value = 1
})

watch(totalPages, (pages) => {
  if (currentPage.value > pages) currentPage.value = pages
})

onMounted(async () => {
  try {
    const res = await getMyAccounts()
    const currencyPriority = { TWD: 0, USD: 1, JPY: 2, EUR: 3, GBP: 4 }
    accounts.value = (res || []).sort((a, b) => {
      const pA = currencyPriority[a.currency] ?? 99
      const pB = currencyPriority[b.currency] ?? 99
      return pA - pB || a.currency.localeCompare(b.currency)
    })
    if (route.query.account) {
      selectedAccount.value = String(route.query.account)
    }
    await fetchTransactions()
  } catch (e) {
    console.error(e)
  }
})

async function fetchTransactions() {
  loading.value = true
  try {
    const params = {}
    if (selectedAccount.value) params.accountNumber = selectedAccount.value
    if (txType.value) params.transactionType = txType.value
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value

    const res = await getMyTransactions(params)
    transactions.value = Array.isArray(res?.content)
      ? res.content
      : Array.isArray(res)
        ? res
        : []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function transactionKey(record, index) {
  return record.transactionId || record.referenceId || `${record.accountNumber}-${record.createdAt}-${index}`
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').split('.')[0]
}

function txTypeLabel(type) {
  const map = {
    TRANSFER: '轉帳',
    DEPOSIT: '存款',
    WITHDRAW: '提款',
    EXCHANGE: '換匯',
    REVERSAL: '沖正',
    INTEREST: '利息',
    CARD_PAYMENT: '信用卡繳款',
    CARD_SETTLEMENT: '信用卡結算',
    CARD_REWARD: '信用卡回饋',
  }
  map.LOAN_DISBURSEMENT = '貸款撥款'
  map.LOAN_REPAYMENT = '貸款還款'
  return map[type] || type || '-'
}

function displayNote(record) {
  if (!record) return '-'
  if (record.transactionType === 'LOAN_DISBURSEMENT') return '貸款核准撥款'
  if (record.transactionType === 'LOAN_REPAYMENT') return '貸款還款'
  return record.note || '-'
}

function formatNum(value) {
  return Number(value || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>

<style scoped>
.user-transactions {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

h2 {
  margin-bottom: 20px;
  color: var(--text-primary);
  font-family: var(--font-heading);
  font-size: 28px;
  letter-spacing: 0;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.field-shell {
  display: flex;
  align-items: center;
  min-height: 42px;
  color: var(--text-secondary);
  background:
    linear-gradient(180deg, rgba(255, 249, 239, 0.92), rgba(249, 244, 235, 0.78)),
    url('/washi-texture.png');
  background-size: auto, 240px 240px;
  border: 1px solid rgba(198, 188, 174, 0.92);
  border-radius: 8px;
}

.field-label {
  padding: 0 12px;
  color: var(--text-secondary);
  font-size: 12px;
  white-space: nowrap;
}

.select-shell {
  position: relative;
  width: 240px;
}

.type-shell {
  width: 150px;
}

.select-shell::after {
  content: '';
  position: absolute;
  top: 50%;
  right: 14px;
  width: 8px;
  height: 8px;
  border-right: 1.5px solid var(--text-secondary);
  border-bottom: 1.5px solid var(--text-secondary);
  transform: translateY(-66%) rotate(45deg);
  pointer-events: none;
}

.filter-control {
  width: 100%;
  min-width: 0;
  min-height: 40px;
  appearance: none;
  color: var(--text-primary);
  background: transparent;
  border: 0;
  border-left: 1px solid rgba(214, 206, 195, 0.72);
  border-radius: 0;
  padding: 0 36px 0 12px;
  font-family: var(--font-body);
  font-size: 14px;
  letter-spacing: 0;
  outline: none;
}

.filter-control:focus {
  box-shadow: inset 0 0 0 2px rgba(92, 107, 95, 0.18);
}

.date-shell {
  display: flex;
  gap: 0;
}

.date-shell label {
  display: flex;
  align-items: center;
}

.date-shell .filter-control {
  width: 150px;
  padding-right: 10px;
}

.date-separator {
  display: grid;
  place-items: center;
  min-width: 32px;
  color: var(--text-secondary);
  border-left: 1px solid rgba(214, 206, 195, 0.72);
}

.transactions-panel {
  overflow: hidden;
  background:
    linear-gradient(180deg, rgba(255, 249, 239, 0.86), rgba(249, 244, 235, 0.72)),
    url('/washi-texture.png');
  background-size: auto, 300px 300px;
  border: 1px solid rgba(214, 206, 195, 0.84);
  border-radius: 18px;
  box-shadow: 0 14px 34px rgba(63, 74, 66, 0.08);
}

.transactions-table-wrap {
  overflow-x: auto;
}

.transactions-table {
  width: 100%;
  min-width: 1040px;
  border-collapse: collapse;
  color: var(--text-primary);
  font-size: 14px;
}

.transactions-table th {
  padding: 14px 12px;
  text-align: left;
  color: var(--text-primary);
  font-weight: 700;
  background: rgba(245, 241, 234, 0.74);
  border-bottom: 1px solid rgba(214, 206, 195, 0.72);
  white-space: nowrap;
}

.transactions-table td {
  padding: 14px 12px;
  border-bottom: 1px solid rgba(214, 206, 195, 0.42);
  vertical-align: middle;
}

.transactions-table tbody tr:hover {
  background: rgba(92, 107, 95, 0.045);
}

.align-right,
.amount-cell {
  text-align: right;
}

.mono-cell {
  font-family: var(--font-mono);
  color: var(--text-secondary);
}

.note-cell {
  max-width: 210px;
  color: var(--text-secondary);
}

.entry-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 46px;
  min-height: 24px;
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.entry-pill.credit {
  color: var(--primary-dark);
  background: rgba(92, 107, 95, 0.12);
  border: 1px solid rgba(92, 107, 95, 0.28);
}

.entry-pill.debit {
  color: var(--accent);
  background: rgba(166, 90, 77, 0.08);
  border: 1px solid rgba(166, 90, 77, 0.24);
}

.amount-cell {
  font-weight: 700;
}

.amount-cell.credit {
  color: var(--primary-dark);
}

.amount-cell.debit {
  color: var(--accent);
}

.transactions-loading-state,
.transactions-empty-state {
  display: grid;
  justify-items: center;
  gap: 8px;
  min-height: 210px;
  padding: 34px 16px 38px;
  color: var(--text-secondary);
  text-align: center;
  background: linear-gradient(180deg, rgba(255, 249, 239, 0.68), rgba(249, 244, 235, 0.5));
}

.transactions-empty-mark {
  width: 76px;
  height: 76px;
}

.transactions-empty-mark svg {
  display: block;
  width: 100%;
  height: 100%;
}

.transactions-empty-state strong,
.transactions-loading-state strong {
  color: var(--text-primary);
  font-family: var(--font-heading);
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0;
}

.transactions-empty-state span {
  color: var(--text-secondary);
  font-size: 14px;
  letter-spacing: 0;
}

.loading-brush {
  width: 72px;
  height: 18px;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent, rgba(92, 107, 95, 0.34), transparent);
  animation: brushLoading 1.2s ease-in-out infinite;
}

@keyframes brushLoading {
  0%, 100% {
    opacity: 0.38;
    transform: scaleX(0.72);
  }

  50% {
    opacity: 1;
    transform: scaleX(1);
  }
}

.transactions-pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 16px;
  color: var(--text-secondary);
  border-top: 1px solid rgba(214, 206, 195, 0.58);
}

.transactions-pagination button,
.page-size-select {
  min-height: 32px;
  padding: 4px 10px;
  color: var(--primary-dark);
  background: rgba(255, 249, 239, 0.66);
  border: 1px solid rgba(198, 188, 174, 0.88);
  border-radius: 8px;
  cursor: pointer;
}

.transactions-pagination button:disabled {
  color: var(--text-disabled);
  cursor: not-allowed;
}

@media (max-width: 680px) {
  .user-transactions {
    padding: 20px 12px;
  }

  .filter-bar,
  .field-shell,
  .date-shell,
  .date-shell label,
  .select-shell,
  .type-shell {
    width: 100%;
  }

  .date-shell {
    display: grid;
  }

  .date-shell .filter-control {
    width: 100%;
  }

  .date-separator {
    min-height: 34px;
    border-left: 0;
    border-top: 1px solid rgba(214, 206, 195, 0.72);
  }

  .transactions-pagination {
    justify-content: center;
    flex-wrap: wrap;
  }
}
</style>
