<template>
  <div style="padding: 24px">
    <!-- 統計卡片 -->
    <a-row :gutter="16" style="margin-bottom: 24px">
      <a-col :span="6">
        <a-statistic title="待處理" :value="stats.pending" value-style="color: #faad14" />
      </a-col>
      <a-col :span="6">
        <a-statistic title="處理中" :value="stats.processing" value-style="color: #1890ff" />
      </a-col>
      <a-col :span="6">
        <a-statistic title="已結案" :value="stats.completed" value-style="color: #52c41a" />
      </a-col>
      <a-col :span="6">
        <a-statistic title="總計" :value="stats.total" />
      </a-col>
    </a-row>

    <!-- 篩選列 -->
    <a-space style="margin-bottom: 16px" wrap>
      <a-select
        v-model:value="filters.status"
        placeholder="全部狀態"
        allow-clear
        style="width: 130px"
        @change="fetchTasks"
      >
        <a-select-option value="PENDING">待處理</a-select-option>
        <a-select-option value="PROCESSING">處理中</a-select-option>
        <a-select-option value="COMPLETED">已結案</a-select-option>
      </a-select>

      <a-select
        v-model:value="filters.scene"
        placeholder="全部場景"
        allow-clear
        style="width: 140px"
        @change="fetchTasks"
      >
        <a-select-option value="LOAN_APPLY">貸款申請</a-select-option>
        <a-select-option value="ACCOUNT_OPEN">帳戶開戶</a-select-option>
        <a-select-option value="CARD_APPLY">信用卡申請</a-select-option>
      </a-select>

      <a-select
        v-model:value="filters.priority"
        placeholder="全部優先度"
        allow-clear
        style="width: 130px"
        @change="fetchTasks"
      >
        <a-select-option :value="1">高（P1）</a-select-option>
        <a-select-option :value="5">中（P5）</a-select-option>
        <a-select-option :value="10">低（P10）</a-select-option>
      </a-select>

      <a-button @click="fetchTasks">
        <template #icon><ReloadOutlined /></template>
        重新整理
      </a-button>
    </a-space>

    <!-- 任務列表 -->
    <a-table
      :columns="columns"
      :data-source="tasks"
      :loading="loading"
      :pagination="pagination"
      row-key="taskId"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'priority'">
          <a-tag :color="priorityColor(record.priority)"> P{{ record.priority }} </a-tag>
        </template>

        <template v-if="column.key === 'status'">
          <a-badge :status="statusBadge(record.status)" :text="statusLabel(record.status)" />
        </template>

        <template v-if="column.key === 'riskLevel'">
          <a-tag :color="riskColor(record.riskLevel)">
            {{ riskLabel(record.riskLevel) }}
          </a-tag>
        </template>

        <template v-if="column.key === 'scene'">
          {{ sceneLabel(record.scene) }}
        </template>

        <template v-if="column.key === 'action'">
          <a-button
            v-if="record.status !== 'COMPLETED'"
            type="link"
            size="small"
            @click="openDecisionModal(record)"
          >
            審核
          </a-button>
          <span v-else style="color: #bfbfbf; font-size: 13px">已結案</span>
        </template>
      </template>
    </a-table>

    <!-- 審核決策 Modal -->
    <a-modal
      v-model:open="modalVisible"
      title="審核決策"
      ok-text="送出決策"
      cancel-text="取消"
      :confirm-loading="submitting"
      @ok="submitDecision"
      @cancel="resetForm"
    >
      <a-descriptions :column="2" bordered size="small" style="margin-bottom: 16px">
        <a-descriptions-item label="任務 ID">#{{ currentTask?.taskId }}</a-descriptions-item>
        <a-descriptions-item label="業務編號">{{ currentTask?.businessId }}</a-descriptions-item>
        <a-descriptions-item label="場景">{{ sceneLabel(currentTask?.scene) }}</a-descriptions-item>
        <a-descriptions-item label="風險等級">
          <a-tag :color="riskColor(currentTask?.riskLevel)">
            {{ riskLabel(currentTask?.riskLevel) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="風險原因" :span="2">
          {{ currentTask?.triggerReason }}
        </a-descriptions-item>
      </a-descriptions>

      <a-form :model="form" layout="vertical">
        <a-form-item label="決策結果" required>
          <a-radio-group v-model:value="form.reviewResult">
            <a-radio value="APPROVED">核准</a-radio>
            <a-radio value="REJECTED">拒絕</a-radio>
            <a-radio value="RETURNED">退回補件</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="備註意見">
          <a-textarea v-model:value="form.adminComment" :rows="3" placeholder="輸入審核意見..." />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import axios from 'axios'

const BASE_URL = '/api/risk/reviewtask'

const tasks = ref([])
const loading = ref(false)
const submitting = ref(false)
const modalVisible = ref(false)
const currentTask = ref(null)

const filters = reactive({
  status: undefined,
  scene: undefined,
  priority: undefined,
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total) => `共 ${total} 筆`,
})

const form = reactive({
  reviewResult: undefined,
  adminComment: '',
})

// ── 統計數字（從列表資料計算）──────────────────────────
const stats = computed(() => ({
  pending: tasks.value.filter((t) => t.status === 'PENDING').length,
  processing: tasks.value.filter((t) => t.status === 'PROCESSING').length,
  completed: tasks.value.filter((t) => t.status === 'COMPLETED').length,
  total: pagination.total,
}))

// ── Table 欄位定義 ────────────────────────────────────
const columns = [
  { title: '任務 ID', dataIndex: 'taskId', key: 'taskId', width: 90 },
  { title: '業務編號', dataIndex: 'businessId', key: 'businessId', ellipsis: true },
  { title: '場景', dataIndex: 'scene', key: 'scene', width: 110 },
  { title: '優先度', dataIndex: 'priority', key: 'priority', width: 90, sorter: true },
  { title: '狀態', dataIndex: 'status', key: 'status', width: 100 },
  { title: '風險等級', dataIndex: 'riskLevel', key: 'riskLevel', width: 100 },
  { title: '建立時間', dataIndex: 'createAt', key: 'createAt', width: 160 },
  { title: '操作', key: 'action', width: 80, fixed: 'right' },
]

// ── API ──────────────────────────────────────────────
async function fetchTasks() {
  loading.value = true
  try {
    const params = {
      page: pagination.current - 1, // Spring 從 0 開始
      size: pagination.pageSize,
    }
    if (filters.status) params.status = filters.status
    if (filters.scene) params.scene = filters.scene
    if (filters.priority) params.priority = filters.priority

    const res = await axios.get(BASE_URL, { params })
    const page = res.data.data

    tasks.value = page.content
    pagination.total = page.totalElements
    pagination.current = page.number + 1
  } catch (e) {
    message.error('載入失敗：' + (e.response?.data?.message || e.message))
  } finally {
    loading.value = false
  }
}

async function submitDecision() {
  if (!form.reviewResult) {
    message.warning('請選擇決策結果')
    return
  }
  submitting.value = true
  try {
    await axios.put(`${BASE_URL}/${currentTask.value.taskId}/decision`, {
      reviewResult: form.reviewResult,
      adminComment: form.adminComment,
    })
    message.success('決策送出成功')
    modalVisible.value = false
    resetForm()
    fetchTasks()
  } catch (e) {
    // 409 樂觀鎖衝突
    if (e.response?.status === 409) {
      message.error('該任務已被其他人審核，請重新整理')
    } else {
      message.error('送出失敗：' + (e.response?.data?.message || e.message))
    }
  } finally {
    submitting.value = false
  }
}

// ── 事件處理 ─────────────────────────────────────────
function handleTableChange(pag, _, sorter) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchTasks()
}

function openDecisionModal(task) {
  currentTask.value = task
  modalVisible.value = true
}

function resetForm() {
  form.reviewResult = undefined
  form.adminComment = ''
  currentTask.value = null
}

// ── 顯示輔助 ─────────────────────────────────────────
function statusLabel(s) {
  return { PENDING: '待處理', PROCESSING: '處理中', COMPLETED: '已結案' }[s] || s
}
function statusBadge(s) {
  return { PENDING: 'warning', PROCESSING: 'processing', COMPLETED: 'success' }[s] || 'default'
}
function sceneLabel(s) {
  return { LOAN_APPLY: '貸款申請', ACCOUNT_OPEN: '帳戶開戶', CARD_APPLY: '信用卡申請' }[s] || s
}
function riskLabel(r) {
  return { HIGH: '高風險', MEDIUM: '中風險', LOW: '低風險' }[r] || r
}
function riskColor(r) {
  return { HIGH: 'red', MEDIUM: 'orange', LOW: 'green' }[r] || 'default'
}
function priorityColor(p) {
  return { 1: 'red', 5: 'orange', 10: 'green' }[p] || 'default'
}

onMounted(fetchTasks)
</script>
