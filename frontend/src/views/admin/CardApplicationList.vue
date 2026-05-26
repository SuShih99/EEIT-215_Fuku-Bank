<script setup>
import { ref, onMounted, watch } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { SearchOutlined, SyncOutlined } from '@ant-design/icons-vue'
import {
  getApplications,
  deleteApplication,
  updateApplicationStatus,
  updateApplicationRemark,
} from '@/api/cardApplication'
import { useRouter } from 'vue-router'
import { getApplicationDocuments, needSupplement } from '@/api/cardAppDoc'
import dayjs from 'dayjs'
import { BASE_URL } from '@/api/axios'

const router = useRouter()

const goDetail = (record) => {
  router.push(`/admin/card-applications/${record.applicationId}`)
}

const applications = ref([])
const loading = ref(false)

const keyword = ref('')
const status = ref(null)

const documentModalVisible = ref(false)
const currentDocuments = ref([])
const supplementRemark = ref('')
const supplementModalVisible = ref(false)

const statusOptions = [
  { label: '全部', value: null },
  { label: '審核中', value: 'PENDING' },
  { label: '已核准', value: 'APPROVED' },
  { label: '已拒絕', value: 'REJECTED' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '需補件', value: 'NEED_SUPPLEMENT' },
]

const applicationStatusLabelMap = {
  PENDING: '審核中',
  APPROVED: '已核准',
  REJECTED: '已拒絕',
  COMPLETED: '已完成',
  NEED_SUPPLEMENT: '需補件',
}

const getApplicationStatusLabel = (statusValue) =>
  applicationStatusLabelMap[statusValue] || statusValue

const getFileUrl = (url) => {
  if (!url) return ''
  if (url.startsWith('http') || url.startsWith('blob:') || url.startsWith('data:')) return url
  return `${BASE_URL}${url.startsWith('/') ? '' : '/'}${url}`
}

const formatDocumentTime = (time) => {
  return time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-'
}

//分頁機
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
})

const remarkInput = ref('')
const remarkModalVisible = ref(false)
const currentRecord = ref(null)

const openRemarkModal = (record) => {
  currentRecord.value = record
  remarkInput.value = record.remark || ''
  remarkModalVisible.value = true
}

//修改備註
const handleUpdateRemark = async () => {
  try {
    await updateApplicationRemark(currentRecord.value.applicationId, remarkInput.value)
    message.success('備註更新成功')
    remarkModalVisible.value = false
    await fetchData()
  } catch (error) {
    message.error('備註更新失敗')
  }
}

// 表格欄位
const columns = [
  { title: 'ID', dataIndex: 'applicationId', key: 'applicationId', width: 80 },
  { title: '客戶姓名', dataIndex: 'customerName', key: 'customerName', width: 150 },
  {
    title: '申請日期',
    dataIndex: 'applyDate',
    key: 'applyDate',
    width: 180,
    customRender: ({ text }) => {
      return text ? dayjs(text).format('YYYY-MM-DD HH:mm') : '-'
    },
  },
  { title: '狀態', dataIndex: 'status', key: 'status', width: 120 },
  { title: '備註', dataIndex: 'remark', key: 'remark', width: 120 },
  { title: '附件', key: 'documents', width: 120 },
  { title: '補件', key: 'supplement', width: 120 },
  { title: '審核', key: 'detail', width: 120 },
  { title: '修改備註', key: 'editRemark', width: 120 },
  { title: '刪除', key: 'delete', width: 120 },
]
//取得資料
const fetchData = async () => {
  loading.value = true
  
  try {
    const data = await getApplications({
      page: pagination.value.current - 1,
      size: pagination.value.pageSize,
      keyword: keyword.value,
      status: status.value,
    })
    applications.value = data.content

    pagination.value.total = data.totalElements
  } catch (error) {
    message.error(error.response?.data?.message || '讀取申請資料失敗')
  } finally {
    loading.value = false
  }
}
//刪除資料
const handleDelete = async (record) => {
  Modal.confirm({
    title: '確定刪除嗎?',
    onOk: async () => {
      try {
        await deleteApplication(record.applicationId)
        await fetchData()
        message.success('刪除成功')
        fetchData()
      } catch (error) {
        message.error(error.response?.data?.message || '刪除失敗')
      }
    },
  })
}
//核准資料
const handleApprove = (record) => {
  handleUpdate(record.applicationId, 'APPROVED')
  message.success('核准成功')
}
//拒絕資料
const handleReject = (record) => {
  handleUpdate(record.applicationId, 'REJECTED')
  message.success('拒絕成功')
}

const handleUpdate = async (id, status) => {
  try {
    await updateApplicationStatus(id, status)
    fetchData()
  } catch (error) {
    message.error(error.response?.data?.message || '狀態更新失敗')
  }
}

//分頁
const handlePageChange = (page) => {
  pagination.value.current = page.current
  pagination.value.pageSize = page.pageSize
  fetchData()
}
const handleSearch = () => {
  pagination.value.current = 1
  fetchData()
}
watch([keyword, status], () => {
  pagination.value.current = 1
  fetchData()
})
const openDocumentsModal = async (record) => {
  try {
    currentRecord.value = record
    currentDocuments.value = await getApplicationDocuments(record.applicationId)
    documentModalVisible.value = true
  } catch (error) {
    message.error('附件查詢失敗')
  }
}

const openSupplementModal = (record) => {
  currentRecord.value = record
  supplementRemark.value = record.remark || '請補上財力證明文件'
  supplementModalVisible.value = true
}

const handleNeedSupplement = async () => {
  try {
    await needSupplement(currentRecord.value.applicationId, supplementRemark.value)
    message.success('已退回補件')
    supplementModalVisible.value = false
    await fetchData()
  } catch (error) {
    message.error('退回補件失敗')
  }
}

onMounted(() => {
  fetchData()
})
</script>
<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">信用卡申請管理</h2>
    </div>

    <!-- 頂部 F 橫劃：搜尋與主操作 -->
    <div class="action-bar">
      <!-- 左側搜尋區 -->
      <div class="search-group">
        <a-input
          v-model:value="keyword"
          placeholder="搜尋使用者 / 備註"
          class="rounded-input search-input"
          allow-clear
          @pressEnter="fetchData"
        >
          <template #prefix><SearchOutlined style="color: #bfbfbf" /></template>
        </a-input>

        <!-- 狀態篩選 -->
        <a-select
          v-model:value="status"
          :options="statusOptions"
          placeholder="狀態"
          style="width: 140px"
          allow-clear
        />

        <a-button type="primary" class="rounded-btn" @click="handleSearch()">
          <template #icon><SearchOutlined /></template>
          搜尋
        </a-button>
        <a-button class="rounded-btn" @click="fetchData()">
          <template #icon><SyncOutlined /></template>
          重新整理
        </a-button>
      </div>
    </div>

    <!-- 表格 -->

    <a-table
      :columns="columns"
      :data-source="applications"
      :loading="loading"
      :pagination="pagination"
      row-key="applicationId"
      class="custom-table"
      :scroll="{ x: 800 }"
      @change="handlePageChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <div :class="['status-tag', `status-${(record.status || '').toLowerCase()}`]">
            <span class="status-dot"></span>
            {{ getApplicationStatusLabel(record.status) }}
          </div>
        </template>
        <template v-else-if="column.key === 'documents'">
          <a-button type="link" @click="openDocumentsModal(record)"> 查看附件 </a-button>
        </template>

        <template v-else-if="column.key === 'supplement'">
          <a-button type="link" danger @click="openSupplementModal(record)"> 退回補件 </a-button>
        </template>

        <!-- 查看明細 -->
        <template v-else-if="column.key === 'detail'">
          <a-button type="link" @click="goDetail(record)"> 審核明細 </a-button>
        </template>

        <!-- 修改備註 -->
        <template v-else-if="column.key === 'editRemark'">
          <a-button type="link" @click="openRemarkModal(record)"> 修改備註 </a-button>
        </template>

        <!-- 刪除 -->
        <template v-else-if="column.key === 'delete'">
          <a-button danger type="link" @click="handleDelete(record)"> 刪除 </a-button>
        </template>
      </template>
    </a-table>
    <!-- modal -->
    <a-modal
      v-model:open="remarkModalVisible"
      title="修改備註"
      @ok="handleUpdateRemark"
      @cancel="remarkModalVisible = false"
    >
      <a-input v-model:value="remarkInput" placeholder="請輸入備註" autofocus></a-input>
    </a-modal>
    <a-modal
  v-model:open="documentModalVisible"
  title="財力證明附件"
  :footer="null"
>
  <a-empty v-if="currentDocuments.length === 0" description="尚未上傳附件" />

  <div v-else class="document-list">
    <div v-for="doc in currentDocuments" :key="doc.documentId" class="document-item">
      <a :href="getFileUrl(doc.fileUrl)" target="_blank">
        {{ doc.fileName || doc.fileUrl }}
      </a>
      <div class="document-time">
        補件時間：{{ formatDocumentTime(doc.uploadedAt) }}
      </div>
    </div>
  </div>
</a-modal>

<a-modal
  v-model:open="supplementModalVisible"
  title="退回補件"
  @ok="handleNeedSupplement"
  @cancel="supplementModalVisible = false"
>
  <a-input
    v-model:value="supplementRemark"
    placeholder="請輸入補件原因，例如：請補上近三個月薪資單"
  />
</a-modal>
  </div>
</template>

<style scoped>
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
  color: #5c6b5f;
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

.status-approved {
  background-color: rgba(82, 196, 26, 0.1);
  color: #389e0d;
}
.status-approved .status-dot {
  background-color: #52c41a;
}

.status-rejected {
  background-color: rgba(255, 77, 79, 0.1);
  color: #d9363e;
}
.status-rejected .status-dot {
  background-color: #ff4d4f;
}

.status-pending {
  background-color: rgba(250, 140, 22, 0.1);
  color: #fa8c16;
}
.status-pending .status-dot {
  background-color: #fa8c16;
}

.status-completed {
  background-color: rgba(82, 196, 26, 0.1);
  color: #389e0d;
}

.status-completed .status-dot {
  background-color: #52c41a;
}
:deep(.ant-btn-link) {
  padding-inline: 0 !important;
}
.status-need_supplement {
  background-color: rgba(22, 119, 255, 0.1);
  color: #1677ff;
}

.status-need_supplement .status-dot {
  background-color: #1677ff;
}

.document-list {
  display: grid;
  gap: 12px;
}

.document-item {
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border, #e5e7eb);
}

.document-item:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.document-time {
  margin-top: 4px;
  color: #6b7280;
  font-size: 13px;
}
</style>
