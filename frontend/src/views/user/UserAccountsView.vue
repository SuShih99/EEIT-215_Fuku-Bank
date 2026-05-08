<template>
  <div class="user-accounts">
    <h2>我的帳戶總覽</h2>

    <a-spin :spinning="loading">
      <div v-if="accounts.length === 0 && !loading" class="empty-hint">
        <a-empty description="尚無帳戶，請先完成開戶申請" />
      </div>

      <!-- 幣別分類 Tabs -->
      <a-tabs v-model:activeKey="activeCurrency" v-if="accounts.length > 0">
        <a-tab-pane key="ALL" tab="全部" />
        <a-tab-pane v-for="c in currencies" :key="c" :tab="c" />
      </a-tabs>

      <div class="account-cards">
        <div v-for="acct in filteredAccounts" :key="acct.accountNumber" class="account-card"
             :class="[`currency-${acct.currency}`, `status-${acct.status?.toLowerCase()}`]">
          <div class="card-header">
            <span class="acct-type">{{ typeLabel(acct.accountType) }}</span>
            <a-tag :color="statusColor(acct.status)">{{ acct.status }}</a-tag>
          </div>
          <div class="card-body">
            <div class="acct-number">{{ acct.accountNumber }}</div>
            <div class="acct-balance">
              <span class="currency-badge">{{ acct.currency }}</span>
              <span class="balance-amount">{{ formatBalance(acct.balance, acct.currency) }}</span>
            </div>
          </div>
          <div class="card-footer">
            <a-button size="small" @click="viewTransactions(acct.accountNumber)">交易紀錄</a-button>
            <a-button size="small" type="primary" @click="goTransfer(acct.accountNumber)" v-if="acct.status === 'ACTIVE' && acct.accountType !== 'LOAN'">轉帳</a-button>
          </div>
        </div>
      </div>
    </a-spin>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyAccounts } from '@/api/customerAccount'

const router = useRouter()
const loading = ref(false)
const accounts = ref([])
const activeCurrency = ref('ALL')

const currencies = computed(() => {
  const set = new Set(accounts.value.map(a => a.currency))
  return [...set].sort()
})

const filteredAccounts = computed(() => {
  if (activeCurrency.value === 'ALL') return accounts.value
  return accounts.value.filter(a => a.currency === activeCurrency.value)
})

onMounted(async () => {
  loading.value = true
  try {
    const res = await getMyAccounts()
    accounts.value = res
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
})

function typeLabel(type) {
  const map = { CHECKING: '活期存款', SAVINGS: '儲蓄存款', TIME_DEPOSIT: '定期存款', LOAN: '貸款帳戶' }
  return map[type] || type
}

function statusColor(s) {
  const map = { ACTIVE: 'green', FROZEN: 'blue', DORMANT: 'orange', CLOSED: 'red', PENDING: 'default' }
  return map[s] || 'default'
}

function formatBalance(val, currency) {
  const num = Number(val) || 0
  if (currency === 'JPY') return num.toLocaleString('ja-JP', { minimumFractionDigits: 0 })
  return num.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function viewTransactions(accountNumber) {
  router.push({ name: 'user-transactions', query: { account: accountNumber } })
}

function goTransfer(accountNumber) {
  router.push({ name: 'user-transfer', query: { from: accountNumber } })
}
</script>

<style scoped>
.user-accounts { max-width: 1100px; margin: 0 auto; padding: 24px; }
h2 { margin-bottom: 20px; color: #1a1a2e; }
.account-cards { display: grid; grid-template-columns: repeat(auto-fill, minmax(320px, 1fr)); gap: 16px; }
.account-card {
  background: #fff; border-radius: 12px; padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06); border-left: 4px solid #1890ff;
  transition: transform 0.2s;
}
.account-card:hover { transform: translateY(-2px); box-shadow: 0 4px 16px rgba(0,0,0,0.1); }
.currency-TWD { border-left-color: #1890ff; }
.currency-USD { border-left-color: #52c41a; }
.currency-JPY { border-left-color: #fa541c; }
.currency-EUR { border-left-color: #722ed1; }
.currency-GBP { border-left-color: #eb2f96; }
.status-frozen { opacity: 0.7; }
.status-closed { opacity: 0.5; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.acct-type { font-weight: 600; color: #333; }
.acct-number { color: #888; font-size: 13px; font-family: monospace; margin-bottom: 8px; }
.acct-balance { display: flex; align-items: baseline; gap: 8px; }
.currency-badge { font-size: 12px; color: #fff; background: #1890ff; padding: 2px 8px; border-radius: 4px; font-weight: 600; }
.balance-amount { font-size: 24px; font-weight: 700; color: #1a1a2e; }
.card-footer { margin-top: 16px; display: flex; gap: 8px; justify-content: flex-end; }
.empty-hint { padding: 60px 0; text-align: center; }
</style>
