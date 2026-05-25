<template>
  <div class="page-container">
    <div class="page-header admin-translog-header">
      <div class="header-copy">
        <h2 class="page-title">交易紀錄查詢</h2>
        <p class="page-subtitle">查詢、篩選與檢視所有帳戶交易紀錄</p>
      </div>
      <a-button danger class="rounded-btn" @click="showReversalModal = true">
        <template #icon><RollbackOutlined /></template>
        沖正
      </a-button>
    </div>

    <section class="filter-panel" aria-label="交易紀錄查詢工具列">
      <div class="filter-toolbar">
        <div class="filter-main">
          <a-input
            v-model:value="searchValue"
            :placeholder="placeholderText"
            class="rounded-input search-input"
            allow-clear
            @press-enter="handleSearch"
          />

          <template v-if="searchType === 'customerIdDateRange'">
            <a-date-picker
              v-model:value="startDate"
              show-time
              placeholder="開始時間"
              format="YYYY-MM-DD HH:mm:ss"
            />
            <a-date-picker
              v-model:value="endDate"
              show-time
              placeholder="結束時間"
              format="YYYY-MM-DD HH:mm:ss"
            />
          </template>

          <a-button type="primary" class="rounded-btn" @click="handleSearch">
            <template #icon><SearchOutlined /></template>
            查詢
          </a-button>
          <a-button class="rounded-btn btn-ghost" @click="handleClear">清除</a-button>
        </div>

        <div class="filter-side">
          <a-radio-group v-model:value="searchType" button-style="solid" @change="handleSearchTypeChange">
            <a-radio-button value="referenceId">依交易編號</a-radio-button>
            <a-radio-button value="accountNumber">依帳號</a-radio-button>
            <a-radio-button value="customerId">依客戶 ID</a-radio-button>
            <a-radio-button value="customerIdDateRange">依客戶 ID + 日期</a-radio-button>
          </a-radio-group>
          <a-dropdown v-if="logs.length > 0">
            <a-button class="rounded-btn btn-ghost">
              匯出 <DownOutlined />
          </a-button>
          <template #overlay>
            <a-menu @click="handleExport">
              <a-menu-item key="excel">Excel (.xlsx)</a-menu-item>
              <a-menu-item key="json">JSON (.json)</a-menu-item>
              <a-menu-item key="xml">XML (.xml)</a-menu-item>
            </a-menu>
              </template>
            </a-dropdown>
        </div>
      </div>
    </section>

    <section class="analysis-panel" aria-label="交易紀錄圖表分析">
      <div class="metric-card">
        <span>總交易數</span>
        <strong>{{ formatCompactAmount(statsData.totalTransactions) }}</strong>
        <small>全站總紀錄</small>
      </div>
      <div class="metric-card risk">
        <span>總轉出金額</span>
        <strong>{{ formatCompactAmount(statsData.totalDebit) }}</strong>
        <small>全站資金流出</small>
      </div>
      <div class="metric-card">
        <span>總轉入金額</span>
        <strong>{{ formatCompactAmount(statsData.totalCredit) }}</strong>
        <small>全站資金流入</small>
      </div>
      <div class="metric-card">
        <span>單筆最大</span>
        <strong>{{ formatCompactAmount(statsData.maxTransactionAmount) }}</strong>
        <small>歷史最大金額</small>
      </div>
      <div class="chart-card">
        <div class="chart-title">收支比例</div>
        <div class="chart-body">
          <Doughnut :data="entryChartData" :options="compactChartOptions" />
        </div>
      </div>
      <div class="chart-card">
        <div class="chart-title">幣別占比</div>
        <div class="chart-body">
          <Doughnut :data="currencyChartData" :options="compactChartOptions" />
        </div>
      </div>
      <div class="chart-card wide">
        <div class="chart-title">交易類型統計</div>
        <div class="chart-body">
          <Bar :data="transactionTypeChartData" :options="barChartOptions" />
        </div>
      </div>
    </section>

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="logs"
      :loading="loading"
      :scroll="{ x: 1200 }"
      row-key="transactionId"
      class="custom-table"
      :pagination="searchType === 'referenceId' && searchValue ? false : {
        current: currentPage,
        pageSize: pageSize,
        total: total,
        showSizeChanger: true,
        showTotal: (t) => `共 ${t} 筆`,
      }"
      :locale="{ emptyText: '目前沒有交易紀錄', triggerDesc: '點擊降冪排序', triggerAsc: '點擊升冪排序', cancelSort: '取消排序' }"
      @change="handleTableChange"
      @resizeColumn="handleResizeColumn"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'referenceId'">
          <span class="reference-cell">
            <span class="reference-text">{{ record.referenceId || '-' }}</span>
            <a-tooltip v-if="record.referenceId" title="複製交易編號">
              <a-button
                type="text"
                size="small"
                class="copy-reference-btn"
                aria-label="複製交易編號"
                @click.stop="copyText(record.referenceId, '已複製交易編號')"
              >
                <template #icon><CopyOutlined /></template>
              </a-button>
            </a-tooltip>
          </span>
        </template>
        <template v-else-if="column.key === 'entryType'">
          <div :class="['status-tag', `entry-${record.entryType.toLowerCase()}`]">
            <span class="status-dot"></span>
            {{ entryTypeMap[record.entryType] || record.entryType }}
          </div>
        </template>
        <template v-else-if="column.key === 'transactionType'">
          <a-tag color="blue">{{ transactionTypeMap[record.transactionType] || record.transactionType }}</a-tag>
        </template>
        <template v-else-if="column.key === 'accountNumber'">
          <span v-if="isBusinessAccountLabel(record.accountNumber)" style="font-weight:600">{{ record.accountNumber }}</span>
          <a v-else @click="handleClickAccount(record.accountNumber)" style="font-weight:600">{{ record.accountNumber }}</a>
        </template>
        <template v-else-if="column.key === 'counterpartAccount'">
          <span v-if="isBusinessAccountLabel(record.counterpartAccount)">{{ record.counterpartAccount }}</span>
          <a v-else-if="record.counterpartAccount" @click="handleClickAccount(record.counterpartAccount)">{{ record.counterpartAccount }}</a>
          <span v-else>-</span>
        </template>
      </template>
    </a-table>

    <!-- 帳戶詳情彈窗 -->
    <a-modal
      v-model:open="showAccountModal"
      title="帳戶詳情"
      :footer="null"
      width="500px"
      :z-index="1100"
    >
      <a-spin v-if="accountLoading" style="display: block; text-align: center; padding: 24px" />
      <a-descriptions
        v-else-if="accountDetail"
        bordered
        :column="1"
        size="small"
      >
        <a-descriptions-item label="帳號">{{ accountDetail.accountNumber }}</a-descriptions-item>
        <a-descriptions-item label="客戶 ID">{{ accountDetail.customerId }}</a-descriptions-item>
        <a-descriptions-item label="客戶姓名">{{ accountDetail.customerName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="帳戶型別">{{ typeMap[accountDetail.accountType] || accountDetail.accountType }}</a-descriptions-item>
        <a-descriptions-item label="幣別">{{ accountDetail.currency }}</a-descriptions-item>
        <a-descriptions-item label="餘額">{{ formatAmount(accountDetail.balance) }}</a-descriptions-item>
        <a-descriptions-item label="狀態">{{ statusMap[accountDetail.status] || accountDetail.status }}</a-descriptions-item>
        <a-descriptions-item label="利率">{{ accountDetail.interestRate ?? '-' }}</a-descriptions-item>
        <a-descriptions-item label="父帳戶">{{ accountDetail.parentAccountNumber || '-' }}</a-descriptions-item>
        <a-descriptions-item label="建立時間">{{ formatTime(accountDetail.createdAt) }}</a-descriptions-item>
        <a-descriptions-item label="更新時間">{{ formatTime(accountDetail.updatedAt) }}</a-descriptions-item>
      </a-descriptions>
      <div v-else style="text-align: center; padding: 24px; color: #999">查無資料</div>
    </a-modal>

    <!-- 沖正彈窗 -->
    <a-modal
      v-model:open="showReversalModal"
      title="交易沖正"
      :footer="null"
      width="500px"
      @cancel="handleReversalClear"
    >
      <a-form layout="vertical" style="margin-top: 16px;">
        <a-form-item label="原始交易編號">
          <a-input v-model:value="reversalForm.originalReferenceId" placeholder="請輸入要沖正的交易編號 (TXN-...)" allow-clear />
        </a-form-item>
        <a-form-item label="沖正原因">
          <a-input v-model:value="reversalForm.reason" placeholder="選填" allow-clear />
        </a-form-item>
        <div style="display: flex; justify-content: flex-end; gap: 12px; margin-top: 16px;">
          <a-button class="rounded-btn btn-ghost" @click="handleReversalClear">取消</a-button>
          <a-button type="primary" danger class="rounded-btn" :loading="reversalLoading" @click="handleReversalSubmit">確認沖正</a-button>
        </div>
      </a-form>

      <!-- 沖正結果 -->
      <div v-if="reversalResult" style="margin-top: 24px; border-top: 1px solid #f0f0f0; padding-top: 16px;">
        <h4 style="margin-bottom: 12px;">沖正結果</h4>
        <p>
          沖正交易編號: <a @click="lookupReferenceId(reversalResult.reversalReferenceId)">{{ reversalResult.reversalReferenceId }}</a>
          <a-button size="small" style="margin-left: 8px" @click="copyText(reversalResult.reversalReferenceId)">複製</a-button>
        </p>
        <p>被沖正的原始編號: <a @click="lookupReferenceId(reversalResult.originalReferenceId)">{{ reversalResult.originalReferenceId }}</a></p>
        <p>沖正時間: {{ formatTime(reversalResult.reversedAt) }}</p>
        <a-divider style="margin: 12px 0" />
        <p style="font-weight: bold; margin-bottom: 8px">沖正明細：</p>
        <div v-for="(detail, index) in reversalResult.details" :key="index" style="margin-bottom: 8px; padding: 8px; background: #fafafa; border-radius: 4px">
          <p>帳號: <a @click="handleClickAccount(detail.accountNumber)">{{ detail.accountNumber }}</a></p>
          <p>沖正金額: {{ formatAmount(detail.reversedAmount) }}</p>
          <p>沖正後餘額: {{ formatAmount(detail.balanceAfter) }}</p>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, h, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { CopyOutlined, DownOutlined, SearchOutlined, RollbackOutlined } from '@ant-design/icons-vue'
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
import * as XLSX from 'xlsx'
import { saveAs } from 'file-saver'
import {
  getTransLogsByReferenceId,
  getTransLogsByAccountNumber,
  getTransLogsByCustomerId,
  getTransLogsByCustomerIdAndDateRange,
  getLatestTransLogs,
  getAccount,
  reversal,
  getTransLogsStats,
} from '@/api/account'

ChartJS.register(ArcElement, BarElement, CategoryScale, LinearScale, Tooltip, Legend)

const searchType = ref('referenceId')
const BUSINESS_ACCOUNT_LABEL = '銀行業務帳戶'
const searchValue = ref('')
const startDate = ref(null)
const endDate = ref(null)
const logs = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const showAccountModal = ref(false)
const accountDetail = ref(null)
const accountLoading = ref(false)

const fetchStats = async () => {
  try {
    const res = await getTransLogsStats()
    statsData.value = res.data.data || { entryType: {}, transactionType: {}, currency: {} }
  } catch (error) {
    console.error('Failed to fetch stats:', error)
  }
}

onMounted(() => {
  fetchData()
  fetchStats()
})

const placeholderText = computed(() => {
  const map = {
    referenceId: '請輸入交易編號 (TXN-...)',
    accountNumber: '請輸入帳號',
    customerId: '請輸入客戶 ID',
    customerIdDateRange: '請輸入客戶 ID',
  }
  return map[searchType.value]
})

const entryTypeMap = {
  DEBIT: '轉出',
  CREDIT: '轉入',
}

const transactionTypeMap = {
  TRANSFER: '轉帳',
  DEPOSIT: '存款',
  WITHDRAW: '提款',
  EXCHANGE: '換匯',
  INTEREST: '利息',
  LOAN_DISBURSEMENT: '貸款撥款',
  LOAN_REPAYMENT: '貸款還款',
  CARD_PAYMENT: '信用卡繳款',
  CARD_SETTLEMENT: '信用卡結算',
  CARD_REWARD: '信用卡回饋',
  REVERSAL: '沖正',
  TRANSFER_FEE: '轉帳手續費',
}

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

const chartPalette = ['#5C6B5F', '#A65A4D', '#C49A3C', '#78909C', '#8D7B68', '#B7A58E']
const compactChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'bottom',
      labels: { boxWidth: 10, usePointStyle: true },
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

const logsOnPage = computed(() => logs.value.length)
const debitTotal = computed(() => logs.value
  .filter(log => log.entryType === 'DEBIT')
  .reduce((sum, log) => sum + Number(log.amount || 0), 0))
const creditTotal = computed(() => logs.value
  .filter(log => log.entryType === 'CREDIT')
  .reduce((sum, log) => sum + Number(log.amount || 0), 0))
const maxTransactionAmount = computed(() => logs.value.reduce((max, log) => Math.max(max, Number(log.amount || 0)), 0))



const statsData = ref({
  entryType: {},
  transactionType: {},
  currency: {},
})

const entryChartData = computed(() => {
  const source = {}
  Object.entries(statsData.value.entryType).forEach(([key, val]) => {
    const label = entryTypeMap[key] || key
    source[label] = (source[label] || 0) + val
  })
  return buildChartData(source, '交易方向')
})

const transactionTypeChartData = computed(() => {
  const source = {}
  Object.entries(statsData.value.transactionType).forEach(([key, val]) => {
    const label = transactionTypeMap[key] || key
    source[label] = (source[label] || 0) + val
  })
  return buildChartData(source, '交易類型')
})

const currencyChartData = computed(() => {
  const source = {}
  Object.entries(statsData.value.currency).forEach(([key, val]) => {
    source[key] = (source[key] || 0) + val
  })
  return buildChartData(source, '幣別')
})

const columns = ref([
  { title: '交易編號', dataIndex: 'referenceId', key: 'referenceId', width: 260, resizable: true, sorter: (a, b) => (a.referenceId || '').localeCompare(b.referenceId || '') },
  {
    title: '帳號',
    dataIndex: 'accountNumber',
    key: 'accountNumber',
    width: 260,
    resizable: true,
    sorter: (a, b) => (a.accountNumber || '').localeCompare(b.accountNumber || ''),
    customRender: ({ text }) => {
      if (!text) return '-'
      if (isBusinessAccountLabel(text)) return text
      return h('a', { onClick: () => handleClickAccount(text) }, text)
    },
  },
  {
    title: '對手方',
    dataIndex: 'counterpartAccount',
    key: 'counterpartAccount',
    width: 140,
    resizable: true,
    sorter: (a, b) => (a.counterpartAccount || '').localeCompare(b.counterpartAccount || ''),
    customRender: ({ text }) => {
      if (!text) return '-'
      if (isBusinessAccountLabel(text)) return text
      return h('a', { onClick: () => handleClickAccount(text) }, text)
    },
  },
  {
    title: '方向',
    dataIndex: 'entryType',
    key: 'entryType',
    width: 90,
    resizable: true,
    sorter: (a, b) => (a.entryType || '').localeCompare(b.entryType || ''),
  },
  {
    title: '類型',
    dataIndex: 'transactionType',
    key: 'transactionType',
    width: 100,
    resizable: true,
    sorter: (a, b) => (a.transactionType || '').localeCompare(b.transactionType || ''),
    customRender: ({ text, record }) => {
      const type = record?.transactionType || text
      const cls = `transaction-badge type-${String(type || '').toLowerCase()}`
      return h('span', { class: cls }, transactionTypeMap[type] || type || '-')
    },
  },
  {
    title: '金額',
    dataIndex: 'amount',
    key: 'amount',
    width: 120,
    resizable: true,
    align: 'right',
    sorter: (a, b) => (a.amount || 0) - (b.amount || 0),
    customRender: ({ text }) => formatAmount(text),
  },
  {
    title: '交易前餘額',
    dataIndex: 'balanceBefore',
    key: 'balanceBefore',
    width: 120,
    resizable: true,
    align: 'right',
    sorter: (a, b) => (a.balanceBefore || 0) - (b.balanceBefore || 0),
    customRender: ({ text }) => formatAmount(text),
  },
  {
    title: '交易後餘額',
    dataIndex: 'balanceAfter',
    key: 'balanceAfter',
    width: 120,
    resizable: true,
    align: 'right',
    sorter: (a, b) => (a.balanceAfter || 0) - (b.balanceAfter || 0),
    customRender: ({ text }) => formatAmount(text),
  },
  {
    title: '幣別',
    dataIndex: 'currency',
    key: 'currency',
    width: 72,
    minWidth: 72,
    resizable: true,
    align: 'center',
    className: 'currency-column',
    sorter: (a, b) => (a.currency || '').localeCompare(b.currency || ''),
  },
  { title: '備註', dataIndex: 'note', key: 'note', width: 150, resizable: true, ellipsis: true },
  {
    title: '時間',
    dataIndex: 'createdAt',
    key: 'createdAt',
    width: 180,
    resizable: true,
    sorter: (a, b) => (a.createdAt || '').localeCompare(b.createdAt || ''),
    customRender: ({ text }) => formatTime(text),
  },
])

function handleResizeColumn(w, col) {
  col.width = w
}

function handleSearchTypeChange() {
  searchValue.value = ''
  startDate.value = null
  endDate.value = null
  logs.value = []
  total.value = 0
  currentPage.value = 1
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

    if (searchType.value === 'referenceId' && searchValue.value) {
      res = await getTransLogsByReferenceId(searchValue.value)
      logs.value = res.data.data
      total.value = res.data.data.length
    } else if (searchType.value === 'accountNumber' && searchValue.value) {
      res = await getTransLogsByAccountNumber(searchValue.value, page, size)
      logs.value = res.data.data.content
      total.value = res.data.data.totalElements
    } else if (searchType.value === 'customerId' && searchValue.value) {
      res = await getTransLogsByCustomerId(searchValue.value, page, size)
      logs.value = res.data.data.content
      total.value = res.data.data.totalElements
    } else if (searchType.value === 'customerIdDateRange' && searchValue.value && startDate.value && endDate.value) {
      const start = startDate.value.format('YYYY-MM-DDTHH:mm:ss')
      const end = endDate.value.format('YYYY-MM-DDTHH:mm:ss')
      res = await getTransLogsByCustomerIdAndDateRange(searchValue.value, start, end, page, size)
      logs.value = res.data.data.content
      total.value = res.data.data.totalElements
    } else if (!searchValue.value) {
      // 條件為空，顯示最新 10 筆
      res = await getLatestTransLogs(page, size)
      logs.value = res.data.data.content
      total.value = res.data.data.totalElements
    } else {
      message.warning('請填寫完整查詢條件')
      loading.value = false
      return
    }
  } catch (err) {
    message.error(err.response?.data?.message || '查詢失敗')
  } finally {
    loading.value = false
  }
}

async function handleClear() {
  searchValue.value = ''
  startDate.value = null
  endDate.value = null
  logs.value = []
  total.value = 0
  currentPage.value = 1
  await fetchData()
}
function handleExport({ key }) {
  if (key === 'excel') exportExcel()
  else if (key === 'json') exportJson()
  else if (key === 'xml') exportXml()
}

function getExportData() {
  return logs.value.map(log => ({
    交易編號: log.referenceId,
    帳號: log.accountNumber,
    對手方: log.counterpartAccount || '',
    方向: entryTypeMap[log.entryType] || log.entryType,
    類型: transactionTypeMap[log.transactionType] || log.transactionType,
    金額: log.amount,
    交易前餘額: log.balanceBefore,
    交易後餘額: log.balanceAfter,
    幣別: log.currency,
    備註: log.note || '',
    時間: formatTime(log.createdAt),
  }))
}

function exportExcel() {
  const data = getExportData()
  const ws = XLSX.utils.json_to_sheet(data)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '交易紀錄')
  const buf = XLSX.write(wb, { bookType: 'xlsx', type: 'array' })
  saveAs(new Blob([buf]), '交易紀錄.xlsx')
  message.success('Excel 匯出成功')
}

function exportJson() {
  const data = getExportData()
  const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
  saveAs(blob, '交易紀錄.json')
  message.success('JSON 匯出成功')
}

function exportXml() {
  const data = getExportData()
  let xml = '<?xml version="1.0" encoding="UTF-8"?>\n<交易紀錄列表>\n'
  data.forEach(item => {
    xml += '  <交易紀錄>\n'
    Object.entries(item).forEach(([key, value]) => {
      xml += `    <${key}>${value}</${key}>\n`
    })
    xml += '  </交易紀錄>\n'
  })
  xml += '</交易紀錄列表>'
  const blob = new Blob([xml], { type: 'application/xml' })
  saveAs(blob, '交易紀錄.xml')
  message.success('XML 匯出成功')
}
const showReversalModal = ref(false)
const reversalLoading = ref(false)
const reversalResult = ref(null)
const reversalForm = reactive({
  originalReferenceId: '',
  reason: '',
})

function handleReversalClear() {
  reversalForm.originalReferenceId = ''
  reversalForm.reason = ''
  reversalResult.value = null
  showReversalModal.value = false
}

async function handleReversalSubmit() {
  if (!reversalForm.originalReferenceId) {
    message.warning('請填寫原始交易編號')
    return
  }
  reversalLoading.value = true
  reversalResult.value = null
  try {
    const res = await reversal({
      originalReferenceId: reversalForm.originalReferenceId,
      reason: reversalForm.reason || null,
    })
    reversalResult.value = res.data.data
    message.success('沖正成功')
  } catch (err) {
    message.error(err.response?.data?.message || '沖正失敗')
  } finally {
    reversalLoading.value = false
  }
}

async function copyText(text, successText = '已複製') {
  if (!text) return
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.setAttribute('readonly', '')
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    message.success(successText)
  } catch (error) {
    message.error('複製失敗，請手動複製')
  }
}

function lookupReferenceId(refId) {
  showReversalModal.value = false
  searchType.value = 'referenceId'
  searchValue.value = refId
  handleSearch()
}

async function handleClickAccount(accountNumber) {
  if (!accountNumber || isBusinessAccountLabel(accountNumber)) return
  accountLoading.value = true
  showAccountModal.value = true
  try {
    const res = await getAccount(accountNumber)
    accountDetail.value = res.data.data
  } catch (err) {
    message.error('查詢帳戶失敗: ' + (err.response?.data?.message || err.message))
    accountDetail.value = null
  } finally {
    accountLoading.value = false
  }
}

function isBusinessAccountLabel(value) {
  return value === BUSINESS_ACCOUNT_LABEL
}
</script>

<style scoped>
.admin-translog-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 12px;
}

.header-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-subtitle {
  margin: 0;
  color: var(--text-secondary);
  font-size: 14px;
}

.filter-panel {
  margin-bottom: 16px;
}

.filter-toolbar {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid rgba(214, 206, 195, 0.82);
  border-radius: 12px;
  background: rgba(255, 249, 239, 0.72);
  box-shadow: 0 6px 16px rgba(63, 74, 66, 0.05);
}

.filter-main,
.filter-side {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.filter-main {
  justify-content: flex-start;
}

.filter-side {
  justify-content: space-between;
}

.filter-main :deep(.ant-input),
.filter-main :deep(.ant-picker) {
  min-width: 180px;
}

.filter-side :deep(.ant-radio-group) {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-side :deep(.ant-radio-button-wrapper) {
  border-color: rgba(214, 206, 195, 0.86);
  color: var(--text-secondary);
  background: rgba(250, 247, 240, 0.88);
  box-shadow: none;
}

.filter-side :deep(.ant-radio-button-wrapper-checked) {
  background: var(--primary);
  border-color: var(--primary);
  color: var(--bg-primary);
}

.analysis-panel {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr));
  gap: 14px;
  margin-bottom: 20px;
  align-items: start;
}

.metric-card,
.chart-card {
  border: 1px solid rgba(214, 206, 195, 0.82);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(255, 250, 243, 0.95), rgba(248, 244, 236, 0.9)),
    url('/washi-texture.webp');
  background-size: auto, 260px 260px;
  box-shadow: 0 6px 16px rgba(63, 74, 66, 0.05);
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
  font-size: 28px;
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
  min-height: 230px;
  padding: 14px;
}

.chart-card.wide {
  grid-column: span 2;
}

.chart-body {
  position: relative;
  height: 185px;
  margin-top: 10px;
}

.chart-body :deep(canvas) {
  width: 100% !important;
  height: 100% !important;
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

.entry-credit { background-color: rgba(82, 196, 26, 0.1); color: #389e0d; }
.entry-credit .status-dot { background-color: #5C6B5F; }

.entry-debit { background-color: rgba(166, 90, 77, 0.1); color: #A65A4D; }
.entry-debit .status-dot { background-color: #A65A4D; }

.transaction-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(92, 107, 95, 0.08);
  color: var(--primary-dark);
  font-size: 12px;
  font-weight: 600;
}

.transaction-badge.type-transfer,
.transaction-badge.type-reversal,
.transaction-badge.type-withdraw {
  background: rgba(166, 90, 77, 0.08);
  color: #A65A4D;
}

.transaction-badge.type-loan_disbursement,
.transaction-badge.type-interest,
.transaction-badge.type-deposit {
  background: rgba(92, 107, 95, 0.08);
  color: var(--primary-dark);
}

.transaction-badge.type-card_payment,
.transaction-badge.type-card_settlement,
.transaction-badge.type-card_reward,
.transaction-badge.type-transfer_fee {
  background: rgba(196, 164, 124, 0.16);
  color: #7b5a2f;
}

.reference-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  max-width: 100%;
}

.reference-text {
  overflow: hidden;
  font-family: var(--font-mono, ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace);
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 190px;
}

.currency-column {
  white-space: nowrap;
}

.currency-column :deep(.ant-table-cell-content),
.currency-code {
  white-space: nowrap;
}

.copy-reference-btn {
  flex: 0 0 auto;
  color: #5c6b5f;
  border-radius: 8px;
}

.copy-reference-btn:hover,
.copy-reference-btn:focus {
  color: #3f4a42;
  background: rgba(92, 107, 95, 0.08);
}

:deep(.custom-table .ant-table-thead > tr > th) {
  background: rgba(244, 238, 229, 0.92);
  color: var(--text-primary);
  border-bottom-color: rgba(214, 206, 195, 0.82);
}

:deep(.custom-table .ant-table-tbody > tr > td) {
  border-bottom-color: rgba(214, 206, 195, 0.45);
}

:deep(.custom-table .ant-table-tbody > tr:hover > td) {
  background: rgba(92, 107, 95, 0.035);
}

:deep(.custom-table .ant-table-cell) {
  padding-top: 12px;
  padding-bottom: 12px;
}

:deep(.custom-table .ant-table-cell-ellipsis) {
  white-space: nowrap;
}

@media (max-width: 1200px) {
  .analysis-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .chart-card.wide {
    grid-column: span 2;
  }
}

@media (max-width: 860px) {
  .admin-translog-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .filter-main :deep(.ant-input),
  .filter-main :deep(.ant-picker) {
    flex: 1 1 100%;
    min-width: 0;
  }

  .filter-side {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-side :deep(.ant-radio-group) {
    width: 100%;
  }

  .filter-side :deep(.ant-radio-button-wrapper) {
    flex: 1 1 calc(50% - 8px);
    text-align: center;
  }

  .filter-side :deep(.ant-dropdown) {
    width: 100%;
  }

  .filter-side :deep(.ant-btn) {
    width: 100%;
  }

  .analysis-panel {
    grid-template-columns: 1fr;
  }

  .chart-card.wide {
    grid-column: auto;
  }

  .chart-body {
    height: 170px;
  }
}

@media (max-width: 560px) {
  .filter-toolbar {
    padding: 12px;
  }

  .filter-side :deep(.ant-radio-button-wrapper) {
    flex: 1 1 100%;
  }

  .reference-text {
    max-width: 140px;
  }
}

@media (max-width: 980px) {
  .analysis-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .analysis-panel,
  .chart-card.wide {
    grid-template-columns: 1fr;
    grid-column: auto;
  }
}
</style>
