<template>
  <div class="create-page">
    <!-- 頁頭工具列 -->
    <div class="create-toolbar">
      <a-button class="back-btn" @click="router.push({ name: 'admin-customers' })">
        返回客戶列表
      </a-button>
      <a-button type="primary" class="demo-btn" @click="fillNextDemo">
        一鍵帶入
      </a-button>
    </div>

    <!-- 主要卡片 -->
    <div class="create-card">
      <!-- 表單 -->
      <a-form
        ref="formRef"
        :model="form"
        layout="vertical"
        class="create-form"
      >
        <div class="form-grid">
          <!-- 身分證字號 -->
          <a-form-item
            label="身分證字號"
            name="idNumber"
            :rules="[{ required: true, message: '請輸入身分證字號' }]"
          >
            <a-input v-model:value="form.idNumber" placeholder="如：A123456789" class="form-input" />
          </a-form-item>

          <!-- 姓名 -->
          <a-form-item
            label="姓名"
            name="name"
            :rules="[{ required: true, message: '請輸入姓名' }]"
          >
            <a-input v-model:value="form.name" placeholder="請輸入中文姓名" class="form-input" />
          </a-form-item>

          <!-- 性別 -->
          <a-form-item
            label="性別"
            name="gender"
            :rules="[{ required: true, message: '請選擇性別' }]"
          >
            <a-select v-model:value="form.gender" placeholder="請選擇性別" class="form-input">
              <a-select-option value="M">男</a-select-option>
              <a-select-option value="F">女</a-select-option>
            </a-select>
          </a-form-item>

          <!-- 生日 -->
          <a-form-item
            label="出生日期"
            name="birthday"
            :rules="[{ required: true, message: '請選擇生日' }]"
          >
            <a-date-picker
              v-model:value="form.birthday"
              style="width: 100%"
              value-format="YYYY-MM-DD"
              placeholder="請選擇出生日期"
              class="form-input"
            />
          </a-form-item>

          <!-- Email -->
          <a-form-item
            label="Email"
            name="email"
            :rules="[
              { required: true, message: '請輸入 Email' },
              { type: 'email', message: '請輸入正確 Email 格式' }
            ]"
          >
            <a-input v-model:value="form.email" placeholder="example@gmail.com" class="form-input" />
          </a-form-item>

          <!-- 電話 -->
          <a-form-item
            label="聯絡電話"
            name="phone"
            :rules="[{ required: true, message: '請輸入電話' }]"
          >
            <a-input v-model:value="form.phone" placeholder="09xxxxxxxx" class="form-input" />
          </a-form-item>

          <!-- 地址（全寬） -->
          <a-form-item
            label="通訊地址"
            name="address"
            class="full-width"
            :rules="[{ required: true, message: '請輸入地址' }]"
          >
            <a-input v-model:value="form.address" placeholder="縣市 + 區域 + 街道 + 門牌" class="form-input" />
          </a-form-item>
        </div>

        <!-- 底部操作 -->
        <div class="form-footer">
          <a-button class="cancel-btn" @click="router.push({ name: 'admin-customers' })">
            取消
          </a-button>
          <a-button
            type="primary"
            class="submit-btn"
            :loading="submitLoading"
            @click="handleSubmit"
          >
            確認新增
          </a-button>
        </div>
      </a-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { createCustomer } from '@/api/customer'

const router = useRouter()
const formRef = ref(null)
const submitLoading = ref(false)
// 10 筆固定輪播示範資料
const demoCustomers = [
  { idNumber: 'A123456789', name: '張志豪', gender: 'M', birthday: '1985-03-15', email: 'jason.chang@gmail.com',  phone: '0912345678', address: '台北市大安區忠孝東路100號3樓' },
  { idNumber: 'B234567890', name: '陳怡君', gender: 'F', birthday: '1990-07-22', email: 'ivy.chen@gmail.com',    phone: '0923456789', address: '新北市板橋區民生路55號8樓' },
  { idNumber: 'C345678901', name: '林建宏', gender: 'M', birthday: '1978-11-08', email: 'ken.lin@gmail.com',     phone: '0934567890', address: '桃園市中壢區復興路200號2樓' },
  { idNumber: 'D456789012', name: '王雅婷', gender: 'F', birthday: '1995-01-30', email: 'tina.wang@gmail.com',   phone: '0945678901', address: '台中市西屯區文心路80號5樓' },
  { idNumber: 'E567890123', name: '黃柏翔', gender: 'M', birthday: '1988-09-14', email: 'alex.huang@gmail.com',  phone: '0956789012', address: '高雄市前鎮區中山路300號10樓' },
  { idNumber: 'F678901234', name: '許家瑩', gender: 'F', birthday: '1992-04-25', email: 'carol.hsu@gmail.com',   phone: '0967890123', address: '台南市東區平東路60號4樓' },
  { idNumber: 'G789012345', name: '吳宗翰', gender: 'M', birthday: '1983-06-19', email: 'michael.wu@gmail.com',  phone: '0978901234', address: '台北市信義區和平東路150號7樓' },
  { idNumber: 'H890123456', name: '劉佳蓉', gender: 'F', birthday: '1997-12-03', email: 'lisa.liu@gmail.com',    phone: '0989012345', address: '新北市中和區中正路90號6樓' },
  { idNumber: 'J901234567', name: '郭冠廷', gender: 'M', birthday: '1975-08-27', email: 'daniel.kuo@gmail.com',  phone: '0912387654', address: '台中市北屯區崇德路250號9樓' },
  { idNumber: 'K012345678', name: '蔡詩涵', gender: 'F', birthday: '1993-02-11', email: 'vivian.tsai@gmail.com', phone: '0923487654', address: '高雄市鼓山區美術館路45號2樓' },
]

const demoIndex = ref(0)

function fillNextDemo() {
  const demo = demoCustomers[demoIndex.value]
  form.idNumber = demo.idNumber
  form.name = demo.name
  form.gender = demo.gender
  form.birthday = demo.birthday
  form.email = demo.email
  form.phone = demo.phone
  form.address = demo.address
  demoIndex.value = (demoIndex.value + 1) % demoCustomers.length
}
// 表單資料
const form = reactive({
  idNumber: '',
  name: '',
  gender: undefined,
  birthday: null,
  email: '',
  phone: '',
  address: '',
})

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    await createCustomer({
      idNumber: form.idNumber,
      name: form.name,
      gender: form.gender,
      birthday: form.birthday,
      email: form.email,
      phone: form.phone,
      address: form.address,
    })
    message.success(`客戶「${form.name}」已成功新增`)
    router.push({ name: 'admin-customers' })
  } catch (err) {
    message.error(err.response?.data?.message || '新增失敗，請確認資料是否重複')
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.create-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 24px;
}

/* 頁頭工具列 */
.create-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.back-btn {
  border-radius: 10px;
  height: 38px;
  border-color: #d9d9d9;
  color: #555;
}

.back-btn:hover {
  border-color: #5C6B5F;
  color: #5C6B5F;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1.3;
}

.page-subtitle {
  margin: 4px 0 0;
  font-size: 14px;
  color: #8c8c8c;
}

/* 主卡片 */
.create-card {
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(92, 107, 95, 0.08);
  overflow: hidden;
}

/* 一鍵帶入區 */
.demo-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 28px;
  background: linear-gradient(135deg, rgba(92, 107, 95, 0.06) 0%, rgba(92, 107, 95, 0.02) 100%);
  border-bottom: 1px solid rgba(92, 107, 95, 0.1);
}

.demo-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #5C6B5F;
}

.demo-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.demo-counter {
  font-size: 13px;
  color: #8c8c8c;
  background: rgba(92, 107, 95, 0.08);
  padding: 4px 10px;
  border-radius: 20px;
}

.demo-btn {
  border-radius: 10px;
  background-color: #5C6B5F !important;
  border-color: #5C6B5F !important;
  color: #fff !important;
  font-weight: 600;
}

.demo-btn:hover {
  background-color: #4A574D !important;
  border-color: #4A574D !important;
  opacity: 0.9;
}

/* 表單 */
.create-form {
  padding: 32px 28px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 24px;
}

.full-width {
  grid-column: 1 / -1;
}

.form-input {
  border-radius: 10px !important;
}

/* 底部按鈕 */
.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.cancel-btn {
  border-radius: 10px;
  height: 40px;
  padding: 0 24px;
}

.submit-btn {
  border-radius: 10px;
  height: 40px;
  padding: 0 32px;
  background-color: #5C6B5F;
  border-color: #5C6B5F;
  font-weight: 600;
}

.submit-btn:hover {
  background-color: #4A574D !important;
  border-color: #4A574D !important;
}
</style>
