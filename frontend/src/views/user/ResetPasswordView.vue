<template>
  <div class="reset-wrapper">
    <div class="reset-card">
      <div class="reset-header">
        <div class="logo-icon">
          <LockOutlined style="font-size: 32px; color: #1677ff" />
        </div>
        <h1 class="reset-title">重設密碼</h1>
        <p class="reset-subtitle">請輸入您的新密碼</p>
      </div>

      <a-form :model="form" layout="vertical" @finish="handleReset" autocomplete="off">
        <a-form-item
          label="新密碼"
          name="newPassword"
          :rules="[
            { required: true, message: '請輸入新密碼' },
            { min: 6, message: '密碼至少 6 個字元' },
          ]"
        >
          <a-input-password v-model:value="form.newPassword" size="large" placeholder="請輸入新密碼" />
        </a-form-item>

        <a-form-item
          label="確認新密碼"
          name="confirmPassword"
          :rules="[
            { required: true, message: '請再次輸入新密碼' },
            { validator: validateConfirm },
          ]"
        >
          <a-input-password v-model:value="form.confirmPassword" size="large" placeholder="請再次輸入新密碼" />
        </a-form-item>

        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" block :loading="loading">
            確認重設
          </a-button>
        </a-form-item>
      </a-form>

      <a-result
        v-if="success"
        status="success"
        title="密碼已成功重設"
        sub-title="請使用新密碼登入"
      >
        <template #extra>
          <a-button type="primary" @click="$router.push('/login')">前往登入</a-button>
        </template>
      </a-result>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { LockOutlined } from '@ant-design/icons-vue'
import { customerResetPassword } from '@/api/customerAuth'

const route = useRoute()
const loading = ref(false)
const success = ref(false)

const form = reactive({
  newPassword: '',
  confirmPassword: '',
})

function validateConfirm(_rule, value) {
  if (value && value !== form.newPassword) {
    return Promise.reject('兩次輸入的密碼不一致')
  }
  return Promise.resolve()
}

async function handleReset() {
  const token = route.query.token
  if (!token) {
    message.error('無效的重設連結')
    return
  }

  loading.value = true
  try {
    await customerResetPassword({
      token,
      newPassword: form.newPassword,
    })
    success.value = true
  } catch (err) {
    message.error(err.response?.data?.message || '密碼重設失敗')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.reset-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
}

.reset-card {
  width: 420px;
  padding: 40px 32px 28px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
}

.reset-header {
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

.reset-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
}

.reset-subtitle {
  margin: 4px 0 0;
  font-size: 14px;
  color: #8c8c8c;
}
</style>
