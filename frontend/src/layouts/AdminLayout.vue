<template>
  <a-layout class="admin-layout">
    <a-layout-sider width="260" theme="light" style="position: fixed; left: 0; top: 0; bottom: 0; z-index: 100">
      <div class="sider-content">
        <div class="logo-container">
          <BankFilled class="logo-icon" />
          <span class="logo-text">爪哇銀行</span>
        </div>

        <a-menu v-model:selectedKeys="selectedKeys" mode="inline">
          
          <a-menu-item key="admin-home" @click="$router.push({ name: 'admin-home' })">
            <template #icon><HomeOutlined /></template>
            <span>首頁</span>
          </a-menu-item>

          <a-menu-item-group title="客戶管理">
            <a-menu-item key="admin-customers" @click="$router.push({ name: 'admin-customers' })">
              <template #icon><UserOutlined /></template>
              <span>客戶管理</span>
            </a-menu-item>
          </a-menu-item-group>

          <a-menu-item-group title="帳戶管理">
            <a-menu-item key="admin-accounts" @click="$router.push({ name: 'admin-accounts' })">
              <template #icon><BankOutlined /></template>
              <span>帳戶管理</span>
            </a-menu-item>
            <a-menu-item key="admin-transfers" @click="$router.push({ name: 'admin-transfers' })">
              <template #icon><SwapOutlined /></template>
              <span>交易操作</span>
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
            
            <template v-if="isAdmin">
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
            </template>
          </a-menu-item-group>

          <template v-if="isAdmin">
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
  HomeOutlined, TeamOutlined, UserOutlined, SettingOutlined, BankFilled, LogoutOutlined,
  BankOutlined, SwapOutlined, ProfileOutlined, FileAddOutlined, AuditOutlined, 
  AppstoreAddOutlined, SolutionOutlined, CreditCardOutlined, AlertOutlined, StopOutlined
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 11-Role Permissions Logic
const adminRoles = ['CFDM', 'CSDM', 'CRDM', 'CRO', 'COO', 'ISSA', 'CISO', 'SYS_SUPER']
const isAdmin = computed(() => {
  return adminRoles.includes(authStore.user?.roleCode)
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

/* =========================================
   側邊欄 Logo 與背景樣式
========================================= */
/* 強制設定側邊欄背景為淡墨綠色，與純白內容區做區隔 */
:deep(.ant-layout-sider) {
  background-color: #f1f3f0 !important;
  /* 移除預設 padding，改用內部 sider-content 處理 */
}

/* 讓內部內容可滾動，隱藏原始滾動條保持美觀 */
.sider-content {
  height: 100vh;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px 15px 60px; /* 底部留白避免被摺疊按鈕擋住 */
}

/* 針對 webkit 瀏覽器自訂滾動條 */
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
  padding: 0 16px 32px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  font-size: 24px;
  color: #5C6B5F; 
}

.logo-text {
  font-weight: 700;
  font-size: 18px;
  letter-spacing: 0.5px;
  color: #4A574D;
}

/* =========================================
   側邊欄選單膠囊化設計 (強制覆蓋黑色)
========================================= */
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

/* 群組標題加上淺淺的橫線與優化字體 */
:deep(.ant-menu-item-group-title) {
  color: #8c9891 !important;
  font-size: 12px !important;
  font-weight: 600;
  margin-top: 16px;
  padding-left: 24px !important;
  border-top: 1px solid rgba(0, 0, 0, 0.04);
  padding-top: 16px;
}

/* 讓第一個群組不要有頂部邊界 */
:deep(.ant-menu-item-group:first-of-type .ant-menu-item-group-title) {
  border-top: none;
  margin-top: 8px;
  padding-top: 8px;
}

/* 確保選中的膠囊是墨綠色，絕對不會是黑色！ */
:deep(.ant-menu-item-selected) {
  background-color: #5C6B5F !important;
  color: #ffffff !important;
  box-shadow: 0 4px 12px rgba(92, 107, 95, 0.2) !important;
}

:deep(.ant-menu-item-selected .anticon) {
  color: #ffffff !important;
}

:deep(.ant-menu-title-content) {
  margin-inline-start: 12px !important;
  font-size: 15px;
}

/* 消除所有點擊時的藍色外框 */
:deep(.ant-menu-item:focus-visible),
:deep(.ant-menu-item:focus) {
  outline: none !important;
  box-shadow: none !important;
}

/* =========================================
   頂部導覽列與右側帳號區塊
========================================= */
.custom-header {
  height: 80px !important; 
  line-height: 80px !important;
  padding: 0 32px !important;
  background: transparent !important;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* 移除 Header 藍色 Hover */
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

/* =========================================
   右下角內容區塊
========================================= */
.admin-content {
  padding: 0 32px 32px;
  overflow-y: auto;
}

/* 頁面切換過場動畫 */
.fade-enter-active, .fade-leave-active { 
  transition: opacity 0.2s ease; 
}
.fade-enter-from, .fade-leave-to { 
  opacity: 0; 
}
</style>
