<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">審核任務</h2>
      <a-space class="filter-bar" wrap :size="12">
        <a-select
          v-model:value="filters.status"
          placeholder="全部狀態"
          allow-clear
          style="width: 150px"
          size="large"
          @change="fetchTasks"
        >
          <a-select-option value="PENDING">待處理</a-select-option>
          <a-select-option value="PROCESSING">處理中</a-select-option>
          <a-select-option value="COMPLETED">已結案</a-select-option>
        </a-select>
        <a-select
          v-model:value="filters.scene"
          placeholder="全部事件"
          allow-clear
          style="width: 160px"
          size="large"
          @change="fetchTasks"
        >
          <a-select-option value="LOAN_APPLY">貸款申請</a-select-option>
          <a-select-option value="ACCOUNT_OPEN">帳戶開戶</a-select-option>
          <a-select-option value="CARD_APPLY">信用卡申請</a-select-option>
          <a-select-option value="TRANSFER_REVIEW">轉帳審核</a-select-option>
        </a-select>
        <a-select
          v-model:value="filters.priority"
          placeholder="全部優先度"
          allow-clear
          style="width: 150px"
          size="large"
          @change="fetchTasks"
        >
          <a-select-option :value="2">高（P2）</a-select-option>
          <a-select-option :value="5">中（P5）</a-select-option>
          <a-select-option :value="10">低（P10）</a-select-option>
        </a-select>
        <a-button size="large" @click="fetchTasks">
          <template #icon>
            <ReloadOutlined/>
          </template>
          重新整理
        </a-button>
      </a-space>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <span class="stat-label">待處理</span>
        <span class="stat-value pending">{{ stats.pending }}</span>
      </div>
      <div class="stat-divider"/>
      <div class="stat-card">
        <span class="stat-label">處理中</span>
        <span class="stat-value processing">{{ stats.processing }}</span>
      </div>
      <div class="stat-divider"/>
      <div class="stat-card">
        <span class="stat-label">已結案</span>
        <span class="stat-value completed">{{ stats.completed }}</span>
      </div>
      <div class="stat-divider"/>
      <div class="stat-card">
        <span class="stat-label">總計</span>
        <span class="stat-value total">{{
            stats.pending + stats.processing + stats.completed
          }}</span>
      </div>
    </div>

    <a-table
      :columns="columns"
      :data-source="tasks"
      :loading="loading"
      :pagination="pagination"
      row-key="taskId"
      :row-class-name="() => 'clickable-row'"
      class="large-table"
      @change="handleTableChange"
      @row-click="onRowClick"
      :custom-row="(record) => ({ onClick: () => onRowClick(record) })"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'createAt'">
          {{ formatDate(record.createAt) }}
        </template>
        <template v-if="column.key === 'priority'">
          <a-tag :color="priorityColor(record.priority)" class="table-badge">P{{
              record.priority
            }}
          </a-tag>
        </template>
        <template v-if="column.key === 'businessId'">
          <a-tooltip :title="record.businessId">
            <span class="mono-id">{{ record.businessId }}</span>
          </a-tooltip>
        </template>
        <template v-if="column.key === 'status'">
          <span class="large-badge-text">
            <a-badge :status="statusBadge(record)" :text="statusLabel(record)"/>
          </span>
        </template>
        <template v-if="column.key === 'riskLevel'">
          <div v-if="record.riskLevel"
               :class="['status-tag', `risk-${record.riskLevel.toLowerCase()}`]">
            <span class="status-dot"></span>
            {{ riskLabel(record.riskLevel) }}
          </div>
          <span v-else style="color: #bfbfbf">—</span>
        </template>
        <template v-if="column.key === 'scene'">{{ sceneLabel(record.scene) }}</template>
        <template v-if="column.key === 'action'">
          <a-button
            v-if="canReviewTask(record)"
            type="link"
            class="action-btn-link"
            @click.stop="handleReviewAction(record)"
          >審核
          </a-button>
          <span v-else-if="isWaitingDocument(record)"
                style="color: #b08a42;" class="action-status-text">待補件</span>
          <span v-else style="color: #bfbfbf;" class="action-status-text">已結案</span>
        </template>
      </template>
    </a-table>

    <a-drawer
      v-model:open="drawerVisible"
      title="任務詳情"
      width="520"
      :body-style="{ padding: '16px 24px' }"
      class="large-drawer"
    >
      <template v-if="drawerTask">
        <a-descriptions
          title="任務資訊"
          bordered
          :column="1"
          size="middle"
          style="margin-bottom: 24px"
        >
          <a-descriptions-item label="業務編號">
            <span style="font-family: monospace">{{ drawerTask.businessId }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="事件">{{ sceneLabel(drawerTask.scene) }}</a-descriptions-item>
          <a-descriptions-item label="優先度">
            <a-tag :color="priorityColor(drawerTask.priority)" class="table-badge">
              P{{ drawerTask.priority }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="狀態">
            <span class="large-badge-text">
              <a-badge :status="statusBadge(drawerTask)" :text="statusLabel(drawerTask)"/>
            </span>
          </a-descriptions-item>
          <a-descriptions-item label="建立時間">{{
              formatDate(drawerTask.createAt)
            }}
          </a-descriptions-item>
          <a-descriptions-item label="結案時間">{{
              drawerTask.processedAt ? formatDate(drawerTask.processedAt) : '—'
            }}
          </a-descriptions-item>
        </a-descriptions>

        <a-descriptions
          title="風控資訊"
          bordered
          :column="1"
          size="middle"
          style="margin-bottom: 24px"
        >
          <a-descriptions-item label="風險等級">
            <div v-if="drawerTask.riskLevel"
                   :class="['status-tag', `risk-${drawerTask.riskLevel.toLowerCase()}`]"
            ><span class="status-dot"></span>
              {{ riskLabel(drawerTask.riskLevel) }}
            </div>
          </a-descriptions-item>
          <a-descriptions-item label="申請金額">
            <span class="highlight-money">
              {{
                drawerTask.transactionAmount
                  ? '$ ' + Number(drawerTask.transactionAmount).toLocaleString('zh-TW')
                  : '—'
              }}
            </span>
          </a-descriptions-item>
          <template v-if="drawerTrigger">
            <a-descriptions-item label="綜合評分">
              {{ drawerTrigger.finalScore ?? '—' }}
            </a-descriptions-item>
            <a-descriptions-item label="聯徵分數">
              {{ drawerTrigger.externalScore ?? '—' }}
            </a-descriptions-item>
            <a-descriptions-item label="職業">
              {{ occupationLabel(drawerTrigger.occupation) }}
            </a-descriptions-item>
            <a-descriptions-item label="年收入">
              {{
                drawerTrigger.annualIncome != null
                  ? '$ ' + Number(drawerTrigger.annualIncome).toLocaleString('zh-TW')
                  : '—'
              }}
            </a-descriptions-item>
            <a-descriptions-item label="他行負債">
              {{
                drawerTrigger.otherBankDebt != null
                  ? '$ ' + Number(drawerTrigger.otherBankDebt).toLocaleString('zh-TW')
                  : '—'
              }}
            </a-descriptions-item>
            <a-descriptions-item label="有不動產">
              {{ drawerTrigger.hasRealEstate ? '是' : '否' }}
            </a-descriptions-item>
            <a-descriptions-item label="PEP 人士">
              <a-tag :color="drawerTrigger.isPep ? 'red' : 'default'" class="table-badge">
                {{ drawerTrigger.isPep ? '是' : '否' }}
              </a-tag>
            </a-descriptions-item>
          </template>
          <template v-else>
            <a-descriptions-item label="風控原因">
              {{ drawerTask.triggerReason || '—' }}
            </a-descriptions-item>
          </template>
        </a-descriptions>

        <div v-if="hasAttachments(drawerTask)" style="margin-top: 24px">
          <h3 class="drawer-sub-section-title">客戶補件附件</h3>
          <div class="attachments-container">
            <a-card
              v-for="doc in parseAttachments(drawerTask.attachments)"
              :key="doc.documentId"
              size="small"
              style="margin-bottom: 12px; border-radius: 6px"
              class="attachment-card"
            >
              <div style="display: flex; align-items: center; justify-content: space-between">
                <div>
                  <a-tag color="blue" class="table-badge">{{ docLabel(doc.documentType) }}</a-tag>
                  <span class="attachment-name">{{ doc.originalName || doc.documentId }}</span>
                </div>

                <a-button
                  type="link"
                  class="action-btn-link"
                  :href="getFileUrl(doc.fileUrl)"
                  target="_blank"
                  v-if="doc.fileUrl"
                >
                  查看文件
                </a-button>
                <span v-else style="color: #bfbfbf; font-size: 14px">無連結</span>
              </div>

              <div
                v-if="doc.fileUrl && isImage(doc.fileUrl)"
                style="
                  margin-top: 8px;
                  text-align: center;
                  background: #fafafa;
                  padding: 8px;
                  border-radius: 4px;
                "
              >
                <img
                  :src="getFileUrl(doc.fileUrl)"
                  style="
                    max-width: 100%;
                    max-height: 180px;
                    object-fit: contain;
                    border: 1px solid #f0f0f0;
                  "
                  alt="補件預覽"
                />
              </div>
            </a-card>
          </div>
        </div>

        <a-descriptions
          v-if="drawerTask.status === 'COMPLETED'"
          title="審核結果"
          bordered
          :column="1"
          size="middle"
          style="margin-top: 24px"
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
              class="table-badge"
            >
              {{
                {APPROVED: '核准', REJECTED: '拒絕', RETURNED: '退回補件'}[
                  drawerTask.reviewResult
                  ]
              }}
            </a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="審核人">{{ drawerTask.assignee || '—' }}</a-descriptions-item>
          <a-descriptions-item label="備註意見">{{
              drawerTask.adminComment || '—'
            }}
          </a-descriptions-item>
        </a-descriptions>

        <div v-if="canReviewTask(drawerTask)" style="margin-top: 24px">
          <a-button type="primary" block size="large" @click="handleReviewAction(drawerTask)">
            進行審核
          </a-button>
        </div>
        <div v-else-if="isWaitingDocument(drawerTask)" style="margin-top: 24px">
          <a-alert
            message="此案件正在等待客戶補件"
            description="客戶送出補件後，案件會回到可審核狀態。"
            type="warning"
            show-icon
            class="large-alert-text"
          />
        </div>
      </template>
    </a-drawer>

    <a-modal
      v-model:open="modalVisible"
      title="審核決策"
      cancel-text="取消"
      :confirm-loading="submitting"
      class="large-modal"
      width="550"
      @cancel="resetForm"
    >
      <template #footer>
        <a-button size="large" @click="handleCancel">取消</a-button>
        <a-tooltip
          :title="
            currentTask?.assignee !== currentUser?.email
              ? '此任務由 ' + currentTask?.assignee + ' 鎖定，您無法送出決策'
              : ''
          "
        >
          <a-button
            type="primary"
            size="large"
            :loading="submitting"
            :disabled="currentTask?.assignee !== currentUser?.email"
            @click="submitDecision"
          >
            送出決策
          </a-button>
        </a-tooltip>
      </template>

      <a-descriptions :column="2" bordered size="middle" style="margin-bottom: 20px">
        <a-descriptions-item label="業務編號">{{ currentTask?.businessId }}</a-descriptions-item>
        <a-descriptions-item label="事件">{{ sceneLabel(currentTask?.scene) }}</a-descriptions-item>
        <a-descriptions-item label="風險等級">
          <a-tag :color="riskColor(currentTask?.riskLevel)" class="table-badge">{{
              riskLabel(currentTask?.riskLevel)
            }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="風控原因" :span="2">
          <template v-if="currentTrigger">
            <a-tag class="table-badge">分數 {{ currentTrigger.finalScore }}</a-tag>
            <a-tag :color="riskColor(currentTrigger.riskLevel)" class="table-badge">{{
                riskLabel(currentTrigger.riskLevel)
              }}
            </a-tag>
            <a-tag class="table-badge">{{ occupationLabel(currentTrigger.occupation) }}</a-tag>
          </template>
          <span v-else>{{ currentTask?.triggerReason || '—' }}</span>
        </a-descriptions-item>
      </a-descriptions>

      <div
        v-if="hasAttachments(currentTask)"
        style="
          margin-bottom: 20px;
          background: #fafafa;
          padding: 14px;
          border-radius: 6px;
          border: 1px dashed #d9d9d9;
        "
      >
        <h4 style="font-size: 15px; font-weight: 600; color: #595959; margin-bottom: 10px">
          客戶本次補件附件預覽：
        </h4>
        <div style="display: flex; flex-direction: column; gap: 10px">
          <div
            v-for="doc in parseAttachments(currentTask.attachments)"
            :key="doc.documentId"
            style="
              display: flex;
              align-items: center;
              justify-content: space-between;
              background: #fff;
              padding: 8px 12px;
              border-radius: 4px;
              border: 1px solid #f0f0f0;
            "
          >
            <div>
              <a-tag color="blue" class="table-badge">{{ docLabel(doc.documentType) }}</a-tag>
              <span
                style="font-size: 13px; color: #8c8c8c; font-family: monospace; margin-left: 8px;">
                {{ doc.originalName || doc.documentId }}
              </span>
            </div>
            <a-button
              type="link"
              class="action-btn-link"
              :href="getFileUrl(doc.fileUrl)"
              target="_blank"
              v-if="doc.fileUrl"
            >
              查看文件
            </a-button>
          </div>
        </div>
      </div>

      <a-form :model="form" layout="vertical" class="large-form">
        <a-form-item label="決策結果" required>
          <a-radio-group v-model:value="form.reviewResult" class="large-radio-group">
            <a-radio value="APPROVED">核准</a-radio>
            <a-radio value="REJECTED">拒絕</a-radio>
            <a-radio value="RETURNED">退回補件</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item v-if="form.reviewResult === 'RETURNED'" label="要求補件文件" required
                     style="margin-top: 12px">
          <a-checkbox-group v-model:value="form.requiredDocuments" class="large-checkbox-group">
            <div style="display: flex; flex-direction: column; gap: 10px">
              <a-checkbox value="ID_CARD">身分證</a-checkbox>
              <a-checkbox value="INCOME_CERT">收入證明</a-checkbox>
              <a-checkbox value="EMPLOYMENT_CERT">在職證明</a-checkbox>
              <a-checkbox value="BANK_STATEMENT">銀行存摺</a-checkbox>
              <a-checkbox value="PROPERTY_CERT">不動產謄本</a-checkbox>
              <a-checkbox value="TITLE_DEED">所有權狀</a-checkbox>
              <a-checkbox value="OTHER">其他（見備註）</a-checkbox>
            </div>
          </a-checkbox-group>
        </a-form-item>

        <a-form-item label="備註意見" style="margin-top: 16px">
          <a-textarea v-model:value="form.adminComment" :rows="3" placeholder="輸入審核意見..."
                      style="font-size: 16px;"/>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import {ref, reactive, onMounted, computed} from 'vue'
import {message} from 'ant-design-vue'
import {ReloadOutlined} from '@ant-design/icons-vue'
import {useAuthStore} from '@/stores/auth'
import {BASE_URL as SERVER_URL} from '@/api/axios'
import axios from 'axios'

const API_PREFIX = '/api/risk/reviewtask'

const tasks = ref([])
const sortField = ref('')
const sortOrder = ref('')

const loading = ref(false)
const submitting = ref(false)
const modalVisible = ref(false)
const drawerVisible = ref(false)
const currentTask = ref(null)
const drawerTask = ref(null)

const authStore = useAuthStore()
const currentUser = computed(() => authStore.user)

const filters = reactive({status: undefined, scene: undefined, priority: undefined})
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50', '100'],
  showTotal: (total) => `共 ${total} 筆`,
})
const form = reactive({reviewResult: undefined, adminComment: '', requiredDocuments: []})

const stats = computed(() => ({
  pending: tasks.value.filter((t) => t.status === 'PENDING').length,
  processing: tasks.value.filter((t) => t.status === 'PROCESSING').length,
  completed: tasks.value.filter((t) => t.status === 'COMPLETED').length,
  total: pagination.total,
}))

const columns = [
  {title: '業務編號', dataIndex: 'businessId', key: 'businessId', width: 160, ellipsis: true},
  {title: '事件', dataIndex: 'scene', key: 'scene', width: 120},
  {title: '優先度', dataIndex: 'priority', key: 'priority', width: 100, sorter: true},
  {title: '狀態', dataIndex: 'status', key: 'status', width: 110},
  {title: '風險等級', dataIndex: 'riskLevel', key: 'riskLevel', width: 110},
  {
    title: '建立時間',
    dataIndex: 'createAt',
    key: 'createAt',
    width: 170,
    sorter: true,
    sortDirections: ['descend', 'ascend', 'descend'],
    defaultSortOrder: 'descend'
  },
  {title: '操作', key: 'action', width: 90, fixed: 'right'},
]

async function fetchTasks() {
  loading.value = true
  try {
    const params = {page: pagination.current - 1, size: pagination.pageSize}
    if (filters.status) params.status = filters.status
    if (filters.scene) params.scene = filters.scene
    if (filters.priority) params.priority = filters.priority

    params.sort = sortField.value && sortOrder.value
      ? `${sortField.value},${sortOrder.value}`
      : 'createAt,desc'

    const res = await axios.get(API_PREFIX, {params, withCredentials: true})
    const page = res.data.data
    tasks.value = page.content
    const backendPageNumber = Number(page.number || 0)
    pagination.total = page.totalElements
    pagination.current = Math.max(1, backendPageNumber + 1)
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
  if (form.reviewResult === 'RETURNED' && form.requiredDocuments.length === 0) {
    message.warning('請至少選擇一項要求補件的文件類型')
    return
  }
  submitting.value = true
  try {
    const payload = {
      reviewResult: form.reviewResult,
      adminComment: form.adminComment,
      requiredDocuments: form.reviewResult === 'RETURNED' ? form.requiredDocuments : []
    }
    await axios.put(
      `${API_PREFIX}/${currentTask.value.taskId}/decision`,
      payload,
      {withCredentials: true},
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

function handleTableChange(pag, _filters, sorter) {
  pagination.current = Math.max(1, Number(pag.current || 1))
  pagination.pageSize = pag.pageSize

  if (sorter && sorter.field) {
    sortField.value = sorter.field
    sortOrder.value = sorter.order === 'ascend' ? 'asc' : 'desc'
  } else {
    sortField.value = ''
    sortOrder.value = ''
  }
  fetchTasks()
}

const handleCancel = () => {
  resetForm()
  modalVisible.value = false
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
  form.requiredDocuments = []
  currentTask.value = null
}

async function handleReviewAction(task) {
  if (!canReviewTask(task)) {
    message.warning(isWaitingDocument(task) ? '此案件正在等待客戶補件' : '此案件不可審核')
    return
  }
  if (task.status !== 'PROCESSING') {
    try {
      await axios.put(`${API_PREFIX}/${task.taskId}/start`, {}, {withCredentials: true})
      await fetchTasks()
      const updated = tasks.value.find((t) => t.taskId === task.taskId)
      currentTask.value = updated || task
    } catch (e) {
      if (e.response?.status === 409) {
        message.error('該案件已由他人鎖定，請重新整理')
      } else {
        message.error('鎖定失敗：' + (e.response?.data?.message || e.message))
      }
      fetchTasks()
      return
    }
  } else {
    currentTask.value = task
  }
  openDecisionModal(currentTask.value)
  drawerVisible.value = false
}

function isWaitingDocument(task) {
  return task?.status === 'PENDING' && task?.substatus === 'WAITING_DOCUMENT'
}

function canReviewTask(task) {
  return task?.status !== 'COMPLETED' && !isWaitingDocument(task)
}

function statusLabel(record) {
  if (record.status === 'PENDING') {
    return (
      {
        NEW: '待處理',
        WAITING_DOCUMENT: '待補件',
        RESUBMITTED: '已補件',
      }[record.substatus] || '待處理'
    )
  }
  return {PROCESSING: '處理中', COMPLETED: '已結案'}[record.status] || record.status
}

function statusBadge(record) {
  if (record.status === 'PENDING') {
    return (
      {
        NEW: 'warning',
        WAITING_DOCUMENT: 'error',
        RESUBMITTED: 'processing',
      }[record.substatus] || 'warning'
    )
  }
  return {PROCESSING: 'processing', COMPLETED: 'success'}[record.status] || 'default'
}

function sceneLabel(s) {
  return (
    {
      LOAN_APPLY: '貸款申請',
      ACCOUNT_OPEN: '帳戶開戶',
      CARD_APPLY: '信用卡申請',
      TRANSFER_REVIEW: '轉帳審核',
    }[s] || s
  )
}

function riskLabel(r) {
  return {HIGH: '高風險', MEDIUM: '中風險', LOW: '低風險'}[r] || r
}

function riskColor(r) {
  return {HIGH: 'red', MEDIUM: 'orange', LOW: 'green'}[r] || 'default'
}

function priorityColor(p) {
  return {1: 'red', 5: 'orange', 10: 'green'}[p] || 'default'
}

onMounted(fetchTasks)

function parseAttachments(attachmentsStr) {
  if (!attachmentsStr) return []
  try {
    const parsed = JSON.parse(attachmentsStr)
    return Array.isArray(parsed) ? parsed : []
  } catch (e) {
    console.error('解析補件資料失敗:', e)
    return []
  }
}

function hasAttachments(task) {
  return parseAttachments(task?.attachments).length > 0
}

const drawerTrigger = computed(() => {
  if (!drawerTask.value?.triggerReason) return null
  try {
    const parsed = JSON.parse(drawerTask.value.triggerReason)
    return typeof parsed === 'object' ? parsed : null
  } catch {
    return null
  }
})

const currentTrigger = computed(() => {
  if (!currentTask.value?.triggerReason) return null
  try {
    return JSON.parse(currentTask.value.triggerReason)
  } catch {
    return null
  }
})

function docLabel(type) {
  return (
    {
      ID_CARD: '身分證',
      INCOME_PROOF: '收入證明',
      PROPERTY_CERT: '不動產權狀',
      TAX_STATEMENT: '扣繳憑單',
    }[type] || type
  )
}

function getFileUrl(url) {
  if (!url) return ''
  if (url.startsWith('http')) return url
  return SERVER_URL + url
}

function occupationLabel(o) {
  return (
    {
      LEGISLATOR_MANAGER: '民意代表／高階主管',
      PROFESSIONAL: '專業人員',
      TECHNICIAN: '技術員及助理專業人員',
      CLERICAL: '事務支援人員',
      SERVICE_SALES: '服務及銷售工作人員',
      AGRICULTURAL: '農林漁牧業生產人員',
      CRAFT_WORKER: '技藝有關工作人員',
      MACHINE_OPERATOR: '機械設備操作及組裝人員',
      ELEMENTARY: '基層技術工及勞力工',
      MILITARY: '軍人',
      NONE: '無',
      OTHER: '其他',
    }[o] || o
  )
}

function formatDate(val) {
  if (!val) return '—'
  const cleanStr = val.replace('T', ' ').replace(/-/g, '/')
  return cleanStr.substring(0, 16)
}

function isImage(url) {
  if (!url) return false
  return /\.(jpg|jpeg|png|gif|webp)$/i.test(url)
}
</script>

<style scoped>
/* ── 整體頁面 ── */
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px; /* 💡 保持一致的留白 */
}

/* ── 頁首：標題 + 篩選列 同一列，左右對齊 ── */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 8px;
}

/* 💡 調整：放大頁面主標題到 24px 完全同步 */
.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #262626;
  line-height: 1;
}

.filter-bar {
  flex-shrink: 0;
}

/* ── 統計卡片列 ── */
.stats-row {
  display: flex;
  align-items: center;
  background: #ffffff;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  padding: 18px 32px; /* 💡 加寬襯墊更舒適 */
  margin-bottom: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.stat-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: #f0f0f0;
  margin: 0 12px;
}

/* 💡 調整：統計卡片字級調整放大 */
.stat-label {
  font-size: 14px;
  color: #8c8c8c;
  white-space: nowrap;
  font-weight: 500;
}

.stat-value {
  font-size: 32px; /* 放大數據值 */
  font-weight: 700;
  line-height: 1;
}

.stat-value.pending {
  color: #faad14;
}

.stat-value.processing {
  color: #1890ff;
}

.stat-value.completed {
  color: #52c41a;
}

.stat-value.total {
  color: #262626;
}

/* ── 表格深度覆蓋：完全比照 CreditList 規範的大字體風格 ── */
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

:deep(.clickable-row) {
  cursor: pointer;
}

:deep(.clickable-row:hover td) {
  background: #fafafa;
}

/* 💡 控制頂部篩選下拉選單與按鈕的字體 */
:deep(.ant-select) {
  font-size: 16px !important;
}

:deep(.ant-btn) {
  font-size: 16px !important;
}

:deep(.ant-pagination) {
  font-size: 15px !important;
}

.mono-id {
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 14px;
  color: #595959;
}

/* 💡 調整：表格內部與卡片內的 Tag 統一文字大小 */
.table-badge {
  font-size: 15px !important;
  padding: 2px 8px !important;
}

.large-badge-text :deep(.ant-badge-status-text) {
  font-size: 16px !important;
}

.action-btn-link {
  font-size: 16px !important;
  padding: 0;
}

.action-status-text {
  font-size: 15px;
  font-weight: 500;
}

/* ── Drawer & Modal 明細視窗樣式優化 ── */
.large-drawer :deep(.ant-drawer-title),
.large-modal :deep(.ant-modal-title) {
  font-size: 18px !important;
  font-weight: 600;
}

/* 描述清單文字放大 */
:deep(.ant-descriptions-title) {
  font-size: 16px !important;
  font-weight: 600;
}

:deep(.ant-descriptions-item-label),
:deep(.ant-descriptions-item-content) {
  font-size: 15px !important;
}

.highlight-money {
  font-weight: 600;
  color: #d9363e;
}

.drawer-sub-section-title {
  font-size: 16px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  margin-bottom: 12px;
}

.attachment-name {
  font-size: 14px;
  color: #595959;
  font-family: monospace;
  margin-left: 8px;
}

.large-alert-text :deep(.ant-alert-message) {
  font-size: 15px !important;
  font-weight: 600;
}

.large-alert-text :deep(.ant-alert-description) {
  font-size: 14px !important;
}

/* 表單元素放大 */
.large-form :deep(.ant-form-item-label > label) {
  font-size: 16px !important;
  font-weight: 600;
}

.large-radio-group :deep(.ant-radio-wrapper),
.large-checkbox-group :deep(.ant-checkbox-wrapper) {
  font-size: 16px !important;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 15px;
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
</style>
