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
          <a-select-option value="LOAN">貸款</a-select-option>
          <a-select-option value="TRANSFER">轉帳</a-select-option>
          <a-select-option value="USER_LOGIN">登入</a-select-option>
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
          <a-select-option value="PASSED">通過</a-select-option>
          <a-select-option value="REJECTED">拒絕</a-select-option>
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
  PASSED: '通過',
  REJECTED: '拒絕',
  MANUAL_REVIEW: '人工審核',
}

const actionTakenColorMap = {
  PASS: 'green',
  REJECT: 'red',
  MANUAL_REVIEW: 'orange',
}

const eventTypeMap = {
  LOAN_APPLY: '貸款',
  TRANSFER: '轉帳',
  USER_LOGIN: '登入',
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
</style>
