<template>
  <a-layout class="admin-layout">
    <a-layout-sider width="260" theme="light" style="position: fixed; left: 0; top: 0; bottom: 0; z-index: 100">
      <div class="sider-content">
        <div class="logo-container">
          <img src="/logo.png" alt="JavaBank" class="logo-img" @click="$router.push({ name: 'admin-home' })" style="cursor: pointer;" />
        </div>

        <a-menu v-model:selectedKeys="selectedKeys" mode="inline">
          
          <a-menu-item key="admin-home" @click="$router.push({ name: 'admin-home' })">
            <template #icon><HomeOutlined /></template>
            <span>首頁</span>
          </a-menu-item>

          <template v-if="!isCISO">
            <a-menu-item-group title="客戶管理">
              <a-menu-item key="admin-customers" @click="$router.push({ name: 'admin-customers' })">
                <template #icon><UserOutlined /></template>
                <span>客戶管理</span>
              </a-menu-item>
            </a-menu-item-group>

            <a-menu-item-group title="帳戶管理">
              <a-menu-item key="admin-account-applications" @click="$router.push({ name: 'admin-account-applications' })">
                <template #icon><SolutionOutlined /></template>
                <span>開戶申請審核</span>
              </a-menu-item>
              <a-menu-item key="admin-accounts" @click="$router.push({ name: 'admin-accounts' })">
                <template #icon><BankOutlined /></template>
                <span>帳戶管理</span>
              </a-menu-item>
              <a-menu-item key="admin-trans-logs" @click="$router.push({ name: 'admin-trans-logs' })">
                <template #icon><ProfileOutlined /></template>
                <span>交易紀錄</span>
              </a-menu-item>
            </a-menu-item-group>

            <a-menu-item-group title="消金貸款業務">
              <a-menu-item key="loan-apply" @click="$router.push({ name: 'loan-apply' })">
                <template #icon><FileAddOutlined /></template>
                <span>貸款進件申請</span>
              </a-menu-item>
              <a-menu-item key="loan-applications" @click="$router.push({ name: 'loan-applications' })">
                <template #icon><AuditOutlined /></template>
                <span>貸款申請管理</span>
              </a-menu-item>
              <a-menu-item key="admin-card-types" @click="$router.push({ name: 'admin-card-types' })">
                <template #icon><AppstoreAddOutlined /></template>
                <span>信用卡卡別管理</span>
              </a-menu-item>
              <a-menu-item key="admin-card-applications" @click="$router.push({ name: 'admin-card-applications' })">
                <template #icon><SolutionOutlined /></template>
                <span>信用卡開卡審核</span>
              </a-menu-item>
              <a-menu-item key="admin-cards" @click="$router.push({ name: 'admin-cards' })">
                <template #icon><CreditCardOutlined /></template>
                <span>信用卡卡片管理</span>
              </a-menu-item>
            </a-menu-item-group>

            <a-menu-item-group title="風險管理">
              <a-menu-item key="admin-risk-events" @click="$router.push({ name: 'admin-risk-events' })">
                <template #icon><AlertOutlined /></template>
                <span>風險事件</span>
              </a-menu-item>
              <a-menu-item key="admin-blacklist" @click="$router.push({ name: 'admin-blacklist' })">
                <template #icon><StopOutlined /></template>
                <span>黑名單</span>
              </a-menu-item>
            </a-menu-item-group>
          </template>

          <template v-if="isAdmin || isCISO">
            <a-menu-item-group title="系統管理">
              <a-menu-item key="admin-employees" @click="$router.push({ name: 'admin-employees' })">
                <template #icon><TeamOutlined /></template>
                <span>員工管理</span>
              </a-menu-item>
              <a-menu-item key="admin-logs" @click="$router.push({ name: 'admin-logs' })">
                <template #icon><SettingOutlined /></template>
                <span>系統日誌</span>
              </a-menu-item>
            </a-menu-item-group>
          </template>

        </a-menu>
      </div>
    </a-layout-sider>

    <a-layout :style="{ marginLeft: '260px' }">
      <a-layout-header class="custom-header">
        <div class="header-search">
        </div>
        
        <div class="header-right">
          <a-tag class="custom-role-tag">
            {{ authStore.user?.roleCode || 'ROLE' }}
          </a-tag>
          <span class="user-name">{{ authStore.user?.empName || '員工姓名' }}</span>
          <a-button shape="round" class="logout-btn" @click="handleLogout">
            <LogoutOutlined /> 登出
          </a-button>
        </div>
      </a-layout-header>

      <a-layout-content class="admin-content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { logout } from '@/api/auth'
import { 
  HomeOutlined, TeamOutlined, UserOutlined, SettingOutlined, LogoutOutlined,
  BankOutlined, ProfileOutlined, FileAddOutlined, AuditOutlined,
  AppstoreAddOutlined, SolutionOutlined, CreditCardOutlined, AlertOutlined, StopOutlined
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isCISO = computed(() => authStore.user?.roleCode === 'CISO')
// 排除 CISO 讓其走獨立邏輯，其他需看到系統管理的管理員可在此定義
const isAdmin = computed(() => {
  const adminRoles = ['ISSA', 'SYS_SUPER', 'SYS_STAFF'] 
  return adminRoles.includes(authStore.user?.roleCode) && !isCISO.value
})

const selectedKeys = ref([route.name])

watch(() => route.name, (val) => {
  selectedKeys.value = [val]
})

async function handleLogout() {
  try {
    await logout()
  } catch (err) {
    console.error('Logout error:', err)
  } finally {
    authStore.clearUser()
    router.push('/admin/login')
  }
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

:deep(.ant-layout-sider) {
  background-color: #f1f3f0 !important;
}

.sider-content {
  height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px 15px 60px;
}

.sider-content::-webkit-scrollbar {
  width: 6px;
}
.sider-content::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
}
.sider-content::-webkit-scrollbar-track {
  background: transparent;
}

.logo-container {
  padding: 8px 16px 24px;
  display: flex;
  justify-content: center;
}

.logo-img {
  width: 120px;
  height: 120px;
  object-fit: contain;
}

.logo-text {
  font-weight: 700;
  font-size: 18px;
  letter-spacing: 0.5px;
  color: #4A574D;
}

:deep(.ant-menu) {
  background: transparent !important;
  border-inline-end: none !important;
}

:deep(.ant-menu-item) {
  height: 48px !important;
  line-height: 48px !important;
  margin-bottom: 8px !important;
  color: #4a4a4a !important;
  font-weight: 500;
  border-radius: 24px !important;
  transition: all 0.3s ease;
}

:deep(.ant-menu-item-group-title) {
  color: #8c9891 !important;
  font-size: 12px !important;
  font-weight: 600;
  margin-top: 16px;
  padding-left: 24px !important;
  border-top: 1px solid rgba(0, 0, 0, 0.04);
  padding-top: 16px;
}

:deep(.ant-menu-item-group:first-of-type .ant-menu-item-group-title) {
  border-top: none;
  margin-top: 8px;
  padding-top: 8px;
}

:deep(.ant-menu-item-selected) {
  background-color: #5C6B5F !important;
  color: #ffffff !important;
  box-shadow: 0 4px 12px rgba(92, 107, 95, 0.2) !important;
}

:deep(.ant-menu-item-selected .anticon) {
  color: #ffffff !important;
}

:deep(.ant-menu-item .anticon) {
  font-size: 20px !important;
}

:deep(.ant-menu-title-content) {
  margin-inline-start: 12px !important;
  font-size: 15px;
}

:deep(.ant-menu-item:focus-visible),
:deep(.ant-menu-item:focus) {
  outline: none !important;
  box-shadow: none !important;
}

.custom-header {
  height: 80px !important; 
  line-height: 80px !important;
  padding: 0 32px !important;
  background: transparent !important;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.custom-header a:hover,
.custom-header .anticon:hover {
  color: #5C6B5F !important;
}

.custom-header *:focus {
  outline: none !important;
  box-shadow: none !important;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  border-style: solid !important;
  border-color: transparent !important;
}

.custom-role-tag {
  background-color: rgba(92, 107, 95, 0.1) !important;
  color: #5C6B5F !important;
  border: 1px solid rgba(92, 107, 95, 0.3) !important;
  font-size: 14px !important;
  font-weight: 600;
  padding: 4px 12px !important;
  border-radius: 6px !important;
  margin: 0;
}

.user-name {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.logout-btn {
  height: 38px;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.admin-content {
  padding: 0 32px 32px;
  overflow-y: auto;
}

.fade-enter-active, .fade-leave-active { 
  transition: opacity 0.2s ease; 
}
.fade-enter-from, .fade-leave-to { 
  opacity: 0; 
}
</style>