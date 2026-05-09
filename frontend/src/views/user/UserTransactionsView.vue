<template>
  <div class="user-transactions">
    <h2>交易紀錄查詢</h2>

    <!-- 篩選列 -->
    <div class="filter-bar">
      <a-select v-model:value="selectedAccount" placeholder="選擇帳戶" style="width: 240px" allowClear @change="fetchTransactions">
        <a-select-option v-for="a in accounts" :key="a.accountNumber" :value="a.accountNumber">
          {{ a.accountNumber }} ({{ a.currency }})
        </a-select-option>
      </a-select>
      <a-range-picker v-model:value="dateRange" style="width: 260px" @change="fetchTransactions" />
      <a-select v-model:value="txType" placeholder="交易類型" style="width: 140px" allowClear @change="fetchTransactions">
        <a-select-option value="TRANSFER">轉帳</a-select-option>
        <a-select-option value="DEPOSIT">存款</a-select-option>
        <a-select-option value="WITHDRAW">提款</a-select-option>
        <a-select-option value="REVERSAL">沖正</a-select-option>
        <a-select-option value="INTEREST">利息</a-select-option>
      </a-select>
    </div>

    <a-table
      :columns="columns"
      :data-source="transactions"
      :loading="loading"
      :pagination="{ pageSize: 20, showSizeChanger: true, showTotal: (t) => `共 ${t} 筆` }"
      row-key="transactionId"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'entryType'">
          <a-tag :color="record.entryType === 'CREDIT' ? 'green' : 'red'">
            {{ record.entryType === 'CREDIT' ? '入帳' : '扣款' }}
          </a-tag>
        </template>
        <template v-if="column.key === 'amount'">
          <span :style="{ color: record.entryType === 'CREDIT' ? '#52c41a' : '#ff4d4f', fontWeight: 600 }">
            {{ record.entryType === 'CREDIT' ? '+' : '-' }}{{ formatNum(record.amount) }}
          </span>
        </template>
        <template v-if="column.key === 'balanceAfter'">
          {{ formatNum(record.balanceAfter) }}
        </template>
        <template v-if="column.key === 'transactionType'">
          {{ txTypeLabel(record.transactionType) }}
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getMyAccounts, getMyTransactions } from '@/api/customerAccount'

const route = useRoute()
const loading = ref(false)
const accounts = ref([])
const transactions = ref([])
const selectedAccount = ref(undefined)
const dateRange = ref(null)
const txType = ref(undefined)

const columns = [
  { title: '日期', dataIndex: 'createdAt', key: 'createdAt', width: 160, sorter: (a, b) => a.createdAt?.localeCompare(b.createdAt) },
  { title: '交易編號', dataIndex: 'referenceId', key: 'referenceId', width: 220, ellipsis: true },
  { title: '帳號', dataIndex: 'accountNumber', key: 'accountNumber', width: 140 },
  { title: '類型', dataIndex: 'transactionType', key: 'transactionType', width: 80 },
  { title: '方向', key: 'entryType', width: 70 },
  { title: '金額', key: 'amount', width: 120, align: 'right', sorter: (a, b) => a.amount - b.amount },
  { title: '交易後餘額', key: 'balanceAfter', width: 120, align: 'right' },
  { title: '幣別', dataIndex: 'currency', key: 'currency', width: 60 },
  { title: '備註', dataIndex: 'note', key: 'note', ellipsis: true },
]

onMounted(async () => {
  try {
    const res = await getMyAccounts()
    const currencyPriority = { TWD: 0, USD: 1, JPY: 2, EUR: 3, GBP: 4 }
    accounts.value = res.sort((a, b) => {
      const pA = currencyPriority[a.currency] ?? 99
      const pB = currencyPriority[b.currency] ?? 99
      return pA - pB || a.currency.localeCompare(b.currency)
    })
    if (route.query.account) {
      selectedAccount.value = route.query.account
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
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0].format('YYYY-MM-DD')
      params.endDate = dateRange.value[1].format('YYYY-MM-DD')
    }
    const res = await getMyTransactions(params)
    // Backend returns PageResponse { content, page, size, totalElements }
    transactions.value = res?.content || res || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function txTypeLabel(t) {
  const map = { TRANSFER: '轉帳', DEPOSIT: '存款', WITHDRAW: '提款', REVERSAL: '沖正', INTEREST: '利息' }
  return map[t] || t
}

function formatNum(v) {
  return Number(v || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>

<style scoped>
.user-transactions { max-width: 1200px; margin: 0 auto; padding: 24px; }
h2 { margin-bottom: 20px; color: #1a1a2e; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
</style>
