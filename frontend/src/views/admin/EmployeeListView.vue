<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">員工管理</h2>
    </div>

    <div class="action-bar">
      <div class="search-group">
        <a-input
          v-model:value="keyword"
          placeholder="搜尋員工姓名或編號..."
          class="rounded-input search-input"
          allow-clear
          @press-enter="handleSearch"
        >
          <template #prefix><SearchOutlined style="color: #bfbfbf" /></template>
        </a-input>
        <a-button type="primary" class="rounded-btn" @click="handleSearch">查詢</a-button>
        <a-button class="rounded-btn btn-ghost" @click="handleClear">清除</a-button>
      </div>

      <div class="global-actions">
        <a-button class="rounded-btn btn-ghost" @click="handleSeed" :loading="seedLoading">一鍵帶入</a-button>
        <a-button v-if="authStore.user?.permLevel === 0" type="primary" class="rounded-btn" @click="openCreateModal">
          <template #icon><PlusOutlined /></template>
          新增
        </a-button>
      </div>
    </div>

    <a-table
      :columns="columns"
      :data-source="employees"
      :loading="loading"
      :scroll="{ x: 1000 }"
      row-key="empId"
      class="custom-table"
      :pagination="{ pageSize: 10, showSizeChanger: false }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'empName'">
          <div class="emp-name-cell">
            <div class="emp-avatar">{{ record.empName.charAt(0) }}</div>
            <div class="emp-info">
              <span class="emp-name-text">{{ record.empName }}</span>
              <span class="emp-id-text">{{ record.empId }}</span>
            </div>
          </div>
        </template>
        <template v-else-if="column.key === 'status'">
          <div :class="['status-tag', `status-${record.status.toLowerCase()}`]">
            <span class="status-dot"></span>
            {{ statusMap[record.status] || record.status }}
          </div>
        </template>
        <template v-else-if="column.key === 'action'">
          <div class="action-cell">
            <a-button v-if="authStore.user?.permLevel === 1" type="link" class="action-btn edit-btn" @click="openEditModal(record)">
              編輯
            </a-button>
            <a-divider v-if="authStore.user?.permLevel === 1" type="vertical" />
            <a-button
              v-if="authStore.user?.permLevel === 1"
              type="link"
              class="action-btn suspend-btn"
              @click="handleSuspend(record.empId)"
              :disabled="record.status === 'SUSPENDED'"
            >
              停用
            </a-button>
          </div>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="showModal"
      :title="isEdit ? '編輯員工' : '新增員工'"
      @ok="handleSubmit"
      :confirm-loading="submitLoading"
      @cancel="resetForm"
    >
      <a-form layout="vertical">
        <a-form-item label="部門">
          <a-select v-model:value="form.deptId" placeholder="請選擇部門" @change="handleDeptChange">
            <a-select-option value="DPT001">DPT001 消費金融部</a-select-option>
            <a-select-option value="DPT002">DPT002 客戶服務部</a-select-option>
            <a-select-option value="DPT003">DPT003 授信審查部</a-select-option>
            <a-select-option value="DPT004">DPT004 營運企劃部</a-select-option>
            <a-select-option value="DPT005">DPT005 資訊安全部</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="角色">
          <a-select v-model:value="form.roleId" placeholder="請先選擇部門">
            <a-select-option v-for="r in filteredRoles" :key="r.id" :value="r.id">
              {{ r.id }} {{ r.code }} {{ r.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="Email">
          <a-input v-model:value="form.email" placeholder="請輸入 Email" />
        </a-form-item>
        <a-form-item v-if="!isEdit" label="密碼">
          <a-input-password v-model:value="form.password" placeholder="請輸入密碼" />
        </a-form-item>
        <a-form-item label="狀態">
          <a-select v-model:value="form.status" placeholder="請選擇狀態">
            <a-select-option value="ACTIVE">啟用</a-select-option>
            <a-select-option value="SUSPENDED">停用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="合約到期日">
          <a-date-picker
            v-model:value="form.contractEndDate"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
            show-time
          />
        </a-form-item>
        <a-form-item label="權限到期日">
          <a-date-picker
            v-model:value="form.permissionExpire"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
            show-time
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { SearchOutlined, PlusOutlined } from '@ant-design/icons-vue'
import {
  getEmployees,
  createEmployee,
  updateEmployee,
  suspendEmployee,
  seedEmployees,
} from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const statusMap = {
  ACTIVE: '啟用',
  SUSPENDED: '停用',
  LOCKED: '鎖定',
}

const allRoles = [
  { id: 'R001', deptId: 'DPT001', code: 'CFSO', name: '消金業務專員' },
  { id: 'R002', deptId: 'DPT001', code: 'CFDM', name: '消金部經理' },
  { id: 'R003', deptId: 'DPT002', code: 'CSVO', name: '客服照會專員' },
  { id: 'R004', deptId: 'DPT002', code: 'CSDM', name: '客服部經理' },
  { id: 'R005', deptId: 'DPT003', code: 'JCRO', name: '初階授信審查員' },
  { id: 'R006', deptId: 'DPT003', code: 'CRDM', name: '授信部經理' },
  { id: 'R007', deptId: 'DPT003', code: 'CRO', name: '風控長' },
  { id: 'R008', deptId: 'DPT004', code: 'OPS_PA', name: '營運企劃專員' },
  { id: 'R009', deptId: 'DPT004', code: 'COO', name: '營運長' },
  { id: 'R010', deptId: 'DPT005', code: 'ISSA', name: '資安監控分析師' },
  { id: 'R011', deptId: 'DPT005', code: 'CISO', name: '資安長' },
  { id: 'R012', deptId: 'DPT005', code: 'SYS_STAFF', name: '職員' },
  { id: 'R013', deptId: 'DPT005', code: 'SYS_SUPER', name: '超級管理員' },
]

const filteredRoles = computed(() => {
  if (!form.deptId) return []
  return allRoles.filter(r => r.deptId === form.deptId)
})

function handleDeptChange() {
  form.roleId = undefined
}

const demoNames = ['周政廷', '許家瑩', '楊宗翰', '賴怡君', '方建宏', '曾婉茹', '廖偉翔', '卓佩樺']
const deptRoleMap = {
  CF:  { deptId: 'DPT001', roleId: 'R001' },
  CS:  { deptId: 'DPT002', roleId: 'R003' },
  CR:  { deptId: 'DPT003', roleId: 'R005' },
  OPS: { deptId: 'DPT004', roleId: 'R008' },
  IS:  { deptId: 'DPT005', roleId: 'R010' },
}

function fillDemoEmployee(deptCode) {
  const mapping = deptRoleMap[deptCode]
  const name = demoNames[Math.floor(Math.random() * demoNames.length)]
  const num = String(Math.floor(Math.random() * 900) + 100)
  form.empId = 'E26' + num
  form.empName = name
  form.deptId = mapping.deptId
  form.roleId = mapping.roleId
  form.email = `demo${num}@javabank.com`
  form.password = '123456'
  form.status = 'ACTIVE'
  form.contractEndDate = null
  form.permissionExpire = '2026-12-31T00:00:00'
}

function formatTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ').substring(0, 19)
}

const keyword = ref('')
const employees = ref([])
const loading = ref(false)

const columns = [
  { title: '員工資訊', dataIndex: 'empName', key: 'empName', width: 200, fixed: 'left' },
  { title: '狀態', dataIndex: 'status', key: 'status', width: 120 },
  { title: '部門', dataIndex: 'deptId', key: 'deptId', width: 100 },
  { title: '角色代碼', dataIndex: 'roleCode', key: 'roleCode', width: 120 },
  { title: 'Email', dataIndex: 'email', key: 'email', width: 220 },
  {
    title: '合約到期',
    dataIndex: 'contractEndDate',
    key: 'contractEndDate',
    width: 170,
    customRender: ({ text }) => formatTime(text),
  },
  { title: '操作', key: 'action', width: 140, align: 'right', fixed: 'right' },
]

async function fetchData() {
  loading.value = true
  try {
    const res = await getEmployees(keyword.value || undefined)
    employees.value = res.data.data
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
  employees.value = []
}

const showModal = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)

const form = reactive({
  empId: '',
  empName: '',
  deptId: undefined,
  roleId: undefined,
  email: '',
  password: '',
  status: undefined,
  contractEndDate: null,
  permissionExpire: null,
})

function resetForm() {
  form.empId = ''
  form.empName = ''
  form.deptId = undefined
  form.roleId = undefined
  form.email = ''
  form.password = ''
  form.status = undefined
  form.contractEndDate = null
  form.permissionExpire = null
  isEdit.value = false
}

function openCreateModal() {
  resetForm()
  showModal.value = true
}

function openEditModal(record) {
  isEdit.value = true
  form.empId = record.empId
  form.empName = record.empName
  form.deptId = record.deptId
  form.roleId = record.roleId
  form.email = record.email
  form.status = record.status
  form.contractEndDate = record.contractEndDate || null
  form.permissionExpire = record.permissionExpire || null
  showModal.value = true
}

async function handleSubmit() {
  submitLoading.value = true
  try {
    const payload = {
      empId: form.empId,
      empName: form.empName,
      deptId: form.deptId,
      roleId: form.roleId,
      email: form.email,
      status: form.status,
      contractEndDate: form.contractEndDate,
      permissionExpire: form.permissionExpire,
    }

    if (isEdit.value) {
      await updateEmployee(form.empId, payload)
      message.success('員工修改成功')
    } else {
      payload.password = form.password
      await createEmployee(payload)
      message.success('員工新增成功')
    }

    showModal.value = false
    resetForm()
    await fetchData()
  } catch (err) {
    message.error(err.response?.data?.message || (isEdit.value ? '修改失敗' : '新增失敗'))
  } finally {
    submitLoading.value = false
  }
}

function handleSuspend(empId) {
  Modal.confirm({
    title: '確定要停用此員工嗎？',
    content: `員工編號: ${empId}，停用後該員工將無法登入系統。`,
    okText: '確定停用',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await suspendEmployee(empId)
        message.success('員工已停用')
        await fetchData()
      } catch (err) {
        message.error(err.response?.data?.message || '停用失敗')
      }
    },
  })
}

const seedLoading = ref(false)

async function handleSeed() {
  seedLoading.value = true
  try {
    const res = await seedEmployees()
    message.success(res.data.data || '資料已帶入')
    await fetchData()
  } catch (err) {
    message.error(err.response?.data?.message || '一鍵帶入失敗')
  } finally {
    seedLoading.value = false
  }
}
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
}

.emp-info {
  display: flex;
  flex-direction: column;
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

.status-suspended {
  background-color: rgba(255, 77, 79, 0.1);
  color: #d9363e;
}
.status-suspended .status-dot { background-color: #ff4d4f; }

.suspend-btn {
  color: #ff4d4f;
}

.suspend-btn:hover {
  color: #d9363e;
  background-color: rgba(255, 77, 79, 0.05);
}
</style>