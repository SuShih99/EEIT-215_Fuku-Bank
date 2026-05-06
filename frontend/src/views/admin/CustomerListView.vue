<template>
  <div style="padding: 24px">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px">
      <h2>客戶管理</h2>
      <div style="display: flex; gap: 8px">
        <a-button type="primary" @click="handleSeed" :loading="seedLoading">帶入測試資料</a-button>
      </div>
    </div>

    <!-- 查詢區 -->
    <div style="margin-bottom: 16px; display: flex; gap: 8px; align-items: center">
      <a-input
        v-model:value="keyword"
        placeholder="搜尋客戶姓名"
        style="width: 200px"
        allow-clear
        @press-enter="handleSearch"
      />
      <a-button type="primary" @click="handleSearch">查詢</a-button>
      <a-button @click="handleClear">清除</a-button>
      <a-button type="dashed" @click="openCreateModal">新增客戶</a-button>
    </div>

    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="customers"
      :loading="loading"
      :scroll="{ x: 1200 }"
      row-key="customerId"
      bordered
    >
      <template #bodyCell="{ column, record }">
        <!-- 性別顯示 -->
        <template v-if="column.key === 'gender'">
          {{ genderMap[record.gender] || record.gender }}
        </template>

        <!-- 狀態顯示 -->
        <template v-else-if="column.key === 'status'">
          <a-tag :color="record.status === 'ACTIVE' ? 'green' : record.status === 'DEACTIVATED' ? 'red' : 'default'">
            {{ statusMap[record.status] || record.status }}
          </a-tag>
        </template>

        <!-- 操作按鈕 -->
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button size="small" @click="openEditModal(record)">編輯</a-button>
            <a-button
              v-if="isSuperAdmin"
              size="small"
              danger
              @click="handleDeactivate(record.customerId)"
              :disabled="record.status === 'DEACTIVATED' || record.status === 'INACTIVE'"
            >停用</a-button>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 新增/編輯 Modal -->
    <a-modal
      v-model:open="showModal"
      :title="isEdit ? '編輯客戶' : '新增客戶'"
      @ok="handleSubmit"
      :confirm-loading="submitLoading"
      @cancel="resetForm"
    >
      <a-form layout="vertical">
        <!-- 一鍵帶入 -->
        <div v-if="!isEdit" style="margin-bottom: 12px">
          <span style="font-size: 12px; color: #999; margin-right: 8px">快速帶入：</span>
          <a-button size="small" @click="fillDemoCustomer('M')">男性客戶</a-button>
          <a-button size="small" @click="fillDemoCustomer('F')">女性客戶</a-button>
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
import {
  getCustomers,
  createCustomer,
  updateCustomer,
  deactivateCustomer,
  seedCustomers,
} from '@/api/customer'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

// === RBAC：只有 SUPER_ADMIN (CISO/CSDM) 才看得到「停用」按鈕 ===
const isSuperAdmin = computed(() => {
  const roleCode = authStore.user?.roleCode
  return ['CISO', 'CSDM'].includes(roleCode)
})

// === 中文對照 ===
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

// === 一鍵帶入 Demo 資料 ===
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

// 台灣身分證字號產生（符合格式但非真實）
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

// === 查詢相關 ===
const keyword = ref('')
const customers = ref([])
const loading = ref(false)

const columns = [
  { title: '客戶 ID', dataIndex: 'customerId', key: 'customerId', width: 110 },
  { title: 'CIF', dataIndex: 'cif', key: 'cif', width: 120 },
  { title: '身分證字號', dataIndex: 'idNumber', key: 'idNumber', width: 130 },
  { title: '姓名', dataIndex: 'name', key: 'name', width: 100 },
  { title: '生日', dataIndex: 'birthday', key: 'birthday', width: 120 },
  { title: '性別', dataIndex: 'gender', key: 'gender', width: 70 },
  { title: 'Email', dataIndex: 'email', key: 'email', width: 180 },
  { title: '電話', dataIndex: 'phone', key: 'phone', width: 130 },
  {
    title: '狀態',
    dataIndex: 'status',
    key: 'status',
    width: 90,
  },
  { title: '操作', key: 'action', width: 150, align: 'center', fixed: 'right' },
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

// === 新增/編輯相關 ===
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

// === 註銷客戶 ===
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

// === 帶入測試資料 ===
const seedLoading = ref(false)

async function handleSeed() {
  seedLoading.value = true
  try {
    const res = await seedCustomers()
    message.success(res.data.data || '測試資料已帶入')
    await fetchData()
  } catch (err) {
    message.error(err.response?.data?.message || '帶入測試資料失敗')
  } finally {
    seedLoading.value = false
  }
}
</script>
