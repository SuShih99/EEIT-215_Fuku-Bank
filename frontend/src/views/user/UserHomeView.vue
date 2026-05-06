<template>
  <div class="user-home">
    <!-- 歡迎卡片 -->
    <div class="welcome-card">
      <div class="welcome-left">
        <h1>{{ greeting }}，{{ customerName }}</h1>
        <p class="welcome-sub">歡迎使用爪哇銀行網路服務</p>
      </div>
      <div class="welcome-avatar" @click="$router.push({ name: 'user-profile' })">
        <a-avatar :size="72" :src="avatarSrc">
          <template #icon><UserOutlined style="font-size: 36px" /></template>
        </a-avatar>
      </div>
    </div>

    <!-- 快捷功能 -->
    <div class="section-title">我的服務</div>
    <div class="service-grid">
      <div
        v-for="item in services"
        :key="item.label"
        class="service-card"
        @click="handleServiceClick(item)"
      >
        <div class="service-icon" :style="{ background: item.bg }">
          <component :is="item.icon" :style="{ color: item.color, fontSize: '28px' }" />
        </div>
        <div class="service-label">{{ item.label }}</div>
        <div class="service-desc">{{ item.desc }}</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  UserOutlined,
  BankOutlined,
  SwapOutlined,
  CreditCardOutlined,
  SafetyCertificateOutlined,
  FileTextOutlined,
  DollarOutlined,
} from '@ant-design/icons-vue'
import { useCustomerAuthStore } from '@/stores/customerAuth'
import { BASE_URL } from '@/api/axios'

const router = useRouter()
const customerAuthStore = useCustomerAuthStore()

const customerName = computed(() => customerAuthStore.customer?.name || '會員')

const avatarSrc = computed(() => {
  const url = customerAuthStore.customer?.avatarUrl
  if (!url) return null
  return url.startsWith('http') ? url : BASE_URL + url
})

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 12) return '早安'
  if (h < 18) return '午安'
  return '晚安'
})

const services = [
  { label: '個人資料', desc: '查看與修改個人資訊', icon: UserOutlined, bg: '#e6f7ff', color: '#1890ff', route: 'user-profile' },
  { label: '帳戶總覽', desc: '查看我的帳戶', icon: BankOutlined, bg: '#f6ffed', color: '#52c41a', route: null },
  { label: '轉帳匯款', desc: '國內轉帳服務', icon: SwapOutlined, bg: '#fff7e6', color: '#fa8c16', route: null },
  { label: '信用卡', desc: '卡片申辦與管理', icon: CreditCardOutlined, bg: '#f9f0ff', color: '#722ed1', route: null },
  { label: '貸款服務', desc: '線上貸款申請', icon: DollarOutlined, bg: '#e6fffb', color: '#13c2c2', route: null },
  { label: '安全中心', desc: '密碼修改與安全設定', icon: SafetyCertificateOutlined, bg: '#fff1f0', color: '#f5222d', route: 'user-profile' },
]

function handleServiceClick(item) {
  if (item.route) {
    router.push({ name: item.route })
  } else {
    message.info('此功能即將推出，敬請期待！')
  }
}
</script>

<style scoped>
.user-home {
  max-width: 900px;
}

.welcome-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 32px 36px;
  margin-bottom: 32px;
  color: #fff;
}

.welcome-left h1 {
  color: #fff;
  font-size: 26px;
  margin: 0 0 8px 0;
  font-weight: 600;
}

.welcome-sub {
  color: rgba(255, 255, 255, 0.75);
  font-size: 15px;
  margin: 0;
}

.welcome-avatar {
  cursor: pointer;
  transition: transform 0.2s;
}

.welcome-avatar:hover {
  transform: scale(1.08);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 16px;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.service-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px 20px;
  text-align: center;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: all 0.25s;
}

.service-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.service-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
}

.service-label {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 4px;
}

.service-desc {
  font-size: 12px;
  color: #8c8c8c;
}

@media (max-width: 700px) {
  .service-grid { grid-template-columns: repeat(2, 1fr); }
  .welcome-card { flex-direction: column; text-align: center; gap: 16px; }
}
</style>
