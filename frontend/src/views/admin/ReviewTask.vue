<template>
  <div class="page-container">
    <!-- 頁首：標題 + 篩選列同一行 -->
    <div class="page-header">
      <h2 class="page-title">審核任務</h2>
      <a-space class="filter-bar" wrap>
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
          <a-select-option value="TRANSFER_REVIEW">轉帳審核</a-select-option>
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

    <!-- 統計卡片：獨立一排，帶底色區分 -->
    <div class="stats-row">
      <div class="stat-card">
        <span class="stat-label">待處理</span>
        <span class="stat-value pending">{{ stats.pending }}</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-card">
        <span class="stat-label">處理中</span>
        <span class="stat-value processing">{{ stats.processing }}</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-card">
        <span class="stat-label">已結案</span>
        <span class="stat-value completed">{{ stats.completed }}</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-card">
        <span class="stat-label">總計</span>
        <span class="stat-value total">{{
          stats.pending + stats.processing + stats.completed
        }}</span>
      </div>
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
          <a-badge :status="statusBadge(record)" :text="statusLabel(record)" />
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
            @click.stop="handleReviewAction(record)"
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
            <a-badge :status="statusBadge(drawerTask)" :text="statusLabel(drawerTask)" />
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
              <a-tag :color="drawerTrigger.isPep ? 'red' : 'default'">
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

        <div v-if="drawerTask.attachments" style="margin-top: 20px">
          <h3
            style="
              font-size: 14px;
              font-weight: 600;
              color: rgba(0, 0, 0, 0.85);
              margin-bottom: 12px;
            "
          >
            客戶補件附件
          </h3>
          <div class="attachments-container">
            <a-card
              v-for="doc in parseAttachments(drawerTask.attachments)"
              :key="doc.documentId"
              size="small"
              style="margin-bottom: 8px; border-radius: 6px"
            >
              <div style="display: flex; align-items: center; justify-content: space-between">
                <div>
                  <a-tag color="blue">{{ docLabel(doc.documentType) }}</a-tag>
                  <span style="font-size: 12px; color: #8c8c8c; font-family: monospace">{{
                    doc.documentId
                  }}</span>
                </div>

                <a-button
                  type="link"
                  size="small"
                  :href="getFileUrl(doc.fileUrl)"
                  target="_blank"
                  v-if="doc.fileUrl"
                >
                  查看文件
                </a-button>
                <span v-else style="color: #bfbfbf; font-size: 12px">無連結</span>
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
                    max-height: 150px;
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
      cancel-text="取消"
      :confirm-loading="submitting"
      @cancel="resetForm"
    >
      <template #footer>
        <a-button @click="handleCancel">取消</a-button>
        <a-tooltip
          :title="
            currentTask?.assignee !== currentUser?.email
              ? '此任務由 ' + currentTask?.assignee + ' 鎖定，您無法送出決策'
              : ''
          "
        >
          <a-button
            type="primary"
            :loading="submitting"
            :disabled="currentTask?.assignee !== currentUser?.email"
            @click="submitDecision"
          >
            送出決策
          </a-button>
        </a-tooltip>
      </template>
      <a-descriptions :column="2" bordered size="small" style="margin-bottom: 16px">
        <a-descriptions-item label="任務 ID">#{{ currentTask?.taskId }}</a-descriptions-item>
        <a-descriptions-item label="業務編號">{{ currentTask?.businessId }}</a-descriptions-item>
        <a-descriptions-item label="場景">{{ sceneLabel(currentTask?.scene) }}</a-descriptions-item>
        <a-descriptions-item label="風險等級">
          <a-tag :color="riskColor(currentTask?.riskLevel)">{{
            riskLabel(currentTask?.riskLevel)
          }}</a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="風控原因" :span="2">
          <template v-if="currentTrigger">
            <a-tag>分數 {{ currentTrigger.finalScore }}</a-tag>
            <a-tag :color="riskColor(currentTrigger.riskLevel)">{{
              riskLabel(currentTrigger.riskLevel)
            }}</a-tag>
            <a-tag>{{ occupationLabel(currentTrigger.occupation) }}</a-tag>
          </template>
          <span v-else>{{ currentTask?.triggerReason || '—' }}</span>
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
import { useAuthStore } from '@/stores/auth'
import { BASE_URL as SERVER_URL } from '@/api/axios'
import axios from 'axios'

const API_PREFIX = '/api/risk/reviewtask'

const tasks = ref([])
const loading = ref(false)
const submitting = ref(false)
const modalVisible = ref(false)
const drawerVisible = ref(false)
const currentTask = ref(null)
const drawerTask = ref(null)

const authStore = useAuthStore()
const currentUser = computed(() => authStore.user)

const filters = reactive({ status: undefined, scene: undefined, priority: undefined })
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50', '100'],
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

    const res = await axios.get(API_PREFIX, { params, withCredentials: true })
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
      `${API_PREFIX}/${currentTask.value.taskId}/decision`,
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

const handleCancel = () => {
  resetForm()
  modalVisible.value = false // 如果 modalVisible 是 ref，記得加 .value
}

function onRowClick(record) {
  console.log('onRowClick', record)
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

async function handleReviewAction(task) {
  // 若已在處理中（自己鎖的）則直接開 Modal
  if (task.status !== 'PROCESSING') {
    try {
      await axios.put(`${API_PREFIX}/${task.taskId}/start`, {}, { withCredentials: true })
      await fetchTasks()
      // 取得最新 task 物件
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
  return { PROCESSING: '處理中', COMPLETED: '已結案' }[record.status] || record.status
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
  return { PROCESSING: 'processing', COMPLETED: 'success' }[record.status] || 'default'
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
  return { HIGH: '高風險', MEDIUM: '中風險', LOW: '低風險' }[r] || r
}
function riskColor(r) {
  return { HIGH: 'red', MEDIUM: 'orange', LOW: 'green' }[r] || 'default'
}
function priorityColor(p) {
  return { 1: 'red', 5: 'orange', 10: 'green' }[p] || 'default'
}

onMounted(fetchTasks)

// 解析資料庫存進來的 attachments JSON 字串
function parseAttachments(attachmentsStr) {
  if (!attachmentsStr) return []
  try {
    return JSON.parse(attachmentsStr)
  } catch (e) {
    console.error('解析補件資料失敗:', e)
    return []
  }
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
// 證件中文標籤對照表
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

// 處理檔案與圖片網址
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

// 判斷是否為圖片，用來決定要不要秀預覽圖
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
  gap: 0;
}

/* ── 頁首：標題 + 篩選列 同一列，左右對齊 ── */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1a1a2e;
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
  padding: 16px 32px;
  margin-bottom: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.stat-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.stat-divider {
  width: 1px;
  height: 36px;
  background: #f0f0f0;
  margin: 0 8px;
}

.stat-label {
  font-size: 13px;
  color: #8c8c8c;
  white-space: nowrap;
}

.stat-value {
  font-size: 28px;
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

/* ── 表格 ── */
:deep(.clickable-row) {
  cursor: pointer;
}
:deep(.clickable-row:hover td) {
  background: #fafafa;
}
</style>
