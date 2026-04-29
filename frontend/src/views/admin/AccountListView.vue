<template>
  <div style="padding: 24px">
    <h2>帳戶管理</h2>

    <!-- 查詢區 -->
    <div style="margin-bottom: 16px; display: flex; gap: 8px; align-items: center">
      <a-input
        v-model:value="accountNumberSearch"
        placeholder="帳號"
        style="width: 150px"
        allow-clear
      />
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
        <a-select-option value="CAD">CAD 加幣</a-select-option>
        <a-select-option value="CHF">CHF 瑞士法郎</a-select-option>
        <a-select-option value="HKD">HKD 港幣</a-select-option>
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
      @resizeColumn="handleResizeColumn"
    />

    <!-- 建立帳戶 Modal -->
    <a-modal
      v-model:open="showCreateModal"
      title="建立帳戶"
      @ok="handleCreate"
      :confirm-loading="createLoading"
    >
      <a-form layout="vertical">
        <a-form-item label="帳戶型別">
          <a-select v-model:value="createForm.accountType" placeholder="請選擇">
            <a-select-option value="CHECKING">活存</a-select-option>
            <a-select-option value="TIME_DEPOSIT">定存</a-select-option>
            <a-select-option value="LOAN">貸款</a-select-option>
            <a-select-option value="SUB_ACCOUNT">子帳戶</a-select-option>
          </a-select>
        </a-form-item>

        <template v-if="createForm.accountType !== 'SUB_ACCOUNT'">
          <a-form-item label="客戶 ID">
            <a-input v-model:value="createForm.customerId" placeholder="請輸入客戶 ID" />
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
        </template>

        <template v-else>
          <a-form-item label="父帳戶帳號">
            <a-input v-model:value="createForm.parentAccountNumber" placeholder="請輸入父帳戶帳號" />
          </a-form-item>
        </template>
      </a-form>
    </a-modal>

    <!-- 子帳戶確認 Modal -->
    <a-modal
      v-model:open="showConfirmModal"
      title="確認父帳戶資訊"
      ok-text="確認建立"
      cancel-text="取消"
      @ok="handleConfirmCreate"
      :confirm-loading="createLoading"
    >
      <div v-if="parentAccountDetail">
        <a-descriptions bordered :column="1" size="small">
          <a-descriptions-item label="父帳戶帳號">{{ parentAccountDetail.accountNumber }}</a-descriptions-item>
          <a-descriptions-item label="客戶 ID">{{ parentAccountDetail.customerId }}</a-descriptions-item>
          <a-descriptions-item label="帳戶型別">{{ typeMap[parentAccountDetail.accountType] || parentAccountDetail.accountType }}</a-descriptions-item>
          <a-descriptions-item label="幣別">{{ parentAccountDetail.currency }}</a-descriptions-item>
          <a-descriptions-item label="餘額">{{ formatAmount(parentAccountDetail.balance) }}</a-descriptions-item>
          <a-descriptions-item label="狀態">{{ statusMap[parentAccountDetail.status] || parentAccountDetail.status }}</a-descriptions-item>
        </a-descriptions>
        <div style="margin-top: 16px; color: #666">
          將為客戶 <strong>{{ parentAccountDetail.customerId }}</strong> 建立 TWD 子帳戶，父帳戶為 <strong>{{ parentAccountDetail.accountNumber }}</strong>。
        </div>
      </div>
    </a-modal>

    <!-- 狀態變更 Modal -->
    <a-modal
      v-model:open="showStatusModal"
      title="變更帳戶狀態"
      @ok="handleStatusChange"
      :confirm-loading="statusModalLoading"
    >
      <a-form layout="vertical">
        <a-form-item label="帳號">
          <a-input :value="statusTarget.accountNumber" disabled />
        </a-form-item>
        <a-form-item label="目前狀態">
          <a-input :value="statusMap[statusTarget.currentStatus] || statusTarget.currentStatus" disabled />
        </a-form-item>
        <a-form-item label="目標狀態">
          <a-select v-model:value="statusTarget.newStatus" placeholder="請選擇目標狀態">
            <a-select-option
              v-for="s in (validTransitions[statusTarget.currentStatus] || [])"
              :key="s"
              :value="s"
            >
              {{ statusMap[s] || s }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="statusTarget.newStatus === 'CLOSED'" label="銷戶確認">
          <a-input v-model:value="closeConfirmText" placeholder="請輸入「我確定」以確認銷戶" />
          <div style="margin-top: 4px; color: #ff4d4f; font-size: 12px">
            銷戶為不可逆操作，請輸入「我確定」後才能執行。
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, h } from 'vue'
import { message } from 'ant-design-vue'
import {
  getAccountsByCustomerId,
  getAccountsByStatus,
  getAccountsByTypeAndCurrency,
  getLatestAccounts,
  createAccount,
  updateAccountStatus,
  getAccount, // Added getAccount
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
const accountNumberSearch = ref('')
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

const columns = ref([
  { title: '帳號', dataIndex: 'accountNumber', key: 'accountNumber', width: 150, resizable: true },
  { title: '客戶 ID', dataIndex: 'customerId', key: 'customerId', width: 100, resizable: true },
  {
    title: '型別',
    dataIndex: 'accountType',
    key: 'accountType',
    width: 100,
    resizable: true,
    customRender: ({ text }) => typeMap[text] || text,
  },
  { title: '幣別', dataIndex: 'currency', key: 'currency', width: 80, resizable: true },
  {
    title: '餘額',
    dataIndex: 'balance',
    key: 'balance',
    width: 120,
    resizable: true,
    align: 'right',
    customRender: ({ text }) => formatAmount(text),
  },
  {
    title: '狀態',
    dataIndex: 'status',
    key: 'status',
    width: 100,
    resizable: true,
    customRender: ({ text }) => statusMap[text] || text,
  },
  {
    title: '建立時間',
    dataIndex: 'createdAt',
    key: 'createdAt',
    width: 180,
    resizable: true,
    customRender: ({ text }) => formatTime(text),
  },
  {
    title: '操作',
    key: 'action',
    width: 100,
    fixed: 'right',
    customRender: ({ record }) => {
      if (record.status === 'CLOSED') return '-'
      return h('a', { onClick: () => openStatusModal(record) }, '變更狀態')
    },
  },
])

function handleResizeColumn(w, col) {
  col.width = w
}

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

    if (accountNumberSearch.value) {
      lastSearchType.value = 'accountNumber'
      try {
        const singleRes = await getAccount(accountNumberSearch.value)
        accounts.value = [singleRes.data.data]
        total.value = 1
      } catch (err) {
        accounts.value = []
        total.value = 0
        message.error(err.response?.data?.message || '查無此帳號')
      }
      loading.value = false
      return
    } else if (customerId.value) {
      lastSearchType.value = 'customerId'
      res = await getAccountsByCustomerId(customerId.value, page, size)
    } else if ((typeFilter.value && !currencyFilter.value) || (!typeFilter.value && currencyFilter.value)) {
      message.warning('請同時選擇帳戶型別和幣別')
      loading.value = false
      return
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

// New state variables for sub-account creation flow
const showConfirmModal = ref(false)
const parentAccountDetail = ref(null)
const confirmLoading = ref(false)

// Replaced handleCreate function
async function handleCreate() {
  // 子帳戶走確認流程
  if (createForm.accountType === 'SUB_ACCOUNT') {
    if (!createForm.parentAccountNumber) {
      message.warning('請輸入父帳戶帳號')
      return
    }
    // 先查詢父帳戶資訊
    createLoading.value = true
    try {
      const res = await getAccount(createForm.parentAccountNumber)
      parentAccountDetail.value = res.data.data
      showCreateModal.value = false
      showConfirmModal.value = true
    } catch (err) {
      message.error('查詢父帳戶失敗: ' + (err.response?.data?.message || err.message))
    } finally {
      createLoading.value = false
    }
    return
  }

  // 非子帳戶，直接建立
  await doCreateAccount({
    customerId: Number(createForm.customerId),
    accountType: createForm.accountType,
    currency: createForm.currency,
    parentAccountNumber: null,
  })
}

async function handleConfirmCreate() {
  // 子帳戶確認後建立，customerId 和 currency 從父帳戶帶入
  const parent = parentAccountDetail.value
  await doCreateAccount({
    customerId: parent.customerId,
    accountType: 'SUB_ACCOUNT',
    currency: 'TWD',
    parentAccountNumber: parent.accountNumber,
  })
  showConfirmModal.value = false
}

async function doCreateAccount(data) {
  createLoading.value = true
  try {
    const res = await createAccount(data)
    message.success('帳戶建立成功: ' + res.data.data.accountNumber)
    showCreateModal.value = false
    showConfirmModal.value = false

    createForm.customerId = ''
    createForm.accountType = undefined
    createForm.currency = undefined
    createForm.parentAccountNumber = ''
    parentAccountDetail.value = null

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
    accountNumberSearch.value = ''
    customerId.value = ''
    statusFilter.value = undefined
    typeFilter.value = undefined
    currencyFilter.value = undefined
    accounts.value = []
    total.value = 0
    currentPage.value = 1
  }

// === 狀態變更相關 ===
const showStatusModal = ref(false)
const statusModalLoading = ref(false)
const statusTarget = reactive({
  accountNumber: '',
  currentStatus: '',
  newStatus: undefined,
})
const closeConfirmText = ref('')

// 合法的狀態轉換選項
const validTransitions = {
  PENDING: ['ACTIVE'],
  ACTIVE: ['FROZEN', 'DORMANT', 'CLOSED'],
  FROZEN: ['ACTIVE', 'CLOSED'],
  DORMANT: ['ACTIVE', 'CLOSED'],
  CLOSED: [],
}

function openStatusModal(record) {
  statusTarget.accountNumber = record.accountNumber
  statusTarget.currentStatus = record.status
  statusTarget.newStatus = undefined
  closeConfirmText.value = ''
  showStatusModal.value = true
}

async function handleStatusChange() {
  if (!statusTarget.newStatus) {
    message.warning('請選擇目標狀態')
    return
  }
  if (statusTarget.newStatus === 'CLOSED' && closeConfirmText.value !== '我確定') {
    message.warning('請輸入「我確定」以確認銷戶')
    return
  }
  statusModalLoading.value = true
  try {
    await updateAccountStatus(statusTarget.accountNumber, statusTarget.newStatus)
    if (statusTarget.newStatus === 'FROZEN') {
      message.success('狀態變更成功，該客戶名下所有帳戶已連動凍結')
    } else {
      message.success('狀態變更成功')
    }
    showStatusModal.value = false
    closeConfirmText.value = ''
    await fetchData()
  } catch (err) {
    message.error(err.response?.data?.message || '狀態變更失敗')
  } finally {
    statusModalLoading.value = false
  }
}
</script>
