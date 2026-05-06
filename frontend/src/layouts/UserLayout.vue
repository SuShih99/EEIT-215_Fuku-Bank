<template>
  <a-layout style="min-height: 100vh; background: #f5f6fa">
    <!-- 頂部 Header -->
    <a-layout-header class="user-header">
      <div class="header-left" @click="$router.push({ name: 'user-home' })">
        <BankOutlined style="font-size: 24px; color: #1677ff" />
        <span class="brand-name">爪哇銀行</span>
      </div>

      <div class="header-right">
        <!-- 圓形大頭照 -->
        <div class="avatar-btn" @click="$router.push({ name: 'user-profile' })">
          <a-avatar
            :size="36"
            :src="avatarSrc"
            style="cursor: pointer"
          >
            <template #icon><UserOutlined /></template>
          </a-avatar>
        </div>
        <span class="welcome-text">歡迎，{{ customerName }}</span>
        <a-button size="small" @click="handleLogout">
          <LogoutOutlined />
          登出
        </a-button>
      </div>
    </a-layout-header>

    <!-- 主內容 -->
    <a-layout-content class="user-content">
      <router-view />
    </a-layout-content>
  </a-layout>
</template>

<script setup>
import { computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { BankOutlined, UserOutlined, LogoutOutlined } from '@ant-design/icons-vue'
import { useCustomerAuthStore } from '@/stores/customerAuth'
import { BASE_URL } from '@/api/axios'

const router = useRouter()
const customerAuthStore = useCustomerAuthStore()

const customerName = computed(() => customerAuthStore.customer?.name || '會員')

const avatarSrc = computed(() => {
  const url = customerAuthStore.customer?.avatarUrl
  if (!url) return null
  // 如果是相對路徑，加上後端 base URL
  return url.startsWith('http') ? url : BASE_URL + url
})

function handleLogout() {
  customerAuthStore.clearCustomer()
  message.success('已登出')
  router.push({ name: 'user-login' })
}

let logoutTimer = null

onMounted(() => {
  // 每 5 分鐘提示一次是否登出 (300,000 ms)
  logoutTimer = setInterval(() => {
    Modal.confirm({
      title: '登出提醒',
      content: '您已登入一段時間，是否需要登出？',
      okText: '登出',
      cancelText: '繼續使用',
      onOk() {
        handleLogout()
      },
    })
  }, 300000)
})

onUnmounted(() => {
  if (logoutTimer) clearInterval(logoutTimer)
})
</script>

<style scoped>
.user-header {
  background: #fff;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
  height: 56px;
  line-height: 56px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.brand-name {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  letter-spacing: 2px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.avatar-btn {
  cursor: pointer;
  transition: transform 0.2s;
}

.avatar-btn:hover {
  transform: scale(1.08);
}

.welcome-text {
  font-size: 14px;
  color: #333;
}

.user-content {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
}
</style>
