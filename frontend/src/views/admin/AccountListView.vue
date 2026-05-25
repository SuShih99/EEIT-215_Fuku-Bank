<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">帳戶管理</h2>
    </div>

    <!-- 頂部 F 橫劃：搜尋與主操作 -->
    <section class="filter-panel" aria-label="帳戶查詢工具列">
      <div class="filter-toolbar accounts-toolbar">
        <div class="filter-main">
        <a-form-item
          class="filter-form-item"
          :validate-status="filterErrors.customerName ? 'error' : ''"
          :help="filterErrors.customerName"
        >
          <a-input
            v-model:value="customerNameSearch"
            placeholder="顧客姓名"
            class="rounded-input filter-input"
            allow-clear
            @blur="validateCustomerName"
          />
        </a-form-item>

        <a-form-item
          class="filter-form-item"
          :validate-status="filterErrors.accountNumber ? 'error' : ''"
          :help="filterErrors.accountNumber"
        >
          <a-input
            v-model:value="accountNumberSearch"
            placeholder="搜尋帳號"
            class="rounded-input filter-input"
            allow-clear
            @blur="validateAccountNumber"
          />
        </a-form-item>

        <a-form-item
          class="filter-form-item"
          :validate-status="filterErrors.customerId ? 'error' : ''"
          :help="filterErrors.customerId"
        >
          <a-input
            v-model:value="customerId"
            placeholder="客戶ID"
            class="rounded-input filter-input"
            allow-clear
            @blur="validateCustomerId"
          />
        </a-form-item>

        <a-select
          v-model:value="statusFilter"
          placeholder="帳戶狀態"
          class="filter-select"
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
          class="filter-select"
          allow-clear
        >
          <a-select-option value="CHECKING">活存</a-select-option>
          <a-select-option value="SAVINGS">儲蓄</a-select-option>
          <a-select-option value="TIME_DEPOSIT">定存</a-select-option>
          <a-select-option value="LOAN">貸款</a-select-option>
          <a-select-option value="SUB_ACCOUNT">子帳戶</a-select-option>
          <a-select-option value="CREDIT_CARD">信用卡</a-select-option>
        </a-select>

        <a-select
          v-model:value="currencyFilter"
          placeholder="幣別"
          class="filter-select currency-select"
          allow-clear
        >
          <a-select-option value="TWD">TWD</a-select-option>
          <a-select-option value="USD">USD</a-select-option>
          <a-select-option value="EUR">EUR</a-select-option>
          <a-select-option value="JPY">JPY</a-select-option>
          <a-select-option value="GBP">GBP</a-select-option>
        </a-select>

        <a-button type="primary" class="rounded-btn" @click="handleSearch">
          <template #icon><SearchOutlined /></template>
          查詢
        </a-button>
        <a-button class="rounded-btn btn-ghost" @click="handleClear">清除</a-button>
        </div>

        <div class="filter-side">
        <a-button v-if="canCreateAccounts" type="primary" class="rounded-btn" @click="showCreateModal = true">
          <template #icon><PlusOutlined /></template>
          建立帳戶
        </a-button>
        </div>
      </div>
    </section>

    <section class="analysis-panel" aria-label="帳戶圖表分析">
      <div class="metric-card">
        <span>總帳戶數</span>
        <strong>{{ formatCompactAmount(statsData.totalAccounts) }}</strong>
        <small>全站總量</small>
      </div>
      <div class="metric-card">
        <span>正常帳戶</span>
        <strong>{{ formatCompactAmount(statsData.status?.ACTIVE || 0) }}</strong>
        <small>全站正常交易</small>
      </div>
      <div class="metric-card risk">
        <span>凍結帳戶</span>
        <strong>{{ formatCompactAmount(statsData.status?.FROZEN || 0) }}</strong>
        <small>需追蹤處理</small>
      </div>
      <div class="metric-card">
        <span>全站總餘額</span>
        <strong>{{ formatCompactAmount(statsData.totalBalance) }}</strong>
        <small>跨幣別等值(示意)</small>
      </div>
      <div class="chart-card">
        <div class="chart-title">狀態分布</div>
        <div class="chart-body" style="cursor: pointer;">
          <Doughnut :data="statusChartData" :options="statusChartOptions" />
        </div>
      </div>
      <div class="chart-card">
        <div class="chart-title">幣別分布</div>
        <div class="chart-body" style="cursor: pointer;">
          <Doughnut :data="currencyChartData" :options="currencyChartOptions" />
        </div>
      </div>
      <div class="chart-card wide">
        <div class="chart-title">帳戶型別</div>
        <div class="chart-body" style="cursor: pointer;">
          <Bar :data="typeChartData" :options="typeChartOptions" />
        </div>
      </div>
    </section>

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="accounts"
      :loading="loading"
      :scroll="{ x: 1000 }"
      row-key="accountNumber"
      class="custom-table"
      :pagination="{
        current: currentPage,
        pageSize: pageSize,
        total: total,
        showSizeChanger: true,
        showTotal: (t) => `共 ${t} 筆`,
      }"
      :locale="{ emptyText: '目前沒有帳戶資料', triggerDesc: '點擊降冪排序', triggerAsc: '點擊升冪排序', cancelSort: '取消排序' }"
      @change="handleTableChange"
      @resizeColumn="handleResizeColumn"
    >
      <template #bodyCell="{ column, record }">
        <!-- F 主幹：最強烈的視覺辨識 (帳號與客戶資訊) -->
        <template v-if="column.key === 'customer'">
          <div class="emp-name-cell">
            <div class="emp-avatar">{{ record.customerName ? record.customerName.charAt(0) : '?' }}</div>
            <div class="emp-info">
              <span class="emp-name-text">{{ record.customerName || '未知客戶' }}</span>
              <span class="emp-id-text">{{ record.customerId }}</span>
            </div>
          </div>
        </template>
        
        <template v-else-if="column.key === 'accountNumber'">
          <span style="font-weight: 600; color: #1a1a2e;">{{ record.accountNumber }}</span>
        </template>

        <!-- 狀態顯示 -->
        <template v-else-if="column.key === 'status'">
          <div :class="['status-tag', `status-${record.status.toLowerCase()}`]">
            <span class="status-dot"></span>
            {{ statusMap[record.status] || record.status }}
          </div>
        </template>

        <!-- F 終點：行動按鈕 -->
        <template v-else-if="column.key === 'action' && canSeeAccountActions">
          <div class="action-cell">
            <a-button 
              v-if="record.status !== 'CLOSED'"
              type="link" 
              class="action-btn edit-btn" 
              @click="openStatusModal(record)"
            >
              變更狀態
            </a-button>
            <span v-else style="color: #999; padding-right: 16px;">不可操作</span>
          </div>
        </template>
      </template>
    </a-table>

    <!-- 建立帳戶 Modal -->
    <a-modal
      v-model:open="showCreateModal"
      title="建立帳戶"
      @ok="handleCreate"
      :confirm-loading="createLoading"
    >
      <a-form layout="vertical">
        <!-- 一鍵帶入 -->
        <div class="demo-fill-section">
          <span style="font-size: 12px; color: #999; margin-right: 8px">快速帶入：</span>
          <a-button size="small" @click="fillDemoAccount('CHECKING', 'TWD')">活存 TWD</a-button>
          <a-button size="small" @click="fillDemoAccount('CHECKING', 'USD')">活存 USD</a-button>
          <a-button size="small" @click="fillDemoAccount('TIME_DEPOSIT', 'TWD')">定存 TWD</a-button>
          <a-button size="small" @click="fillDemoAccount('LOAN', 'TWD')">貸款 TWD</a-button>
        </div>

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
          <a-descriptions-item label="客戶姓名">{{ parentAccountDetail.customerName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="帳戶型別">{{ typeMap[parentAccountDetail.accountType] || parentAccountDetail.accountType }}</a-descriptions-item>
          <a-descriptions-item label="幣別">{{ parentAccountDetail.currency }}</a-descriptions-item>
          <a-descriptions-item label="餘額">{{ formatAmount(parentAccountDetail.balance) }}</a-descriptions-item>
          <a-descriptions-item label="狀態">{{ statusMap[parentAccountDetail.status] || parentAccountDetail.status }}</a-descriptions-item>
        </a-descriptions>
        <div style="margin-top: 16px; color: #666">
          將為客戶 <strong>{{ parentAccountDetail.customerName || parentAccountDetail.customerId }}</strong> 建立 TWD 子帳戶，父帳戶為 <strong>{{ parentAccountDetail.accountNumber }}</strong>。
        </div>
      </div>
    </a-modal>

    <!-- 狀態變更 Modal -->
    <a-modal
      v-model:open="showStatusModal"
      title="變更帳戶狀態"
      ok-text="確認變更"
      cancel-text="取消"
      @ok="handleStatusChange"
      :confirm-loading="statusModalLoading"
    >
      <a-alert
        v-if="statusActionError"
        :message="statusActionError"
        type="error"
        show-icon
        style="margin-bottom: 16px"
      />
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
          <div class="close-confirm-row">
            <a-input v-model:value="closeConfirmText" placeholder="請輸入「我確定」以確認銷戶" />
            <a-button class="close-confirm-fill-btn" @click="fillCloseConfirmText">
              帶入我確定
            </a-button>
          </div>
          <div style="margin-top: 4px; color: #ff4d4f; font-size: 12px">
            銷戶為不可逆操作，請輸入「我確定」後才能執行。
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { Bar, Doughnut } from 'vue-chartjs'
import {
  ArcElement,
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  Tooltip,
} from 'chart.js'
import { getErrorMessage } from '@/utils/errorMessages'
import { useAuthStore } from '@/stores/auth'
import {
  searchAdminAccounts,
  createAccount,
  updateAccountStatus,
  getAccount,
  getAccountsStats,
} from '@/api/account'

ChartJS.register(ArcElement, BarElement, CategoryScale, LinearScale, Tooltip, Legend)

const authStore = useAuthStore()
const canCreateAccounts = computed(() => authStore.user?.roleCode === 'CFDM')
const canSeeAccountActions = computed(() => ['CFDM', 'CFSO'].includes(authStore.user?.roleCode))
const statusMap = {
  PENDING: '待啟用',
  ACTIVE: '正常',
  FROZEN: '凍結',
  DORMANT: '靜止戶',
  CLOSED: '已銷戶',
}

const typeMap = {
  CHECKING: '活存',
  SAVINGS: '儲蓄',
  TIME_DEPOSIT: '定存',
  LOAN: '貸款',
  SUB_ACCOUNT: '子帳戶',
  CREDIT_CARD: '信用卡',
}
function formatAmount(value) {
  if (value == null) return '-'
  return Number(value).toLocaleString()
}

function formatCompactAmount(value) {
  const amount = Number(value || 0)
  if (amount >= 100000000) return `${(amount / 100000000).toFixed(1)} 億`
  if (amount >= 10000) return `${(amount / 10000).toFixed(1)} 萬`
  return amount.toLocaleString()
}

function formatTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ').substring(0, 19)
}
const customerNameSearch = ref('')
const accountNumberSearch = ref('')
const customerId = ref('')
const statusFilter = ref(undefined)
const typeFilter = ref(undefined)
const currencyFilter = ref(undefined)
const filterErrors = reactive({
  customerName: '',
  accountNumber: '',
  customerId: '',
})
const accounts = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const chartPalette = ['#5C6B5F', '#A65A4D', '#C49A3C', '#78909C', '#8D7B68', '#B7A58E']
const compactChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'bottom',
      labels: { boxWidth: 8, usePointStyle: true, font: { size: 11 } },
    },
  },
}
const barChartOptions = {
  ...compactChartOptions,
  scales: {
    y: {
      beginAtZero: true,
      ticks: { precision: 0 },
      grid: { color: 'rgba(92, 107, 95, 0.09)' },
    },
    x: { grid: { display: false } },
  },
}

const statusChartOptions = {
  ...compactChartOptions,
  plugins: {
    ...compactChartOptions.plugins,
    legend: {
      ...compactChartOptions.plugins.legend,
      labels: { boxWidth: 8, usePointStyle: true, font: { size: 11 } },
    },
  },
  onClick: (event, elements, chart) => {
    if (elements.length > 0) {
      const index = elements[0].index
      const label = chart.data.labels[index]
      const entry = Object.entries(statusMap).find(([, val]) => val === label)
      if (entry) {
        statusFilter.value = entry[0]
        handleSearch()
      }
    }
  }
}

const currencyChartOptions = {
  ...compactChartOptions,
  plugins: {
    ...compactChartOptions.plugins,
    legend: {
      ...compactChartOptions.plugins.legend,
      labels: { boxWidth: 8, usePointStyle: true, font: { size: 11 } },
    },
  },
  onClick: (event, elements, chart) => {
    if (elements.length > 0) {
      const index = elements[0].index
      const label = chart.data.labels[index]
      currencyFilter.value = label
      handleSearch()
    }
  }
}

const typeChartOptions = {
  ...barChartOptions,
  plugins: {
    ...barChartOptions.plugins,
    legend: {
      ...barChartOptions.plugins.legend,
      labels: { boxWidth: 8, usePointStyle: true, font: { size: 11 } },
    },
  },
  onClick: (event, elements, chart) => {
    if (elements.length > 0) {
      const index = elements[0].index
      const label = chart.data.labels[index]
      const entry = Object.entries(typeMap).find(([, val]) => val === label)
      if (entry) {
        typeFilter.value = entry[0]
        handleSearch()
      }
    }
  }
}

function countBy(items, getter) {
  return items.reduce((acc, item) => {
    const key = getter(item) || '未分類'
    acc[key] = (acc[key] || 0) + 1
    return acc
  }, {})
}

function buildChartData(source, label) {
  const entries = Object.entries(source)
  return {
    labels: entries.map(([key]) => key),
    datasets: [{
      label,
      data: entries.map(([, value]) => value),
      backgroundColor: entries.map((_, index) => chartPalette[index % chartPalette.length]),
      borderWidth: 0,
      borderRadius: 6,
    }],
  }
}

const accountsOnPage = computed(() => accounts.value.length)
const activeAccounts = computed(() => accounts.value.filter(account => account.status === 'ACTIVE').length)
const frozenAccounts = computed(() => accounts.value.filter(account => account.status === 'FROZEN').length)
const pageBalanceTotal = computed(() => accounts.value.reduce((sum, account) => sum + Number(account.balance || 0), 0))

const statsData = ref({
  status: {},
  currency: {},
  accountType: {},
})

const statusChartData = computed(() => {
  const source = {}
  Object.entries(statsData.value.status).forEach(([key, val]) => {
    const label = statusMap[key] || key
    source[label] = (source[label] || 0) + val
  })
  return buildChartData(source, '帳戶狀態')
})

const currencyChartData = computed(() => {
  const source = {}
  Object.entries(statsData.value.currency).forEach(([key, val]) => {
    source[key] = (source[key] || 0) + val
  })
  return buildChartData(source, '幣別')
})

const typeChartData = computed(() => {
  const source = {}
  Object.entries(statsData.value.accountType).forEach(([key, val]) => {
    const label = typeMap[key] || key
    source[label] = (source[label] || 0) + val
  })
  return buildChartData(source, '帳戶型別')
})

// 記錄當前用哪種查詢，換頁時要用
const lastSearchType = ref('')

const hasFilterErrors = computed(() => Object.values(filterErrors).some(Boolean))

const fetchStats = async () => {
  try {
    const res = await getAccountsStats()
    statsData.value = res.data.data || { status: {}, currency: {}, accountType: {} }
  } catch (error) {
    console.error('Failed to fetch stats:', error)
  }
}

onMounted(() => {
  fetchData()
  fetchStats()
})

const baseColumns = [
  { title: '客戶資訊', dataIndex: 'customerName', key: 'customer', width: 160, fixed: 'left', resizable: true, sorter: (a, b) => (a.customerName || '').localeCompare(b.customerName || '') },
  { title: '帳號', dataIndex: 'accountNumber', key: 'accountNumber', width: 150, resizable: true, sorter: (a, b) => (a.accountNumber || '').localeCompare(b.accountNumber || '') },
  {
    title: '型別',
    dataIndex: 'accountType',
    key: 'accountType',
    width: 100,
    resizable: true,
    sorter: (a, b) => (a.accountType || '').localeCompare(b.accountType || ''),
    customRender: ({ text }) => typeMap[text] || text,
  },
  { title: '幣別', dataIndex: 'currency', key: 'currency', width: 80, resizable: true, sorter: (a, b) => (a.currency || '').localeCompare(b.currency || '') },
  {
    title: '餘額',
    dataIndex: 'balance',
    key: 'balance',
    width: 130,
    resizable: true,
    align: 'right',
    sorter: (a, b) => (a.balance || 0) - (b.balance || 0),
    customRender: ({ text }) => formatAmount(text),
  },
  { title: '狀態', dataIndex: 'status', key: 'status', width: 100, resizable: true, sorter: (a, b) => (a.status || '').localeCompare(b.status || '') },
  {
    title: '建立時間',
    dataIndex: 'createdAt',
    key: 'createdAt',
    width: 160,
    resizable: true,
    sorter: (a, b) => (a.createdAt || '').localeCompare(b.createdAt || ''),
    customRender: ({ text }) => formatTime(text),
  },
]

const columns = computed(() => {
  if (!canSeeAccountActions.value) {
    return baseColumns
  }
  return [...baseColumns, { title: '操作', key: 'action', width: 120, fixed: 'right' }]
})

function handleResizeColumn(w, col) {
  col.width = w
}

async function handleSearch() {
  validateFilters()
  if (hasFilterErrors.value) {
    return
  }
  currentPage.value = 1
  await fetchData()
}

async function handleTableChange(pagination) {
  currentPage.value = pagination.current
  pageSize.value = pagination.pageSize
  await fetchData()
}

async function fetchData() {
  validateFilters()
  if (hasFilterErrors.value) {
    return
  }

  loading.value = true
  try {
    const page = currentPage.value - 1
    const size = pageSize.value
    lastSearchType.value = 'adminSearch'
    const res = await searchAdminAccounts(buildSearchParams(), page, size)

    accounts.value = res.data.data.content
    total.value = res.data.data.totalElements
  } catch (err) {
    message.error(getErrorMessage(err, '查詢失敗'))
  } finally {
    loading.value = false
  }
}

function buildSearchParams() {
  return {
    customerName: normalizeFilterValue(customerNameSearch.value),
    accountNumber: normalizeFilterValue(accountNumberSearch.value),
    customerId: normalizeFilterValue(customerId.value),
    status: statusFilter.value || undefined,
    type: typeFilter.value || undefined,
    currency: currencyFilter.value || undefined,
  }
}

function normalizeFilterValue(value) {
  const normalized = value?.trim()
  return normalized || undefined
}

function validateFilters() {
  validateCustomerName()
  validateAccountNumber()
  validateCustomerId()
}

function validateCustomerName() {
  filterErrors.customerName = /[0-9０-９]/.test(customerNameSearch.value || '')
    ? '顧客姓名不可包含數字'
    : ''
}

function validateAccountNumber() {
  filterErrors.accountNumber = /[^0-9]/.test(accountNumberSearch.value || '')
    ? '帳號只能輸入數字'
    : ''
}

function validateCustomerId() {
  filterErrors.customerId = /[\u3400-\u9fff]/.test(customerId.value || '')
    ? '客戶 ID 不可包含中文'
    : ''
}
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
      message.error(getErrorMessage(err, '查詢父帳戶失敗'))
    } finally {
      createLoading.value = false
    }
    return
  }

  // 非子帳戶，直接建立
  await doCreateAccount({
    customerId: createForm.customerId,
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
    message.error(getErrorMessage(err, '建立失敗'))
  } finally {
    createLoading.value = false
  }
}
// customer_profile 的真實 customer_id（來自 seed 資料）
const demoCustomerIds = [
  'X7K9P2M4', 'V4L6T1Y8', 'D3H8F5G2', 'B9W1C7R5', 'P6M4N2Q8',
  'K1T9V5L3', 'Y5R4W1H6', 'G7N3M8P2', 'J2F6K9V1', 'Q4W8C1T7',
]

function fillDemoAccount(type, currency) {
  createForm.accountType = type
  createForm.currency = currency
  createForm.customerId = demoCustomerIds[Math.floor(Math.random() * demoCustomerIds.length)]
  createForm.parentAccountNumber = ''
}

async function handleClear() {
  customerNameSearch.value = ''
  accountNumberSearch.value = ''
  customerId.value = ''
  statusFilter.value = undefined
  typeFilter.value = undefined
  currencyFilter.value = undefined
  filterErrors.customerName = ''
  filterErrors.accountNumber = ''
  filterErrors.customerId = ''
  accounts.value = []
  total.value = 0
  currentPage.value = 1
  await fetchData()
}
const showStatusModal = ref(false)
const statusModalLoading = ref(false)
const statusTarget = reactive({
  accountNumber: '',
  currentStatus: '',
  newStatus: undefined,
})
const closeConfirmText = ref('')
const statusActionError = ref('')

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
  statusActionError.value = ''
  showStatusModal.value = true
}

function fillCloseConfirmText() {
  closeConfirmText.value = '我確定'
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
    statusActionError.value = getErrorMessage(err, '狀態變更失敗')
    message.error(statusActionError.value)
  } finally {
    statusModalLoading.value = false
  }
}
</script>

<style scoped>
.analysis-panel {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr));
  gap: 14px;
  margin-bottom: 20px;
}

.metric-card,
.chart-card {
  border: 1px solid rgba(214, 206, 195, 0.72);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(255, 249, 239, 0.92), rgba(249, 244, 235, 0.82)),
    url('/washi-texture.webp');
  background-size: auto, 260px 260px;
  box-shadow: 0 10px 28px rgba(63, 74, 66, 0.08);
}

.metric-card {
  min-height: 104px;
  padding: 16px;
  display: grid;
  gap: 6px;
}

.metric-card span,
.chart-title {
  color: #5C6B5F;
  font-size: 13px;
  font-weight: 700;
}

.metric-card strong {
  color: #2B2B2B;
  font-size: 30px;
  line-height: 1;
}

.metric-card small {
  color: #8c8c8c;
  font-size: 12px;
}

.metric-card.risk strong {
  color: #A65A4D;
}

.chart-card {
  min-height: 226px;
  padding: 12px 14px;
}

.chart-card.wide {
  grid-column: span 2;
}

.chart-body {
  position: relative;
  height: 176px;
  margin-top: 8px;
}

.chart-body :deep(canvas) {
  width: 100% !important;
  height: 100% !important;
}

.filter-panel {
  margin-bottom: 16px;
}

.filter-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: start;
  padding: 16px;
  border: 1px solid rgba(214, 206, 195, 0.82);
  border-radius: 12px;
  background: rgba(255, 249, 239, 0.72);
  box-shadow: 0 6px 16px rgba(63, 74, 66, 0.05);
}

.filter-main {
  display: grid;
  grid-template-columns:
    minmax(150px, 1fr)
    minmax(150px, 1fr)
    minmax(150px, 1fr)
    minmax(132px, 0.78fr)
    minmax(132px, 0.78fr)
    minmax(112px, 0.62fr)
    auto
    auto;
  gap: 12px;
  align-items: start;
  min-width: 0;
}

.filter-side {
  display: flex;
  align-items: start;
  justify-content: flex-end;
}

.filter-input,
.filter-select,
.filter-main :deep(.ant-input-affix-wrapper),
.filter-main :deep(.ant-select) {
  width: 100%;
  min-width: 0;
}

.filter-main :deep(.ant-input),
.filter-main :deep(.ant-select-selector) {
  min-width: 0;
}

.filter-main :deep(.ant-form-item) {
  margin-bottom: 0;
}

.filter-main :deep(.ant-form-item-explain-error) {
  font-size: 12px;
  line-height: 1.3;
  margin-top: 4px;
}

.accounts-toolbar .filter-side :deep(.ant-btn) {
  min-width: 120px;
}

.filter-form-item {
  margin-bottom: 0;
  min-width: 0;
}

.demo-fill-section {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 16px;
  padding: 10px 12px;
  background: #fafafa;
  border-radius: 6px;
  border: 1px dashed #d9d9d9;
}

.close-confirm-row {
  display: flex;
  gap: 8px;
}

.close-confirm-row :deep(.ant-input) {
  flex: 1 1 auto;
  min-width: 0;
}

.close-confirm-fill-btn {
  flex: 0 0 auto;
  color: #a65a4d;
  background: rgba(255, 249, 239, 0.72);
  border-color: rgba(166, 90, 77, 0.28);
  border-radius: 8px;
  font-weight: 600;
}

.close-confirm-fill-btn:hover,
.close-confirm-fill-btn:focus {
  color: #8f463d;
  background: rgba(166, 90, 77, 0.08);
  border-color: rgba(166, 90, 77, 0.42);
}

@media (max-width: 980px) {
  .analysis-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .filter-toolbar {
    grid-template-columns: 1fr;
  }

  .filter-main {
    grid-template-columns: repeat(2, minmax(180px, 1fr));
  }

  .filter-side {
    justify-content: flex-start;
  }
}

@media (max-width: 560px) {
  .analysis-panel,
  .chart-card.wide {
    grid-template-columns: 1fr;
    grid-column: auto;
  }

  .filter-main :deep(.ant-input),
  .filter-main :deep(.ant-select-selector) {
    min-width: 0;
    width: 100%;
  }

  .filter-main {
    grid-template-columns: 1fr;
  }

  .filter-side,
  .filter-side :deep(.ant-btn) {
    width: 100%;
  }

  .close-confirm-row {
    flex-wrap: wrap;
  }

  .close-confirm-fill-btn {
    width: 100%;
  }
}

/* F-Pattern 專用組件 */
.emp-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.emp-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: rgba(92, 107, 95, 0.1);
  color: #5C6B5F;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 14px;
}

.emp-info {
  display: flex;
  flex-direction: column;
}

.emp-name-text {
  font-weight: 600;
  color: #1a1a2e;
  font-size: 14px;
}

.emp-id-text {
  font-size: 11px;
  color: #8c8c8c;
  margin-top: 2px;
}

/* 狀態標籤 */
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.status-active { background-color: rgba(82, 196, 26, 0.1); color: #389e0d; }
.status-active .status-dot { background-color: #52c41a; }

.status-frozen { background-color: rgba(255, 77, 79, 0.1); color: #d9363e; }
.status-frozen .status-dot { background-color: #ff4d4f; }

.status-dormant { background-color: rgba(250, 140, 22, 0.1); color: #fa8c16; }
.status-dormant .status-dot { background-color: #fa8c16; }

.status-pending { background-color: rgba(22, 119, 255, 0.1); color: #1677ff; }
.status-pending .status-dot { background-color: #1677ff; }

.status-closed { background-color: #f5f5f5; color: #8c8c8c; border: 1px solid #d9d9d9;}
.status-closed .status-dot { background-color: #bfbfbf; }
</style>
