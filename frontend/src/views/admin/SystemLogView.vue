<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">系統日誌</h2>
    </div>

    <div class="action-bar" style="justify-content: flex-end;">
      <!-- 僅 CISO (Lvl4+) 才能匯出日誌 -->
      <div class="global-actions" v-if="isCISO">
        <a-button class="rounded-btn btn-ghost" @click="handleExportCsv">
          <template #icon><DownloadOutlined /></template>
          匯出 CSV
        </a-button>
        <a-button type="primary" class="rounded-btn" @click="handleExportPdf">
          <template #icon><DownloadOutlined /></template>
          匯出 PDF
        </a-button>
      </div>
    </div>

    <a-table
      :columns="columns"
      :data-source="logs"
      :loading="loading"
      row-key="id"
      class="custom-table"
      :scroll="{ x: 1000 }"
      :pagination="{ pageSize: 15, showSizeChanger: false }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'emp'">
          <div class="emp-name-cell">
            <div class="emp-avatar">{{ (record.empName && record.empName !== '???') ? record.empName.charAt(0) : '系' }}</div>
            <div class="emp-info">
              <a-popconfirm
                v-if="canSuspendFromLog(record)"
                title="確定要停用此員工帳號？"
                ok-text="停用"
                cancel-text="取消"
                @confirm="handleSuspendFromLog(record)"
              >
                <button class="emp-name-link">
                  {{ (record.empName && record.empName !== '???') ? record.empName : '系統' }}
                </button>
              </a-popconfirm>
              <span v-else class="emp-name-text">{{ (record.empName && record.empName !== '???') ? record.empName : '系統' }}</span>
              <span class="emp-id-text">{{ (record.empId && record.empId !== '???') ? record.empId : 'SYSTEM' }}</span>
            </div>
          </div>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-tag class="action-tag" :color="actionColor(record.action)">{{ record.action }}</a-tag>
        </template>
        <template v-else-if="column.key === 'target'">
          <span class="mono-text">{{ record.target || '-' }}</span>
        </template>
        <template v-else-if="column.key === 'details'">
          <a-tooltip :title="record.details">
            <span class="details-cell">{{ (record.details && record.details !== '???') ? record.details : '-' }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.key === 'ipAddress'">
          <span class="mono-text">{{ record.ipAddress || '-' }}</span>
        </template>
        <template v-else-if="column.key === 'actionTime'">
          <span class="time-text">{{ formatTime(record.actionTime) }}</span>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import { getActionLogs, exportLogsCsv, exportLogsPdf, suspendEmployee } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
// 只有 permLevel >= 4 的 CISO 才能匯出
const permLevel = computed(() => authStore.user?.permLevel ?? 0)
const isCISO = computed(() => permLevel.value >= 4)
const currentEmpId = computed(() => authStore.user?.empId)

const logs = ref([])
const loading = ref(false)

const columns = [
  { title: 'ID',    dataIndex: 'id',         key: 'id',         width: 60 },
  { title: '操作人員', dataIndex: 'empName',  key: 'emp',        width: 160 },
  { title: '動作',  dataIndex: 'action',     key: 'action',     width: 150 },
  { title: '目標',  dataIndex: 'target',     key: 'target',     width: 120 },
  {
    title: '詳情',
    dataIndex: 'details',
    key: 'details',
    width: 400,
    ellipsis: true,
  },
  { title: '時間',  dataIndex: 'actionTime', key: 'actionTime', width: 170 },
  { title: 'IP',    dataIndex: 'ipAddress',  key: 'ipAddress',  width: 120 },
]

const actionColorMap = {
  LOGIN:      'green',
  LOGOUT:     'default',
  CREATE_EMP: 'blue',
  UPDATE_EMP: 'orange',
  SUSPEND_EMP: 'red',
  FAILED_LOGIN: 'red',
  UNAUTHORIZED_UPDATE_ACCOUNT_STATUS: 'volcano',
  UNAUTHORIZED_DEACTIVATE_CUSTOMER: 'volcano',
  DEACTIVATE_CUSTOMER: 'red',
  ACTIVATE_CUSTOMER: 'green',
  UPLOAD_CUSTOMER_DOCUMENT: 'blue',
  SEED_CUSTOMER_DATA: 'purple',
  SEED_DATA:  'purple',
  CREATE_CUSTOMER: 'cyan',
  UPDATE_CUSTOMER: 'geekblue',
  EXPORT_CUSTOMER: 'blue',
  REVIEW_CREDIT: 'gold',
  READ_CONFIG: 'default',
}

function actionColor(action) {
  if (String(action || '').startsWith('UNAUTHORIZED_')) return 'volcano'
  if (String(action || '').startsWith('FAILED_')) return 'red'
  return actionColorMap[action] || 'default'
}

function canSuspendFromLog(record) {
  return isCISO.value
    && record.empId
    && record.empId !== 'SYSTEM'
    && record.empId !== 'UNKNOWN'
    && record.empId !== currentEmpId.value
}

async function handleSuspendFromLog(record) {
  try {
    await suspendEmployee(record.empId)
    message.success(`員工「${record.empName}」已停用`)
    await fetchData()
  } catch (err) {
    message.error(err.response?.data?.message || '停用員工失敗')
  }
}

function formatTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ').substring(0, 19)
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getActionLogs()
    logs.value = res.data.data
  } catch (err) {
    message.error('載入日誌失敗')
  } finally {
    loading.value = false
  }
}

async function handleExportCsv() {
  try {
    const res = await exportLogsCsv()
    downloadFile(res.data, 'system_logs.csv')
  } catch (err) {
    message.error('匯出 CSV 失敗')
  }
}

async function handleExportPdf() {
  try {
    const res = await exportLogsPdf()
    downloadFile(res.data, 'system_logs.pdf')
  } catch (err) {
    message.error('匯出 PDF 失敗')
  }
}

function downloadFile(data, filename) {
  const url = window.URL.createObjectURL(new Blob([data]))
  const link = document.createElement('a')
  link.href = url
  link.setAttribute('download', filename)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.emp-name-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.emp-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background-color: rgba(92, 107, 95, 0.1);
  color: #5C6B5F;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 13px;
  flex-shrink: 0;
}

.emp-info {
  display: flex;
  flex-direction: column;
}

.emp-name-text {
  font-weight: 600;
  color: #1a1a2e;
  font-size: 13px;
}

.emp-name-link {
  border: 0;
  padding: 0;
  background: transparent;
  color: #5C6B5F;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.emp-name-link:hover {
  text-decoration: underline;
}

.emp-id-text {
  font-size: 11px;
  color: #8c8c8c;
  margin-top: 1px;
}

.action-tag {
  max-width: 128px;
  white-space: normal;
  overflow-wrap: anywhere;
  word-break: break-word;
  line-height: 1.35;
  padding: 3px 7px;
}

/* 詳情欄位：截斷 + hover 顯示全文 */
.details-cell {
  display: block;
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #444;
  cursor: default;
}

/* 等寬字體：IP / 目標 */
.mono-text {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 12px;
  color: #555;
}

/* 時間格式 */
.time-text {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
}
</style>
