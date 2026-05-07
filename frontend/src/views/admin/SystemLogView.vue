<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">系統日誌</h2>
    </div>

    <div class="action-bar" style="justify-content: flex-end;">
      <div class="global-actions">
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
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'emp'">
          <div class="emp-name-cell">
            <div class="emp-avatar">{{ record.empName ? record.empName.charAt(0) : '系' }}</div>
            <div class="emp-info">
              <span class="emp-name-text">{{ record.empName || '系統' }}</span>
              <span class="emp-id-text">{{ record.empId || 'SYSTEM' }}</span>
            </div>
          </div>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-tag color="blue">{{ record.action }}</a-tag>
        </template>
        <template v-else-if="column.key === 'actionTime'">
          {{ formatTime(record.actionTime) }}
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { DownloadOutlined } from '@ant-design/icons-vue'
import { getActionLogs, exportLogsCsv, exportLogsPdf } from '@/api/auth'

const logs = ref([])
const loading = ref(false)

const columns = [
  { title: '日誌 ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '操作人員', dataIndex: 'empName', key: 'emp', width: 160 },
  { title: '動作', dataIndex: 'action', key: 'action', width: 120 },
  { title: '目標', dataIndex: 'target', key: 'target', width: 100 },
  { title: '詳情', dataIndex: 'details', key: 'details' },
  { title: '時間', dataIndex: 'actionTime', key: 'actionTime', width: 170 },
  { title: 'IP', dataIndex: 'ipAddress', key: 'ipAddress', width: 130 },
]

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
</style>