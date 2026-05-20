<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">風控事件查詢</h2>
    </div>

    <div class="action-bar">
      <div class="search-group">
        <a-select
          v-model:value="filterParams.eventType"
          placeholder="事件類型"
          style="width: 150px"
          allow-clear
          size="large"
        >
          <a-select-option value="LOAN">貸款申請</a-select-option>
          <a-select-option value="TRANSFER_REVIEW">轉帳審核</a-select-option>
          <a-select-option value="LOGIN_CHECK">登入檢查</a-select-option>
          <a-select-option value="CREDIT_CARD">信用卡</a-select-option>
        </a-select>
        <a-select
          v-model:value="filterParams.riskLevel"
          placeholder="風險等級"
          style="width: 150px"
          allow-clear
          size="large"
        >
          <a-select-option value="LOW">低風險</a-select-option>
          <a-select-option value="MEDIUM">中風險</a-select-option>
          <a-select-option value="HIGH">高風險</a-select-option>
          <a-select-option value="SUSPENDED">拒絕往來/凍結</a-select-option>
        </a-select>
        <a-select
          v-model:value="filterParams.actionTaken"
          placeholder="採取行動"
          style="width: 150px"
          allow-clear
          size="large"
        >
          <a-select-option value="PASS">通過</a-select-option>
          <a-select-option value="REJECT">拒絕</a-select-option>
          <a-select-option value="MANUAL_REVIEW">人工審核</a-select-option>
          <a-select-option value="WARNING">警告</a-select-option>
        </a-select>
        <a-button type="primary" class="rounded-btn" size="large" @click="loadData">
          <template #icon>
            <SearchOutlined/>
          </template>
          查詢
        </a-button>
      </div>
    </div>

    <a-table
      :columns="columns"
      :data-source="dataSource"
      :pagination="pagination"
      :loading="loading"
      :scroll="{ x: 800 }"
      @change="handleTableChange"
      row-key="logId"
      class="custom-table large-table"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'riskLevel'">
          <div :class="['status-tag', `risk-${record.riskLevel.toLowerCase()}`]">
            <span class="status-dot"></span>
            {{ riskLevelMap[record.riskLevel] || record.riskLevel }}
          </div>
        </template>
        <template v-else-if="column.key === 'actionTaken'">
          <a-tag :color="actionTakenColorMap[record.actionTaken]" class="table-badge">
            {{ actionTakenMap[record.actionTaken] || record.actionTaken }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'eventType'">
          <a-tag color="blue" class="table-badge">
            {{ eventTypeMap[record.eventType] || record.eventType }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'createdAt'">
          {{ formatDate(record.createdAt) }}
        </template>
      </template>

      <template #expandedRowRender="{ record }">
        <div style="margin-bottom: 12px; font-size: 15px;">
          <span style="color: #8c8c8c; margin-right: 8px;">觸發原因</span>
          <a-tag :color="actionTakenColorMap[record.actionTaken] || 'default'" class="table-badge">
            {{ record.triggerReason || '—' }}
          </a-tag>
        </div>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import {ref, reactive, onMounted} from 'vue'
import {SearchOutlined} from '@ant-design/icons-vue'
import api from '@/api/axios'

const dataSource = ref([])
const loading = ref(false)

const filterParams = reactive({
  eventType: null,
  actionTaken: null,
  riskLevel: null,
  sortBy: null,
  sortDir: null,
})

const riskLevelMap = {
  LOW: '低風險',
  MEDIUM: '中風險',
  HIGH: '高風險',
  SUSPENDED: '拒絕往來',
}

const actionTakenMap = {
  PASS: '通過',
  REJECT: '拒絕',
  MANUAL_REVIEW: '人工審核',
  WARNING: '警告'
}

const actionTakenColorMap = {
  PASS: 'green',
  REJECT: 'red',
  MANUAL_REVIEW: 'gray',
  WARNING: 'gold',
}

const eventTypeMap = {
  LOAN_APPLY: '貸款申請',
  TRANSFER_REVIEW: '轉帳審核',
  LOGIN_CHECK: '登入檢查',
  CREDIT_CARD: '信用卡',
}

const columns = [
  {title: '事件 ID', dataIndex: 'logId', key: 'logId', width: 100},
  {
    title: '時間',
    dataIndex: 'createdAt',
    key: 'createdAt',
    width: 180,
    sorter: true,
    sortDirections: ['descend', 'ascend', 'descend'],
    defaultSortOrder: 'descend'
  },
  {title: '事件類型', dataIndex: 'eventType', key: 'eventType', width: 120},
  {title: '目標識別碼', dataIndex: 'targetIdentifier', key: 'targetIdentifier', width: 150},
  {title: '採取行動', dataIndex: 'actionTaken', key: 'actionTaken', width: 120},
  {title: '風險等級', dataIndex: 'riskLevel', key: 'riskLevel', width: 120, fixed: 'right'},
]

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50', '100'],
  showTotal: (total) => `共 ${total} 筆`,
})

const loadData = async () => {
  loading.value = true

  try {
    const cleanParams = {
      page: pagination.current - 1,
      size: pagination.pageSize,
    }

    if (filterParams.eventType) cleanParams.eventType = filterParams.eventType
    if (filterParams.actionTaken) cleanParams.actionTaken = filterParams.actionTaken
    if (filterParams.riskLevel) cleanParams.riskLevel = filterParams.riskLevel

    if (filterParams.sortBy) {
      cleanParams.sort = `${filterParams.sortBy},${filterParams.sortDir}`
    } else {
      cleanParams.sort = 'createdAt,desc'
    }

    const res = await api.get('/api/risk/riskevent/search', {
      params: cleanParams,
    })
    dataSource.value = res.data.data.content
    pagination.total = res.data.data.totalElements
  } catch (error) {
    console.error('載入失敗', error)
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag, filter, sorter) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  if (sorter?.field) {
    filterParams.sortBy = sorter.field
    filterParams.sortDir = sorter.order === 'descend' ? 'desc' : 'asc'
  } else {
    filterParams.sortBy = null
    filterParams.sortDir = null
  }
  loadData()
}

function formatDate(val) {
  if (!val) return '—'
  const cleanStr = val.replace('T', ' ').replace(/-/g, '/')
  return cleanStr.substring(0, 16)
}

onMounted(() => loadData())
</script>

<style scoped>
/* 💡 樣式優化：完全比照 CreditList 規範的大字體風格 */
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 放大頁面主標題 */
.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #262626;
}

/* 深度覆蓋 Table：字體全面放大 */
.large-table :deep(.ant-table) {
  font-size: 16px !important;
}

:deep(.ant-table-thead > tr > th) {
  font-size: 16px !important;
  color: #262626 !important;
  font-weight: 600;
}

:deep(.ant-table-tbody > tr > td) {
  font-size: 16px !important;
  color: #262626 !important;
}

/* 控制頂部搜尋下拉選單與按鈕的字體 */
:deep(.ant-select) {
  font-size: 16px !important;
}

:deep(.ant-btn) {
  font-size: 16px !important;
}

:deep(.ant-pagination) {
  font-size: 15px !important;
}

/* 內嵌狀態與 Tag 樣式微調放大 */
.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 15px; /* 配合大表格調整 */
  font-weight: 600;
}

.table-badge {
  font-size: 15px !important;
  padding: 2px 8px !important;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.risk-low {
  background-color: rgba(82, 196, 26, 0.1);
  color: #389e0d;
}
.risk-low .status-dot {
  background-color: #52c41a;
}

.risk-medium {
  background-color: rgba(250, 140, 22, 0.1);
  color: #fa8c16;
}
.risk-medium .status-dot {
  background-color: #fa8c16;
}

.risk-high {
  background-color: rgba(255, 77, 79, 0.1);
  color: #d9363e;
}
.risk-high .status-dot {
  background-color: #ff4d4f;
}

.risk-suspended {
  background-color: rgba(114, 46, 209, 0.1);
  color: #531dab;
}
.risk-suspended .status-dot {
  background-color: #722ed1;
}

.action-bar {
  margin-bottom: 8px;
}

.search-group {
  display: flex;
  gap: 12px;
  align-items: center;
}
</style>
