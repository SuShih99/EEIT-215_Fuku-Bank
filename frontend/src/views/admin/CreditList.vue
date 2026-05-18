<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">客戶信用評分管理</h2>
      <a-input-search
        v-model:value="keyword"
        placeholder="搜尋客戶姓名或 ID"
        size="large"
        style="width: 300px"
        @search="handleSearch"
      />
    </div>

    <a-alert
      message="資料敏感提醒：此頁面顯示客戶信用資料，所有查看行為均已記錄。請勿截圖或外流。"
      type="warning"
      show-icon
      closable
      class="large-alert"
    />

    <a-table
      :columns="columns"
      :data-source="credits"
      :loading="loading"
      :pagination="pagination"
      row-key="customerId"
      class="large-table"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'riskLevel'">
          <a-tag :color="riskColor(record.riskLevel)" class="table-risk-badge">
            {{ riskLabel(record.riskLevel) }}
          </a-tag>
        </template>
        <template v-if="column.key === 'lastUpdatedAt'">
          {{ formatDate(record.lastUpdatedAt) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-button type="link" class="action-btn" @click="viewDetail(record.customerId)">
            查看完整評分
          </a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const BASE_URL = '/api/risk/creditscore'

const credits = ref([])
const loading = ref(false)
const keyword = ref('')

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50'],
  showTotal: (total) => `共 ${total} 筆`,
})

const columns = [
  { title: '客戶 ID', dataIndex: 'customerId', key: 'customerId', width: 150 },
  { title: '客戶姓名', dataIndex: 'customerName', key: 'customerName', width: 150 },
  { title: '風險等級', dataIndex: 'riskLevel', key: 'riskLevel', width: 120 },
  { title: '最後更新', dataIndex: 'lastUpdatedAt', key: 'lastUpdatedAt', width: 180 },
  { title: '操作', key: 'action', width: 150, fixed: 'right' },
]

async function fetchCredits() {
  loading.value = true
  try {
    const params = {
      page: pagination.current - 1,
      size: pagination.pageSize,
    }
    if (keyword.value) params.keyword = keyword.value

    const res = await axios.get(BASE_URL, { params, withCredentials: true })
    const page = res.data.data
    credits.value = page.content
    const backendPageNumber = Number(page.number || 0)
    pagination.total = page.totalElements || 0
    pagination.current = Math.max(1, backendPageNumber + 1)
  } catch (e) {
    message.error('載入失敗：' + (e.response?.data?.message || e.message))
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchCredits()
}

function handleTableChange(pag) {
  pagination.current = Math.max(1, Number(pag.current || 1))
  pagination.pageSize = pag.pageSize
  fetchCredits()
}

function viewDetail(customerId) {
  router.push(`/admin/risk/credit/${customerId}`)
}

function riskLabel(r) {
  return { HIGH: '高風險', MEDIUM: '中風險', LOW: '低風險' }[r] || r
}

function riskColor(r) {
  return { HIGH: 'red', MEDIUM: 'orange', LOW: 'green' }[r] || 'default'
}

function formatDate(val) {
  if (!val) return '—'
  const cleanStr = val.replace('T', ' ').replace(/-/g, '/')
  return cleanStr.substring(0, 16)
}

onMounted(fetchCredits)
</script>

<style scoped>
/* 完全比照 RiskEventView 的留白，移除任何自訂背景與內襯 padding */
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

.large-alert {
  font-size: 16px !important;
}

/* 深度覆蓋 Table：字體全面放大，顏色維持一致，不花俏 */
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

:deep(.ant-input) {
  font-size: 16px !important;
}

:deep(.ant-pagination) {
  font-size: 15px !important;
}

.table-risk-badge {
  font-size: 15px !important;
}

.action-btn {
  font-size: 16px !important;
  padding: 0;
}
</style>
