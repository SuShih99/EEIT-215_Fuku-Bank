<script setup>
import { ref, onMounted, h, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { DownOutlined, SearchOutlined } from '@ant-design/icons-vue'
import { Bar, Doughnut } from 'vue-chartjs'
import {
  ArcElement,
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  Tooltip,
} from 'chart.js'
import { BASE_URL } from '@/api/axios'
import { useAuthStore } from '@/stores/auth'
import {
  getAccountApplications,
  approveAccountApplication,
  rejectAccountApplication,
  supplementAccountApplication,
} from '@/api/accountApplication'
import dayjs from 'dayjs'

ChartJS.register(ArcElement, BarElement, CategoryScale, LinearScale, Tooltip, Legend)

// === 角色權限判斷 ===
const authStore = useAuthStore()
// 雙重判斷：permLevel 數字 或 roleCode 字串
const APPROVER_ROLES = ['CFDM', 'CSDM', 'CRDM', 'CRO', 'COO', 'CISO', 'ISSA']
const canApprove = computed(() => {
  const level = parseInt(authStore.user?.permLevel ?? 0)
  const code = authStore.user?.roleCode ?? ''
  return level >= 2 || APPROVER_ROLES.includes(code)
})

// === 資料 ===
const applications = ref([])
const loading = ref(false)
const status = ref(null)

const statusOptions = [
  { label: '全部', value: null },
  { label: '審核中', value: 'PENDING' },
  { label: '需補件', value: 'SUPPLEMENT_REQUIRED' },
  { label: '已核准', value: 'APPROVED' },
  { label: '已駁回', value: 'REJECTED' },
  { label: '已取消', value: 'CANCELLED' },
]

const riskFlagMap = {
  NORMAL: { label: '正常', color: 'green' },
  WATCH: { label: '觀察中', color: 'gold' },
  PEP: { label: 'PEP', color: 'orange' },
  HIGH_RISK: { label: '高風險', color: 'red' },
  HIGH_FREQUENCY: { label: '高頻', color: 'red' },
  PEP_HIGH_FREQUENCY: { label: 'PEP+高頻', color: 'red' },
}

const accountTypeMap = {
  CHECKING: '活期帳戶',
  SAVINGS: '儲蓄帳戶',
  TIME_DEPOSIT: '定期存款',
  LOAN: '貸款帳戶',
  SUB_ACCOUNT: '子帳戶',
}

const chartPalette = ['#5C6B5F', '#A65A4D', '#C49A3C', '#78909C', '#8D7B68', '#B7A58E']
const compactChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      position: 'bottom',
      labels: { boxWidth: 10, usePointStyle: true },
    },
  },
}
const barChartOptions = {
  ...compactChartOptions,
  scales: {
    y: {
      beginAtZero: true,
      ticks: { precision: 0 },
      grid: { color: 'rgba(92, 107, 95, 0.09)' },
    },
    x: { grid: { display: false } },
  },
}

const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
})

function countBy(items, getter) {
  return items.reduce((acc, item) => {
    const key = getter(item) || '未分類'
    acc[key] = (acc[key] || 0) + 1
    return acc
  }, {})
}

function buildChartData(source, label) {
  const entries = Object.entries(source)
  return {
    labels: entries.map(([key]) => key),
    datasets: [{
      label,
      data: entries.map(([, value]) => value),
      backgroundColor: entries.map((_, index) => chartPalette[index % chartPalette.length]),
      borderWidth: 0,
      borderRadius: 6,
    }],
  }
}

const totalApplicationsOnPage = computed(() => applications.value.length)
const pendingApplications = computed(() => applications.value.filter(item => item.status === 'PENDING').length)
const approvedApplications = computed(() => applications.value.filter(item => item.status === 'APPROVED').length)
const watchedApplications = computed(() => applications.value.filter(item => item.riskFlag && item.riskFlag !== 'NORMAL').length)
const statusChartData = computed(() => buildChartData(
  countBy(applications.value, item => statusOptions.find(option => option.value === item.status)?.label || item.status),
  '申請狀態',
))
const riskChartData = computed(() => buildChartData(
  countBy(applications.value, item => riskFlagMap[item.riskFlag]?.label || item.riskFlag || '未標記'),
  '風險標記',
))
const accountTypeChartData = computed(() => buildChartData(
  countBy(applications.value, item => accountTypeMap[item.accountType] || item.accountType),
  '帳戶類型',
))

// === 表格欄位 ===
const columns = ref([
  { title: '申請編號', dataIndex: 'applicationNo', key: 'applicationNo', width: 240, resizable: true, sorter: (a, b) => (a.applicationNo || '').localeCompare(b.applicationNo || '') },
  { title: '申請人', dataIndex: 'name', key: 'name', width: 140, resizable: true, sorter: (a, b) => (a.name || '').localeCompare(b.name || '') },
  { title: '帳戶類型', dataIndex: 'accountType', key: 'accountType', width: 110, resizable: true, sorter: (a, b) => (a.accountType || '').localeCompare(b.accountType || '') },
  { title: '身分證字號', dataIndex: 'idNumber', key: 'idNumber', width: 140, resizable: true, sorter: (a, b) => (a.idNumber || '').localeCompare(b.idNumber || '') },
  { title: '風險標記', dataIndex: 'riskFlag', key: 'riskFlag', width: 110, resizable: true, sorter: (a, b) => (a.riskFlag || '').localeCompare(b.riskFlag || '') },
  {
    title: '申請時間',
    dataIndex: 'createdAt',
    key: 'createdAt',
    width: 170,
    resizable: true,
    sorter: (a, b) => (a.createdAt || '').localeCompare(b.createdAt || ''),
  },
  { title: '狀態', dataIndex: 'status', key: 'status', width: 110, resizable: true, sorter: (a, b) => (a.status || '').localeCompare(b.status || '') },
  { title: '操作', key: 'action', width: 150, fixed: 'right' },
])

function handleResizeColumn(w, col) {
  col.width = w
}

// === 展開列 ===
const expandedRowKeys = ref([])

// === 取得資料 ===
const fetchData = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.value.current - 1,
      size: pagination.value.pageSize,
    }
    if (status.value) params.status = status.value

    const data = await getAccountApplications(params)
    applications.value = data.content || []
    pagination.value.total = data.totalElements || 0
  } catch (error) {
    console.error(error)
    message.error('載入申請列表失敗')
  } finally {
    loading.value = false
  }
}

// === 核准 ===
const handleApprove = (record) => {
  Modal.confirm({
    title: '確認核准此開戶申請？',
    content: `將為「${record.name}」建立${accountTypeMap[record.accountType] || record.accountType}帳戶`,
    okText: '確認核准',
    okType: 'primary',
    cancelText: '取消',
    async onOk() {
      try {
        await approveAccountApplication(record.id)
        message.success('核准成功，帳戶已建立')
        await fetchData()
      } catch (error) {
        message.error(error.response?.data?.message || '核准失敗')
      }
    },
  })
}

// === 要求補件 ===
const handleSupplement = (record) => {
  let reason = ''
  Modal.confirm({
    title: '要求補件',
    content: h('div', [
      h('p', { style: 'margin-bottom: 8px; color: #666;' }, `申請人：${record.name}`),
      h('textarea', {
        placeholder: '請輸入需補件原因（必填）',
        rows: 3,
        style: 'width: 100%; border: 1px solid #d9d9d9; border-radius: 6px; padding: 8px; resize: vertical;',
        onInput: (e) => { reason = e.target.value },
      }),
    ]),
    okText: '確認補件',
    cancelText: '取消',
    async onOk() {
      if (!reason.trim()) {
        message.warning('請輸入補件原因')
        throw new Error('empty reason')
      }
      try {
        await supplementAccountApplication(record.id, reason)
        message.success('已通知客戶補件')
        await fetchData()
      } catch (error) {
        if (error.message !== 'empty reason') {
          message.error(error.response?.data?.message || '操作失敗')
        }
        throw error
      }
    },
  })
}

// === 駁回 ===
const handleReject = (record) => {
  let reason = ''
  Modal.confirm({
    title: '駁回開戶申請',
    content: h('div', [
      h('p', { style: 'margin-bottom: 8px; color: #666;' }, `申請人：${record.name}`),
      h('textarea', {
        placeholder: '請輸入駁回原因（必填）',
        rows: 3,
        style: 'width: 100%; border: 1px solid #d9d9d9; border-radius: 6px; padding: 8px; resize: vertical;',
        onInput: (e) => { reason = e.target.value },
      }),
    ]),
    okText: '確認駁回',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      if (!reason.trim()) {
        message.warning('請輸入駁回原因')
        throw new Error('empty reason') // 阻止 modal 關閉
      }
      try {
        await rejectAccountApplication(record.id, reason)
        message.success('已駁回')
        await fetchData()
      } catch (error) {
        if (error.message !== 'empty reason') {
          message.error(error.response?.data?.message || '駁回失敗')
        }
        throw error
      }
    },
  })
}

// === 查看證件圖片 ===
const previewVisible = ref(false)
const previewImage = ref('')
const previewTitle = ref('')

const showImage = (url, title) => {
  previewImage.value = url.startsWith('http') ? url : BASE_URL + url
  previewTitle.value = title
  previewVisible.value = true
}

// === 分頁 ===
const handlePageChange = (page) => {
  pagination.value.current = page.current
  fetchData()
}

// === 格式化 ===
const formatDate = (val) => {
  if (!val) return '-'
  return dayjs(val).format('YYYY-MM-DD HH:mm')
}

onMounted(fetchData)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">開戶申請審核</h2>
    </div>

    <section class="analysis-panel" aria-label="開戶申請圖表分析">
      <div class="metric-card">
        <span>本頁申請</span>
        <strong>{{ totalApplicationsOnPage }}</strong>
        <small>目前查詢結果</small>
      </div>
      <div class="metric-card">
        <span>待審核</span>
        <strong>{{ pendingApplications }}</strong>
        <small>需要主管處理</small>
      </div>
      <div class="metric-card">
        <span>已核准</span>
        <strong>{{ approvedApplications }}</strong>
        <small>完成建帳流程</small>
      </div>
      <div class="metric-card risk">
        <span>風險關注</span>
        <strong>{{ watchedApplications }}</strong>
        <small>非正常風險標記</small>
      </div>
      <div class="chart-card">
        <div class="chart-title">狀態分布</div>
        <div class="chart-body">
          <Doughnut :data="statusChartData" :options="compactChartOptions" />
        </div>
      </div>
      <div class="chart-card">
        <div class="chart-title">風險標記</div>
        <div class="chart-body">
          <Doughnut :data="riskChartData" :options="compactChartOptions" />
        </div>
      </div>
      <div class="chart-card wide">
        <div class="chart-title">帳戶類型</div>
        <div class="chart-body">
          <Bar :data="accountTypeChartData" :options="barChartOptions" />
        </div>
      </div>
    </section>

    <!-- 搜尋列 -->
    <div class="action-bar">
      <div class="search-group">
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
          @click="() => { pagination.current = 1; fetchData() }"
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
      row-key="id"
      class="custom-table"
      :scroll="{ x: 1000 }"
      :expandedRowKeys="expandedRowKeys"
      :locale="{ triggerDesc: '點擊降冪排序', triggerAsc: '點擊升冪排序', cancelSort: '取消排序' }"
      @change="handlePageChange"
      @resizeColumn="handleResizeColumn"
      @expand="(expanded, record) => {
        expandedRowKeys = expanded ? [record.id] : []
      }"
    >
      <template #emptyText>
        <div class="account-application-empty-state">
          <div class="account-application-empty-mark" aria-hidden="true"></div>
          <strong>{{ status ? '目前沒有符合條件的申請' : '目前尚無開戶申請' }}</strong>
          <span>{{ status ? '請切換審核狀態或重新搜尋。' : '客戶送出申請後，會在這裡等待審核。' }}</span>
        </div>
      </template>

      <!-- 欄位自訂 -->
      <template #bodyCell="{ column, record }">

        <!-- 申請人 -->
        <template v-if="column.key === 'name'">
          <div class="emp-name-cell">
            <div class="emp-avatar">{{ record.name ? record.name.charAt(0) : '?' }}</div>
            <div class="emp-info">
              <span class="emp-name-text">{{ record.name }}</span>
              <span class="emp-id-text">{{ record.customerId }}</span>
            </div>
          </div>
        </template>

        <!-- 帳戶類型 -->
        <template v-else-if="column.key === 'accountType'">
          {{ accountTypeMap[record.accountType] || record.accountType }}
          <span v-if="record.currency && record.currency !== 'TWD'" style="color: #8c8c8c; font-size: 12px;">
            ({{ record.currency }})
          </span>
        </template>

        <!-- 風險標記 -->
        <template v-else-if="column.key === 'riskFlag'">
          <a-tag :color="riskFlagMap[record.riskFlag]?.color || 'default'">
            {{ riskFlagMap[record.riskFlag]?.label || record.riskFlag }}
          </a-tag>
        </template>

        <!-- 申請時間 -->
        <template v-else-if="column.key === 'createdAt'">
          {{ formatDate(record.createdAt) }}
        </template>

        <!-- 狀態 -->
        <template v-else-if="column.key === 'status'">
          <div :class="['status-tag', `status-${String(record.status || '').toLowerCase()}`]">
            <span class="status-dot"></span>
            {{ statusOptions.find(o => o.value === record.status)?.label || record.status }}
          </div>
        </template>

        <!-- 操作 -->
        <template v-else-if="column.key === 'action'">
          <div class="action-cell">
            <a-dropdown>
              <a-button type="link" class="action-btn">
                操作 <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu>
                  <!-- 核准：需 permLevel >= 2 (主管) -->
                  <a-menu-item
                    @click="canApprove && handleApprove(record)"
                    :disabled="record.status !== 'PENDING' || !canApprove"
                  >
                    <a-tooltip v-if="!canApprove" title="權限不足：需主管 (CFDM) 以上才能核准">
                      <span style="color:#bbb">核准</span>
                    </a-tooltip>
                    <span v-else>核准</span>
                  </a-menu-item>
                  <!-- 要求補件：需 permLevel >= 2 -->
                  <a-menu-item
                    @click="canApprove && handleSupplement(record)"
                    :disabled="record.status !== 'PENDING' || !canApprove"
                  >
                    <a-tooltip v-if="!canApprove" title="權限不足：需主管 (CFDM) 以上才能操作">
                      <span style="color:#bbb">要求補件</span>
                    </a-tooltip>
                    <span v-else>要求補件</span>
                  </a-menu-item>
                  <!-- 駁回：需 permLevel >= 2 -->
                  <a-menu-item
                    danger
                    @click="canApprove && handleReject(record)"
                    :disabled="record.status !== 'PENDING' || !canApprove"
                  >
                    <a-tooltip v-if="!canApprove" title="權限不足：需主管 (CFDM) 以上才能駁回">
                      <span style="color:#bbb">駁回</span>
                    </a-tooltip>
                    <span v-else>駁回</span>
                  </a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
        </template>
      </template>

      <!-- 展開列：詳細資訊 -->
      <template #expandedRowRender="{ record }">
        <div class="expand-grid">
          <!-- 左側：個人資訊 -->
          <div class="expand-section">
            <h4 class="expand-heading">個人資料</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <span class="detail-label">出生日期</span>
                <span class="detail-value">{{ record.birthday || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">國籍</span>
                <span class="detail-value">{{ record.nationality || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">手機</span>
                <span class="detail-value">{{ record.phone || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">戶籍地址</span>
                <span class="detail-value">{{ record.registeredAddress || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">現居地址</span>
                <span class="detail-value">{{ record.currentAddress || '-' }}</span>
              </div>
            </div>
          </div>

          <!-- 中間：職業 & 法遵 -->
          <div class="expand-section">
            <h4 class="expand-heading">職業 & 法遵</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <span class="detail-label">職業</span>
                <span class="detail-value">{{ record.occupation || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">任職機構</span>
                <span class="detail-value">{{ record.employer || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">月交易量</span>
                <span class="detail-value">{{ record.estimatedMonthlyTx ? record.estimatedMonthlyTx + ' 萬' : '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">開戶目的</span>
                <span class="detail-value">{{ record.accountPurpose || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">資金來源</span>
                <span class="detail-value">{{ record.fundSource || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">稅務居住國</span>
                <span class="detail-value">{{ record.taxResidency || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">PEP 聲明</span>
                <span class="detail-value">
                  <a-tag v-if="record.isPep" color="orange">是</a-tag>
                  <span v-else>否</span>
                </span>
              </div>
            </div>
          </div>

          <!-- 右側：證件圖片 -->
          <div class="expand-section">
            <h4 class="expand-heading">證件圖片</h4>
            <div class="id-card-grid">
              <div class="id-card-thumb" @click="showImage(record.idFrontUrl, '身分證正面')">
                <img :src="BASE_URL + record.idFrontUrl" alt="身分證正面" />
                <span>正面</span>
              </div>
              <div class="id-card-thumb" @click="showImage(record.idBackUrl, '身分證反面')">
                <img :src="BASE_URL + record.idBackUrl" alt="身分證反面" />
                <span>反面</span>
              </div>
              <div class="id-card-thumb" @click="showImage(record.secondIdUrl, '第二證件')">
                <img :src="BASE_URL + record.secondIdUrl" alt="第二證件" />
                <span>第二證件</span>
              </div>
            </div>
          </div>

          <!-- 審核資訊 -->
          <div v-if="record.status !== 'PENDING'" class="expand-section expand-full">
            <h4 class="expand-heading">審核結果</h4>
            <div class="detail-grid">
              <div class="detail-item">
                <span class="detail-label">審核人員</span>
                <span class="detail-value">{{ record.reviewedBy || '-' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">審核時間</span>
                <span class="detail-value">{{ formatDate(record.reviewedAt) }}</span>
              </div>
              <div v-if="record.createdAccountNumber" class="detail-item">
                <span class="detail-label">建立帳號</span>
                <span class="detail-value" style="font-family: monospace; font-weight: 600;">
                  {{ record.createdAccountNumber }}
                </span>
              </div>
              <div v-if="record.rejectReason" class="detail-item">
                <span class="detail-label">駁回原因</span>
                <span class="detail-value" style="color: #d9363e;">{{ record.rejectReason }}</span>
              </div>
            </div>
          </div>
        </div>
      </template>
    </a-table>

    <!-- 圖片預覽 Modal -->
    <a-modal
      v-model:open="previewVisible"
      :title="previewTitle"
      :footer="null"
      width="640px"
    >
      <img :src="previewImage" :alt="previewTitle" style="width: 100%; border-radius: 8px;" />
    </a-modal>
  </div>
</template>

<style scoped>
.analysis-panel {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr));
  gap: 14px;
  margin-bottom: 20px;
}

.metric-card,
.chart-card {
  border: 1px solid rgba(214, 206, 195, 0.72);
  border-radius: 8px;
  background:
    linear-gradient(180deg, rgba(255, 249, 239, 0.92), rgba(249, 244, 235, 0.82)),
    url('/washi-texture.png');
  background-size: auto, 260px 260px;
  box-shadow: 0 10px 28px rgba(63, 74, 66, 0.08);
}

.metric-card {
  min-height: 104px;
  padding: 16px;
  display: grid;
  gap: 6px;
}

.metric-card span,
.chart-title {
  color: #5C6B5F;
  font-size: 13px;
  font-weight: 700;
}

.metric-card strong {
  color: #2B2B2B;
  font-size: 30px;
  line-height: 1;
}

.metric-card small {
  color: #8c8c8c;
  font-size: 12px;
}

.metric-card.risk strong {
  color: #A65A4D;
}

.chart-card {
  min-height: 260px;
  padding: 16px;
}

.chart-card.wide {
  grid-column: span 2;
}

.chart-body {
  position: relative;
  height: 205px;
  margin-top: 10px;
}

.chart-body :deep(canvas) {
  width: 100% !important;
  height: 100% !important;
}

/* 人名 cell */
.emp-name-cell { display: flex; align-items: center; gap: 12px; }
.emp-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  background-color: rgba(92, 107, 95, 0.1); color: #5C6B5F;
  display: flex; align-items: center; justify-content: center;
  font-weight: 700; font-size: 14px;
}
.emp-info { display: flex; flex-direction: column; }
.emp-name-text { font-weight: 600; color: #2B2B2B; font-size: 14px; }
.emp-id-text { font-size: 11px; color: #8c8c8c; margin-top: 2px; }

/* 狀態標籤 */
.status-tag {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 4px 10px; border-radius: 999px; font-size: 12px; font-weight: 600;
}
.status-dot { width: 6px; height: 6px; border-radius: 50%; }
.status-approved { background-color: rgba(74, 140, 92, 0.10); color: #1a7a40; }
.status-approved .status-dot { background-color: #4A8C5C; }
.status-rejected { background-color: rgba(166, 90, 77, 0.10); color: #A65A4D; }
.status-rejected .status-dot { background-color: #A65A4D; }
.status-pending { background-color: rgba(196, 154, 60, 0.10); color: #7a6000; }
.status-pending .status-dot { background-color: #C49A3C; }
.status-supplement_required { background-color: rgba(92, 107, 95, 0.08); color: #4A574D; }
.status-supplement_required .status-dot { background-color: #5C6B5F; }
.status-cancelled { background-color: rgba(234, 228, 218, 0.62); color: #8c8c8c; border: 1px solid #d9d9d9; }
.status-cancelled .status-dot { background-color: #bfbfbf; }

/* 展開列 */
.expand-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 24px;
  padding: 16px 8px;
}

.expand-full {
  grid-column: 1 / -1;
}

.expand-section {
  background: rgba(255, 249, 239, 0.78);
  border: 1px solid rgba(214, 206, 195, 0.78);
  border-radius: 14px;
  padding: 16px;
}

.expand-heading {
  font-size: 13px;
  font-weight: 600;
  color: #5C6B5F;
  margin-bottom: 12px;
  letter-spacing: 1px;
}

.detail-grid {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.detail-label {
  font-size: 12px;
  color: #8c8c8c;
  flex-shrink: 0;
  min-width: 72px;
}

.detail-value {
  font-size: 13px;
  color: #2B2B2B;
  text-align: right;
  word-break: break-all;
}

/* 證件圖片 */
.id-card-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 8px;
}

.id-card-thumb {
  cursor: pointer;
  border: 1px solid rgba(214, 206, 195, 0.72);
  border-radius: 10px;
  overflow: hidden;
  text-align: center;
  transition: border-color 0.2s;
}

.id-card-thumb:hover {
  border-color: #5C6B5F;
}

.id-card-thumb img {
  width: 100%;
  aspect-ratio: 3 / 2;
  object-fit: cover;
  display: block;
}

.id-card-thumb span {
  display: block;
  font-size: 11px;
  color: #8c8c8c;
  padding: 4px 0;
}

.account-application-empty-state {
  display: flex;
  min-height: 220px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #6f6a60;
  text-align: center;
  background: linear-gradient(145deg, rgba(255, 249, 239, 0.82), rgba(246, 239, 227, 0.74));
}

.account-application-empty-state strong {
  color: #2f312b;
  font-size: 18px;
  font-weight: 700;
}

.account-application-empty-state span {
  color: #8a8477;
  font-size: 13px;
}

.account-application-empty-mark {
  position: relative;
  width: 68px;
  height: 68px;
  border-radius: 22px;
  background: linear-gradient(145deg, rgba(255, 249, 239, 0.96), rgba(242, 233, 216, 0.92));
  border: 1px solid rgba(214, 206, 195, 0.82);
  box-shadow: 0 10px 26px rgba(63, 74, 66, 0.06);
}

.account-application-empty-mark::before,
.account-application-empty-mark::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  border-radius: 999px;
  background: rgba(111, 102, 85, 0.3);
}

.account-application-empty-mark::before {
  top: 18px;
  width: 26px;
  height: 3px;
}

.account-application-empty-mark::after {
  top: 30px;
  width: 18px;
  height: 3px;
  box-shadow: 0 10px 0 rgba(111, 102, 85, 0.3);
}

@media (max-width: 980px) {
  .analysis-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .analysis-panel,
  .chart-card.wide {
    grid-template-columns: 1fr;
    grid-column: auto;
  }
}
</style>
