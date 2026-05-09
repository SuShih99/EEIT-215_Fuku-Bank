<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">客戶管理</h2>
    </div>

    <div class="action-bar">
      <div class="search-group">
        <a-input
          v-model:value="keyword"
          placeholder="搜尋客戶姓名或身分證..."
          class="rounded-input search-input"
          allow-clear
          @press-enter="handleSearch"
        >
          <template #prefix><SearchOutlined style="color: #bfbfbf" /></template>
        </a-input>
        <a-button type="primary" class="rounded-btn" @click="handleSearch">查詢</a-button>
        <a-button class="rounded-btn btn-ghost" @click="handleClear">清除</a-button>
        <a-button type="primary" class="rounded-btn" @click="goCreate">
          新增客戶
        </a-button>
      </div>
    </div>

    <a-table
      :columns="columns"
      :data-source="customers"
      :loading="loading"
      :scroll="{ x: 1720 }"
      row-key="customerId"
      class="custom-table"
      :pagination="{ pageSize: 10, showSizeChanger: false }"
      :locale="{ triggerDesc: '點擊降冪排序', triggerAsc: '點擊升冪排序', cancelSort: '取消排序' }"
      @resizeColumn="handleResizeColumn"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <div class="emp-name-cell">
            <div class="emp-avatar">{{ firstChar(record.name) }}</div>
            <div class="emp-info">
              <span class="emp-name-text">{{ displayValue(record.name) }}</span>
              <span class="emp-id-text">{{ displayValue(record.customerId) }}</span>
            </div>
          </div>
        </template>
        <template v-else-if="column.key === 'gender'">
          {{ genderMap[record.gender] || displayValue(record.gender) }}
        </template>
        <template v-else-if="column.key === 'currentAddress'">
          <a-tooltip :title="record.currentAddress || record.address || '-'">
            <span class="truncate-cell">{{ record.currentAddress || record.address || '-' }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.key === 'occupationInfo'">
          <div class="stack-cell">
            <span class="primary-text">{{ displayValue(record.occupation) }}</span>
            <span class="secondary-text">{{ displayValue(record.employer) }}</span>
          </div>
        </template>
        <template v-else-if="column.key === 'compliance'">
          <div class="tag-stack">
            <a-tag :color="record.isPep ? 'red' : 'green'">
              PEP {{ record.isPep ? '是' : '否' }}
            </a-tag>
            <span class="secondary-text">{{ displayValue(record.taxResidency) }}</span>
          </div>
        </template>
        <template v-else-if="column.key === 'latestApplication'">
          <div class="stack-cell">
            <span :class="['application-status', applicationStatusClass(record.latestAccountApplicationStatus)]">
              {{ applicationStatusMap[record.latestAccountApplicationStatus] || displayValue(record.latestAccountApplicationStatus) }}
            </span>
            <span class="secondary-text">{{ displayValue(record.latestAccountApplicationNo) }}</span>
          </div>
        </template>
        <template v-else-if="column.key === 'status'">
          <div :class="['status-tag', `status-${String(record.status || '').toLowerCase()}`]">
            <span class="status-dot"></span>
            {{ statusMap[record.status] || displayValue(record.status) }}
          </div>
        </template>
        <template v-else-if="column.key === 'action'">
          <div class="action-cell">
            <a-button type="link" class="action-btn edit-btn" @click="openEditModal(record)">
              編輯
            </a-button>
            <a-divider type="vertical" />
            <template v-if="record.status === 'INACTIVE' || record.status === 'DEACTIVATED'">
              <a-button
                type="link"
                class="action-btn resume-btn"
                @click="handleActivate(record)"
              >
                啟用
              </a-button>
            </template>
            <template v-else>
              <a-button
                type="link"
                class="action-btn suspend-btn"
                @click="handleDeactivate(record)"
              >
                停用
              </a-button>
            </template>
          </div>
        </template>
      </template>

      <template #expandedRowRender="{ record }">
        <div class="customer-detail-grid">
          <section class="detail-section">
            <h4>基本與地址</h4>
            <dl>
              <div>
                <dt>國籍</dt>
                <dd>{{ displayValue(record.nationality) }}</dd>
              </div>
              <div>
                <dt>生日</dt>
                <dd>{{ displayValue(record.birthday) }}</dd>
              </div>
              <div>
                <dt>Email</dt>
                <dd>{{ displayValue(record.email) }}</dd>
              </div>
              <div>
                <dt>戶籍地址</dt>
                <dd>{{ displayValue(record.registeredAddress) }}</dd>
              </div>
              <div>
                <dt>現居地址</dt>
                <dd>{{ displayValue(record.currentAddress || record.address) }}</dd>
              </div>
            </dl>
          </section>

          <section class="detail-section">
            <h4>職業與法遵</h4>
            <dl>
              <div>
                <dt>職業</dt>
                <dd>{{ displayValue(record.occupation) }}</dd>
              </div>
              <div>
                <dt>任職機構</dt>
                <dd>{{ displayValue(record.employer) }}</dd>
              </div>
              <div>
                <dt>預估月交易量</dt>
                <dd>{{ formatMonthlyTx(record.estimatedMonthlyTx) }}</dd>
              </div>
              <div>
                <dt>開戶目的</dt>
                <dd>{{ accountPurposeMap[record.accountPurpose] || displayValue(record.accountPurpose) }}</dd>
              </div>
              <div>
                <dt>資金來源</dt>
                <dd>{{ fundSourceMap[record.fundSource] || displayValue(record.fundSource) }}</dd>
              </div>
              <div>
                <dt>稅務居民</dt>
                <dd>{{ displayValue(record.taxResidency) }}</dd>
              </div>
            </dl>
          </section>

          <section class="detail-section">
            <h4>最近開戶申請</h4>
            <dl>
              <div>
                <dt>申請編號</dt>
                <dd>{{ displayValue(record.latestAccountApplicationNo) }}</dd>
              </div>
              <div>
                <dt>申請狀態</dt>
                <dd>{{ applicationStatusMap[record.latestAccountApplicationStatus] || displayValue(record.latestAccountApplicationStatus) }}</dd>
              </div>
              <div>
                <dt>帳戶類型</dt>
                <dd>{{ accountTypeMap[record.latestAppliedAccountType] || displayValue(record.latestAppliedAccountType) }}</dd>
              </div>
              <div>
                <dt>幣別</dt>
                <dd>{{ displayValue(record.latestAppliedCurrency) }}</dd>
              </div>
              <div>
                <dt>風險標記</dt>
                <dd>{{ riskFlagMap[record.latestAccountApplicationRiskFlag] || displayValue(record.latestAccountApplicationRiskFlag) }}</dd>
              </div>
              <div>
                <dt>審核人員</dt>
                <dd>{{ displayValue(record.latestAccountApplicationReviewedBy) }}</dd>
              </div>
              <div>
                <dt>審核時間</dt>
                <dd>{{ displayValue(record.latestAccountApplicationReviewedAt) }}</dd>
              </div>
              <div>
                <dt>駁回/補件原因</dt>
                <dd>{{ displayValue(record.latestAccountApplicationRejectReason) }}</dd>
              </div>
              <div>
                <dt>建立帳號</dt>
                <dd>{{ displayValue(record.createdAccountNumber) }}</dd>
              </div>
              <div>
                <dt>同步時間</dt>
                <dd>{{ displayValue(record.accountApplicationSyncedAt) }}</dd>
              </div>
            </dl>
          </section>
        </div>
      </template>
    </a-table>

    <!-- 編輯 Modal（僅修改聯絡資訊）-->
    <a-modal
      v-model:open="showEditModal"
      title="編輯客戶資料"
      @ok="handleSubmitEdit"
      :confirm-loading="submitLoading"
      @cancel="resetEditForm"
      ok-text="儲存變更"
      cancel-text="取消"
    >
      <a-form layout="vertical">
        <a-form-item label="身分證字號">
          <a-input :value="editForm.idNumber" disabled />
        </a-form-item>
        <a-form-item label="姓名">
          <a-input v-model:value="editForm.name" placeholder="請輸入姓名" />
        </a-form-item>
        <a-form-item label="生日">
          <a-date-picker
            v-model:value="editForm.birthday"
            style="width: 100%"
            value-format="YYYY-MM-DD"
            placeholder="請選擇生日"
          />
        </a-form-item>
        <a-form-item label="性別">
          <a-select v-model:value="editForm.gender" placeholder="請選擇性別">
            <a-select-option value="M">男</a-select-option>
            <a-select-option value="F">女</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="Email">
          <a-input v-model:value="editForm.email" placeholder="請輸入 Email" />
        </a-form-item>
        <a-form-item label="電話">
          <a-input v-model:value="editForm.phone" placeholder="請輸入電話" />
        </a-form-item>
        <a-form-item label="地址">
          <a-input v-model:value="editForm.address" placeholder="請輸入地址" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import {
  getCustomers,
  updateCustomer,
  deactivateCustomer,
  activateCustomer,
} from '@/api/customer'

const router = useRouter()

const statusMap = {
  ACTIVE: '正常',
  DEACTIVATED: '已停用',
  PENDING: '待審核',
  FROZEN: '凍結',
  INACTIVE: '停用',
}

const genderMap = {
  M: '男',
  F: '女',
}

const applicationStatusMap = {
  PENDING: '待審核',
  SUPPLEMENT_REQUIRED: '需補件',
  APPROVED: '已核准',
  REJECTED: '已駁回',
  CANCELLED: '已取消',
}

const accountTypeMap = {
  CHECKING: '活期帳戶',
  SAVINGS: '儲蓄帳戶',
  TIME_DEPOSIT: '定期存款',
  LOAN: '貸款帳戶',
  SUB_ACCOUNT: '子帳戶',
}

const accountPurposeMap = {
  SALARY: '薪轉',
  SAVINGS: '儲蓄',
  INVESTMENT: '投資',
  PAYMENT: '支付',
  DAILY_EXPENSE: '日常支出',
  BUSINESS: '商業用途',
  FOREIGN_EXCHANGE: '外匯',
  LOAN: '貸款',
  OTHER: '其他',
}

const fundSourceMap = {
  SALARY: '薪資',
  BUSINESS_INCOME: '營業收入',
  INVESTMENT: '投資收益',
  INHERITANCE: '繼承',
  RETIREMENT: '退休金',
  SAVINGS: '存款',
  OTHER: '其他',
}

const riskFlagMap = {
  NORMAL: '一般',
  WATCH: '觀察',
  PEP: 'PEP',
  HIGH_RISK: '高風險',
  HIGH_FREQUENCY: '高頻申請',
  PEP_HIGH_FREQUENCY: 'PEP + 高頻',
}

const keyword = ref('')
const customers = ref([])
const loading = ref(false)

const columns = ref([
  { title: '客戶資訊', dataIndex: 'name', key: 'name', width: 190, fixed: 'left', resizable: true, sorter: (a, b) => (a.name || '').localeCompare(b.name || '') },
  { title: 'CIF', dataIndex: 'cif', key: 'cif', width: 125, resizable: true, sorter: (a, b) => (a.cif || '').localeCompare(b.cif || '') },
  { title: '身分證字號', dataIndex: 'idNumber', key: 'idNumber', width: 130, resizable: true, sorter: (a, b) => (a.idNumber || '').localeCompare(b.idNumber || '') },
  { title: '國籍', dataIndex: 'nationality', key: 'nationality', width: 75, resizable: true, sorter: (a, b) => (a.nationality || '').localeCompare(b.nationality || '') },
  { title: '性別', dataIndex: 'gender', key: 'gender', width: 65, resizable: true, sorter: (a, b) => (a.gender || '').localeCompare(b.gender || '') },
  { title: '電話', dataIndex: 'phone', key: 'phone', width: 130, resizable: true, sorter: (a, b) => (a.phone || '').localeCompare(b.phone || '') },
  { title: '現居地址', dataIndex: 'currentAddress', key: 'currentAddress', width: 230, resizable: true, sorter: (a, b) => (a.currentAddress || a.address || '').localeCompare(b.currentAddress || b.address || '') },
  { title: '職業/任職機構', key: 'occupationInfo', width: 180, resizable: true, sorter: (a, b) => (a.occupation || '').localeCompare(b.occupation || '') },
  { title: '法遵', key: 'compliance', width: 115, resizable: true, sorter: (a, b) => Number(b.isPep || 0) - Number(a.isPep || 0) },
  { title: '最近開戶申請', key: 'latestApplication', width: 170, resizable: true, sorter: (a, b) => (a.latestAccountApplicationStatus || '').localeCompare(b.latestAccountApplicationStatus || '') },
  { title: '建立帳號', dataIndex: 'createdAccountNumber', key: 'createdAccountNumber', width: 130, resizable: true, sorter: (a, b) => (a.createdAccountNumber || '').localeCompare(b.createdAccountNumber || '') },
  { title: '顧客狀態', dataIndex: 'status', key: 'status', width: 105, resizable: true, sorter: (a, b) => (a.status || '').localeCompare(b.status || '') },
  { title: '操作', key: 'action', width: 140, fixed: 'right' },
])

function handleResizeColumn(w, col) {
  col.width = w
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getCustomers(keyword.value || undefined)
    customers.value = res.data.data
  } catch (err) {
    message.error(err.response?.data?.message || '查詢失敗')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  fetchData()
}

function handleClear() {
  keyword.value = ''
  customers.value = []
}

function goCreate() {
  router.push({ name: 'admin-customers-create' })
}

function displayValue(value) {
  return value === null || value === undefined || value === '' ? '-' : value
}

function firstChar(value) {
  return value ? String(value).charAt(0) : '?'
}

function formatMonthlyTx(value) {
  if (value === null || value === undefined || value === '') return '-'
  return `${value} 萬元`
}

function applicationStatusClass(status) {
  return String(status || 'none').toLowerCase().replace('_', '-')
}

// ===========================
// 編輯 Modal
// ===========================
const showEditModal = ref(false)
const submitLoading = ref(false)
const editingCustomerId = ref('')

const editForm = reactive({
  idNumber: '',
  name: '',
  birthday: null,
  gender: undefined,
  email: '',
  phone: '',
  address: '',
})

function resetEditForm() {
  editForm.idNumber = ''
  editForm.name = ''
  editForm.birthday = null
  editForm.gender = undefined
  editForm.email = ''
  editForm.phone = ''
  editForm.address = ''
  editingCustomerId.value = ''
}

function openEditModal(record) {
  editingCustomerId.value = record.customerId
  editForm.idNumber = record.idNumber
  editForm.name = record.name
  editForm.birthday = record.birthday || null
  editForm.gender = record.gender
  editForm.email = record.email
  editForm.phone = record.phone
  editForm.address = record.address
  showEditModal.value = true
}

async function handleSubmitEdit() {
  submitLoading.value = true
  try {
    await updateCustomer(editingCustomerId.value, {
      idNumber: editForm.idNumber,
      name: editForm.name,
      birthday: editForm.birthday,
      gender: editForm.gender,
      email: editForm.email,
      phone: editForm.phone,
      address: editForm.address,
    })
    message.success('客戶資料已更新')
    showEditModal.value = false
    resetEditForm()
    await fetchData()
  } catch (err) {
    message.error(err.response?.data?.message || '修改失敗')
  } finally {
    submitLoading.value = false
  }
}

// ===========================
// 停用客戶（帶確認對話框）
// ===========================
function handleDeactivate(record) {
  Modal.confirm({
    title: '確定要停用此客戶嗎？',
    content: `姓名：${record.name}（${record.customerId}），停用後該客戶所有服務將暫停。`,
    okText: '確定停用',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deactivateCustomer(record.customerId)
        message.success(`客戶「${record.name}」已停用`)
        await fetchData()
      } catch (err) {
        message.error(err.response?.data?.message || '停用失敗')
      }
    },
  })
}

// ===========================
// 重新啟用客戶
// ===========================
function handleActivate(record) {
  Modal.confirm({
    title: '確定要重新啟用此客戶嗎？',
    content: `姓名：${record.name}（${record.customerId}），啟用後該客戶可再次登入。`,
    okText: '確定啟用',
    okType: 'primary',
    cancelText: '取消',
    async onOk() {
      try {
        await activateCustomer(record.customerId)
        message.success(`客戶「${record.name}」已重新啟用`)
        await fetchData()
      } catch (err) {
        message.error(err.response?.data?.message || '啟用失敗')
      }
    },
  })
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
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: rgba(92, 107, 95, 0.1);
  color: #5C6B5F;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
  flex-shrink: 0;
}

.emp-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.emp-name-text {
  font-weight: 600;
  color: #1a1a2e;
  font-size: 15px;
}

.emp-id-text {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}

.truncate-cell {
  display: inline-block;
  max-width: 210px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.stack-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.primary-text {
  color: #1a1a2e;
  font-weight: 600;
  line-height: 1.35;
}

.secondary-text {
  color: #8c8c8c;
  font-size: 12px;
  line-height: 1.35;
}

.tag-stack {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.application-status {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  border-radius: 999px;
  padding: 2px 9px;
  font-size: 12px;
  font-weight: 700;
  background: rgba(140, 140, 140, 0.12);
  color: #595959;
}

.application-status.pending {
  background: rgba(250, 140, 22, 0.12);
  color: #d46b08;
}

.application-status.supplement-required {
  background: rgba(250, 173, 20, 0.16);
  color: #ad6800;
}

.application-status.approved {
  background: rgba(82, 196, 26, 0.12);
  color: #389e0d;
}

.application-status.rejected,
.application-status.cancelled {
  background: rgba(255, 77, 79, 0.12);
  color: #cf1322;
}

.customer-detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  padding: 14px 8px;
}

.detail-section {
  min-width: 0;
}

.detail-section h4 {
  margin: 0 0 10px;
  color: #1a1a2e;
  font-size: 14px;
  font-weight: 700;
}

.detail-section dl {
  display: grid;
  grid-template-columns: minmax(88px, auto) minmax(0, 1fr);
  gap: 8px 12px;
  margin: 0;
}

.detail-section dl > div {
  display: contents;
}

.detail-section dt {
  color: #8c8c8c;
  font-size: 12px;
}

.detail-section dd {
  min-width: 0;
  margin: 0;
  color: #303030;
  overflow-wrap: anywhere;
  font-size: 13px;
}

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

.status-active {
  background-color: rgba(82, 196, 26, 0.1);
  color: #389e0d;
}
.status-active .status-dot { background-color: #52c41a; }

.status-deactivated, .status-frozen, .status-inactive {
  background-color: rgba(255, 77, 79, 0.1);
  color: #d9363e;
}
.status-deactivated .status-dot,
.status-frozen .status-dot,
.status-inactive .status-dot { background-color: #ff4d4f; }

.status-pending {
  background-color: rgba(250, 140, 22, 0.1);
  color: #fa8c16;
}
.status-pending .status-dot { background-color: #fa8c16; }

.suspend-btn {
  color: #ff4d4f;
}

.suspend-btn:hover {
  color: #d9363e;
  background-color: rgba(255, 77, 79, 0.05);
}

.resume-btn {
  color: #52c41a;
}

.resume-btn:hover {
  color: #389e0d;
  background-color: rgba(82, 196, 26, 0.05);
}

@media (max-width: 1100px) {
  .customer-detail-grid {
    grid-template-columns: 1fr;
  }
}
</style>
