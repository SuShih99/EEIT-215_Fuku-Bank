<template>
  <div class="customer-page home-page">
    <!-- 歡迎卡片 -->
    <section class="welcome-card" aria-label="歡迎">
      <div class="welcome-left">
        <p class="welcome-greeting">{{ greeting }}，</p>
        <h1 class="welcome-name">{{ customerName }}</h1>
        <p class="welcome-sub">歡迎使用爪哇銀行網路服務</p>
      </div>
      <div class="welcome-right">
        <button
          class="welcome-avatar"
          aria-label="前往會員中心"
          @click="$router.push({ name: 'user-profile' })"
        >
          <img v-if="avatarSrc" :src="avatarSrc" class="avatar-img" alt="使用者大頭照" />
          <span v-else class="avatar-fallback" aria-hidden="true">{{ customerInitial }}</span>
        </button>
        <div class="welcome-ink" aria-hidden="true"></div>
      </div>
    </section>

    <!-- 快捷服務 -->
    <section aria-label="我的服務">
      <h2 class="section-title">我的服務</h2>
      <div class="service-grid">
        <article
          v-for="item in services"
          :key="item.label"
          class="service-card"
          tabindex="0"
          role="button"
          :aria-label="item.label"
          @click="handleServiceClick(item)"
          @keydown.enter="handleServiceClick(item)"
        >
          <div class="service-icon" v-html="item.svg" aria-hidden="true"></div>
          <h3 class="service-label">{{ item.label }}</h3>
          <p class="service-desc">{{ item.desc }}</p>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCustomerAuthStore } from '@/stores/customerAuth'
import { BASE_URL } from '@/api/axios'

const router = useRouter()
const customerAuthStore = useCustomerAuthStore()

const customerName = computed(() => customerAuthStore.customer?.name || '會員')
const customerInitial = computed(() => (customerAuthStore.customer ?.name || '會')[0])

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
  {
    label: '個人資料',
    desc: '查看與修改個人資訊',
    svg: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>',
    route: 'user-profile',
  },
  {
    label: '帳戶總覽',
    desc: '查看我的帳戶餘額',
    svg: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="M2 10h20"/></svg>',
    route: null,
  },
  {
    label: '轉帳匯款',
    desc: '國內轉帳服務',
    svg: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M7 17l9.2-9.2M17 17V7H7"/></svg>',
    route: null,
  },
  {
    label: '信用卡',
    desc: '卡片申辦與管理',
    svg: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="4" width="22" height="16" rx="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>',
    route: null,
  },
  {
    label: '貸款服務',
    desc: '線上貸款申請',
    svg: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>',
    route: null,
  },
  {
    label: '安全中心',
    desc: '密碼修改與安全設定',
    svg: '<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>',
    route: 'user-profile',
  },
]

function handleServiceClick(item) {
  if (item.route) {
    router.push({ name: item.route })
  } else {
    alert('此功能即將推出，敬請期待！')
  }
}
</script>

<style scoped>
.home-page {
  max-width: 900px;
}

/* === Welcome === */
.welcome-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--bg-secondary);
  border-radius: var(--radius-lg);
  padding: var(--space-6) var(--space-6);
  margin-bottom: var(--space-6);
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(214, 206, 195, 0.5);
}

.welcome-greeting {
  font-size: var(--text-body);
  color: var(--text-secondary);
  margin-bottom: var(--space-1);
}

.welcome-name {
  font-size: 30px;
  margin-bottom: var(--space-2);
  letter-spacing: 2px;
}

.welcome-sub {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.welcome-right {
  position: relative;
}

.welcome-avatar {
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  border-radius: 50%;
  position: relative;
  z-index: 1;
  transition: transform var(--duration) var(--ease);
}

.welcome-avatar:hover { transform: scale(1.06); }

.avatar-img {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid rgba(214, 206, 195, 0.6);
  display: block;
}

.avatar-fallback {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-card);
  color: var(--primary);
  font-family: var(--font-heading);
  font-size: 28px;
  font-weight: 600;
  border: 3px solid rgba(214, 206, 195, 0.6);
}

.welcome-ink {
  position: absolute;
  top: -40px;
  right: -40px;
  width: 160px;
  height: 160px;
  background: radial-gradient(circle, rgba(92, 107, 95, 0.08) 0%, transparent 70%);
  border-radius: 50%;
}

/* === Services === */
.section-title {
  font-family: var(--font-heading);
  font-size: var(--text-h3);
  font-weight: 600;
  margin-bottom: var(--space-4);
  letter-spacing: 3px;
}

.service-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-3);
}

.service-card {
  background: var(--bg-card);
  border: 1px solid rgba(214, 206, 195, 0.4);
  border-radius: var(--radius-md);
  padding: var(--space-4) var(--space-3);
  text-align: center;
  cursor: pointer;
  transition: transform var(--duration) var(--ease),
              box-shadow var(--duration) var(--ease),
              border-color var(--duration) var(--ease);
}

.service-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
  border-color: var(--primary);
}

.service-card:active {
  transform: translateY(-1px);
  box-shadow: var(--shadow-sm);
}

.service-icon {
  width: 52px;
  height: 52px;
  margin: 0 auto var(--space-3);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary);
  background: var(--primary-light);
  border-radius: 50%;
}

.service-label {
  font-size: var(--text-body);
  margin-bottom: var(--space-1);
}

.service-desc {
  font-size: var(--text-xs);
  color: var(--text-secondary);
}

@media (max-width: 700px) {
  .service-grid { grid-template-columns: repeat(2, 1fr); }
  .welcome-card {
    flex-direction: column;
    text-align: center;
    gap: var(--space-4);
    padding: var(--space-4);
  }
  .welcome-name { font-size: var(--text-h2); }
}
</style>
