<template>
  <div class="page-container">
    <!-- 頁首：標題左、操作列右 -->
    <div class="page-header">
      <h2 class="page-title">黑名單</h2>
      <div class="header-actions">
        <span class="filter-label">顯示狀態：</span>
        <a-radio-group v-model:value="filterStatus" @change="handleFilterChange">
          <a-radio-button :value="true">已啟用</a-radio-button>
          <a-radio-button :value="false">已停用</a-radio-button>
          <a-radio-button :value="null">全部</a-radio-button>
        </a-radio-group>
        <a-button @click="fillBlacklistTemplate">帶入高風險用戶</a-button>
        <a-button type="primary" @click="openModal('create')">新增黑名單</a-button>
      </div>
    </div>

    <!-- 資料表格 -->
    <a-table
      :columns="columns"
      :data-source="dataSource"
      :pagination="pagination"
      :loading="loading"
      :scroll="{ x: 1300 }"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'listType'">
          {{ typeLabelMap[record.listType] || record.listType }}
        </template>

        <template v-if="column.key === 'status'">
          <a-switch
            v-model:checked="record.status"
            @change="(checked) => handleStatusChange(record, checked)"
          />
        </template>

        <template v-if="column.key === 'action'">
          <a @click="openModal('edit', record)">編輯</a>
        </template>
      </template>
    </a-table>

    <!-- 新增 / 編輯 Modal -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalType === 'create' ? '新增黑名單' : '編輯黑名單'"
      ok-text="確認"
      cancel-text="取消"
      :confirm-loading="submitting"
      @ok="handleModalOk"
      @cancel="resetForm"
    >
      <a-form ref="formRef" :model="formData" :rules="rules" layout="vertical">
        <a-form-item label="類型" name="listType">
          <a-select
            v-model:value="formData.listType"
            :disabled="modalType === 'edit'"
            @change="() => formRef.clearValidate('listValue')"
          >
            <a-select-option value="ID_CARD">身份證字號</a-select-option>
            <a-select-option value="EMAIL">電子郵件</a-select-option>
            <a-select-option value="PHONE">電話</a-select-option>
            <a-select-option value="ACCOUNT_NO">帳戶</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="資料" name="listValue">
          <a-input
            v-model:value="formData.listValue"
            :disabled="modalType === 'edit'"
            :placeholder="valuePlaceholder"
          />
        </a-form-item>

        <a-form-item label="備註原因" name="reason">
          <a-textarea v-model:value="formData.reason" :rows="3" />
        </a-form-item>

        <a-form-item label="解封時間（不選則為永久）" name="expireAt">
          <a-date-picker
            v-model:value="formData.expireAt"
            show-time
            placeholder="選擇解封日期時間"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
          <div style="font-size: 12px; color: #999; margin-top: 4px">
            超過此時間後，系統將自動不再攔截該筆資料。
          </div>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import api from '@/api/axios'

const BASE_URL = '/api/risk/blacklist'

const loading = ref(false)
const submitting = ref(false)
const dataSource = ref([])
const modalVisible = ref(false)
const modalType = ref('create')
const filterStatus = ref(true)
const formRef = ref(null)

const formData = reactive({
  listType: 'ID_CARD',
  listValue: '',
  reason: '',
  expireAt: null,
  status: true,
})

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total) => `共 ${total} 筆`,
})

const typeLabelMap = {
  ID_CARD: '身份證字號',
  EMAIL: '電子郵件',
  PHONE: '電話',
  ACCOUNT_NO: '帳戶',
}

// 依類型動態調整 placeholder
const valuePlaceholder = computed(
  () =>
    ({
      ID_CARD: '例：A123456789',
      EMAIL: '例：user@example.com',
      PHONE: '例：0912345678',
    })[formData.listType] || '',
)

// 表單驗證規則
const rules = computed(() => ({
  listType: [{ required: true, message: '請選擇類型' }],
  listValue: [
    { required: true, message: '此欄位為必填', trigger: 'blur' },
    {
      validator: async (_rule, value) => {
        if (!value) return Promise.resolve()
        const patterns = {
          EMAIL: { re: /^[^\s@]+@[^\s@]+\.[^\s@]+$/, msg: '請輸入正確的電子郵件格式' },
          PHONE: { re: /^09\d{8}$/, msg: '請輸入正確的手機號碼格式' },
          ID_CARD: { re: /^[A-Z][12]\d{8}$/, msg: '請輸入正確的身分證格式' },
        }
        const p = patterns[formData.listType]
        if (p && !p.re.test(value)) return Promise.reject(p.msg)
        return Promise.resolve()
      },
      trigger: 'change',
    },
  ],
  reason: [{ required: true, message: '請輸入原因', trigger: 'blur' }],
}))

function formatDate(value) {
  if (!value) return '—'
  const d = new Date(value)
  if (isNaN(d)) return value
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}/${pad(d.getMonth() + 1)}/${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const columns = [
  { title: '類型', dataIndex: 'listType', key: 'listType', width: 120 },
  { title: '資料', dataIndex: 'listValue', key: 'listValue', width: 180, ellipsis: true },
  { title: '啟用狀態', dataIndex: 'status', key: 'status', width: 100 },
  { title: '原因', dataIndex: 'reason', key: 'reason', width: 200, ellipsis: true },
  { title: '資料來源', dataIndex: 'source', key: 'source', width: 150, ellipsis: true },
  { title: '建立時間', dataIndex: 'createdAt', key: 'createdAt', width: 160, customRender: ({ text }) => formatDate(text) },
  { title: '解封時間', dataIndex: 'expireAt', key: 'expireAt', width: 160, customRender: ({ text }) => formatDate(text) },
  { title: '更新時間', dataIndex: 'updatedAt', key: 'updatedAt', width: 160, customRender: ({ text }) => formatDate(text) },
  { title: '操作', key: 'action', width: 80, fixed: 'right' },
]

async function fetchList() {
  loading.value = true
  try {
    const params = {
      page: pagination.current - 1,
      size: pagination.pageSize,
      sort: 'createdAt,desc',
    }
    // filterStatus 為 null 時不帶參數，避免後端收到 'null' 字串
    if (filterStatus.value !== null) {
      params.activated = filterStatus.value
    }

    const res = await api.get(BASE_URL, { params })
    dataSource.value = res.data.data.content
    pagination.total = res.data.data.totalElements
  } catch {
    message.error('讀取失敗')
  } finally {
    loading.value = false
  }
}

async function handleStatusChange(record, checked) {
  try {
    // 對應 PUT /api/risk/blacklist/{type}/{value}/status
    await api.put(`${BASE_URL}/${record.listType}/${record.listValue}/status`, null, {
      params: { status: checked },
    })
    message.success('狀態更新成功')
  } catch {
    record.status = !checked // 失敗時還原開關
    message.error('狀態更新失敗')
  }
}

async function handleModalOk() {
  try {
    await formRef.value.validateFields()
    submitting.value = true

    if (modalType.value === 'create') {
      // 對應 POST /api/risk/blacklist/create
      await api.post(`${BASE_URL}/create`, formData)
      message.success('新增成功')
    } else {
      // 對應 POST /api/risk/blacklist/{type}/{value}/update
      await api.post(`${BASE_URL}/${formData.listType}/${formData.listValue}/update`, formData)
      message.success('更新成功')
    }

    modalVisible.value = false
    resetForm()
    fetchList()
  } catch (error) {
    if (!error.errorFields) {
      message.error(error.response?.data?.message || '伺服器錯誤')
    }
  } finally {
    submitting.value = false
  }
}

function handleTableChange(pag) {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchList()
}

function handleFilterChange() {
  pagination.current = 1
  fetchList()
}

function openModal(type, record = null) {
  modalType.value = type
  if (type === 'edit' && record) {
    Object.assign(formData, {
      listType: record.listType,
      listValue: record.listValue,
      reason: record.reason,
      expireAt: record.expireAt ?? null,
      status: record.status,
    })
  } else {
    resetForm()
  }
  modalVisible.value = true
}

function resetForm() {
  formData.listType = 'ID_CARD'
  formData.listValue = ''
  formData.reason = ''
  formData.expireAt = null
  formData.status = true
  formRef.value?.clearValidate()
}

function fillBlacklistTemplate() {
  openModal('create')
  // override reset defaults
  formData.listType = 'ACCOUNT_NO'
  formData.listValue = '070101074383'
  formData.reason = '高風險用戶'
  formData.expireAt = null
  // clear validation since we just changed values
  setTimeout(() => formRef.value?.clearValidate(), 0)
}

onMounted(fetchList)
</script>

<style scoped>
:deep(.ant-table) {
  font-size: 15px;
}

:deep(.ant-table-thead > tr > th) {
  font-size: 15px;
  font-weight: 600;
}

:deep(.ant-table-tbody > tr > td) {
  font-size: 15px;
}

.page-container {
  display: flex;
  flex-direction: column;
  gap: 0;
}

/* 頁首：標題 + 操作列同一行，左右對齊 */
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-label {
  font-size: 14px;
  color: #595959;
  white-space: nowrap;
}
</style>
