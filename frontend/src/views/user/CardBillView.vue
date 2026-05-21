<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import dayjs from 'dayjs'
import { getBills, getUnbilledBills,getBilledTransactions } from '@/api/userCardBill'
import { payCard, getPaymentAccounts } from '@/api/userCardPayment'
import { message } from 'ant-design-vue'

const loading = ref(false)
const bills = ref([])
const paymentAccounts = ref([])
const selectedAccount = ref('')
const selectedHistoryBill = ref(null)
const payAmount = ref('')

const selectedCardId = ref(null)
const billedTransactions = ref([])

const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
})

const activeTab = ref('BILLED')
const unbilledTransactions = ref([])

const formatMoney = (value) => {
  return Number(value || 0).toLocaleString()
}

const getRemainingAmount = (bill) => {
  return Math.max(Number(bill.totalAmount || 0) - Number(bill.paidAmount || 0), 0)
}

const getMinimumDueAmount = (bill) => {
  const remainingAmount = getRemainingAmount(bill)
  const unpaidMinimum = Math.max(Number(bill.minimumPayment || 0) - Number(bill.paidAmount || 0), 0)

  return unpaidMinimum > 0 ? Math.min(unpaidMinimum, remainingAmount) : remainingAmount
}

const historyBills = computed(() => {
  return [...bills.value].sort((a, b) =>
    String(b.billingMonth).localeCompare(String(a.billingMonth)),
  )
})

const displayBill = computed(() => {
  if (bills.value.length === 0) return null

  return historyBills.value[0]
})
  

const currentBill = computed(() => {
  if (selectedHistoryBill.value) {
    return bills.value.find((bill) => bill.billId === selectedHistoryBill.value)
  }

  return displayBill.value
})

const getCreditUsagePercent = (bill) => {
  const creditLimit = Number(bill?.creditLimit || 0)
  const availableCredit = Number(bill?.availableCredit || 0)

  if (creditLimit <= 0) return 0

  return Math.min(Math.max((availableCredit / creditLimit) * 100, 0), 100)
}

const fetchBills = async () => {
  loading.value = true

  try {
    const res = await getBills(pagination.value.current - 1, pagination.value.pageSize)

    console.log(res)

    bills.value = res.content
      .map((item) => ({
        ...item,
        dueDate: dayjs(item.dueDate).format('YYYY-MM-DD'),
      }))
      .sort((a, b) => String(b.billingMonth).localeCompare(String(a.billingMonth)))

    pagination.value.total = res.totalElements
  } catch (error) {
    console.error('Failed to fetch bills:', error)
  } finally {
    loading.value = false
  }
}

const fetchUnbilledTransactions = async () => {
  loading.value = true

  try {
    const res = await getUnbilledBills(pagination.value.current - 1, pagination.value.pageSize)

    console.log(res)

    unbilledTransactions.value = res.content
      .map((item) => ({
        ...item,
        transactionDate: dayjs(item.transactionDate).format('YYYY-MM-DD HH:mm'),
      }))
      .sort((a, b) => dayjs(b.txnDate).valueOf() - dayjs(a.txnDate).valueOf())
  } catch (error) {
    console.error('Failed to fetch unbilled transactions:', error)
    message.error(error.response?.data?.message || '無法獲取未出帳交易')
  } finally {
    loading.value = false
  }
}

const handlePayment = async (bill) => {
  try {
    const amount = Number(payAmount.value)
    const remainingAmount = getRemainingAmount(bill)

    if (!selectedAccount.value) {
      message.warning('請選擇扣款帳戶')
      return
    }

    if (!Number.isFinite(amount) || amount <= 0) {
      message.warning('請輸入大於 0 的繳費金額')
      return
    }

    if (amount > remainingAmount) {
      message.warning(`繳費金額不可超過剩餘應繳 NT$ ${formatMoney(remainingAmount)}`)
      return
    }

    await payCard({
      billIds: bill.billIds || [bill.billId],
      fromAccountNumber: selectedAccount.value,
      creditCardAccountNumber: bill.creditCardAccountNumber,
      amount,
      note: '信用卡繳費',
    })

    message.success('繳費成功')
    payAmount.value = ''
    selectedHistoryBill.value = null

    fetchBills()
    fetchAccounts()
  } catch (error) {
    console.error('Payment failed:', error)
    message.error(error.response?.data?.message || '繳費失敗')
  }
}

const fetchAccounts = async () => {
  try {
    const res = await getPaymentAccounts()

    console.log(res)

    paymentAccounts.value = res

    if (res.length > 0) {
      selectedAccount.value = res[0].accountNumber
    }
  } catch (error) {
    console.error('Failed to fetch accounts:', error)
  }
}

const fetchBilledTransactions = async () => {
  if (!currentBill.value?.billId || !selectedCardId.value) {
    billedTransactions.value = []
    return
  }

  const res = await getBilledTransactions(
    currentBill.value.billId,
    selectedCardId.value
  )

  billedTransactions.value = res
}

watch([currentBill, selectedCardId], fetchBilledTransactions)


watch(activeTab, async (newTab) => {
  if (newTab === 'BILLED') {
    await fetchBills()
  } else if (newTab === 'UNBILLED') {
    await fetchUnbilledTransactions()
  }
})

watch(selectedHistoryBill, () => {
  payAmount.value = ''
})

onMounted(() => {
  fetchBills()
  fetchAccounts()
})
</script>
<template>
  <div class="bill-page">
    <h2 class="page-title">我的帳單</h2>

    <div class="tabs">
      <button
        :class="['tab-btn', activeTab === 'UNBILLED' ? 'active' : '']"
        @click="activeTab = 'UNBILLED'"
      >
        未出帳交易
      </button>

      <button
        :class="['tab-btn', activeTab === 'BILLED' ? 'active' : '']"
        @click="activeTab = 'BILLED'"
      >
        已出帳單
      </button>
    </div>

    <div v-if="loading" class="loading">載入中...</div>

    <div v-if="activeTab === 'BILLED' && bills.length === 0" class="empty">
      尚無帳單資料
    </div>

    <div v-else-if="activeTab === 'UNBILLED' && unbilledTransactions.length === 0" class="empty">
      尚無未出帳交易
    </div>

    <!-- 已出帳單 -->
    <div v-if="activeTab === 'BILLED' && currentBill" class="bill-layout">
      <!-- 左邊：帳單資訊 -->
      <div class="bill-card">
        <div class="bill-header">
          <div>
            <div class="label">帳單月份</div>
            <div class="value">{{ currentBill.billingMonth }}</div>
          </div>

          <div class="status" :class="currentBill.billStatus">
            {{
              currentBill.billStatus === 'PAID'
                ? '已繳費'
                : currentBill.billStatus === 'UNPAID'
                  ? '未繳費'
                  : currentBill.billStatus === 'PARTIAL'
                    ? '部分繳款'
                    : '逾期'
            }}
          </div>
        </div>

        <div class="bill-body">
          <div class="form-section">
            <label>選擇帳單</label>
            <select v-model="selectedHistoryBill" class="history-select">
              <option :value="null">目前帳單</option>
              <option v-for="bill in historyBills" :key="bill.billId" :value="bill.billId">
                {{ bill.billingMonth }} -
                {{
                  bill.billStatus === 'PAID'
                    ? '已繳清'
                    : bill.billStatus === 'PARTIAL'
                      ? '部分繳款'
                      : '未繳費'
                }}
              </option>
            </select>
          </div>

          <div class="form-section">
            <label>選擇卡片查看交易明細</label>
            <select v-model="selectedCardId" class="history-select">
              <option :value="null">請選擇卡片</option>
              <option
                v-for="card in currentBill.cards || []"
                :key="card.cardId"
                :value="card.cardId"
              >
                {{ card.cardNumber }}
              </option>
            </select>
          </div>

          <div class="info-row">
            <span>帳單金額</span>
            <strong>NT$ {{ formatMoney(currentBill.totalAmount) }}</strong>
          </div>

          <div class="info-row">
            <span>最低應繳</span>
            <strong>NT$ {{ formatMoney(currentBill.minimumPayment) }}</strong>
          </div>

          <div class="info-row">
            <span>已繳金額</span>
            <strong>NT$ {{ formatMoney(currentBill.paidAmount) }}</strong>
          </div>

          <div class="info-row">
            <span>本期回饋金</span>
            <strong>NT$ {{ formatMoney(currentBill.cashbackAmount) }}</strong>
          </div>

          <div class="info-row">
            <span>信用額度</span>
            <strong>NT$ {{ formatMoney(currentBill.creditLimit) }}</strong>
          </div>

          <div class="info-row">
            <span>可用額度</span>
            <strong>NT$ {{ formatMoney(currentBill.availableCredit) }}</strong>
          </div>

          <div class="credit-bar-box">
            <div class="credit-bar-text">
              <span>可用額度比例</span>
              <strong>{{ getCreditUsagePercent(currentBill).toFixed(0) }}%</strong>
            </div>

            <div class="credit-bar">
              <div
                class="credit-bar-fill"
                :style="{ width: getCreditUsagePercent(currentBill) + '%' }"
              ></div>
            </div>
          </div>

          <div class="info-row">
            <span>繳費截止日</span>
            <strong>{{ currentBill.dueDate }}</strong>
          </div>
        </div>

        <div
          v-if="currentBill.billStatus !== 'PAID' && getRemainingAmount(currentBill) > 0"
          class="account-select"
        >
          <select v-model="selectedAccount" class="account-dropdown">
            <option
              v-for="account in paymentAccounts"
              :key="account.accountNumber"
              :value="account.accountNumber"
            >
              {{ account.accountNumber }}
              餘額: NT$ {{ formatMoney(account.balance) }}
            </option>
          </select>
        </div>

        <div
          v-if="currentBill.billStatus !== 'PAID' && getRemainingAmount(currentBill) > 0"
          class="payment-section"
        >
          <div class="quick-actions">
            <button class="quick-btn" @click="payAmount = getMinimumDueAmount(currentBill)">
              最低應繳
            </button>

            <button class="quick-btn" @click="payAmount = getRemainingAmount(currentBill)">
              全額繳清
            </button>
          </div>

          <div class="pay-row">
            <input
              v-model="payAmount"
              type="number"
              class="pay-input"
              min="1"
              step="1"
              :max="getRemainingAmount(currentBill)"
              placeholder="請輸入繳費金額"
            />

            <button class="pay-btn" @click="handlePayment(currentBill)">
              立即繳費
            </button>
          </div>
        </div>
      </div>

      <!-- 右邊：交易明細 -->
      <div class="txn-card">
        <div class="txn-header">
          <div>
            <div class="label">已出帳交易</div>
            <h3>交易明細</h3>
          </div>
        </div>

        <div v-if="!selectedCardId" class="txn-empty">
          請先選擇左側卡片
        </div>

        <div v-else-if="billedTransactions.length === 0" class="txn-empty">
          此卡片本期沒有交易
        </div>

        <table v-else class="txn-table">
          <thead>
            <tr>
              <th>日期</th>
              <th>商店</th>
              <th class="amount-col">金額</th>
            </tr>
          </thead>

          <tbody>
            <tr v-for="txn in billedTransactions" :key="txn.txnId">
              <td>{{ dayjs(txn.txnDate).format('YYYY-MM-DD') }}</td>
              <td>{{ txn.merchantName }}</td>
              <td class="amount-col">NT$ {{ formatMoney(txn.txnAmount) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 未出帳交易 -->
    <div v-else-if="activeTab === 'UNBILLED'" class="bill-list">
      <div v-for="txn in unbilledTransactions" :key="txn.txnId" class="bill-card">
        <div class="bill-body">
          <div class="info-row">
            <span>商店</span>
            <strong>{{ txn.merchantName }}</strong>
          </div>

          <div class="info-row">
            <span>刷卡時間</span>
            <strong>{{ dayjs(txn.txnDate).format('YYYY-MM-DD HH:mm') }}</strong>
          </div>

          <div class="info-row">
            <span>金額</span>
            <strong>NT$ {{ formatMoney(txn.txnAmount) }}</strong>
          </div>

          <div class="info-row">
            <span>卡號</span>
            <strong>{{ txn.cardNumber }}</strong>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<style scoped>
.bill-page {
  max-width: 1320px;
  margin: 0 auto;
  padding: 24px;
}

.page-title {
  margin-bottom: 20px;
  color: var(--text-primary);
  font-family: var(--font-heading);
  font-size: var(--text-h2);
  font-weight: 600;
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  padding: 6px;
  width: fit-content;
  border: 1px solid rgba(214, 206, 195, 0.86);
  border-radius: 12px;
  background: rgba(255, 249, 239, 0.72);
}

.tab-btn {
  border: 1px solid transparent;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  background: transparent;
  color: var(--text-secondary);
  font-weight: 600;
}

.tab-btn.active {
  background: var(--primary);
  border-color: var(--primary);
  color: white;
}

.loading,
.empty {
  text-align: center;
  margin-top: 16px;
  padding: 32px 20px;
  color: var(--text-secondary);
  border: 1px solid rgba(214, 206, 195, 0.86);
  border-radius: 12px;
  background: rgba(255, 249, 239, 0.92);
}

.bill-layout {
  display: grid;
  grid-template-columns: minmax(520px, 1fr) minmax(420px, 0.9fr);
  gap: 20px;
  align-items: start;
}

.bill-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.bill-card,
.txn-card {
  border-radius: 12px;
  background: rgba(255, 249, 239, 0.92);
  border: 1px solid rgba(214, 206, 195, 0.86);
  box-shadow: 0 12px 36px rgba(63, 74, 66, 0.08);
  padding: 20px;
}

.txn-card {
  position: sticky;
  top: 24px;
}

.bill-header,
.txn-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(214, 206, 195, 0.8);
}

.txn-header h3 {
  margin: 4px 0 0;
  color: var(--text-primary);
  font-size: 22px;
}

.label {
  color: var(--text-secondary);
  font-size: 13px;
}

.value {
  color: var(--text-primary);
  font-size: 22px;
  font-weight: 700;
}

.bill-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 4px;
}

.form-section label {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.info-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid rgba(214, 206, 195, 0.64);
  color: var(--text-secondary);
  font-size: 14px;
}

.info-row strong {
  color: var(--text-primary);
  font-weight: 600;
  text-align: right;
}

.status {
  flex-shrink: 0;
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}

.status.PAID {
  background: rgba(92, 107, 95, 0.12);
  color: var(--primary-dark);
}

.status.UNPAID {
  background: rgba(196, 164, 124, 0.18);
  color: #7b5a2f;
}

.status.PARTIAL {
  background: rgba(92, 107, 95, 0.08);
  color: var(--primary);
}

.status.OVERDUE {
  background: var(--accent-light);
  color: var(--accent);
}

.account-select {
  margin: 18px 0 12px;
}

.account-dropdown,
.history-select {
  width: 100%;
  min-height: 44px;
  border: 1px solid rgba(214, 206, 195, 0.86);
  border-radius: 8px;
  padding: 10px 14px;
  color: var(--text-primary);
  background: rgba(250, 250, 247, 0.92);
  font-size: 15px;
  outline: none;
}

.account-dropdown:focus,
.history-select:focus,
.pay-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.payment-section {
  margin-top: 20px;
  padding: 16px;
  border: 1px solid rgba(214, 206, 195, 0.86);
  border-radius: 8px;
  background: rgba(250, 250, 247, 0.84);
}

.quick-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.quick-btn {
  border: 1px solid rgba(214, 206, 195, 0.86);
  background: rgba(255, 249, 239, 0.72);
  color: var(--primary-dark);
  padding: 8px 14px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}

.pay-row {
  display: flex;
  gap: 12px;
}

.pay-input {
  flex: 1;
  min-height: 44px;
  border: 1px solid rgba(214, 206, 195, 0.86);
  border-radius: 8px;
  padding: 10px 14px;
  background: rgba(250, 250, 247, 0.92);
  font-size: 15px;
  outline: none;
}

.pay-btn {
  min-height: 44px;
  border: 1px solid var(--primary);
  background: #3e5c4b;
  color: white;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}

.credit-bar-box {
  padding: 4px 0 12px;
  border-bottom: 1px solid rgba(214, 206, 195, 0.64);
}

.credit-bar-text {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  color: var(--text-secondary);
  font-size: 14px;
}

.credit-bar-text strong {
  color: var(--text-primary);
}

.credit-bar {
  width: 100%;
  height: 10px;
  border-radius: 999px;
  background: rgba(214, 206, 195, 0.5);
  overflow: hidden;
}

.credit-bar-fill {
  height: 100%;
  border-radius: 999px;
  background: #6cd399;
}

.txn-empty {
  padding: 32px 16px;
  text-align: center;
  color: var(--text-secondary);
  border: 1px dashed rgba(214, 206, 195, 0.9);
  border-radius: 10px;
  background: rgba(250, 250, 247, 0.55);
}

.txn-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.txn-table th,
.txn-table td {
  padding: 12px 10px;
  border-bottom: 1px solid rgba(214, 206, 195, 0.64);
  text-align: left;
  color: var(--text-primary);
}

.txn-table th {
  color: var(--text-secondary);
  font-weight: 700;
}

.amount-col {
  text-align: right !important;
  white-space: nowrap;
}

@media (max-width: 1024px) {
  .bill-layout {
    grid-template-columns: 1fr;
  }

  .txn-card {
    position: static;
  }
}

@media (max-width: 640px) {
  .bill-page {
    padding: 16px 0;
  }

  .tabs {
    width: 100%;
  }

  .tab-btn {
    flex: 1;
    padding: 10px 12px;
  }

  .bill-header,
  .info-row,
  .pay-row {
    flex-direction: column;
    align-items: stretch;
  }

  .info-row strong {
    text-align: left;
  }

  .pay-btn {
    width: 100%;
  }
}
</style>