<template>
  <div style="padding: 24px">
    <h2>帳戶管理</h2>

    <!-- 查詢區 -->
    <div style="margin-bottom: 16px; display: flex; gap: 8px; align-items: center">
      <a-input
        v-model:value="customerId"
        placeholder="客戶 ID"
        style="width: 150px"
        allow-clear
      />

      <a-select
        v-model:value="statusFilter"
        placeholder="帳戶狀態"
        style="width: 150px"
        allow-clear
      >
        <a-select-option value="PENDING">待啟用</a-select-option>
        <a-select-option value="ACTIVE">正常</a-select-option>
        <a-select-option value="FROZEN">凍結</a-select-option>
        <a-select-option value="DORMANT">靜止戶</a-select-option>
        <a-select-option value="CLOSED">已銷戶</a-select-option>
      </a-select>

      <a-select
        v-model:value="typeFilter"
        placeholder="帳戶型別"
        style="width: 150px"
        allow-clear
      >
        <a-select-option value="CHECKING">活存</a-select-option>
        <a-select-option value="TIME_DEPOSIT">定存</a-select-option>
        <a-select-option value="LOAN">貸款</a-select-option>
        <a-select-option value="SUB_ACCOUNT">子帳戶</a-select-option>
      </a-select>

      <a-select
        v-model:value="currencyFilter"
        placeholder="幣別"
        style="width: 150px"
        allow-clear
      >
        <a-select-option value="TWD">TWD</a-select-option>
        <a-select-option value="USD">USD</a-select-option>
        <a-select-option value="EUR">EUR</a-select-option>
        <a-select-option value="JPY">JPY</a-select-option>
        <a-select-option value="GBP">GBP</a-select-option>
        <a-select-option value="CNY">CNY</a-select-option>
        <a-select-option value="AUD">AUD</a-select-option>
        <a-select-option value="CAD">CAD</a-select-option>
        <a-select-option value="CHF">CHF</a-select-option>
        <a-select-option value="HKD">HKD</a-select-option>
      </a-select>

      <a-button type="primary" @click="handleSearch">查詢</a-button>
      <a-button @click="showCreateModal = true">建立帳戶</a-button>
      <a-button danger @click="handleClear">清除</a-button>
    </div>

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="accounts"
      :loading="loading"
      :scroll="{ x: 800 }"
      row-key="accountNumber"
      :pagination="{
        current: currentPage,
        pageSize: pageSize,
        total: total,
        showSizeChanger: true,
        showTotal: (t) => `共 ${t} 筆`,
      }"
      @change="handleTableChange"
    />

    <!-- 建立帳戶 Modal -->
    <a-modal
      v-model:open="showCreateModal"
      title="建立帳戶"
      @ok="handleCreate"
      :confirm-loading="createLoading"
    >
      <a-form layout="vertical">
        <a-form-item label="客戶 ID">
          <a-input v-model:value="createForm.customerId" placeholder="請輸入客戶 ID" />
        </a-form-item>

        <a-form-item label="帳戶型別">
          <a-select v-model:value="createForm.accountType" placeholder="請選擇">
            <a-select-option value="CHECKING">活存</a-select-option>
            <a-select-option value="TIME_DEPOSIT">定存</a-select-option>
            <a-select-option value="LOAN">貸款</a-select-option>
            <a-select-option value="SUB_ACCOUNT">子帳戶</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="幣別">
          <a-select v-model:value="createForm.currency" placeholder="請選擇">
            <a-select-option value="TWD">TWD 新台幣</a-select-option>
            <a-select-option value="USD">USD 美元</a-select-option>
            <a-select-option value="EUR">EUR 歐元</a-select-option>
            <a-select-option value="JPY">JPY 日圓</a-select-option>
            <a-select-option value="GBP">GBP 英鎊</a-select-option>
            <a-select-option value="CNY">CNY 人民幣</a-select-option>
            <a-select-option value="AUD">AUD 澳幣</a-select-option>
            <a-select-option value="CAD">CAD 加幣</a-select-option>
            <a-select-option value="CHF">CHF 瑞士法郎</a-select-option>
            <a-select-option value="HKD">HKD 港幣</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          v-if="createForm.accountType === 'SUB_ACCOUNT'"
          label="父帳戶帳號"
        >
          <a-input v-model:value="createForm.parentAccountNumber" placeholder="請輸入父帳戶帳號" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import {
  getAccountsByCustomerId,
  getAccountsByStatus,
  getAccountsByTypeAndCurrency,
  getLatestAccounts,
  createAccount,
} from '@/api/account'

// === 狀態/型別 中文對照 ===
const statusMap = {
  PENDING: '待啟用',
  ACTIVE: '正常',
  FROZEN: '凍結',
  DORMANT: '靜止戶',
  CLOSED: '已銷戶',
}

const typeMap = {
  CHECKING: '活存',
  TIME_DEPOSIT: '定存',
  LOAN: '貸款',
  SUB_ACCOUNT: '子帳戶',
}

// === 格式化工具 ===
function formatAmount(value) {
  if (value == null) return '-'
  return Number(value).toLocaleString()
}

function formatTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ').substring(0, 19)
}

// === 查詢相關 ===
const customerId = ref('')
const statusFilter = ref(undefined)
const typeFilter = ref(undefined)
const currencyFilter = ref(undefined)
const accounts = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 記錄當前用哪種查詢，換頁時要用
const lastSearchType = ref('')

const columns = [
  { title: '帳號', dataIndex: 'accountNumber', key: 'accountNumber', width: 150 },
  {
    title: '型別',
    dataIndex: 'accountType',
    key: 'accountType',
    width: 100,
    customRender: ({ text }) => typeMap[text] || text,
  },
  { title: '幣別', dataIndex: 'currency', key: 'currency', width: 80 },
  {
    title: '餘額',
    dataIndex: 'balance',
    key: 'balance',
    width: 120,
    align: 'right',
    customRender: ({ text }) => formatAmount(text),
  },
  {
    title: '狀態',
    dataIndex: 'status',
    key: 'status',
    width: 100,
    customRender: ({ text }) => statusMap[text] || text,
  },
  {
    title: '建立時間',
    dataIndex: 'createdAt',
    key: 'createdAt',
    width: 180,
    customRender: ({ text }) => formatTime(text),
  },
]

async function handleSearch() {
  currentPage.value = 1
  await fetchData()
}

async function handleTableChange(pagination) {
  currentPage.value = pagination.current
  pageSize.value = pagination.pageSize
  await fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    let res
    const page = currentPage.value - 1
    const size = pageSize.value

    if (customerId.value) {
      lastSearchType.value = 'customerId'
      res = await getAccountsByCustomerId(customerId.value, page, size)
    } else if (typeFilter.value && currencyFilter.value) {
      lastSearchType.value = 'typeAndCurrency'
      res = await getAccountsByTypeAndCurrency(typeFilter.value, currencyFilter.value, page, size)
    } else if (statusFilter.value) {
      lastSearchType.value = 'status'
      res = await getAccountsByStatus(statusFilter.value, page, size)
    } else {
      lastSearchType.value = 'latest'
      res = await getLatestAccounts(page, size)
    }

    accounts.value = res.data.data.content
    total.value = res.data.data.totalElements
  } catch (err) {
    message.error(err.response?.data?.message || '查詢失敗')
  } finally {
    loading.value = false
  }
}

// === 建立帳戶相關 ===
const showCreateModal = ref(false)
const createLoading = ref(false)

const createForm = reactive({
  customerId: '',
  accountType: undefined,
  currency: undefined,
  parentAccountNumber: '',
})

async function handleCreate() {
  createLoading.value = true
  try {
    const res = await createAccount({
      customerId: Number(createForm.customerId),
      accountType: createForm.accountType,
      currency: createForm.currency,
      parentAccountNumber: createForm.parentAccountNumber || null,
    })
    message.success('帳戶建立成功: ' + res.data.data.accountNumber)
    showCreateModal.value = false

    createForm.customerId = ''
    createForm.accountType = undefined
    createForm.currency = undefined
    createForm.parentAccountNumber = ''

    if (lastSearchType.value) {
      await fetchData()
    }
  } catch (err) {
    message.error(err.response?.data?.message || '建立失敗')
  } finally {
    createLoading.value = false
  }
  }
  

  function handleClear() {
    customerId.value = ''
    statusFilter.value = undefined
    typeFilter.value = undefined
    currencyFilter.value = undefined
    accounts.value = []
    total.value = 0
    currentPage.value = 1
  }
</script>
