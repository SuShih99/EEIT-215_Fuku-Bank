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
        <a-button type="primary" class="rounded-btn" @click="goCreate">
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
            <a-button type="link" class="action-btn edit-btn" @click="openEditModal(record)">
              編輯
            </a-button>
            <a-divider type="vertical" />
            <template v-if="record.status === 'SUSPENDED'">
              <a-button
                type="link"
                class="action-btn resume-btn"
                @click="handleResume(record)"
              >
                啟用
              </a-button>
            </template>
            <template v-else>
              <a-button
                type="link"
                class="action-btn suspend-btn"
                @click="handleSuspend(record)"
              >
                停用
              </a-button>
            </template>
          </div>
        </template>
      </template>
    </a-table>

    <!-- 編輯 Modal（僅修改，不新增）-->
    <a-modal
      v-model:open="showEditModal"
      title="編輯員工資料"
      @ok="handleSubmitEdit"
      :confirm-loading="submitLoading"
      @cancel="resetEditForm"
      ok-text="儲存變更"
      cancel-text="取消"
    >
      <a-form layout="vertical">
        <a-form-item label="員工編號">
          <a-input :value="editForm.empId" disabled />
        </a-form-item>
        <a-form-item label="姓名">
          <a-input v-model:value="editForm.empName" placeholder="請輸入姓名" />
        </a-form-item>
        <a-form-item label="部門">
          <a-select v-model:value="editForm.deptId" placeholder="請選擇部門" @change="handleEditDeptChange">
            <a-select-option value="DPT001">DPT001 消費金融部</a-select-option>
            <a-select-option value="DPT002">DPT002 客戶服務部</a-select-option>
            <a-select-option value="DPT003">DPT003 授信審查部</a-select-option>
            <a-select-option value="DPT004">DPT004 營運企劃部</a-select-option>
            <a-select-option value="DPT005">DPT005 資訊安全部</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="角色">
          <a-select v-model:value="editForm.roleId" placeholder="請先選擇部門">
            <a-select-option v-for="r in editFilteredRoles" :key="r.id" :value="r.id">
              {{ r.id }} · {{ r.code }} · {{ r.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="Email">
          <a-input v-model:value="editForm.email" placeholder="請輸入 Email" />
        </a-form-item>
        <a-form-item label="合約到期日">
          <a-date-picker
            v-model:value="editForm.contractEndDate"
            style="width: 100%"
            value-format="YYYY-MM-DDTHH:mm:ss"
            show-time
          />
        </a-form-item>
        <a-form-item label="權限到期日">
          <a-date-picker
            v-model:value="editForm.permissionExpire"
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import {
  getEmployees,
  updateEmployee,
  suspendEmployee,
  resumeEmployee,
} from '@/api/auth'

const router = useRouter()

const statusMap = {
  ACTIVE: '啟用',
  SUSPENDED: '停用',
  LOCKED: '鎖定',
}

const allRoles = [
  { id: 'R001', deptId: 'DPT001', code: 'CFSO',    name: '消金業務專員' },
  { id: 'R002', deptId: 'DPT001', code: 'CFDM',    name: '消金部經理' },
  { id: 'R003', deptId: 'DPT002', code: 'CSVO',    name: '客服照會專員' },
  { id: 'R004', deptId: 'DPT002', code: 'CSDM',    name: '客服部經理' },
  { id: 'R005', deptId: 'DPT003', code: 'JCRO',    name: '初階授信審查員' },
  { id: 'R006', deptId: 'DPT003', code: 'CRDM',    name: '授信部經理' },
  { id: 'R007', deptId: 'DPT003', code: 'CRO',     name: '風控長' },
  { id: 'R008', deptId: 'DPT004', code: 'OPS_PA',  name: '營運企劃專員' },
  { id: 'R009', deptId: 'DPT004', code: 'COO',     name: '營運長' },
  { id: 'R010', deptId: 'DPT005', code: 'ISSA',    name: '資安監控分析師' },
  { id: 'R011', deptId: 'DPT005', code: 'CISO',    name: '資安長' },
  { id: 'R012', deptId: 'DPT005', code: 'SYS_STAFF', name: '職員' },
  { id: 'R013', deptId: 'DPT005', code: 'SYS_SUPER', name: '超級管理員' },
]

const editFilteredRoles = computed(() => {
  if (!editForm.deptId) return []
  return allRoles.filter(r => r.deptId === editForm.deptId)
})

function handleEditDeptChange() {
  editForm.roleId = undefined
}

function formatTime(value) {
  if (!value) return '-'
  return value.replace('T', ' ').substring(0, 19)
}

const keyword = ref('')
const employees = ref([])
const loading = ref(false)

const columns = [
  { title: '員工資訊', dataIndex: 'empName',         key: 'empName',         width: 180, fixed: 'left' },
  { title: '狀態',    dataIndex: 'status',          key: 'status',          width: 100 },
  { title: '部門',    dataIndex: 'deptId',          key: 'deptId',          width: 100 },
  { title: '角色代碼', dataIndex: 'roleCode',        key: 'roleCode',        width: 120 },
  { title: 'Email',   dataIndex: 'email',           key: 'email',           width: 240 },
  {
    title: '合約到期',
    dataIndex: 'contractEndDate',
    key: 'contractEndDate',
    width: 170,
    customRender: ({ text }) => formatTime(text),
  },
  { title: '操作', key: 'action', width: 140, fixed: 'right' },
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

function goCreate() {
  router.push({ name: 'admin-employees-create' })
}
// 編輯 Modal
const showEditModal = ref(false)
const submitLoading = ref(false)

const editForm = reactive({
  empId: '',
  empName: '',
  deptId: undefined,
  roleId: undefined,
  email: '',
  contractEndDate: null,
  permissionExpire: null,
})

function resetEditForm() {
  editForm.empId = ''
  editForm.empName = ''
  editForm.deptId = undefined
  editForm.roleId = undefined
  editForm.email = ''
  editForm.contractEndDate = null
  editForm.permissionExpire = null
}

function openEditModal(record) {
  editForm.empId = record.empId
  editForm.empName = record.empName
  editForm.deptId = record.deptId
  editForm.roleId = record.roleId
  editForm.email = record.email
  editForm.contractEndDate = record.contractEndDate || null
  editForm.permissionExpire = record.permissionExpire || null
  showEditModal.value = true
}

async function handleSubmitEdit() {
  if (!editForm.roleId) {
    message.warning('請選擇角色再下儲存')
    return
  }
  submitLoading.value = true
  try {
    await updateEmployee(editForm.empId, {
      empId: editForm.empId,
      empName: editForm.empName,
      deptId: editForm.deptId,
      roleId: editForm.roleId,
      email: editForm.email,
      contractEndDate: editForm.contractEndDate,
      permissionExpire: editForm.permissionExpire,
    })
    message.success('員工資料已更新')
    showEditModal.value = false
    resetEditForm()
    await fetchData()
  } catch (err) {
    message.error(err.response?.data?.message || '修改失敗')
  } finally {
    submitLoading.value = false
  }
}
// 停用員工（帶確認對話框）
function handleSuspend(record) {
  Modal.confirm({
    title: '確定要停用此員工嗎？',
    content: `姓名：${record.empName}（${record.empId}），停用後該員工將無法登入系統。`,
    okText: '確定停用',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await suspendEmployee(record.empId)
        message.success(`員工「${record.empName}」已停用`)
        await fetchData()
      } catch (err) {
        message.error(err.response?.data?.message || '停用失敗')
      }
    },
  })
}
// 重新啟用員工
function handleResume(record) {
  Modal.confirm({
    title: '確定要重新啟用此員工嗎？',
    content: `姓名：${record.empName}（${record.empId}），啟用後該員工可再次登入系統。`,
    okText: '確定啟用',
    okType: 'primary',
    cancelText: '取消',
    async onOk() {
      try {
        await resumeEmployee(record.empId)
        message.success(`員工「${record.empName}」已重新啟用`)
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

.resume-btn {
  color: #52c41a;
}

.resume-btn:hover {
  color: #389e0d;
  background-color: rgba(82, 196, 26, 0.05);
}
</style>