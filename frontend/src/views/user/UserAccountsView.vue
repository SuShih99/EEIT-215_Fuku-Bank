<template>
  <div class="user-accounts">
    <h2 class="accounts-title">我的帳戶總覽</h2>

    <a-spin :spinning="loading">
      <div v-if="accounts.length === 0 && !loading" class="empty-hint">
        <a-empty description="尚無帳戶，請先完成開戶申請" />
      </div>

      <!-- 幣別分類 Tabs -->
      <div v-if="accounts.length > 0" class="currency-tabs">
        <button
          class="tab-item"
          :class="{ active: activeCurrency === 'ALL' }"
          @click="activeCurrency = 'ALL'"
        >全部</button>
        <button
          v-for="c in currencies"
          :key="c"
          class="tab-item"
          :class="[`tab-${c.toLowerCase()}`, { active: activeCurrency === c }]"
          @click="activeCurrency = c"
        >{{ c }}</button>
      </div>

      <div class="account-cards">
        <div
          v-for="acct in filteredAccounts"
          :key="acct.accountNumber"
          class="account-card"
          :class="[
            `currency-${acct.currency.toLowerCase()}`,
            `status-card-${acct.status?.toLowerCase()}`
          ]"
        >
          <!-- 左側幣別色條 -->
          <div class="card-accent-bar"></div>

          <div class="card-content">
            <div class="card-header">
              <span class="acct-type">{{ typeLabel(acct.accountType) }}</span>
              <span class="status-badge" :class="`status-${acct.status?.toLowerCase()}`">
                {{ statusLabel(acct.status) }}
              </span>
            </div>

            <div class="card-body">
              <div class="acct-number">{{ acct.accountNumber }}</div>
              <div class="acct-balance">
                <span class="currency-badge" :class="`badge-${acct.currency.toLowerCase()}`">{{ acct.currency }}</span>
                <span class="balance-amount">{{ formatBalance(acct.balance, acct.currency) }}</span>
              </div>
            </div>

            <div class="card-footer">
              <button class="btn-secondary" @click="viewTransactions(acct.accountNumber)">
                交易紀錄
              </button>
              <button
                v-if="acct.status === 'ACTIVE' && acct.accountType !== 'LOAN'"
                class="btn-primary"
                @click="goTransfer(acct.accountNumber)"
              >
                轉帳
              </button>
            </div>
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

const currencyPriority = { TWD: 0, USD: 1, JPY: 2, EUR: 3, GBP: 4 }
const currencyOrder = (c) => currencyPriority[c] ?? 99

const currencies = computed(() => {
  const set = new Set(accounts.value.map(a => a.currency))
  return [...set].sort((a, b) => currencyOrder(a) - currencyOrder(b) || a.localeCompare(b))
})

const filteredAccounts = computed(() => {
  const list = activeCurrency.value === 'ALL'
    ? [...accounts.value]
    : accounts.value.filter(a => a.currency === activeCurrency.value)
  return list.sort((a, b) => currencyOrder(a.currency) - currencyOrder(b.currency) || a.currency.localeCompare(b.currency))
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
  const map = { CHECKING: '活期存款', SAVINGS: '儲蓄存款', TIME_DEPOSIT: '定期存款', LOAN: '貸款帳戶', SUB_ACCOUNT: '子帳戶' }
  return map[type] || type
}

function statusLabel(s) {
  const map = { ACTIVE: '正常', FROZEN: '凍結', DORMANT: '靜止', CLOSED: '已銷戶', PENDING: '待啟用' }
  return map[s] || s
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
/* ======================================
   Java Bank — 帳戶總覽 侘寂風格
   幣別低飽和識別色系統
   ====================================== */

/* --- 幣別輔助色 tokens --- */
.user-accounts {
  --currency-twd: #5C6B5F; /* 台幣：品牌主綠灰 */
  --currency-usd: #4F6358; /* 美元：深綠灰 */
  --currency-jpy: #8A6F4D; /* 日圓：茶金色 */
  --currency-eur: #6B6678; /* 歐元：灰紫色 */
  --currency-gbp: #7A5C54; /* 英鎊：棕紅色 */
  --currency-cny: #6B5F52; /* 人民幣：棕灰色 */
  --currency-aud: #5E6B5C; /* 澳幣：橄欖灰 */
  --currency-cad: #6A5C5C; /* 加幣：灰玫瑰 */
  --currency-chf: #5C5F6B; /* 瑞郎：藍灰色 */
  --currency-hkd: #5F6B68; /* 港幣：青灰色 */

  max-width: 1100px;
  margin: 0 auto;
  padding: var(--space-5) var(--space-4);
}

/* --- 標題 --- */
.accounts-title {
  font-family: var(--font-heading);
  font-size: var(--text-h2);
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: var(--space-4);
  letter-spacing: 0.5px;
}

/* ==========================================
   幣別 Tabs — 選中時顯示對應幣別色底線
   ========================================== */
.currency-tabs {
  display: flex;
  gap: var(--space-1);
  border-bottom: 1px solid var(--border);
  margin-bottom: var(--space-4);
  overflow-x: auto;
}

.tab-item {
  position: relative;
  background: none;
  border: none;
  padding: var(--space-2) var(--space-3);
  font-family: var(--font-body);
  font-size: var(--text-body);
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: color var(--duration) var(--ease);
  white-space: nowrap;
}

.tab-item::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: transparent;
  transition: background var(--duration) var(--ease);
}

.tab-item:hover { color: var(--primary-dark); }

/* 「全部」Tab active */
.tab-item.active {
  color: var(--primary);
  font-weight: 600;
}
.tab-item.active::after {
  background: var(--primary);
}

/* 各幣別 Tab active 底線 */
.tab-twd.active        { color: var(--currency-twd); }
.tab-twd.active::after  { background: var(--currency-twd); }
.tab-usd.active        { color: var(--currency-usd); }
.tab-usd.active::after  { background: var(--currency-usd); }
.tab-jpy.active        { color: var(--currency-jpy); }
.tab-jpy.active::after  { background: var(--currency-jpy); }
.tab-eur.active        { color: var(--currency-eur); }
.tab-eur.active::after  { background: var(--currency-eur); }
.tab-gbp.active        { color: var(--currency-gbp); }
.tab-gbp.active::after  { background: var(--currency-gbp); }
.tab-cny.active        { color: var(--currency-cny); }
.tab-cny.active::after  { background: var(--currency-cny); }
.tab-aud.active        { color: var(--currency-aud); }
.tab-aud.active::after  { background: var(--currency-aud); }
.tab-cad.active        { color: var(--currency-cad); }
.tab-cad.active::after  { background: var(--currency-cad); }
.tab-chf.active        { color: var(--currency-chf); }
.tab-chf.active::after  { background: var(--currency-chf); }
.tab-hkd.active        { color: var(--currency-hkd); }
.tab-hkd.active::after  { background: var(--currency-hkd); }

/* ==========================================
   帳戶卡片 Grid
   ========================================== */
.account-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--space-3);
}

/* --- 單張帳戶卡片 --- */
.account-card {
  display: flex;
  background: #FFF9EF;
  border: 1px solid var(--border);
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 18px rgba(63, 74, 66, 0.07);
  transition: transform var(--duration) var(--ease),
              box-shadow var(--duration) var(--ease);
}

.account-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 28px rgba(63, 74, 66, 0.12);
}

/* 凍結 / 銷戶卡片降低對比 */
.status-card-frozen  { opacity: 0.72; }
.status-card-closed  { opacity: 0.55; }
.status-card-dormant { opacity: 0.72; }

/* ==========================================
   1) 左側色條 — 依幣別
   ========================================== */
.card-accent-bar {
  width: 5px;
  flex-shrink: 0;
  background: var(--primary); /* fallback */
}

.currency-twd .card-accent-bar { background: var(--currency-twd); }
.currency-usd .card-accent-bar { background: var(--currency-usd); }
.currency-jpy .card-accent-bar { background: var(--currency-jpy); }
.currency-eur .card-accent-bar { background: var(--currency-eur); }
.currency-gbp .card-accent-bar { background: var(--currency-gbp); }
.currency-cny .card-accent-bar { background: var(--currency-cny); }
.currency-aud .card-accent-bar { background: var(--currency-aud); }
.currency-cad .card-accent-bar { background: var(--currency-cad); }
.currency-chf .card-accent-bar { background: var(--currency-chf); }
.currency-hkd .card-accent-bar { background: var(--currency-hkd); }

/* ==========================================
   3) 極淡卡片背景 tint — 依幣別
   ========================================== */
.currency-twd { background-color: rgba(92, 107, 95, 0.04); }
.currency-usd { background-color: rgba(79, 99, 88, 0.04); }
.currency-jpy { background-color: rgba(138, 111, 77, 0.05); }
.currency-eur { background-color: rgba(107, 102, 120, 0.04); }
.currency-gbp { background-color: rgba(122, 92, 84, 0.04); }
.currency-cny { background-color: rgba(107, 95, 82, 0.04); }
.currency-aud { background-color: rgba(94, 107, 92, 0.04); }
.currency-cad { background-color: rgba(106, 92, 92, 0.04); }
.currency-chf { background-color: rgba(92, 95, 107, 0.04); }
.currency-hkd { background-color: rgba(95, 107, 104, 0.04); }

/* ==========================================
   卡片內容
   ========================================== */
.card-content {
  flex: 1;
  padding: var(--space-3) var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

/* --- 卡片 Header --- */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.acct-type {
  font-family: var(--font-heading);
  font-weight: 600;
  font-size: var(--text-body);
  color: var(--text-primary);
}

/* 狀態標籤 — 統一低飽和 */
.status-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  font-size: var(--text-xs);
  font-weight: 600;
  border-radius: 20px;
  letter-spacing: 0.5px;
}

.status-active {
  color: var(--primary-dark);
  background: rgba(92, 107, 95, 0.12);
  border: 1px solid rgba(92, 107, 95, 0.30);
}

.status-pending {
  color: var(--text-secondary);
  background: var(--bg-secondary);
  border: 1px solid var(--border);
}

.status-frozen {
  color: #4A6B8A;
  background: rgba(74, 107, 138, 0.10);
  border: 1px solid rgba(74, 107, 138, 0.30);
}

.status-dormant {
  color: #8A7A4A;
  background: rgba(138, 122, 74, 0.10);
  border: 1px solid rgba(138, 122, 74, 0.30);
}

.status-closed {
  color: var(--text-disabled, #A8A199);
  background: rgba(168, 161, 153, 0.10);
  border: 1px solid rgba(168, 161, 153, 0.30);
}

/* --- 卡片 Body --- */
.acct-number {
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  color: var(--text-secondary);
  letter-spacing: 0.5px;
}

.acct-balance {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
  margin-top: var(--space-1);
}

/* ==========================================
   2) 幣別 Badge — 依幣別色
   ========================================== */
.currency-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 700;
  color: #F5F1EA;
  background: var(--primary); /* fallback */
  padding: 3px 8px;
  border-radius: 5px;
  letter-spacing: 0.5px;
  line-height: 1;
}

.badge-twd { background: var(--currency-twd); }
.badge-usd { background: var(--currency-usd); }
.badge-jpy { background: var(--currency-jpy); }
.badge-eur { background: var(--currency-eur); }
.badge-gbp { background: var(--currency-gbp); }
.badge-cny { background: var(--currency-cny); }
.badge-aud { background: var(--currency-aud); }
.badge-cad { background: var(--currency-cad); }
.badge-chf { background: var(--currency-chf); }
.badge-hkd { background: var(--currency-hkd); }

/* ==========================================
   金額 — 使用清晰的金融資訊字體
   ========================================== */
.balance-amount {
  font-family: 'Inter', 'Noto Sans TC', var(--font-body);
  font-size: 26px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

/* --- 卡片 Footer --- */
.card-footer {
  display: flex;
  gap: var(--space-2);
  justify-content: flex-end;
  margin-top: var(--space-2);
  padding-top: var(--space-2);
  border-top: 1px solid rgba(214, 206, 195, 0.4);
}

/* 按鈕 — 次要 */
.btn-secondary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  padding: 6px 14px;
  font-size: var(--text-sm);
  font-weight: 500;
  font-family: var(--font-body);
  color: var(--primary-dark);
  background: transparent;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--duration) var(--ease);
}

.btn-secondary:hover {
  border-color: var(--primary);
  background: var(--primary-light);
  color: var(--primary-dark);
}

/* 按鈕 — 主要 */
.btn-primary {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  padding: 6px 14px;
  font-size: var(--text-sm);
  font-weight: 600;
  font-family: var(--font-body);
  color: #F5F1EA;
  background: var(--primary);
  border: 1px solid var(--primary);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--duration) var(--ease);
}

.btn-primary:hover {
  background: var(--primary-dark);
  border-color: var(--primary-dark);
  box-shadow: 0 2px 8px rgba(63, 74, 66, 0.18);
}

/* --- Empty 狀態 --- */
.empty-hint {
  padding: var(--space-8) 0;
  text-align: center;
}

/* --- 響應式 --- */
@media (max-width: 680px) {
  .account-cards {
    grid-template-columns: 1fr;
  }

  .balance-amount {
    font-size: 22px;
  }
}
</style>
