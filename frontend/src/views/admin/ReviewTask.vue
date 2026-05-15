<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">審核任務</h2>
    </div>

    <div class="action-bar">
      <!-- 統計卡片 -->
      <a-row :gutter="16" style="margin-bottom: 24px">
        <a-col :span="6">
          <a-statistic title="待處理" :value="stats.pending" value-style="{color: #faad14}" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="處理中" :value="stats.processing" value-style="{color: #1890ff}" />
        </a-col>
        <a-col :span="6">
          <a-statistic title="已結案" :value="stats.completed" value-style="{color: #52c41a}" />
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
    </div>

    <!-- 任務列表 -->
    <a-table
      :columns="columns"
      :data-source="tasks"
      :loading="loading"
      :pagination="pagination"
      row-key="taskId"
      :row-class-name="() => 'clickable-row'"
      @change="handleTableChange"
      @row-click="onRowClick"
      :custom-row="(record) => ({ onClick: () => onRowClick(record) })"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'priority'">
          <a-tag :color="priorityColor(record.priority)">P{{ record.priority }}</a-tag>
        </template>
        <template v-if="column.key === 'status'">
          <a-badge :status="statusBadge(record.status)" :text="statusLabel(record.status)" />
        </template>
        <template v-if="column.key === 'riskLevel'">
          <a-tag v-if="record.riskLevel" :color="riskColor(record.riskLevel)">{{
            riskLabel(record.riskLevel)
          }}</a-tag>
          <span v-else style="color: #bfbfbf">—</span>
        </template>
        <template v-if="column.key === 'scene'">{{ sceneLabel(record.scene) }}</template>
        <template v-if="column.key === 'action'">
          <a-button
            v-if="record.status !== 'COMPLETED'"
            type="link"
            size="small"
            @click.stop="openDecisionModal(record)"
            >審核</a-button
          >
          <span v-else style="color: #bfbfbf; font-size: 13px">已結案</span>
        </template>
      </template>
    </a-table>

    <!-- ── 詳情 Drawer ── -->
    <a-drawer
      v-model:open="drawerVisible"
      title="任務詳情"
      width="480"
      :body-style="{ padding: '16px 24px' }"
    >
      <template v-if="drawerTask">
        <a-descriptions
          title="任務資訊"
          bordered
          :column="1"
          size="small"
          style="margin-bottom: 20px"
        >
          <a-descriptions-item label="任務 ID">#{{ drawerTask.taskId }}</a-descriptions-item>
          <a-descriptions-item label="業務編號">
            <span style="font-family: monospace">{{ drawerTask.businessId }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="場景">{{ sceneLabel(drawerTask.scene) }}</a-descriptions-item>
          <a-descriptions-item label="優先度">
            <a-tag :color="priorityColor(drawerTask.priority)">P{{ drawerTask.priority }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="狀態">
            <a-badge
              :status="statusBadge(drawerTask.status)"
              :text="statusLabel(drawerTask.status)"
            />
          </a-descriptions-item>
          <a-descriptions-item label="建立時間">{{ drawerTask.createAt }}</a-descriptions-item>
          <a-descriptions-item label="結案時間">{{
            drawerTask.processedAt || '—'
          }}</a-descriptions-item>
        </a-descriptions>

        <a-descriptions
          title="風控資訊"
          bordered
          :column="1"
          size="small"
          style="margin-bottom: 20px"
        >
          <a-descriptions-item label="風險等級">
            <a-tag v-if="drawerTask.riskLevel" :color="riskColor(drawerTask.riskLevel)">
              {{ riskLabel(drawerTask.riskLevel) }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="申請金額">
            {{
              drawerTask.transactionAmount
                ? '$ ' + Number(drawerTask.transactionAmount).toLocaleString('zh-TW')
                : '—'
            }}
          </a-descriptions-item>
          <a-descriptions-item label="風控原因">{{
            drawerTask.triggerReason || '—'
          }}</a-descriptions-item>

          <!-- 展開 metaData -->
          <template v-if="parsedMeta(drawerTask.metaData)">
            <a-descriptions-item label="最終分數">
              {{ parsedMeta(drawerTask.metaData)?.finalScore ?? '—' }}
            </a-descriptions-item>
            <a-descriptions-item label="年收入">
              {{
                parsedMeta(drawerTask.metaData)?.annualIncome
                  ? '$ ' +
                    Number(parsedMeta(drawerTask.metaData).annualIncome).toLocaleString('zh-TW')
                  : '—'
              }}
            </a-descriptions-item>
            <a-descriptions-item label="他行負債">
              {{
                parsedMeta(drawerTask.metaData)?.otherBankDebt
                  ? '$ ' +
                    Number(parsedMeta(drawerTask.metaData).otherBankDebt).toLocaleString('zh-TW')
                  : '—'
              }}
            </a-descriptions-item>
            <a-descriptions-item label="有不動產">
              {{ parsedMeta(drawerTask.metaData)?.hasRealEstate ? '是' : '否' }}
            </a-descriptions-item>
          </template>
        </a-descriptions>

        <a-descriptions
          v-if="drawerTask.status === 'COMPLETED'"
          title="審核結果"
          bordered
          :column="1"
          size="small"
        >
          <a-descriptions-item label="決策">
            <a-tag
              :color="
                drawerTask.reviewResult === 'APPROVED'
                  ? 'green'
                  : drawerTask.reviewResult === 'REJECTED'
                    ? 'red'
                    : 'orange'
              "
            >
              {{
                { APPROVED: '核准', REJECTED: '拒絕', RETURNED: '退回補件' }[
                  drawerTask.reviewResult
                ]
              }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="審核人">{{ drawerTask.assignee || '—' }}</a-descriptions-item>
          <a-descriptions-item label="備註意見">{{
            drawerTask.adminComment || '—'
          }}</a-descriptions-item>
        </a-descriptions>

        <!-- 未結案：顯示審核按鈕 -->
        <div v-if="drawerTask.status !== 'COMPLETED'" style="margin-top: 20px">
          <a-button type="primary" block @click="handleReviewAction(drawerTask)">
            進行審核
          </a-button>
        </div>
      </template>
    </a-drawer>

    <!-- ── 審核決策 Modal ── -->
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
          <a-tag :color="riskColor(currentTask?.riskLevel)">{{
            riskLabel(currentTask?.riskLevel)
          }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="風控原因" :span="2">{{
          currentTask?.triggerReason
        }}</a-descriptions-item>
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
const drawerVisible = ref(false)
const currentTask = ref(null)
const drawerTask = ref(null)

const filters = reactive({ status: undefined, scene: undefined, priority: undefined })
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total) => `共 ${total} 筆`,
})
const form = reactive({ reviewResult: undefined, adminComment: '' })

const stats = computed(() => ({
  pending: tasks.value.filter((t) => t.status === 'PENDING').length,
  processing: tasks.value.filter((t) => t.status === 'PROCESSING').length,
  completed: tasks.value.filter((t) => t.status === 'COMPLETED').length,
  total: pagination.total,
}))

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

async function fetchTasks() {
  loading.value = true
  try {
    const params = { page: pagination.current - 1, size: pagination.pageSize }
    if (filters.status) params.status = filters.status
    if (filters.scene) params.scene = filters.scene
    if (filters.priority) params.priority = filters.priority

    const res = await axios.get(BASE_URL, { params, withCredentials: true })
    const page = res.data.data
    tasks.value = page.content
    // 確保 page.number 是有效數字，若為 undefined/null 則預設為 0
    const backendPageNumber = Number(page.number || 0)
    pagination.total = page.totalElements // 後端返回的總元素數量
    pagination.current = Math.max(1, backendPageNumber + 1) // 確保當前頁碼至少為 1
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
    await axios.put(
      `${BASE_URL}/${currentTask.value.taskId}/decision`,
      { reviewResult: form.reviewResult, adminComment: form.adminComment },
      { withCredentials: true },
    )
    message.success('決策送出成功')
    modalVisible.value = false
    resetForm()
    fetchTasks()
  } catch (e) {
    if (e.response?.status === 409) message.error('該任務已被其他人審核，請重新整理')
    else message.error('送出失敗：' + (e.response?.data?.message || e.message))
  } finally {
    submitting.value = false
  }
}

function handleTableChange(pag) {
  // 確保 pag.current 是有效數字，若為 undefined/null/0 則預設為 1
  pagination.current = Math.max(1, Number(pag.current || 1)) // 確保當前頁碼至少為 1
  pagination.pageSize = pag.pageSize // 更新每頁顯示數量
  fetchTasks()
}

function onRowClick(record) {
  drawerTask.value = record
  drawerVisible.value = true
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

const handleReviewAction = (task) => {
  openDecisionModal(task)
  drawerVisible.value = false
}

function parsedMeta(metaData) {
  if (!metaData) return null
  try {
    return JSON.parse(metaData)
  } catch {
    return null
  }
}

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

<style scoped>
:deep(.clickable-row) {
  cursor: pointer;
}
:deep(.clickable-row:hover td) {
  background: #fafafa;
}
</style>
