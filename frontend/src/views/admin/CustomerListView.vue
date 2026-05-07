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
        <a-button type="primary" class="rounded-btn" @click="openCreateModal">
          新增客戶
        </a-button>
      </div>
      <div class="global-actions">
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
            <a-divider v-if="isSuperAdmin" type="vertical" />
            <a-button
              v-if="isSuperAdmin"
              type="link"
              class="action-btn suspend-btn"
              @click="handleDeactivate(record.customerId)"
              :disabled="record.status === 'DEACTIVATED' || record.status === 'INACTIVE'"
            >
              停用
            </a-button>
          </div>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="showModal"
      :title="isEdit ? '編輯客戶' : '新增客戶'"
      @ok="handleSubmit"
      :confirm-loading="submitLoading"
      @cancel="resetForm"
    >
      <a-form layout="vertical">
        <div v-if="!isEdit" style="margin-bottom: 12px; display: flex; gap: 8px;">
          <span style="font-size: 12px; color: #999; margin-right: 8px; line-height: 24px;">快速帶入：</span>
          <a-button size="small" @click="fillDemoCustomer('M')">男性客戶</a-button>
          <a-button size="small" @click="fillDemoCustomer('F')">女性客戶</a-button>
          <a-button size="small" type="dashed" @click="handleSeed" :loading="seedLoading">一鍵帶入</a-button>
        </div>

        <a-form-item label="身分證字號">
          <a-input v-model:value="form.idNumber" placeholder="請輸入身分證字號" :disabled="isEdit" />
        </a-form-item>
        <a-form-item label="姓名">
          <a-input v-model:value="form.name" placeholder="請輸入姓名" />
        </a-form-item>
        <a-form-item label="生日">
          <a-date-picker
            v-model:value="form.birthday"
            style="width: 100%"
            value-format="YYYY-MM-DD"
            placeholder="請選擇生日"
          />
        </a-form-item>
        <a-form-item label="性別">
          <a-select v-model:value="form.gender" placeholder="請選擇性別">
            <a-select-option value="M">男</a-select-option>
            <a-select-option value="F">女</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="Email">
          <a-input v-model:value="form.email" placeholder="請輸入 Email" />
        </a-form-item>
        <a-form-item label="電話">
          <a-input v-model:value="form.phone" placeholder="請輸入電話" />
        </a-form-item>
        <a-form-item label="地址">
          <a-input v-model:value="form.address" placeholder="請輸入地址" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { message, Modal } from 'ant-design-vue'
import { SearchOutlined } from '@ant-design/icons-vue'
import {
  getCustomers,
  createCustomer,
  updateCustomer,
  deactivateCustomer,
  seedCustomers,
} from '@/api/customer'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const isSuperAdmin = computed(() => {
  const roleCode = authStore.user?.roleCode
  return ['CISO', 'CSDM'].includes(roleCode)
})

const statusMap = {
  ACTIVE: '正常',
  DEACTIVATED: '已註銷',
  PENDING: '待審核',
  FROZEN: '凍結',
}

const genderMap = {
  M: '男',
  F: '女',
}

const maleNames = ['張志豪', '林建宏', '黃柏翔', '吳宗翰', '陳俊傑', '劉冠廷', '周政廷', '方建宏']
const femaleNames = ['陳怡君', '林佳蓉', '王雅婷', '黃詩涵', '許家瑩', '曾婉茹', '卓佩樺', '賴怡君']
const cities = ['台北市', '新北市', '桃園市', '台中市', '高雄市', '台南市']
const districts = {
  '台北市': ['中正區', '大安區', '信義區', '松山區', '中山區'],
  '新北市': ['板橋區', '中和區', '新莊區', '三重區', '永和區'],
  '桃園市': ['桃園區', '中壢區', '龜山區', '八德區'],
  '台中市': ['西屯區', '北屯區', '南屯區', '豐原區'],
  '高雄市': ['前鎮區', '鼓山區', '左營區', '三民區'],
  '台南市': ['東區', '中西區', '安平區', '北區'],
}
const roads = ['中正路', '民生路', '忠孝東路', '復興南路', '和平東路', '仁愛路', '光復路', '建國路']

function generateIdNumber(gender) {
  const cityCode = 'ABCDEFGHJKLMNPQRSTUVWXYZ'
  const letter = cityCode[Math.floor(Math.random() * cityCode.length)]
  const genderDigit = gender === 'M' ? '1' : '2'
  let digits = genderDigit
  for (let i = 0; i < 8; i++) {
    digits += Math.floor(Math.random() * 10)
  }
  return letter + digits
}

function fillDemoCustomer(gender) {
  const names = gender === 'M' ? maleNames : femaleNames
  const name = names[Math.floor(Math.random() * names.length)]
  const city = cities[Math.floor(Math.random() * cities.length)]
  const district = districts[city][Math.floor(Math.random() * districts[city].length)]
  const road = roads[Math.floor(Math.random() * roads.length)]
  const num = Math.floor(Math.random() * 200) + 1
  const floor = Math.floor(Math.random() * 12) + 1

  const year = Math.floor(Math.random() * 30) + 1970
  const month = String(Math.floor(Math.random() * 12) + 1).padStart(2, '0')
  const day = String(Math.floor(Math.random() * 28) + 1).padStart(2, '0')

  const phoneNum = '09' + String(Math.floor(Math.random() * 100000000)).padStart(8, '0')
  const emailPrefix = `demo${Math.floor(Math.random() * 9000) + 1000}`

  form.idNumber = generateIdNumber(gender)
  form.name = name
  form.birthday = `${year}-${month}-${day}`
  form.gender = gender
  form.email = `${emailPrefix}@gmail.com`
  form.phone = phoneNum
  form.address = `${city}${district}${road}${num}號${floor}樓`
}

const keyword = ref('')
const customers = ref([])
const loading = ref(false)

const columns = [
  { title: '客戶資訊', dataIndex: 'name', key: 'name', width: 200, fixed: 'left' },
  { title: 'CIF', dataIndex: 'cif', key: 'cif', width: 120 },
  { title: '身分證字號', dataIndex: 'idNumber', key: 'idNumber', width: 130 },
  { title: '性別', dataIndex: 'gender', key: 'gender', width: 70 },
  { title: '生日', dataIndex: 'birthday', key: 'birthday', width: 120 },
  { title: 'Email', dataIndex: 'email', key: 'email', width: 180 },
  { title: '電話', dataIndex: 'phone', key: 'phone', width: 130 },
  { title: '狀態', dataIndex: 'status', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 140, align: 'right', fixed: 'right' },
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

const showModal = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const editingCustomerId = ref('')

const form = reactive({
  idNumber: '',
  name: '',
  birthday: null,
  gender: undefined,
  email: '',
  phone: '',
  address: '',
})

function resetForm() {
  form.idNumber = ''
  form.name = ''
  form.birthday = null
  form.gender = undefined
  form.email = ''
  form.phone = ''
  form.address = ''
  isEdit.value = false
  editingCustomerId.value = ''
}

function openCreateModal() {
  resetForm()
  showModal.value = true
}

function openEditModal(record) {
  isEdit.value = true
  editingCustomerId.value = record.customerId
  form.idNumber = record.idNumber
  form.name = record.name
  form.birthday = record.birthday || null
  form.gender = record.gender
  form.email = record.email
  form.phone = record.phone
  form.address = record.address
  showModal.value = true
}

async function handleSubmit() {
  submitLoading.value = true
  try {
    const payload = {
      idNumber: form.idNumber,
      name: form.name,
      birthday: form.birthday,
      gender: form.gender,
      email: form.email,
      phone: form.phone,
      address: form.address,
    }

    if (isEdit.value) {
      await updateCustomer(editingCustomerId.value, payload)
      message.success('客戶修改成功')
    } else {
      await createCustomer(payload)
      message.success('客戶新增成功')
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

function handleDeactivate(customerId) {
  Modal.confirm({
    title: '確定要註銷此客戶嗎？',
    content: `客戶 ID: ${customerId}，註銷後該客戶所有服務將停止。`,
    okText: '確定註銷',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deactivateCustomer(customerId)
        message.success('客戶已註銷')
        await fetchData()
      } catch (err) {
        message.error(err.response?.data?.message || '註銷失敗')
      }
    },
  })
}

const seedLoading = ref(false)

async function handleSeed() {
  seedLoading.value = true
  try {
    const res = await seedCustomers()
    message.success(res.data.data || '一鍵帶入成功')
    await fetchData()
    showModal.value = false
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

.status-deactivated, .status-frozen {
  background-color: rgba(255, 77, 79, 0.1);
  color: #d9363e;
}
.status-deactivated .status-dot, .status-frozen .status-dot { background-color: #ff4d4f; }

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
</style>