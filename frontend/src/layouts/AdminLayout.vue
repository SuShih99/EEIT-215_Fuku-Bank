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
            </a-menu-item-group>

            <a-menu-item-group title="信用卡業務">
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
              <a-menu-item key="admin-card-txns" @click="$router.push({ name: 'admin-card-txns' })">
                <template #icon><ProfileOutlined /></template>
                <span>信用卡交易管理</span>
              </a-menu-item>
              <a-menu-item key="admin-card-bills" @click="$router.push({ name: 'admin-card-bills' })">
                <template #icon><CreditCardOutlined /></template>
                <span>信用卡帳單管理</span>
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
          <div class="admin-timer">
            <span class="timer-text">剩餘登出時間: {{ formatTime(countdown) }}</span>
            <a-button size="small" :type="isTimerPaused ? 'primary' : 'default'" @click="isTimerPaused = !isTimerPaused">
              {{ isTimerPaused ? '繼續' : '暫停' }}
            </a-button>
            <a-button size="small" @click="triggerIdleAlert">Demo Alert</a-button>
          </div>
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
import { Modal } from 'ant-design-vue'
import { onMounted, onUnmounted } from 'vue'
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
const countdown = ref(300)
const isTimerPaused = ref(false)

watch(() => route.name, (val) => {
  selectedKeys.value = [val]
})

function formatTime(seconds) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

function triggerIdleAlert() {
  Modal.info({
    title: '您還在線嗎？',
    content: '系統偵測到您已登入一段時間，請確認是否繼續使用。',
    okText: '還在，繼續使用',
    centered: true,
    maskClosable: false,
    onOk: () => {
      countdown.value = 300 // 重設倒數
    }
  })
}

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

let idleTimer = null

onMounted(() => {
  // 每 1 秒更新倒數
  idleTimer = setInterval(() => {
    if (isTimerPaused.value) return
    
    if (countdown.value > 0) {
      countdown.value--
    } else {
      triggerIdleAlert()
      countdown.value = 300
    }
  }, 1000)
})

onUnmounted(() => {
  if (idleTimer) clearInterval(idleTimer)
})
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
  gap: 20px;
  border-style: solid !important;
  border-color: transparent !important;
}

.admin-timer {
  display: flex;
  align-items: center;
  gap: 12px;
  background: rgba(92, 107, 95, 0.05);
  padding: 6px 16px;
  border-radius: 6px;
  border: 1px solid rgba(92, 107, 95, 0.3);
  line-height: normal;
}

.timer-text {
  font-size: 14px;
  color: #5C6B5F;
  font-weight: 600;
  min-width: 100px;
  white-space: nowrap;
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
  padding: 24px 32px 32px;
  overflow-y: auto;
}

.fade-enter-active, .fade-leave-active { 
  transition: opacity 0.2s ease; 
}
.fade-enter-from, .fade-leave-to { 
  opacity: 0; 
}
</style>