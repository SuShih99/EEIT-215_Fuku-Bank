<template>
  <div class="customer-page user-layout">
    <!-- 頂部導覽列 -->
    <header class="user-header">
      <!-- 上層：Logo + 使用者區 -->
      <div class="header-top">
        <div class="header-top-inner">
          <JbLogo size="sm" clickable @navigate="$router.push({ name: 'user-home' })" />
            <div class="header-user">
              <!-- Countdown and Demo Button -->
              <div class="session-timer">
                <span class="session-timer-text">自動登出倒數: {{ formatTime(countdown) }}</span>
                <button class="session-continue-btn" @click="isTimerPaused = !isTimerPaused">
                  {{ isTimerPaused ? '保持登入' : '暫停計時' }}
                </button>
                <button class="demo-mode-badge" @click="triggerIdleAlert">Demo 模式</button>
              </div>
              
              <button
                class="avatar-btn"
                aria-label="前往會員中心"
                @click="$router.push({ name: 'user-profile' })"
              >
                <img v-if="avatarSrc" :src="avatarSrc" class="user-avatar" alt="使用者大頭照" />
                <span v-else class="user-avatar avatar-placeholder" aria-hidden="true">{{ customerInitial }}</span>
              </button>
              <span class="user-name">{{ customerName }}</span>
              <button class="logout-btn" @click="handleLogout">登出</button>
            </div>
        </div>
      </div>

      <!-- 下層：主導覽選單 -->
      <nav class="mega-nav" aria-label="主導覽">
        <div class="mega-nav-inner">
          <div
            v-for="(menu, idx) in menus"
            :key="idx"
            class="mega-nav-item"
            @mouseenter="handleMouseEnter(idx)"
            @mouseleave="handleMouseLeave"
          >
            <button
              class="mega-nav-trigger"
              :class="{ active: openMenu === idx }"
              :aria-expanded="openMenu === idx"
              @click="toggleMenu(idx)"
            >
              <span class="mega-nav-icon" v-html="menu.svg" aria-hidden="true"></span>
              <span>{{ menu.label }}</span>
              <svg class="mega-nav-arrow" width="10" height="6" viewBox="0 0 10 6" fill="none">
                <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.3" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>

            <!-- 下拉面板 -->
            <transition name="dropdown">
              <div v-if="openMenu === idx" class="mega-dropdown">
                <ul class="dropdown-list">
                  <li v-for="sub in menu.children" :key="sub.label">
                    <a
                      href="#"
                      class="dropdown-link"
                      :class="{ disabled: !sub.route }"
                      @click.prevent="handleSubClick(sub)"
                    >
                      <span class="dropdown-link-text">{{ sub.label }}</span>
                      <span v-if="sub.desc" class="dropdown-link-desc">{{ sub.desc }}</span>
                    </a>
                  </li>
                </ul>
              </div>
            </transition>
          </div>
        </div>
      </nav>
    </header>

    <!-- 主內容 -->
    <main class="user-content">
      <router-view />
    </main>

    <!-- Idle Reminder Modal -->
    <transition name="modal-fade">
      <div v-if="idleModal.visible" class="jb-modal-overlay">
        <div class="jb-modal jb-card">
          <h3 class="jb-modal-title">您還在線嗎？</h3>
          <p class="jb-modal-content">
            系統偵測到您已登入一段時間，請確認是否繼續使用。<br />
            若未於 {{ graceCountdown }} 秒內確認，系統將自動登出。
          </p>
          <div class="jb-modal-actions">
            <button class="jb-btn jb-btn-primary" style="width: 100%" @click="handleStillHere">還在，繼續使用</button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCustomerAuthStore } from '@/stores/customerAuth'
import { BASE_URL } from '@/api/axios'
import JbLogo from '@/components/JbLogo.vue'

const router = useRouter()
const customerAuthStore = useCustomerAuthStore()
const openMenu = ref(-1)
let leaveTimer = null

function handleMouseEnter(idx) {
  clearTimeout(leaveTimer)
  openMenu.value = idx
}

function handleMouseLeave() {
  leaveTimer = setTimeout(() => {
    openMenu.value = -1
  }, 120)
}

function toggleMenu(idx) {
  clearTimeout(leaveTimer)
  openMenu.value = openMenu.value === idx ? -1 : idx
}

const customerName = computed(() => customerAuthStore.customer?.name || '會員')
const customerInitial = computed(() => (customerAuthStore.customer?.name || '會')[0])

const avatarSrc = computed(() => {
  const url = customerAuthStore.customer?.avatarUrl
  if (!url) return null
  return url.startsWith('http') ? url : BASE_URL + url
})

const menus = [
  {
    label: '我的帳戶',
    svg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="M2 10h20"/></svg>',
    route: null,
    children: [
      { label: '查看所有帳戶', desc: '帳戶餘額與明細總覽', route: 'user-accounts' },
      { label: '電子存摺', desc: '帳戶封面與銀行資料', route: 'user-e-passbook' },
      { label: '查看所有交易紀錄', desc: '全帳戶交易歷史', route: 'user-transactions' },
    ],
  },
  {
    label: '轉帳匯款',
    svg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M7 17l9.2-9.2M17 17V7H7"/></svg>',
    route: null,
    children: [
      { label: '國內轉帳', desc: '台幣即時轉帳', route: 'user-transfer' },
      { label: '預約轉帳', desc: '設定未來轉帳', route: 'user-scheduled-transfer' },
      { label: '常用帳號管理', desc: '管理常用收款帳戶', route: 'user-favorite-accounts' },
      { label: '換匯', desc: '外幣兌換服務', route: 'user-exchange' },
    ],
  },
  {
    label: '信用卡',
    svg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="4" width="22" height="16" rx="2"/><line x1="1" y1="10" x2="23" y2="10"/></svg>',
    route: null,
    children: [
      { label: '卡片總覽', desc: '查看持有卡片', route: 'user-card-types' },
      { label: '線上申辦', desc: '申請新信用卡', route: 'user-card-applications' },
      { label: '帳單查詢', desc: '查看信用卡帳單', route: null },
    ],
  },
  {
    label: '貸款服務',
    svg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>',
    route: null,
    children: [
      { label: '信用貸款', desc: '線上申請信貸', route: null },
      { label: '房屋貸款', desc: '房貸方案查詢', route: null },
      { label: '貸款試算', desc: '利率與還款試算', route: null },
    ],
  },
  {
    label: '個人資料',
    svg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>',
    route: 'user-profile',
    children: [
      { label: '基本資料', desc: '查看與修改個人資訊', route: 'user-profile' },
      { label: '通知設定', desc: '管理通知偏好', route: null },
    ],
  },
  {
    label: '安全中心',
    svg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>',
    route: 'user-profile',
    children: [
      { label: '密碼修改', desc: '變更登入密碼', route: 'user-profile' },
      { label: '登入紀錄', desc: '查看近期登入活動', route: 'user-security-login-records' },
      { label: '裝置管理', desc: '管理已授權裝置', route: 'user-security-devices' },
    ],
  },
]

function handleSubClick(sub) {
  if (sub.route) {
    router.push({ name: sub.route })
    openMenu.value = -1
  } else {
    alert('此功能即將推出，敬請期待！')
  }
}

function closeOnOutsideClick(e) {
  if (!e.target.closest('.mega-nav-item')) {
    openMenu.value = -1
  }
}

const idleModal = reactive({
  visible: false
})

const countdown = ref(300) // 5 分鐘 = 300 秒
const graceCountdown = ref(30)
const isTimerPaused = ref(false)
let secondTimer = null
let graceTimer = null

function formatTime(seconds) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m}:${s.toString().padStart(2, '0')}`
}

function triggerIdleAlert() {
  if (idleModal.visible) return
  idleModal.visible = true
  graceCountdown.value = 30
  startGraceTimer()
}

function handleStillHere() {
  idleModal.visible = false
  stopGraceTimer()
  countdown.value = 300 // 重設倒數
}

function startGraceTimer() {
  stopGraceTimer()
  graceTimer = setInterval(() => {
    if (graceCountdown.value > 0) {
      graceCountdown.value--
      return
    }

    stopGraceTimer()
    handleLogout()
  }, 1000)
}

function stopGraceTimer() {
  if (graceTimer) {
    clearInterval(graceTimer)
    graceTimer = null
  }
}

onMounted(() => {
  document.addEventListener('click', closeOnOutsideClick)
  
  // 每 1 秒更新倒數
  secondTimer = setInterval(() => {
    if (idleModal.visible) return
    if (isTimerPaused.value) return // 暫停時不動作
    
    if (countdown.value > 0) {
      countdown.value--
    } else {
      triggerIdleAlert()
    }
  }, 1000)
})

onUnmounted(() => {
  document.removeEventListener('click', closeOnOutsideClick)
  if (secondTimer) clearInterval(secondTimer)
  stopGraceTimer()
})

function handleLogout() {
  stopGraceTimer()
  customerAuthStore.clearCustomer()
  router.push({ name: 'user-login' })
}
</script>

<style scoped>
.user-layout {
  min-height: 100vh;
  background: #f5f1ea url('/java-bank-wabi-bg.webp') center / cover fixed;
}

/* === Header Top (Logo + User) === */
.user-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(245, 241, 234, 0.95);
  backdrop-filter: blur(12px);
}

.header-top {
  border-bottom: 1px solid var(--border);
  padding-bottom: 12px;
}

.header-top-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 var(--space-8);
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-user {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.welcome-text {
  font-size: var(--text-sm);
  color: var(--text-secondary);
}

.user-name {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
}

.session-timer {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background-color: rgba(234, 228, 218, 0.72);
  border: 1px solid rgba(214, 206, 195, 0.9);
  border-radius: 999px;
  color: var(--text-secondary);
  box-shadow: 0 4px 14px rgba(63, 74, 66, 0.05);
}

.session-timer-text {
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
}

.session-continue-btn {
  padding: 4px 14px;
  color: var(--primary-dark);
  background-color: rgba(255, 249, 239, 0.55);
  border: 1px solid rgba(92, 107, 95, 0.28);
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.2s ease;
  cursor: pointer;
}

.session-continue-btn:hover {
  color: var(--bg-primary);
  background-color: var(--primary);
  border-color: var(--primary);
}

.demo-mode-badge {
  padding: 4px 12px;
  color: var(--accent);
  background-color: rgba(166, 90, 77, 0.06);
  border: 1px solid rgba(166, 90, 77, 0.26);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.demo-mode-badge:hover {
  background-color: rgba(166, 90, 77, 0.12);
  border-color: rgba(166, 90, 77, 0.42);
}

.logout-btn {
  padding: 9px 18px;
  color: var(--text-secondary);
  background-color: rgba(255, 249, 239, 0.55);
  border: 1px solid var(--border);
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s ease;
  cursor: pointer;
}

.logout-btn:hover {
  color: var(--accent);
  background-color: rgba(166, 90, 77, 0.06);
  border-color: rgba(166, 90, 77, 0.32);
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
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid var(--border);
  display: block;
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(234, 228, 218, 0.75);
  border: 1px solid var(--border);
  color: var(--primary-dark);
  font-family: var(--font-heading);
  font-size: var(--text-sm);
  font-weight: 600;
  width: 36px;
  height: 36px;
  border-radius: 50%;
}

/* === Mega Nav Bar === */
.mega-nav {
  border-bottom: 1px solid var(--border);
  background: rgba(245, 241, 234, 0.98);
}

.mega-nav-inner {
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 var(--space-8);
  display: flex;
  align-items: stretch;
}

.mega-nav-item {
  position: relative;
  flex: 1;
}

.mega-nav-trigger {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  width: 100%;
  padding: 18px var(--space-3);
  font-size: 16px;
  font-family: var(--font-body);
  font-weight: 500;
  color: var(--text-primary);
  background: none;
  border: none;
  cursor: pointer;
  white-space: nowrap;
  transition: color var(--duration) var(--ease),
              background var(--duration) var(--ease);
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}

.mega-nav-trigger:hover,
.mega-nav-trigger.active {
  color: var(--primary-dark);
  background: rgba(92, 107, 95, 0.06);
  border-radius: 8px;
  border-bottom-color: transparent;
}

.mega-nav-icon {
  display: flex;
  align-items: center;
  color: inherit;
  opacity: 0.7;
}

.mega-nav-trigger:hover .mega-nav-icon,
.mega-nav-trigger.active .mega-nav-icon {
  opacity: 1;
}

.mega-nav-arrow {
  margin-left: 2px;
  transition: transform var(--duration) var(--ease);
}

.mega-nav-trigger.active .mega-nav-arrow {
  transform: rotate(180deg);
}

/* === Dropdown Panel === */
.mega-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  min-width: 240px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 0 0 var(--radius-md) var(--radius-md);
  box-shadow: var(--shadow-lg);
  z-index: 200;
  padding: var(--space-2) 0;
}

.dropdown-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.dropdown-link {
  display: flex;
  flex-direction: column;
  padding: var(--space-3) var(--space-4);
  text-decoration: none;
  color: var(--text-primary);
  transition: background var(--duration) var(--ease);
}

.dropdown-link:hover {
  background: var(--primary-light);
}

.dropdown-link.disabled {
  cursor: default;
}

.dropdown-link.disabled:hover {
  background: rgba(92, 107, 95, 0.04);
}

.dropdown-link-text {
  font-size: var(--text-body);
  font-weight: 500;
  color: var(--text-primary);
  line-height: 1.4;
}

.dropdown-link-desc {
  font-size: var(--text-xs);
  color: var(--text-secondary);
  margin-top: 2px;
  line-height: 1.4;
}

.dropdown-link:hover .dropdown-link-text {
  color: var(--primary);
}

/* === Dropdown Transition === */
.dropdown-enter-active {
  transition: opacity 0.18s var(--ease), transform 0.18s var(--ease);
}
.dropdown-leave-active {
  transition: opacity 0.12s var(--ease), transform 0.12s var(--ease);
}
.dropdown-enter-from {
  opacity: 0;
  transform: translateY(-4px);
}
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* === Content === */
.user-content {
  max-width: 1400px;
  margin: 0 auto;
  padding: var(--space-6) var(--space-8);
}

/* === Mobile === */
@media (max-width: 900px) {
  .mega-nav-inner {
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
    padding: 0 var(--space-3);
  }
  .mega-nav-inner::-webkit-scrollbar { display: none; }

  .mega-nav-trigger {
    padding: var(--space-2) var(--space-3);
    font-size: var(--text-xs);
  }

  .mega-nav-icon { display: none; }
}

@media (max-width: 700px) {
  .header-top-inner {
    padding: 0 var(--space-3);
    height: 64px;
  }
  .user-content {
    padding: var(--space-4) var(--space-3);
  }
  .mega-dropdown {
    position: fixed;
    left: var(--space-3);
    right: var(--space-3);
    min-width: unset;
    border-radius: var(--radius-md);
  }
}

/* === Modal Styles (Consistent with Profile) === */
.jb-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(43, 43, 43, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}
.jb-modal {
  width: 90%;
  max-width: 400px;
  padding: var(--space-6);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-xl);
  text-align: center;
}
.jb-modal-title {
  font-family: var(--font-heading);
  font-size: 20px;
  color: var(--text-primary);
  margin: 0;
}
.jb-modal-content {
  font-size: 16px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0;
}
.jb-modal-actions {
  margin-top: var(--space-2);
  display: flex;
  gap: var(--space-3);
  justify-content: center;
}
.modal-fade-enter-active, .modal-fade-leave-active { transition: all 0.4s var(--ease); }
.modal-fade-enter-from, .modal-fade-leave-to { opacity: 0; transform: scale(0.95); }
</style>
