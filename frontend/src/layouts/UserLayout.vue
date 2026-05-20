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
                <span class="session-timer-text">自動登出倒數：{{ formatTime(countdown) }}</span>
                <button class="session-continue-btn" :class="{ paused: isTimerPaused }" @click="isTimerPaused = !isTimerPaused">
                  保持登入
                </button>
                <button class="demo-mode-badge" @click="triggerIdleAlert">Demo 模式</button>
              </div>

              <div class="notification-bell-wrap" ref="notificationWrapRef">
                <button
                  class="notification-bell-btn"
                  :class="{ active: isNotificationOpen }"
                  aria-label="通知中心"
                  @click.stop="toggleNotificationDropdown"
                >
                  <svg width="19" height="19" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                    <path d="M15 17H9m8-4v-3a5 5 0 10-10 0v3l-2 2v1h14v-1l-2-2zm-4 5a2 2 0 01-4 0" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                  <span v-if="unreadCount > 0" class="notification-badge">{{ badgeText }}</span>
                </button>

                <transition name="dropdown">
                  <div v-if="isNotificationOpen" class="notification-dropdown" @click.stop>
                    <div class="notification-dropdown-header">
                      <div>
                        <p class="notification-dropdown-eyebrow">最新通知</p>
                        <h3>通知中心</h3>
                      </div>
                      <button
                        class="notification-mark-all-btn"
                        type="button"
                        :disabled="unreadCount === 0 || isNotificationLoading"
                        @click="handleMarkAllAsRead"
                      >
                        全部標記為已讀
                      </button>
                    </div>

                    <div v-if="isNotificationLoading" class="notification-empty">通知載入中...</div>
                    <div v-else-if="notificationError" class="notification-empty notification-empty-error">
                      通知載入失敗，請稍後再試。
                    </div>
                    <div v-else-if="notifications.length === 0" class="notification-empty">
                      <strong>目前沒有通知</strong>
                      <span>新的帳戶、交易與系統提醒會顯示在這裡。</span>
                    </div>
                    <div v-else class="notification-list">
                      <button
                        v-for="item in notifications"
                        :key="item.id"
                        class="notification-item"
                        :class="{ unread: !item.isRead }"
                        @click="handleNotificationClick(item)"
                      >
                        <div class="notification-item-dot" :class="{ unread: !item.isRead }"></div>
                        <div class="notification-item-body">
                          <div class="notification-item-top">
                            <strong>{{ item.title }}</strong>
                            <span>{{ formatNotificationTime(item.createdAt) }}</span>
                          </div>
                          <p>{{ item.message }}</p>
                        </div>
                      </button>
                    </div>

                    <div class="notification-dropdown-footer">
                      <button class="notification-settings-link" type="button" @click="goNotificationSettings">
                        通知設定
                      </button>
                    </div>
                  </div>
                </transition>
              </div>

              <button
                class="avatar-btn"
                aria-label="前往會員中心"
                @click="$router.push({ name: 'user-profile' })"
              >
                <img v-if="avatarSrc" :src="avatarSrc" class="user-avatar" alt="使用者大頭照" />
                <img v-else src="/default_photo.png" class="user-avatar" alt="預設大頭照" />
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
              :ref="(el) => setMenuTriggerRef(el, idx)"
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
              <div
                v-if="openMenu === idx"
                class="mega-dropdown"
                :style="dropdownStyle"
              >
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
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useCustomerAuthStore } from '@/stores/customerAuth'
import { BASE_URL } from '@/api/axios'
import {
  getCustomerNotifications,
  getCustomerUnreadNotificationCount,
  markAllCustomerNotificationsAsRead,
  markCustomerNotificationAsRead,
} from '@/api/notification'
import JbLogo from '@/components/JbLogo.vue'
import dayjs from 'dayjs'

const router = useRouter()
const customerAuthStore = useCustomerAuthStore()
const openMenu = ref(-1)
const menuTriggerRefs = ref([])
const dropdownStyle = ref({})
const isNotificationOpen = ref(false)
const isNotificationLoading = ref(false)
const notificationError = ref(false)
const notifications = ref([])
const unreadCount = ref(0)
const notificationWrapRef = ref(null)
let leaveTimer = null
let compactNavMedia = null
const isCompactNav = ref(false)

function setMenuTriggerRef(el, idx) {
  menuTriggerRefs.value[idx] = el || null
}

function updateCompactNavState() {
  isCompactNav.value = window.innerWidth <= 900
}

function updateDropdownPosition(idx = openMenu.value) {
  if (!isCompactNav.value || idx < 0) {
    dropdownStyle.value = {}
    return
  }

  const trigger = menuTriggerRefs.value[idx]
  if (!trigger) return

  const rect = trigger.getBoundingClientRect()
  const viewportWidth = window.innerWidth
  const dropdownWidth = Math.min(240, viewportWidth - 24)
  const minLeft = 12
  const maxLeft = Math.max(minLeft, viewportWidth - dropdownWidth - 12)
  const left = Math.min(Math.max(rect.left, minLeft), maxLeft)

  dropdownStyle.value = {
    top: `${rect.bottom + 8}px`,
    left: `${left}px`,
    right: 'auto',
    width: `${dropdownWidth}px`,
    minWidth: `${Math.min(200, dropdownWidth)}px`,
  }
}

function handleMouseEnter(idx) {
  clearTimeout(leaveTimer)
  openMenu.value = idx
  nextTick(() => updateDropdownPosition(idx))
}

function handleMouseLeave() {
  leaveTimer = setTimeout(() => {
    openMenu.value = -1
    dropdownStyle.value = {}
  }, 120)
}

function toggleMenu(idx) {
  clearTimeout(leaveTimer)
  openMenu.value = openMenu.value === idx ? -1 : idx
  if (openMenu.value === -1) {
    dropdownStyle.value = {}
    return
  }
  nextTick(() => updateDropdownPosition(idx))
}

const customerName = computed(() => customerAuthStore.customer?.name || '會員')
const badgeText = computed(() => (unreadCount.value > 99 ? '99+' : String(unreadCount.value)))

const avatarSrc = computed(() => {
  const url = customerAuthStore.customer?.avatarUrl
  if (!url) return null
  if (url.startsWith('http')) return url
  if (url.startsWith('/uploads/')) return BASE_URL + url
  return url
})

const menus = [
  {
    label: '我的帳戶',
    svg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="M2 10h20"/></svg>',
    route: null,
    children: [
      { label: '查看所有帳戶', desc: '帳戶餘額與明細總覽', route: 'user-accounts' },
      { label: '開戶申請', desc: '申請外幣、子帳戶與其他帳戶類型', route: 'user-account-application' },
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
      { label: '卡片總覽', desc: '查看所有信用卡', route: 'user-card-types' },
      { label: '線上申辦', desc: '申請新信用卡', route: 'user-card-applications' },
      {label:'卡片管理', desc:'查看持有信用卡', route:'user-cards'},
      {label:'交易管理', desc:'查看與刷卡交易', route:'user-card-txns'},
      { label: '帳單查詢', desc: '查看信用卡帳單', route: 'user-card-bills' },
    ],
  },
  {
    label: '貸款服務',
    svg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M12 6v6l4 2"/></svg>',
    route: null,
    children: [
      { label: '快速申請貸款', desc: '線上貸款申請，快速撥款', route: 'user-loan-apply' },
      { label: '查詢申貸狀態', desc: '查看所有貸款申請進度', route: 'user-loan-status' },
      { label: '貸款帳戶', desc: '查看核准中的貸款帳戶', route: 'user-loan-accounts' },
    ],
  },
  {
    label: '個人設定',
    svg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>',
    route: 'user-profile',
    children: [
      { label: '基本資料', desc: '查看與修改個人資訊', route: 'user-profile' },
      { label: '通知設定', desc: '設定站內通知類型', route: 'user-notification-settings' },
    ],
  },
  {
    label: '安全中心',
    svg: '<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>',
    route: 'user-profile',
    children: [
      { label: '密碼與帳號修改', desc: '變更登入密碼與使用者帳號', route: 'user-security-password' },
      { label: '登入紀錄', desc: '查看近期登入活動', route: 'user-security-login-records' },
      { label: '裝置管理', desc: '管理已授權裝置', route: 'user-security-devices' },
    ],
  },
]

function handleSubClick(sub) {
  if (sub.action === 'notifications') {
    isNotificationOpen.value = true
    if (notifications.value.length === 0 && !isNotificationLoading.value) {
      loadNotifications()
    }
    openMenu.value = -1
    return
  }

  if (sub.route) {
    router.push({ name: sub.route })
    openMenu.value = -1
  } else {
    alert('此功能即將推出，敬請期待！')
  }
}

function toggleNotificationDropdown() {
  isNotificationOpen.value = !isNotificationOpen.value
  if (isNotificationOpen.value && notifications.value.length === 0 && !isNotificationLoading.value) {
    loadNotifications()
  }
}

async function loadNotifications() {
  isNotificationLoading.value = true
  notificationError.value = false

  try {
    const [listRes, countRes] = await Promise.all([
      getCustomerNotifications(),
      getCustomerUnreadNotificationCount(),
    ])

    notifications.value = listRes.data?.data || []
    unreadCount.value = countRes.data?.data?.count ?? 0
  } catch (error) {
    console.error('載入通知失敗', error)
    notificationError.value = true
    notifications.value = []
  } finally {
    isNotificationLoading.value = false
  }
}

function formatNotificationTime(value) {
  if (!value) return ''
  return dayjs(value).format('YYYY/MM/DD HH:mm')
}

async function handleNotificationClick(item) {
  try {
    if (!item.isRead) {
      await markCustomerNotificationAsRead(item.id)
      item.isRead = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }

    isNotificationOpen.value = false

    if (item.linkUrl) {
      if (/^https?:\/\//i.test(item.linkUrl)) {
        window.location.href = item.linkUrl
      } else {
        router.push(item.linkUrl)
      }
    }
  } catch (error) {
    console.error('標記通知已讀失敗', error)
  }
}

async function handleMarkAllAsRead() {
  if (unreadCount.value === 0 || isNotificationLoading.value) return

  try {
    await markAllCustomerNotificationsAsRead()
    notifications.value = notifications.value.map((item) => ({ ...item, isRead: true }))
    unreadCount.value = 0
  } catch (error) {
    console.error('全部標記已讀失敗', error)
  }
}

function goNotificationSettings() {
  isNotificationOpen.value = false
  router.push({ name: 'user-notification-settings' })
}

function closeOnOutsideClick(e) {
  if (!e.target.closest('.mega-nav-item')) {
    openMenu.value = -1
    dropdownStyle.value = {}
  }
  if (!e.target.closest('.notification-bell-wrap')) {
    isNotificationOpen.value = false
  }
}

function handleViewportChange() {
  updateCompactNavState()
  if (openMenu.value !== -1) {
    nextTick(() => updateDropdownPosition())
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
  updateCompactNavState()
  compactNavMedia = window.matchMedia('(max-width: 900px)')
  document.addEventListener('click', closeOnOutsideClick)
  window.addEventListener('resize', handleViewportChange)
  window.addEventListener('scroll', handleViewportChange, true)
  compactNavMedia.addEventListener('change', handleViewportChange)
  loadNotifications()

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
  window.removeEventListener('resize', handleViewportChange)
  window.removeEventListener('scroll', handleViewportChange, true)
  compactNavMedia?.removeEventListener('change', handleViewportChange)
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
  width: 100%;
  max-width: 100%;
  overflow-x: hidden;
}

/* === Header Top (Logo + User) === */
.user-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(245, 241, 234, 0.95);
  backdrop-filter: blur(12px);
  width: 100%;
  max-width: 100%;
}

.header-top {
  border-bottom: 1px solid var(--border);
  padding-bottom: 12px;
}

.header-top-inner {
  width: 100%;
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
  min-width: 0;
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

.notification-bell-wrap {
  position: relative;
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.notification-bell-btn {
  position: relative;
  width: 38px;
  height: 38px;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: rgba(255, 249, 239, 0.82);
  color: var(--text-primary);
  display: grid;
  place-items: center;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease, background-color 0.18s ease;
}

.notification-bell-btn:hover,
.notification-bell-btn.active {
  border-color: rgba(92, 107, 95, 0.34);
  box-shadow: 0 8px 18px rgba(63, 74, 66, 0.08);
  transform: translateY(-1px);
  background: rgba(255, 249, 239, 0.96);
}

.notification-bell-btn svg {
  display: block;
}

.notification-badge {
  position: absolute;
  top: -3px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 999px;
  background-color: var(--accent);
  color: #fffaf2;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
  box-shadow: 0 3px 8px rgba(118, 56, 53, 0.14);
}

.notification-dropdown {
  position: absolute;
  top: calc(100% + 12px);
  right: 0;
  width: min(348px, calc(100vw - 24px));
  padding: 16px;
  border-radius: 18px;
  border: 1px solid var(--border);
  background: rgba(255, 249, 239, 0.96);
  box-shadow: 0 14px 32px rgba(63, 74, 66, 0.1);
  backdrop-filter: blur(10px);
  z-index: 120;
}

.notification-dropdown-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.notification-dropdown-eyebrow {
  margin: 0 0 4px;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--text-secondary);
  text-transform: uppercase;
}

.notification-dropdown-header h3 {
  margin: 0;
  font-size: 18px;
  color: var(--text-primary);
}

.notification-mark-all-btn {
  border: 1px solid rgba(214, 206, 195, 0.88);
  border-radius: 999px;
  background: rgba(255, 249, 239, 0.74);
  color: var(--text-primary);
  font-size: 12px;
  padding: 6px 11px;
  cursor: pointer;
  transition: background-color 0.18s ease, border-color 0.18s ease, opacity 0.18s ease;
}

.notification-mark-all-btn:hover:not(:disabled) {
  border-color: rgba(92, 107, 95, 0.26);
  background: rgba(92, 107, 95, 0.05);
}

.notification-mark-all-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.notification-list {
  display: grid;
  gap: 10px;
  max-height: min(430px, 60vh);
  overflow: auto;
  padding-right: 2px;
}

.notification-item {
  width: 100%;
  text-align: left;
  border: 1px solid var(--border);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.78);
  padding: 12px;
  display: grid;
  grid-template-columns: 10px minmax(0, 1fr);
  gap: 10px;
  cursor: pointer;
  transition: transform 0.16s ease, box-shadow 0.16s ease, border-color 0.16s ease, background-color 0.16s ease;
}

.notification-item:hover {
  transform: translateY(-1px);
  border-color: rgba(92, 107, 95, 0.24);
  box-shadow: 0 8px 20px rgba(63, 74, 66, 0.06);
}

.notification-item.unread {
  background-color: rgba(92, 107, 95, 0.05);
}

.notification-item-dot {
  width: 10px;
  height: 10px;
  margin-top: 5px;
  border-radius: 999px;
  background: transparent;
}

.notification-item-dot.unread {
  background: rgba(118, 56, 53, 0.68);
}

.notification-item-body {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.notification-item-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

.notification-item-top strong {
  color: var(--text-primary);
  font-size: 14px;
  line-height: 1.45;
}

.notification-item-top span {
  color: var(--text-secondary);
  font-size: 12px;
  white-space: nowrap;
  flex-shrink: 0;
}

.notification-item-body p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.55;
  word-break: break-word;
}

.notification-empty {
  padding: 16px 12px;
  border: 1px dashed rgba(214, 206, 195, 0.88);
  border-radius: 14px;
  color: var(--text-secondary);
  display: grid;
  gap: 4px;
  text-align: center;
}

.notification-empty strong {
  color: var(--text-primary);
  font-size: 14px;
}

.notification-empty span {
  font-size: 13px;
  line-height: 1.55;
}

.notification-empty-error {
  background: rgba(166, 90, 77, 0.04);
  border-color: rgba(166, 90, 77, 0.2);
  color: var(--accent);
}

.notification-dropdown-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(214, 206, 195, 0.72);
  display: flex;
  justify-content: flex-end;
}

.notification-settings-link {
  border: 1px solid rgba(214, 206, 195, 0.82);
  border-radius: 999px;
  background: rgba(255, 249, 239, 0.7);
  color: var(--text-primary);
  padding: 7px 12px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.18s ease, border-color 0.18s ease, color 0.18s ease;
}

.notification-settings-link:hover {
  border-color: rgba(92, 107, 95, 0.28);
  background: rgba(92, 107, 95, 0.05);
  color: var(--primary-dark);
}

.session-timer {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 14px;
  background-color: rgba(255, 249, 239, 0.76);
  border: 1px solid rgba(214, 206, 195, 0.88);
  border-radius: 18px;
  color: var(--text-secondary);
  box-shadow: 0 6px 18px rgba(63, 74, 66, 0.05);
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
  background-color: rgba(255, 249, 239, 0.92);
  border: 1px solid rgba(92, 107, 95, 0.28);
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.2s ease;
  cursor: pointer;
}

.session-continue-btn:hover {
  color: var(--primary);
  background-color: rgba(92, 107, 95, 0.08);
  border-color: rgba(92, 107, 95, 0.42);
}

.session-continue-btn.paused {
  color: var(--accent);
  border-color: rgba(166, 90, 77, 0.24);
  background-color: rgba(255, 249, 239, 0.98);
}

.session-continue-btn.paused:hover {
  color: var(--accent);
  background-color: rgba(166, 90, 77, 0.08);
  border-color: rgba(166, 90, 77, 0.34);
}

.demo-mode-badge {
  padding: 4px 12px;
  color: var(--accent);
  background-color: rgba(166, 90, 77, 0.08);
  border: 1px solid rgba(166, 90, 77, 0.28);
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.demo-mode-badge:hover {
  background-color: rgba(166, 90, 77, 0.12);
  border-color: rgba(166, 90, 77, 0.38);
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
  width: 100%;
  max-width: 100%;
}

.mega-nav-inner {
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 0 var(--space-8);
  display: flex;
  align-items: stretch;
}

.mega-nav-item {
  position: relative;
  flex: 1;
  min-width: 0;
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
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: var(--space-6) var(--space-8);
  overflow-x: hidden;
}

/* === Mobile === */
@media (max-width: 900px) {
  .mega-nav {
    overflow-x: auto;
    overflow-y: visible;
    -webkit-overflow-scrolling: touch;
    scrollbar-width: none;
  }

  .mega-nav::-webkit-scrollbar { display: none; }

  .mega-nav-inner {
    width: max-content;
    min-width: max-content;
    max-width: none;
    flex-wrap: nowrap;
    padding: 0 var(--space-3);
  }

  .mega-nav-item {
    flex: 0 0 auto;
    min-width: auto;
  }

  .mega-nav-trigger {
    width: auto;
    padding: 12px 14px;
    font-size: var(--text-xs);
  }

  .mega-nav-icon { display: none; }

  .mega-dropdown {
    position: fixed;
    width: min(240px, calc(100vw - 24px));
    min-width: 200px;
    border-radius: var(--radius-md);
  }
}

@media (max-width: 768px) {
  .user-header {
    padding: 0;
  }

  .header-top {
    padding: 0 0 10px;
  }

  .header-top-inner {
    min-height: 64px;
    height: auto;
    padding: 8px 16px 0;
    align-items: flex-start;
    gap: 8px 12px;
    flex-wrap: wrap;
  }

  .header-top-inner :deep(.jb-logo-img) {
    max-width: 96px;
    height: auto;
  }

  .header-user {
    flex: 1 1 auto;
    justify-content: flex-end;
    gap: 8px;
    min-width: 0;
    flex-wrap: wrap;
  }

  .session-timer {
    order: 2;
    width: 100%;
    margin: 4px 0 0;
    padding: 8px 10px;
    justify-content: space-between;
    gap: 8px;
    border-radius: 14px;
    flex-wrap: wrap;
  }

  .session-timer-text {
    font-size: 12px;
    white-space: normal;
    line-height: 1.35;
    min-width: 0;
    flex: 1 1 140px;
  }

  .session-continue-btn {
    padding: 6px 10px;
    font-size: 12px;
    flex-shrink: 0;
  }

  .demo-mode-badge {
    display: none;
  }

  .notification-bell-btn {
    width: 36px;
    height: 36px;
  }

  .notification-dropdown {
    right: 0;
    left: auto;
    width: min(348px, calc(100vw - 20px));
  }

  .notification-dropdown-footer {
    justify-content: stretch;
  }

  .notification-settings-link {
    width: 100%;
  }

  .user-name {
    display: none;
  }

  .logout-btn {
    padding: 7px 10px;
    font-size: 12px;
    flex-shrink: 0;
  }

  .user-avatar,
  .avatar-placeholder {
    width: 34px;
    height: 34px;
    flex-shrink: 0;
  }

  .user-content {
    max-width: 100%;
    padding: 16px;
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
