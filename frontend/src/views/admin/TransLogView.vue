<template>
  <div class="page-container">
    <div class="page-header" style="display: flex; justify-content: space-between; align-items: center;">
      <h2 class="page-title">交易紀錄查詢</h2>
      <!-- 查詢方式切換置於右上角 -->
      <div style="display: flex; flex-direction: column; align-items: flex-end; gap: 10px;">
        <a-radio-group v-model:value="searchType" button-style="solid" @change="handleSearchTypeChange">
          <a-radio-button value="referenceId">依交易編號</a-radio-button>
          <a-radio-button value="accountNumber">依帳號</a-radio-button>
          <a-radio-button value="customerId">依客戶 ID</a-radio-button>
          <a-radio-button value="customerIdDateRange">依客戶 ID + 日期</a-radio-button>
        </a-radio-group>
        <a-button danger class="rounded-btn" @click="showReversalModal = true">
          <template #icon><RollbackOutlined /></template>
          沖正
        </a-button>
      </div>
    </div>

    <!-- 頂部 F 橫劃：搜尋與主操作 -->
    <div class="action-bar">
      <!-- 左側搜尋區 -->
      <div class="search-group">
        <a-input
          v-model:value="searchValue"
          :placeholder="placeholderText"
          class="rounded-input search-input"
          style="width: 280px;"
          allow-clear
          @press-enter="handleSearch"
        />

        <template v-if="searchType === 'customerIdDateRange'">
          <a-date-picker
            v-model:value="startDate"
            show-time
            placeholder="開始時間"
            format="YYYY-MM-DD HH:mm:ss"
            style="width: 180px"
          />
          <a-date-picker
            v-model:value="endDate"
            show-time
            placeholder="結束時間"
            format="YYYY-MM-DD HH:mm:ss"
            style="width: 180px"
          />
        </template>

        <a-button type="primary" class="rounded-btn" @click="handleSearch">
          <template #icon><SearchOutlined /></template>
          查詢
        </a-button>
        <a-button class="rounded-btn btn-ghost" @click="handleClear">清除</a-button>
      </div>

      <!-- 右側全域操作區 -->
      <div class="global-actions">
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

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="logs"
      :loading="loading"
      :scroll="{ x: 1200 }"
      row-key="transactionId"
      class="custom-table"
      :pagination="searchType === 'referenceId' ? false : {
        current: currentPage,
        pageSize: pageSize,
        total: total,
        showSizeChanger: true,
        showTotal: (t) => `共 ${t} 筆`,
      }"
      :locale="{ triggerDesc: '點擊降冪排序', triggerAsc: '點擊升冪排序', cancelSort: '取消排序' }"
      @change="handleTableChange"
      @resizeColumn="handleResizeColumn"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'entryType'">
          <div :class="['status-tag', `entry-${record.entryType.toLowerCase()}`]">
            <span class="status-dot"></span>
            {{ entryTypeMap[record.entryType] || record.entryType }}
          </div>
        </template>
        <template v-else-if="column.key === 'transactionType'">
          <a-tag color="blue">{{ transactionTypeMap[record.transactionType] || record.transactionType }}</a-tag>
        </template>
        <template v-else-if="column.key === 'accountNumber'">
          <a @click="handleClickAccount(record.accountNumber)" style="font-weight:600">{{ record.accountNumber }}</a>
        </template>
        <template v-else-if="column.key === 'counterpartAccount'">
          <a v-if="record.counterpartAccount" @click="handleClickAccount(record.counterpartAccount)">{{ record.counterpartAccount }}</a>
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
import { ref, reactive, computed, h } from 'vue'
import { message } from 'ant-design-vue'
import { DownOutlined, SearchOutlined, RollbackOutlined } from '@ant-design/icons-vue'
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
} from '@/api/account'

const searchType = ref('referenceId')
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
  INTEREST: '利息',
  LOAN_DISBURSEMENT: '貸款撥款',
  LOAN_REPAYMENT: '貸款還款',
  REVERSAL: '沖正',
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

function formatTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ').substring(0, 19)
}

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
  { title: '幣別', dataIndex: 'currency', key: 'currency', width: 60, resizable: true, sorter: (a, b) => (a.currency || '').localeCompare(b.currency || '') },
  { title: '備註', dataIndex: 'note', key: 'note', width: 120, resizable: true },
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

function handleClear() {
  searchValue.value = ''
  startDate.value = null
  endDate.value = null
  logs.value = []
  total.value = 0
  currentPage.value = 1
}

// === 匯出功能 ===
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

// === 沖正功能 ===
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

function copyText(text) {
  navigator.clipboard.writeText(text).then(() => {
    message.success('已複製')
  })
}

function lookupReferenceId(refId) {
  showReversalModal.value = false
  searchType.value = 'referenceId'
  searchValue.value = refId
  handleSearch()
}

async function handleClickAccount(accountNumber) {
  if (!accountNumber) return
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
</script>

<style scoped>
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
.entry-credit .status-dot { background-color: #52c41a; }

.entry-debit { background-color: rgba(255, 77, 79, 0.1); color: #d9363e; }
.entry-debit .status-dot { background-color: #ff4d4f; }
</style>
