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
      :scroll="{ x: 1000 }"
      row-key="customerId"
      class="custom-table"
      :pagination="{ pageSize: 10, showSizeChanger: false }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <div class="emp-name-cell">
            <div class="emp-avatar">{{ record.name.charAt(0) }}</div>
            <div class="emp-info">
              <span class="emp-name-text">{{ record.name }}</span>
              <span class="emp-id-text">{{ record.customerId }}</span>
            </div>
          </div>
        </template>
        <template v-else-if="column.key === 'gender'">
          {{ genderMap[record.gender] || record.gender }}
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

const keyword = ref('')
const customers = ref([])
const loading = ref(false)

const columns = [
  { title: '客戶資訊',  dataIndex: 'name',      key: 'name',      width: 180, fixed: 'left' },
  { title: 'CIF',       dataIndex: 'cif',        key: 'cif',       width: 120 },
  { title: '身分證字號', dataIndex: 'idNumber',   key: 'idNumber',  width: 130 },
  { title: '性別',      dataIndex: 'gender',     key: 'gender',    width: 60 },
  { title: '生日',      dataIndex: 'birthday',   key: 'birthday',  width: 110 },
  { title: 'Email',     dataIndex: 'email',      key: 'email',     width: 200 },
  { title: '電話',      dataIndex: 'phone',      key: 'phone',     width: 130 },
  { title: '狀態',      dataIndex: 'status',     key: 'status',    width: 100 },
  { title: '操作',      key: 'action',           width: 140, fixed: 'right' },
]

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
</style>