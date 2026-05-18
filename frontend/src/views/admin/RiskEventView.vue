<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">風控事件查詢</h2>
    </div>

    <!-- 頂部 F 橫劃：搜尋與操作列 -->
    <div class="action-bar">
      <!-- 左側搜尋區 -->
      <div class="search-group">
        <a-select
          v-model:value="filterParams.eventType"
          placeholder="事件類型"
          style="width: 150px"
          allow-clear
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
        >
          <a-select-option value="PASS">通過</a-select-option>
          <a-select-option value="REJECT">拒絕</a-select-option>
          <a-select-option value="MANUAL_REVIEW">人工審核</a-select-option>
        </a-select>
        <a-button type="primary" class="rounded-btn" @click="loadData">
          <template #icon><SearchOutlined /></template>
          查詢
        </a-button>
      </div>
    </div>

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :pagination="pagination"
      :loading="loading"
      :scroll="{ x: 800 }"
      @change="handleTableChange"
      row-key="logId"
      class="custom-table"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'riskLevel'">
          <div :class="['status-tag', `risk-${record.riskLevel.toLowerCase()}`]">
            <span class="status-dot"></span>
            {{ riskLevelMap[record.riskLevel] || record.riskLevel }}
          </div>
        </template>
        <template v-else-if="column.key === 'actionTaken'">
          <a-tag :color="actionTakenColorMap[record.actionTaken]">{{
            actionTakenMap[record.actionTaken] || record.actionTaken
          }}</a-tag>
        </template>
        <template v-else-if="column.key === 'eventType'">
          <a-tag color="blue">{{ eventTypeMap[record.eventType] || record.eventType }}</a-tag>
        </template>
        <template v-else-if="column.key === 'createdAt'">
          {{ formatDate(record.createdAt) }}
        </template>
      </template>

      <template #expandedRowRender="{ record }">
        <div class="metadata-detail">
          <h4 class="detail-title">原始元數據 (JSON Metadata)</h4>
          <div v-if="record.metaData" class="json-code-box">
            <pre>{{ formatJson(record.metaData) }}</pre>
          </div>
          <a-empty v-else description="此事件無附加元數據資訊" :image="false" />
        </div>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import api from '@/api/axios'

const dataSource = ref([])
const loading = ref(false)

const filterParams = reactive({
  eventType: null,
  actionTaken: null,
  riskLevel: null,
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
}

const actionTakenColorMap = {
  PASS: 'green',
  REJECT: 'red',
  MANUAL_REVIEW: 'orange',
}

const eventTypeMap = {
  LOAN_APPLY: '貸款申請',
  TRANSFER_REVIEW: '轉帳審核',
  LOGIN_CHECK: '登入檢查',
  CREDIT_CARD: '信用卡',
}

const columns = [
  { title: '事件 ID', dataIndex: 'logId', key: 'logId', width: 100 },
  { title: '時間', dataIndex: 'createdAt', key: 'createdAt', width: 180 },
  { title: '事件類型', dataIndex: 'eventType', key: 'eventType', width: 120 },
  { title: '目標識別碼', dataIndex: 'targetIdentifier', key: 'targetIdentifier', width: 150 },
  { title: '採取行動', dataIndex: 'actionTaken', key: 'actionTaken', width: 120 },
  { title: '風險等級', dataIndex: 'riskLevel', key: 'riskLevel', width: 120, fixed: 'right' },
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
    // 處理請求參數：移除 null 或空字串，避免後端解析 Enum 噴錯
    const cleanParams = {
      page: pagination.current - 1,
      size: pagination.pageSize,
    }

    if (filterParams.eventType) cleanParams.eventType = filterParams.eventType
    if (filterParams.actionTaken) cleanParams.actionTaken = filterParams.actionTaken
    if (filterParams.riskLevel) cleanParams.riskLevel = filterParams.riskLevel

    const res = await api.get('/api/risk/riskevent/search', {
      params: cleanParams,
    })
    // 對接 ApiResponse 包裝的 Spring Page 物件
    dataSource.value = res.data.data.content
    pagination.total = res.data.data.totalElements
  } catch (error) {
    console.error('載入失敗', error)
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  loadData()
}

const formatJson = (data) => {
  if (!data) return ''
  if (typeof data === 'string') {
    try {
      // 如果是字串則解析後格式化
      return JSON.stringify(JSON.parse(data), null, 2)
    } catch (e) {
      return data
    }
  }
  // 如果已經是物件則直接格式化
  return JSON.stringify(data, null, 2)
}

function formatDate(val) {
  if (!val) return '—'
  const cleanStr = val.replace('T', ' ').replace(/-/g, '/')
  return cleanStr.substring(0, 16)
}

onMounted(() => loadData())
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

/* Metadata 展開內容樣式 */
.metadata-detail {
  padding: 16px;
  background: #fdfdfd;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
}

.detail-title {
  font-size: 14px;
  font-weight: 600;
  color: #262626;
  margin-bottom: 12px;
}

.json-code-box {
  background: #1e1e1e;
  padding: 16px;
  border-radius: 6px;
  max-height: 500px;
  overflow-y: auto;
  box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.15);
}

.json-code-box pre {
  margin: 0;
  color: #9cdcfe; /* 淺藍色，類似 VS Code 屬性名顏色 */
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  tab-size: 2;
}
</style>
