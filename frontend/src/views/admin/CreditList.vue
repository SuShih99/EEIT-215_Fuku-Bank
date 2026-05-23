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
          <div v-if="record.riskLevel"
               :class="['status-tag', `risk-${record.riskLevel.toLowerCase()}`]">
            <span class="status-dot"></span>
            {{ riskLabel(record.riskLevel) }}
          </div>
          <span v-else style="color: #bfbfbf">—</span>
        </template>
        <template v-if="column.key === 'lastUpdatedAt'">
          {{ formatDate(record.lastUpdatedAt) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-button type="link" class="action-btn" @click="viewDetail(record.customerId)">
            <FileTextOutlined /> 評估報告
          </a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import {ref, reactive, onMounted} from 'vue'
import {message} from 'ant-design-vue'
import {FileTextOutlined} from '@ant-design/icons-vue'
import {useRouter} from 'vue-router'
import api from '@/api/axios'

const router = useRouter()
const BASE_URL = '/api/risk/creditscore'

const credits = ref([])
const loading = ref(false)
const keyword = ref('')
const sortField = ref('')
const sortOrder = ref('')

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50'],
  showTotal: (total) => `共 ${total} 筆`,
})

const columns = [
  {title: 'CIF (客戶代碼)', dataIndex: 'cif', key: 'cif', width: 150},
  {title: '姓名', dataIndex: 'customerName', key: 'customerName', width: 120},
  {title: '風險等級', dataIndex: 'riskLevel', key: 'riskLevel', width: 120},
  {
    title: '最後更新',
    dataIndex: 'lastUpdatedAt',
    key: 'lastUpdatedAt',
    width: 180,
    sorter: true,
    sortDirections: ['descend', 'ascend', 'descend'],
    defaultSortOrder: 'descend'
  },
  {title: '操作', key: 'action', width: 150, fixed: 'right'},
]

async function fetchCredits() {
  loading.value = true
  try {
    const params = {
      page: pagination.current - 1,
      size: pagination.pageSize,
    }
    if (keyword.value) params.keyword = keyword.value
    params.sort = sortField.value && sortOrder.value
      ? `${sortField.value},${sortOrder.value}`
      : 'lastUpdatedAt,desc'

    const res = await api.get(BASE_URL, {params})
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

function handleTableChange(pag, _filters, sorter) {
  pagination.current = Math.max(1, Number(pag.current || 1))
  pagination.pageSize = pag.pageSize
  if (sorter?.field) {
    sortField.value = sorter.field
    sortOrder.value = sorter.order === 'ascend' ? 'asc' : 'desc'
  } else {
    sortField.value = ''
    sortOrder.value = ''
  }
  fetchCredits()
}

function viewDetail(customerId) {
  router.push(`/admin/risk/credit/${customerId}`)
}

function riskLabel(r) {
  return {HIGH: '高風險', MEDIUM: '中風險', LOW: '低風險'}[r] || r
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

.action-btn {
  font-size: 16px !important;
  padding: 0;
}

/* ── 💡 調整：沉穩、專業金融質感的風險藥丸樣式 ── */
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
  color: #276339;                             /* 穩重的深松針綠文字 */
}
.risk-low .status-dot {
  background-color: #2f7a46;
}

/* 中風險：從亮橘色改為「洗鍊古銅烤漆琥珀」 */
.risk-medium {
  background-color: rgba(181, 107, 24, 0.08); /* 偏向木質與皮革的沉穩琥珀 bg */
  color: #91520f;                             /* 深古銅橘文字 */
}
.risk-medium .status-dot {
  background-color: #b56b18;
}

/* 高風險：從刺眼大紅改為「高級波爾多深磚紅」 */
.risk-high {
  background-color: rgba(166, 29, 36, 0.07); /* 帶有灰色調的暗紅 bg */
  color: #8c1c21;                            /* 經典金融高風險的深磚紅/酒紅文字 */
}
.risk-high .status-dot {
  background-color: #a61d24;
}

/* 拒絕往來/凍結：從亮紫色改為「午夜深靛紫」 */
.risk-suspended {
  background-color: rgba(83, 29, 171, 0.07); /* 深邃的午夜紫 bg */
  color: #43168c;                            /* 穩重的深靛紫文字 */
}
.risk-suspended .status-dot {
  background-color: #531dab;
}
</style>
