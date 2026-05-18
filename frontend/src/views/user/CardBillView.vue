<script setup>
import { ref, onMounted, watch } from 'vue'
import dayjs from 'dayjs'
import { getBills, getUnbilledBills } from '@/api/userCardBill'
import { payCard, getPaymentAccounts } from '@/api/userCardPayment'
import { message } from 'ant-design-vue'

const loading = ref(false)
const bills = ref([])
const paymentAccounts = ref([])
const selectedAccount = ref('')
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
})

const activeTab = ref('BILLED')
const unbilledTransactions = ref([])

const columns = [
  {
    title: '帳單編號',
    dataIndex: 'billId',
    key: 'billId',
  },
  {
    title: '帳單月份',
    dataIndex: 'billingMonth',
    key: 'billingMonth',
  },
  {
    title: '帳單金額',
    dataIndex: 'totalAmount',
    key: 'totalAmount',
  },
  {
    title: '最低應繳金額',
    dataIndex: 'minimumPayment',
    key: 'minimumPayment',
  },
  {
    title: '已繳金額',
    dataIndex: 'paidAmount',
    key: 'paidAmount',
  },
  {
    title: '繳費截止日',
    dataIndex: 'dueDate',
    key: 'dueDate',
  },
]

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

const fetchBills = async () => {
  loading.value = true
  try {
    const res = await getBills(pagination.value.current - 1, pagination.value.pageSize)

    console.log(res)

    bills.value = res.content.map((item) => ({
      ...item,
      dueDate: dayjs(item.dueDate).format('YYYY-MM-DD'),
    }))
    .sort((a, b) => dayjs(b.billingMonth).valueOf() - dayjs(a.billingMonth).valueOf())

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
    unbilledTransactions.value = res.content.map((item) => ({
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
    const amount = Number(bill.payAmount)
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
      billId: bill.billId,
      fromAccountNumber: selectedAccount.value,
      creditCardAccountNumber: bill.creditCardAccountNumber,
      amount,
      note: '信用卡繳費',
    })
    message.success('繳費成功')
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

watch(activeTab, async (newTab) => {
  if (newTab === 'BILLED') {
    await fetchBills()
  } else if (newTab === 'UNBILLED') {
    await fetchUnbilledTransactions()
  }
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

    <div v-if="activeTab === 'BILLED' && bills.length === 0" class="empty">尚無帳單資料</div>

    <div v-else-if="activeTab === 'UNBILLED' && unbilledTransactions.length === 0" class="empty">
      尚無未出帳交易
    </div>

    <div v-if="activeTab === 'BILLED'" class="bill-list">
      <div v-for="bill in bills" :key="bill.billId" class="bill-card">
        <div class="bill-header">
          <div>
            <div class="label">帳單月份</div>
            <div class="value">
              {{ bill.billingMonth }}
            </div>
          </div>

          <div class="status" :class="bill.billStatus">
            {{
              bill.billStatus === 'PAID'
                ? '已繳費'
                : bill.billStatus === 'UNPAID'
                  ? '未繳費'
                  : bill.billStatus === 'PARTIAL'
                    ? '部分繳款'
                    : '逾期'
            }}
          </div>
        </div>

        <div class="bill-body">
          <div class="info-row">
            <span>信用卡帳戶</span>
            <strong>{{ bill.creditCardAccountNumber }}</strong>
          </div>
          <div class="info-row">
            <span>帳單金額</span>
            <strong> NT$ {{ formatMoney(bill.totalAmount) }} </strong>
          </div>

          <div class="info-row">
            <span>最低應繳</span>
            <strong> NT$ {{ formatMoney(bill.minimumPayment) }} </strong>
          </div>

          <div class="info-row">
            <span>已繳金額</span>
            <strong> NT$ {{ formatMoney(bill.paidAmount) }} </strong>
          </div>
          <div class="info-row">
            <span>信用額度</span>
            <strong> NT$ {{ formatMoney(bill.creditLimit) }} </strong>
          </div>
          <div class="info-row">
            <span>可用額度</span>
            <strong> NT$ {{ formatMoney(bill.availableCredit) }} </strong>
          </div>

          <div class="info-row">
            <span>繳費截止日</span>
            <strong>
              {{ bill.dueDate }}
            </strong>
          </div>
        </div>
        <div v-if="bill.billStatus !== 'PAID' && getRemainingAmount(bill) > 0" class="account-select">
          <select v-model="selectedAccount" class="account-dropdown">
            <option
              v-for="account in paymentAccounts"
              :key="account.accountNumber"
              :value="account.accountNumber"
            >
              {{ account.accountNumber }}
              餘額: NT$ {{ account.balance }}
            </option>
          </select>
        </div>

        <div v-if="bill.billStatus !== 'PAID' && getRemainingAmount(bill) > 0" class="payment-section">
          <div class="quick-actions">
            <button class="quick-btn" @click="bill.payAmount = getMinimumDueAmount(bill)">
              最低應繳
            </button>

            <button class="quick-btn" @click="bill.payAmount = getRemainingAmount(bill)">全額繳清</button>
          </div>

          <div class="pay-row">
            <input
              v-model="bill.payAmount"
              type="number"
              class="pay-input"
              min="1"
              step="1"
              :max="getRemainingAmount(bill)"
              placeholder="請輸入繳費金額"
            />

            <button class="pay-btn" @click="handlePayment(bill)">立即繳費</button>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="bill-list">
      <div v-for="txn in unbilledTransactions" :key="txn.txnId" class="bill-card">
        <div class="bill-body">
          <div class="info-row">
            <span>商店</span>
            <strong>{{ txn.merchantName }}</strong>
          </div>

          <div class="info-row">
            <span>刷卡時間</span>
            <strong>
              {{ dayjs(txn.txnDate).format('YYYY-MM-DD HH:mm') }}
            </strong>
          </div>

          <div class="info-row">
            <span>金額</span>
            <strong> NT$ {{ formatMoney(txn.txnAmount) }} </strong>
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
  max-width: 960px;
  margin: 0 auto;
  padding: 24px;
}

.page-title {
  margin-bottom: 20px;
  color: var(--text-primary);
  font-family: var(--font-heading);
  font-size: var(--text-h2);
  font-weight: 600;
  line-height: var(--leading-heading);
}

.bill-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.bill-card {
  border-radius: 12px;
  background: rgba(255, 249, 239, 0.92);
  border: 1px solid rgba(214, 206, 195, 0.86);
  box-shadow: 0 12px 36px rgba(63, 74, 66, 0.08);
  padding: 20px;
}

.bill-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(214, 206, 195, 0.8);
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

.loading,
.empty {
  text-align: center;
  margin-top: 16px;
  padding: 32px 20px;
  color: var(--text-secondary);
  border: 1px solid rgba(214, 206, 195, 0.86);
  border-radius: 12px;
  background: rgba(255, 249, 239, 0.92);
  box-shadow: 0 12px 36px rgba(63, 74, 66, 0.06);
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
  transition:
    border-color var(--duration) var(--ease),
    background var(--duration) var(--ease);
}

.quick-btn:hover {
  border-color: var(--primary);
  background: var(--primary-light);
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
  color: var(--text-primary);
  background: rgba(250, 250, 247, 0.92);
  font-family: var(--font-body);
  font-size: 15px;
  outline: none;
  transition:
    border-color var(--duration) var(--ease),
    box-shadow var(--duration) var(--ease);
}

.pay-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
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
  transition:
    background var(--duration) var(--ease),
    border-color var(--duration) var(--ease),
    box-shadow var(--duration) var(--ease);
}

.pay-btn:hover {
  background: var(--primary-dark);
  border-color: var(--primary-dark);
  box-shadow: 0 4px 16px rgba(107, 95, 80, 0.1);
}
.account-select {
  margin: 18px 0 12px;
}

.account-dropdown {
  width: 100%;
  min-height: 44px;
  border: 1px solid rgba(214, 206, 195, 0.86);
  border-radius: 8px;
  padding: 10px 14px;
  color: var(--text-primary);
  background: rgba(250, 250, 247, 0.92);
  font-family: var(--font-body);
  font-size: 15px;
  outline: none;
  transition:
    border-color var(--duration) var(--ease),
    box-shadow var(--duration) var(--ease);
}

.account-dropdown:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
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
  box-shadow: 0 12px 36px rgba(63, 74, 66, 0.06);
}

.tab-btn {
  border: 1px solid transparent;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  background: transparent;
  color: var(--text-secondary);
  font-family: var(--font-body);
  font-weight: 600;
  transition:
    background var(--duration) var(--ease),
    color var(--duration) var(--ease),
    border-color var(--duration) var(--ease);
}

.tab-btn:hover {
  color: var(--primary-dark);
  background: var(--primary-light);
}

.tab-btn.active {
  background: var(--primary);
  border-color: var(--primary);
  color: white;
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
