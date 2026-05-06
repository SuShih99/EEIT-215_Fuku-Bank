<template>
  <div class="register-wrapper">
    <div class="register-card">
      <!-- Logo 區 -->
      <div class="register-header">
        <div class="logo-icon">
          <BankOutlined style="font-size: 32px; color: #1677ff" />
        </div>
        <h1 class="register-title">註冊</h1>
        <p class="register-subtitle">加入爪哇銀行，開啟您的數位金融旅程</p>
      </div>

      <!-- 註冊表單 -->
      <a-form :model="form" layout="vertical" @finish="handleRegister" autocomplete="off">
        <div class="form-grid">
          <a-form-item
            label="姓名"
            name="name"
            :rules="[{ required: true, message: '請輸入姓名' }]"
          >
            <a-input v-model:value="form.name" placeholder="請輸入姓名" />
          </a-form-item>

          <a-form-item
            label="生日"
            name="birthday"
            :rules="[{ required: true, message: '請選擇生日' }]"
          >
            <a-date-picker
              v-model:value="form.birthday"
              style="width: 100%"
              value-format="YYYY-MM-DD"
              placeholder="請選擇生日"
            />
          </a-form-item>

          <a-form-item
            label="性別"
            name="gender"
            :rules="[{ required: true, message: '請選擇性別' }]"
          >
            <a-select v-model:value="form.gender" placeholder="請選擇性別">
              <a-select-option value="M">男</a-select-option>
              <a-select-option value="F">女</a-select-option>
            </a-select>
          </a-form-item>

          <a-form-item
            label="身分證字號"
            name="idNumber"
            :rules="[{ required: true, message: '請輸入身分證字號' }]"
          >
            <a-input v-model:value="form.idNumber" placeholder="例如：A123456789" />
          </a-form-item>

          <a-form-item
            label="使用者帳號"
            name="username"
            :rules="[{ required: true, message: '請輸入使用者帳號' }]"
          >
            <a-input v-model:value="form.username" placeholder="請輸入帳號（英文字母或數字）" />
          </a-form-item>

          <a-form-item
            label="密碼"
            name="password"
            :rules="[{ required: true, message: '請輸入密碼' }]"
          >
            <a-input-password v-model:value="form.password" placeholder="請輸入密碼" />
          </a-form-item>

          <a-form-item
            label="手機號碼"
            name="phone"
            :rules="[{ required: true, message: '請輸入手機號碼' }]"
            class="full-width"
          >
            <a-input v-model:value="form.phone" placeholder="例如：0912345678" />
          </a-form-item>

          <a-form-item
            label="信箱"
            name="email"
            :rules="[
              { required: true, message: '請輸入信箱' },
              { type: 'email', message: '信箱格式不正確' },
            ]"
            class="full-width"
          >
            <a-input v-model:value="form.email" placeholder="請輸入電子信箱" />
          </a-form-item>

          <a-form-item
            label="地址"
            name="address"
            :rules="[{ required: true, message: '請輸入地址' }]"
            class="full-width"
          >
            <a-input v-model:value="form.address" placeholder="請輸入通訊地址" />
          </a-form-item>
        </div>

        <a-form-item style="margin-top: 8px">
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            block
            :loading="loading"
          >
            註冊
          </a-button>
        </a-form-item>
      </a-form>

      <div class="login-link">
        已有帳號？<router-link to="/login">立即登入</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { BankOutlined } from '@ant-design/icons-vue'
import { customerRegister } from '@/api/customerAuth'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  name: '',
  birthday: null,
  gender: undefined,
  idNumber: '',
  username: '',
  password: '',
  phone: '',
  email: '',
  address: '',
})

async function handleRegister() {
  loading.value = true
  try {
    await customerRegister({
      name: form.name,
      birthday: form.birthday,
      gender: form.gender,
      idNumber: form.idNumber,
      username: form.username,
      password: form.password,
      phone: form.phone,
      email: form.email,
      address: form.address,
    })
    message.success('註冊成功！請登入您的帳號')
    router.push('/login')
  } catch (err) {
    message.error(err.response?.data?.message || '註冊失敗，請檢查輸入資料')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
  padding: 40px 16px;
}

.register-card {
  width: 560px;
  max-width: 100%;
  padding: 36px 32px 24px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.register-header {
  text-align: center;
  margin-bottom: 28px;
}

.logo-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e6f4ff;
  border-radius: 50%;
}

.register-title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: #1a1a2e;
}

.register-subtitle {
  margin: 4px 0 0;
  font-size: 14px;
  color: #8c8c8c;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
}

.form-grid .full-width {
  grid-column: 1 / -1;
}

.login-link {
  text-align: center;
  margin-top: 12px;
  font-size: 14px;
  color: #8c8c8c;
}

.login-link a {
  color: #1677ff;
  text-decoration: none;
  font-weight: 500;
}

.login-link a:hover {
  text-decoration: underline;
}
</style>
