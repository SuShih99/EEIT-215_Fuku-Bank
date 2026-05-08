<script setup>
import { ref, onMounted } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  getApplications,
  deleteApplication,
  updateApplicationStatus,
  updateApplicationRemark,
} from '@/api/cardApplication'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { DownOutlined, SearchOutlined } from '@ant-design/icons-vue'

const router = useRouter()

const goDetail = (record) => {
  router.push(`/admin/card-applications/${record.applicationId}`)
}

const applications = ref([])
const loading = ref(false)

const keyword = ref('')
const status = ref(null)

const statusOptions = [
  { label: '全部', value: null },
  { label: '審核中', value: 'PENDING' },
  { label: '核准', value: 'APPROVED' },
  { label: '拒絕', value: 'REJECTED' },
]

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
  { title: '使用者', dataIndex: 'customerName', key: 'customerName', width: 150 },
  {
    title: '申請日期',
    dataIndex: 'applyDate',
    key: 'applyDate',
    width: 180,
    customRender: (record) => {
      return dayjs(record.applyDate).format('YYYY-MM-DD HH:mm')
    },
  },
  { title: '狀態', dataIndex: 'status', key: 'status', width: 120 },
  { title: '備註', dataIndex: 'remark', key: 'remark' },
  { title: '操作', key: 'action', width: 120, fixed: 'right' },
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

    console.log('applications:', applications.value)

    pagination.value.total = data.totalElements
  } catch (error) {
    console.log(error)
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
        console.log(error)
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
    console.log(error)
  }
}

//分頁
const handlePageChange = (page) => {
  pagination.value.current = page.current
  fetchData()
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

        <a-button
          type="primary"
          class="rounded-btn"
          @click="
            () => {
              pagination.current = 1
              fetchData()
            }
          "
        >
          <template #icon><SearchOutlined /></template>
          搜尋
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
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'customerName'">
          <div class="emp-name-cell">
            <div class="emp-avatar">{{ record.customerName ? record.customerName.charAt(0) : '?' }}</div>
            <div class="emp-info">
              <span class="emp-name-text">{{ record.customerName }}</span>
              <span class="emp-id-text">{{ record.applicationId }}</span>
            </div>
          </div>
        </template>

        <template v-else-if="column.key === 'status'">
          <div :class="['status-tag', `status-${record.status.toLowerCase()}`]">
            <span class="status-dot"></span>
            {{ statusOptions.find(opt => opt.value === record.status)?.label || record.status }}
          </div>
        </template>

        <template v-else-if="column.key === 'action'">
          <div class="action-cell">
            <a-dropdown>
              <a-button type="link" class="action-btn">
                操作 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <a-menu-item @click="handleApprove(record)">核准</a-menu-item>
                  <a-menu-item @click="handleReject(record)">拒絕</a-menu-item>
                  <a-menu-item @click="openRemarkModal(record)">修改備註</a-menu-item>
                  <a-menu-item danger @click="handleDelete(record)">刪除</a-menu-item>
                  <a-menu-item @click="goDetail(record)">查看明細</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
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

.status-approved { background-color: rgba(82, 196, 26, 0.1); color: #389e0d; }
.status-approved .status-dot { background-color: #52c41a; }

.status-rejected { background-color: rgba(255, 77, 79, 0.1); color: #d9363e; }
.status-rejected .status-dot { background-color: #ff4d4f; }

.status-pending { background-color: rgba(250, 140, 22, 0.1); color: #fa8c16; }
.status-pending .status-dot { background-color: #fa8c16; }
</style>
