<template>
  <div class="customer-page verify-page">
    <main class="verify-container">
      <section class="verify-card jb-card">
        <div class="verify-mark" :class="statusClass">
          <span v-if="status === 'loading'">...</span>
          <span v-else-if="status === 'success'">✓</span>
          <span v-else>!</span>
        </div>

        <p class="verify-eyebrow">Email Verification</p>
        <h1 class="verify-title">{{ title }}</h1>
        <p class="verify-text">{{ description }}</p>

        <div class="verify-actions">
          <button class="jb-btn jb-btn-primary jb-btn-lg" @click="goPrimary">
            {{ primaryLabel }}
          </button>
          <button class="jb-btn jb-btn-secondary jb-btn-lg" @click="router.push('/')">
            返回首頁
          </button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { customerVerifyEmail } from '@/api/customerAuth'

const route = useRoute()
const router = useRouter()
const status = ref('loading')
const description = ref('正在確認您的電子郵件驗證連結，請稍候。')

const title = computed(() => {
  if (status.value === 'success') return '電子郵件驗證完成'
  if (status.value === 'error') return '驗證連結無法使用'
  return '正在驗證信箱'
})

const primaryLabel = computed(() => status.value === 'success' ? '前往登入' : '返回註冊')
const statusClass = computed(() => `mark-${status.value}`)

function goPrimary() {
  if (status.value === 'success') {
    router.push('/login')
    return
  }
  router.push('/register')
}

onMounted(async () => {
  const token = typeof route.query.token === 'string' ? route.query.token : ''
  if (!token) {
    status.value = 'error'
    description.value = '驗證連結缺少必要資訊，請重新註冊或重新索取驗證信。'
    return
  }

  try {
    const res = await customerVerifyEmail(token)
    status.value = 'success'
    description.value = res.data?.data || '您的信箱已成功完成驗證，現在可以登入並開始使用服務。'
  } catch (err) {
    status.value = 'error'
    description.value = err.response?.data?.message || '此驗證連結可能已失效或已被使用，請重新索取驗證信。'
  }
})
</script>

<style scoped>
.verify-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background:
    radial-gradient(circle at top right, rgba(176, 187, 170, 0.16), transparent 32%),
    var(--bg-primary);
}

.verify-container {
  width: 100%;
  max-width: 560px;
}

.verify-card {
  text-align: center;
  padding: 56px 40px;
}

.verify-mark {
  width: 78px;
  height: 78px;
  margin: 0 auto 20px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
  font-weight: 700;
}

.mark-loading {
  color: var(--primary-dark);
  background: rgba(109, 125, 108, 0.12);
}

.mark-success {
  color: #48604a;
  background: rgba(109, 125, 108, 0.16);
}

.mark-error {
  color: #a05847;
  background: rgba(196, 134, 117, 0.14);
}

.verify-eyebrow {
  margin: 0 0 8px;
  font-size: 13px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-secondary);
}

.verify-title {
  margin: 0 0 14px;
}

.verify-text {
  margin: 0 auto;
  max-width: 420px;
  line-height: 1.9;
  color: var(--text-secondary);
}

.verify-actions {
  margin-top: 28px;
  display: flex;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 600px) {
  .verify-card {
    padding: 40px 24px;
  }

  .verify-actions {
    flex-direction: column;
  }

  .verify-actions .jb-btn {
    width: 100%;
  }
}
</style>
