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
const fetchBills = async () => {
  loading.value = true
  try {
    const res = await getBills(pagination.value.current - 1, pagination.value.pageSize)

    console.log(res)

    bills.value = res.content.map((item) => ({
      ...item,
      dueDate: dayjs(item.dueDate).format('YYYY-MM-DD'),
    }))

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
  } catch (error) {
    console.error('Failed to fetch unbilled transactions:', error)
    message.error(error.response?.data?.message || '無法獲取未出帳交易')
  } finally {
    loading.value = false
  }
}

const handlePayment = async (bill) => {
  try {
    await payCard({
      fromAccountNumber: selectedAccount.value,
      creditCardAccountNumber: bill.creditCardAccountNumber,
      amount: bill.payAmount,
      note: '信用卡繳費',
    })
    message.success('繳費成功')
    fetchBills()
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
                  : '逾期'
            }}
          </div>
        </div>

        <div class="bill-body">
          <div class="info-row">
            <span>帳單金額</span>
            <strong> NT$ {{ bill.totalAmount }} </strong>
          </div>

          <div class="info-row">
            <span>最低應繳</span>
            <strong> NT$ {{ bill.minimumPayment }} </strong>
          </div>

          <div class="info-row">
            <span>已繳金額</span>
            <strong> NT$ {{ bill.paidAmount }} </strong>
          </div>
          <div class="info-row">
            <span>可用額度</span>
            <strong> NT$ {{ bill.availableCredit }} </strong>
          </div>
          <div class="info-row">
            <span>信用卡帳戶</span>
            <strong>{{ bill.creditCardAccountNumber }}</strong>
          </div>

          <div class="info-row">
            <span>信用額度</span>
            <strong> NT$ {{ bill.creditLimit }} </strong>
          </div>

          <div class="info-row">
            <span>繳費截止日</span>
            <strong>
              {{ bill.dueDate }}
            </strong>
          </div>
        </div>
        <div class="account-select">
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

        <div class="payment-section">
          <div class="quick-actions">
            <button class="quick-btn" @click="bill.payAmount = bill.minimumPayment">
              最低應繳
            </button>

            <button class="quick-btn" @click="bill.payAmount = bill.totalAmount">全額繳清</button>
          </div>

          <div class="pay-row">
            <input
              v-model="bill.payAmount"
              type="number"
              class="pay-input"
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
            <strong> NT$ {{ txn.txnAmount }} </strong>
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
  padding: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 24px;
}

.bill-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.bill-card {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.bill-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.label {
  color: #999;
  font-size: 14px;
}

.value {
  font-size: 22px;
  font-weight: bold;
}

.bill-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 8px;
}

.status {
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: bold;
}

.status.PAID {
  background: #e8f7ee;
  color: #1f9254;
}

.status.UNPAID {
  background: #fff7e6;
  color: #d48806;
}

.status.OVERDUE {
  background: #fff1f0;
  color: #cf1322;
}

.loading,
.empty {
  text-align: center;
  margin-top: 40px;
  color: #999;
}

.payment-section {
  margin-top: 20px;
}

.quick-actions {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.quick-btn {
  border: none;
  background: #f3f0e8;
  color: #8b6b3f;
  padding: 8px 14px;
  border-radius: 10px;
  cursor: pointer;
  font-weight: bold;
}

.quick-btn:hover {
  background: #e7dcc7;
}

.pay-row {
  display: flex;
  gap: 12px;
}

.pay-input {
  flex: 1;
  border: 1px solid #ddd;
  border-radius: 10px;
  padding: 10px;
  font-size: 16px;
}

.pay-btn {
  border: none;
  background: #3e5c4b;
  color: white;
  padding: 10px 20px;
  border-radius: 10px;
  cursor: pointer;
  font-weight: bold;
}

.pay-btn:hover {
  opacity: 0.9;
}
.account-select {
  margin-bottom: 12px;
}

.account-dropdown {
  width: 100%;
  border: 1px solid #ddd;
  border-radius: 10px;
  padding: 10px;
  font-size: 16px;
}
.tabs {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.tab-btn {
  border: none;
  padding: 10px 20px;
  border-radius: 999px;
  cursor: pointer;
  background: #f0f0f0;
}

.tab-btn.active {
  background: #3e5c4b;
  color: white;
}
</style>
