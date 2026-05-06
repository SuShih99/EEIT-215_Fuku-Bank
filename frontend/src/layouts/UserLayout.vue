<template>
  <div class="customer-page user-layout">
    <!-- 頂部導覽列 -->
    <header class="user-header">
      <div class="header-inner">
        <a class="header-left" href="/user/home" @click.prevent="$router.push({ name: 'user-home' })">
          <div class="jb-stamp" style="width:36px;height:36px;font-size:13px;border-width:1.5px;">福</div>
          <span class="brand-name">JAVA_BANK</span>
        </a>

        <nav class="header-right" aria-label="使用者導覽">
          <button
            class="avatar-btn"
            :aria-label="'前往會員中心'"
            @click="$router.push({ name: 'user-profile' })"
          >
            <img
              v-if="avatarSrc"
              :src="avatarSrc"
              class="user-avatar"
              alt="使用者大頭照"
            />
            <span v-else class="user-avatar avatar-placeholder" aria-hidden="true">
              {{ customerInitial }}
            </span>
          </button>
          <span class="welcome-text">{{ customerName }}</span>
          <button class="jb-btn jb-btn-secondary jb-btn-sm" @click="handleLogout">登出</button>
        </nav>
      </div>
    </header>

    <!-- 主內容 -->
    <main class="user-content">
      <router-view />
    </main>
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
const customerInitial = computed(() => (customerAuthStore.customer?.name || '會')[0])

const avatarSrc = computed(() => {
  const url = customerAuthStore.customer?.avatarUrl
  if (!url) return null
  return url.startsWith('http') ? url : BASE_URL + url
})

function handleLogout() {
  customerAuthStore.clearCustomer()
  router.push({ name: 'user-login' })
}
</script>

<style scoped>
.user-layout {
  min-height: 100vh;
  background: var(--bg-primary);
}

/* === Header === */
.user-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(245, 241, 234, 0.92);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border);
}

.header-inner {
  max-width: 1080px;
  margin: 0 auto;
  padding: 0 var(--space-6);
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  text-decoration: none;
  color: inherit;
}

.brand-name {
  font-family: var(--font-heading);
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 2px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.avatar-btn {
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  border-radius: 50%;
  transition: transform var(--duration) var(--ease);
}

.avatar-btn:hover { transform: scale(1.06); }

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid var(--border);
  display: block;
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  color: var(--primary);
  font-family: var(--font-heading);
  font-size: var(--text-body);
  font-weight: 600;
}

.welcome-text {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

/* === Content === */
.user-content {
  max-width: 1080px;
  margin: 0 auto;
  padding: var(--space-5) var(--space-6);
}

@media (max-width: 700px) {
  .header-inner { padding: 0 var(--space-3); height: 56px; }
  .user-content { padding: var(--space-4) var(--space-3); }
  .brand-name { display: none; }
}
</style>
