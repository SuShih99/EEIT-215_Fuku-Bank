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

          <a-descriptions-item label="風控原因">
            {{ drawerTask.triggerReason || '—' }}
          </a-descriptions-item>

          <template v-if="drawerMeta">
            <a-descriptions-item label="聯徵分數">
              {{ drawerMeta.externalScore ?? '—' }} 分
            </a-descriptions-item>
            <a-descriptions-item label="職業">
              {{ occupationLabel(drawerMeta.occupation) }}
            </a-descriptions-item>
            <a-descriptions-item label="年收入">
              {{
                drawerMeta.annualIncome != null
                  ? '$ ' + Number(drawerMeta.annualIncome).toLocaleString('zh-TW')
                  : '—'
              }}
            </a-descriptions-item>
            <a-descriptions-item label="他行負債">
              {{
                drawerMeta.otherBankDebt != null
                  ? '$ ' + Number(drawerMeta.otherBankDebt).toLocaleString('zh-TW')
                  : '—'
              }}
            </a-descriptions-item>

            <a-descriptions-item label="計算負債比 (DTI)">
              <span v-if="drawerMeta.annualIncome && drawerMeta.otherBankDebt != null">
                <strong
                  :style="{ color: ((drawerMeta.otherBankDebt / drawerMeta.annualIncome) * 100) > 60 ? '#a61d24' : '#262626' }">
                  {{ ((drawerMeta.otherBankDebt / drawerMeta.annualIncome) * 100).toFixed(2) }} %
                </strong>
              </span>
              <span v-else>—</span>
            </a-descriptions-item>
            <a-descriptions-item label="貸後負債比">
<span v-if="drawerMeta && drawerMeta.annualIncome && drawerMeta.otherBankDebt != null && drawerTask.transactionAmount != null">
  <!-- 貸後負債比 = (他行總負債 + 本次申請金額) ÷ 年收入 × 100% -->
  <strong
    :style="{
      color: ((Number(drawerMeta.otherBankDebt) + Number(drawerTask.transactionAmount)) / drawerMeta.annualIncome * 100) > 100
        ? '#cf1322'
        : ((Number(drawerMeta.otherBankDebt) + Number(drawerTask.transactionAmount)) / drawerMeta.annualIncome * 100) >= 70
          ? '#d46b08'
          : '#333'
    }">
    {{
      ((Number(drawerMeta.otherBankDebt) + Number(drawerTask.transactionAmount)) / drawerMeta.annualIncome * 100).toFixed(1)
    }} %
  </strong>
</span>
              <span v-else>—</span>
            </a-descriptions-item>

            <!-- DBR 22倍法規上限 -->
            <a-descriptions-item label="DBR 可用額度"
                                 v-if="drawerMeta.annualIncome && drawerMeta.otherBankDebt != null && drawerTask.scene === 'LOAN_APPLY'">
              <span v-if="(Math.floor(drawerMeta.annualIncome / 12) * 22 - Number(drawerMeta.otherBankDebt)) > 0">
                <strong style="color: #135200;">
                  $ {{ Math.max(0, Math.floor(drawerMeta.annualIncome / 12) * 22 - Number(drawerMeta.otherBankDebt)).toLocaleString('zh-TW') }}
                </strong>
                <span style="color: #8c8c8c; font-size: 13px; margin-left: 6px;">
                  （月收入 ${{ Math.floor(drawerMeta.annualIncome / 12).toLocaleString('zh-TW') }} × 22倍上限）
                </span>
              </span>
              <strong v-else style="color: #cf1322;">已超過 DBR 22倍上限</strong>
            </a-descriptions-item>

            <a-descriptions-item label="風控合規警示"
                                 v-if="drawerMeta.isPep || drawerMeta.externalScore < 550">
              <div style="color: #cf1322; font-weight: 600;">
                <span v-if="drawerMeta.isPep">⚠️ 觸發 PEP 高風險反洗錢審查流程<br/></span>
                <span v-if="drawerMeta.externalScore < 550">❌ 外部信用分數過低，請從嚴審核</span>
              </div>
            </a-descriptions-item>
            <a-descriptions-item label="有不動產">
              <a-tag :color="drawerMeta.isPep ? 'red' : 'default'" class="table-badge">
                {{ drawerMeta.hasRealEstate ? '是' : '否' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="PEP 人士">
              <a-tag :color="drawerMeta.isPep ? 'red' : 'default'" class="table-badge">
                {{ drawerMeta.isPep ? '是' : '否' }}
              </a-tag>
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
                  @click="openPreview(doc)"
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
          {{ currentTask?.triggerReason || '—' }}
        </a-descriptions-item>

        <a-descriptions-item v-if="currentMeta" label="數據摘要" :span="2">
          <div style="display: flex; flex-wrap: wrap; gap: 6px;">
            <a-tag color="purple" class="table-badge">聯徵: {{
                currentMeta.externalScore
              }}分
            </a-tag>
            <a-tag class="table-badge">{{ occupationLabel(currentMeta.occupation) }}</a-tag>
            <a-tag v-if="currentMeta.annualIncome" color="volcano" class="table-badge">
              現有負債比: {{
                ((currentMeta.otherBankDebt / currentMeta.annualIncome) * 100).toFixed(1)
              }}%
            </a-tag>
            <a-tag v-if="currentMeta.annualIncome && currentTask?.transactionAmount" color="red" class="table-badge">
              貸後負債比: {{
                ((Number(currentMeta.otherBankDebt) + Number(currentTask.transactionAmount)) / currentMeta.annualIncome * 100).toFixed(1)
              }}%
            </a-tag>
            <a-tag v-if="currentMeta.annualIncome && currentTask?.scene === 'LOAN_APPLY'" color="green" class="table-badge">
              DBR剩餘: ${{
                Math.max(0, Math.floor(currentMeta.annualIncome / 12) * 22 - Number(currentMeta.otherBankDebt)).toLocaleString('zh-TW')
              }}
            </a-tag>
          </div>
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
              @click="openPreview(doc)"
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

    <!-- 檔案預覽 Modal -->
    <a-modal
      v-model:open="previewVisible"
      :title="previewTitle"
      :footer="null"
      width="800px"
      style="top: 50px"
    >
      <div style="text-align: center; max-height: 70vh; overflow-y: auto;">
        <img
          v-if="isImageUrl(previewUrl)"
          :src="previewUrl"
          style="max-width: 100%; object-fit: contain;"
          alt="圖片預覽"
        />
        <iframe
          v-else-if="isPdfUrl(previewUrl)"
          :src="previewUrl"
          style="width: 100%; height: 60vh; border: none;"
        ></iframe>
        <div v-else style="padding: 40px 0;">
          <p>此檔案格式不支援線上預覽，請下載後查看：</p>
          <a-button type="primary" :href="previewUrl" download target="_blank">
            下載檔案
          </a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import {ref, reactive, onMounted, computed} from 'vue'
import {message} from 'ant-design-vue'
import {ReloadOutlined} from '@ant-design/icons-vue'
import {useAuthStore} from '@/stores/auth'
import api, {BASE_URL as SERVER_URL} from '@/api/axios'

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

const previewVisible = ref(false)
const previewUrl = ref('')
const previewTitle = ref('')

const openPreview = (doc) => {
  previewUrl.value = getFileUrl(doc.fileUrl)
  previewTitle.value = doc.originalName || doc.documentId || '檔案預覽'
  previewVisible.value = true
}

const isImageUrl = (url) => {
  if (!url) return false
  const ext = url.split('.').pop()?.toLowerCase()
  return ['jpg', 'jpeg', 'png', 'gif', 'webp'].includes(ext) || url.startsWith('data:image')
}

const isPdfUrl = (url) => {
  if (!url) return false
  const ext = url.split('.').pop()?.toLowerCase()
  return ext === 'pdf'
}

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

    const res = await api.get(API_PREFIX, {params})
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
    await api.put(
      `${API_PREFIX}/${currentTask.value.taskId}/decision`,
      payload
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
      await api.put(`${API_PREFIX}/${task.taskId}/start`)
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
  return {HIGH: '高風險', MEDIUM: '中風險', LOW: '低風險', SUSPENDED: '拒絕往來'}[r] || r
}

function riskColor(r) {
  return {HIGH: 'red', MEDIUM: 'orange', LOW: 'green', SUSPENDED: 'purple'}[r] || 'default'
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

// 1. 保留原本嘗試解析 triggerReason 的邏輯（如果它是純文字，會安全地回傳 null）
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
const drawerMeta = computed(() => {
  if (!drawerTask.value?.metaData) return null
  try {
    const parsed = JSON.parse(drawerTask.value.metaData)
    return typeof parsed === 'object' ? parsed : null
  } catch (e) {
    console.error('解析詳情視窗 metaData 失敗:', e)
    return null
  }
})

const currentMeta = computed(() => {
  if (!currentTask.value?.metaData) return null
  try {
    return JSON.parse(currentTask.value.metaData)
  } catch (e) {
    console.error('解析決策視窗 metaData 失敗:', e)
    return null
  }
})

function docLabel(type) {
  return (
    {
      ID_CARD: '身分證',
      INCOME_PROOF: '收入證明',
      INCOME_CERT: '收入證明',
      PROPERTY_CERT: '不動產證明',
      TAX_STATEMENT: '扣繳憑單',
      BANK_STATEMENT: '銀行存摺',
      EMPLOYMENT_CERT: '在職證明',
      TITLE_DEED: '所有權狀',
      OTHER: '其他文件',
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
  gap: 16px; }

/* ── 頁首：標題 + 篩選列 同一列，左右對齊 ── */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 8px;
}

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
  padding: 18px 32px; margin-bottom: 8px;
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
  color: #b57e02;
}

.stat-value.processing {
  color: #054786;
}

.stat-value.completed {
  color: #2d8702;
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
  color: #276339; /* 穩重的深松針綠文字 */
}

.risk-low .status-dot {
  background-color: #2f7a46;
}

/* 中風險：從亮橘色改為「洗鍊古銅烤漆琥珀」 */
.risk-medium {
  background-color: rgba(181, 107, 24, 0.08); /* 偏向木質與皮革的沉穩琥珀 bg */
  color: #91520f; /* 深古銅橘文字 */
}

.risk-medium .status-dot {
  background-color: #b56b18;
}

/* 高風險：從刺眼大紅改為「高級波爾多深磚紅」 */
.risk-high {
  background-color: rgba(166, 29, 36, 0.07); /* 帶有灰色調的暗紅 bg */
  color: #8c1c21; /* 經典金融高風險的深磚紅/酒紅文字 */
}

.risk-high .status-dot {
  background-color: #a61d24;
}

/* 拒絕往來/凍結：從亮紫色改為「午夜深靛紫」 */
.risk-suspended {
  background-color: rgba(83, 29, 171, 0.07); /* 深邃的午夜紫 bg */
  color: #43168c; /* 穩重的深靛紫文字 */
}

.risk-suspended .status-dot {
  background-color: #531dab;
}
</style>
