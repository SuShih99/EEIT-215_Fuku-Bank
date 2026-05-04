<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider
      v-model:collapsed="collapsed"
      collapsible
      :width="200"
      :collapsed-width="80"
      style="position: fixed; left: 0; top: 0; bottom: 0; z-index: 100"
    >
      <div style="height: 32px; margin: 16px; color: white; text-align: center; font-size: 18px">
        {{ collapsed ? '爪哇' : '爪哇銀行' }}
      </div>
      <a-menu theme="dark" mode="inline" :selected-keys="selectedKeys">
        <a-menu-item key="admin-home" @click="$router.push('/admin')">
          <HomeOutlined />
          <span>首頁</span>
        </a-menu-item>
        <a-menu-item key="admin-accounts" @click="$router.push('/admin/accounts')">
          <BankOutlined />
          <span>帳戶管理</span>
        </a-menu-item>
        <a-menu-item key="admin-transfers" @click="$router.push('/admin/transfers')">
          <SwapOutlined />
          <span>交易操作</span>
        </a-menu-item>
        <a-menu-item key="admin-trans-logs" @click="$router.push('/admin/trans-logs')">
          <FileTextOutlined />
          <span>交易紀錄</span>
        </a-menu-item>
        <a-menu-item key="admin-loan-apply" @click="$router.push('/admin/loan-apply')">
          <FileTextOutlined />
          <span>貸款申請(測試用)</span>
        </a-menu-item>
        <a-menu-item
          key="admin-loan-applications"
          @click="$router.push('/admin/loan-applications')"
        >
          <FileTextOutlined />
          <span>貸款申請管理</span>
        </a-menu-item>
        <a-menu-item key="admin-employees" @click="$router.push('/admin/employees')">
          <TeamOutlined />
          <span>員工管理</span>
        </a-menu-item>
        <a-menu-item key="admin-customers" @click="$router.push('/admin/customers')">
          <UserOutlined />
          <span>客戶管理</span>
        </a-menu-item>
        <a-menu-item key="admin-risk-events" @click="$router.push('/admin/risk-events')">
          <FileTextOutlined />
          <span>風險事件</span>
        </a-menu-item>
        <a-menu-item key="admin-blacklist" @click="$router.push('/admin/blacklist')">
          <FileTextOutlined />
          <span>黑名單</span>
        </a-menu-item>
        <a-menu-item key="admin-card-types" @click="$router.push('/admin/card-types')">
          <FileTextOutlined />
          <span>卡別管理</span>
        </a-menu-item>
        <a-menu-item key="admin-card-applications" @click="$router.push('/admin/card-applications')">
          <FileTextOutlined />
          <span>信用卡申請管理</span>
        </a-menu-item>
        <a-menu-item key="admin-cards" @click="$router.push('/admin/cards')">
          <FileTextOutlined />
          <span>卡片管理</span>
        </a-menu-item>


      </a-menu>
    </a-layout-sider>

    <a-layout :style="{ marginLeft: collapsed ? '80px' : '200px', transition: 'margin-left 0.2s' }">
      <a-layout-header style="background: #fff; padding: 0 24px; display: flex; align-items: center; justify-content: space-between">
        <span style="font-size: 16px">管理端</span>
        <!-- 使用者資訊 & 登出 -->
        <div v-if="authStore.isLoggedIn" style="display: flex; align-items: center; gap: 12px">
          <a-tag color="blue">{{ authStore.user?.roleCode }}</a-tag>
          <span style="font-size: 14px; color: #333">{{ authStore.user?.empName }}</span>
          <a-button size="small" @click="handleLogout">
            <LogoutOutlined />
            登出
          </a-button>
        </div>
      </a-layout-header>
      <a-layout-content style="margin: 16px">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  HomeOutlined,
  BankOutlined,
  SwapOutlined,
  FileTextOutlined,
  TeamOutlined,
  UserOutlined,
  LogoutOutlined,
} from '@ant-design/icons-vue'
import { logout } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const collapsed = ref(false)
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const selectedKeys = computed(() => [route.name])

async function handleLogout() {
  try {
    await logout()
  } catch {
    // 即使後端登出失敗，前端也要清除狀態
  }
  authStore.clearUser()
  message.success('已登出')
  router.push({ name: 'admin-login' })
}
</script>
