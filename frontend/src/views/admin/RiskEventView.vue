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
          <div :class="['status-tag', `action-${record.actionTaken.toLowerCase()}`]">
            {{ actionTakenMap[record.actionTaken] || record.actionTaken }}
          </div>
        </template>
        <template v-else-if="column.key === 'eventType'">
          <div>
            {{ eventTypeMap[record.eventType] || record.eventType }}
          </div>
        </template>
        <template v-else-if="column.key === 'createdAt'">
          {{ formatDate(record.createdAt) }}
        </template>
        <template v-else-if="column.key === 'businessId'">
          <a-tooltip :title="record.businessId">
            <span class="mono-id">{{ record.businessId }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.key === 'targetIdentifier'">
          <a-tooltip :title="record.targetIdentifier">
            <span class="mono-id">{{ record.targetIdentifier }}</span>
          </a-tooltip>
        </template>
      </template>

      <template #expandedRowRender="{ record }">
        <div :class="['status-tag', `action-${record.actionTaken.toLowerCase()}`]"
             style="display:inline-flex">
          {{ record.triggerReason || '—' }}
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

const eventTypeMap = {
  LOAN_APPLY: '貸款申請',
  TRANSFER_REVIEW: '轉帳審核',
  LOGIN_CHECK: '登入檢查',
  CREDIT_CARD: '信用卡',
}

const columns = [
  {title: '事件 ID', dataIndex: 'logId', key: 'logId', width: 50},
  {
    title: '時間',
    dataIndex: 'createdAt',
    key: 'createdAt',
    width: 120,
    sorter: true,
    sortDirections: ['descend', 'ascend', 'descend'],
    defaultSortOrder: 'descend'
  },
  {title: '事件類型', dataIndex: 'eventType', key: 'eventType', width: 100},
  {title: '業務編號', dataIndex: 'businessId', key: 'businessId', width: 150, ellipsis: true},
  {
    title: '目標識別碼',
    dataIndex: 'targetIdentifier',
    key: 'targetIdentifier',
    width: 150,
    ellipsis: true
  },
  {title: '採取行動', dataIndex: 'actionTaken', key: 'actionTaken', width: 100},
  {title: '風險等級', dataIndex: 'riskLevel', key: 'riskLevel', width: 100, fixed: 'right'},
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

.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.2;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

/* 低風險：從鮮綠色改為「沉穩英倫松針綠」 */
.risk-low {
  background-color: rgba(47, 122, 70, 0.08); /* 降低明度與飽和度的綠 bg */
  color: #276339; /* 穩重的深松針綠文字 */
}

.risk-low .status-dot {
  background-color: #2f7a46;
}

/* 中風險：從亮橘色改為「洗鍊古銅烤漆琥珀」 */
.risk-medium {
  background-color: rgba(181, 107, 24, 0.08); /* 偏向木質與皮革的沉穩琥珀 bg */
  color: #91520f; /* 深古銅橘文字 */
}

.risk-medium .status-dot {
  background-color: #b56b18;
}

/* 高風險：從刺眼大紅改為「高級波爾多深磚紅」 */
.risk-high {
  background-color: rgba(166, 29, 36, 0.07); /* 帶有灰色調的暗紅 bg */
  color: #8c1c21; /* 經典金融高風險的深磚紅/酒紅文字 */
}

.risk-high .status-dot {
  background-color: #a61d24;
}

/* 拒絕往來/凍結：從亮紫色改為「午夜深靛紫」 */
.risk-suspended {
  background-color: rgba(83, 29, 171, 0.07); /* 深邃的午夜紫 bg */
  color: #43168c; /* 穩重的深靛紫文字 */
}

.action-pass {
  background-color: rgba(47, 122, 70, 0.08); /* 降低明度與飽和度的綠 bg */
  color: #276339; /* 穩重的深松針綠文字 */
}

.action-reject {
  background-color: rgba(166, 29, 36, 0.07); /* 帶有灰色調的暗紅 bg */
  color: #8c1c21; /* 經典金融高風險的深磚紅/酒紅文字 */
}

.action-manual_review {
  background-color: rgba(0, 0, 0, 0.06);
  color: #595959;
}

.action-warning {
  background-color: rgba(250, 173, 20, 0.1);
  color: #d48806;
}

.action-bar {
  margin-bottom: 8px;
}

.search-group {
  display: flex;
  gap: 12px;
  align-items: center;
}

.mono-id {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 14px;
  color: #595959;
  cursor: default;
}
</style>
